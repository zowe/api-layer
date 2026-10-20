/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.apache.tomcat.websocket;

import jakarta.websocket.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(5)
class BufferedFrameClientTest {
    private final WsSession session = mock(WsSession.class);
    private final AsyncChannelWrapper channel = mock(AsyncChannelWrapper.class);
    private final List<Boolean> received = new ArrayList<>();
    private final ByteArrayOutputStream payload = new ByteArrayOutputStream();
    private ByteBuffer pendingRead;
    private CompletionHandler<Integer, Object> completion;
    private WsFrameClient frame;

    @BeforeEach
    void setup() {
        when(session.getMaxBinaryMessageBufferSize()).thenReturn(65_536);
        when(session.getMaxTextMessageBufferSize()).thenReturn(32_768);
        when(session.getBinaryMessageHandler()).thenReturn((MessageHandler.Partial<ByteBuffer>) (data, last) -> {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            payload.writeBytes(bytes);
            received.add(last);
            frame.suspend();
        });
        doAnswer(call -> {
            pendingRead = call.getArgument(0);
            completion = call.getArgument(2);
            return null;
        }).when(channel).read(any(ByteBuffer.class), isNull(), any(CompletionHandler.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 8192})
    void resumesEveryBufferedFragmentWithoutMoreNetworkTraffic(int fragmentSize) {
        byte[] data = new byte[fragmentSize];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        var input = ByteBuffer.allocate(3 * (fragmentSize + 4));
        appendFrame(input, 2, data);
        appendFrame(input, 0, data);
        appendFrame(input, 128, data);
        input.flip();
        frame = new WsFrameClient(input, channel, session, null);

        frame.startInputProcessing();
        assertThat(received).containsExactly(false);
        frame.resume();
        assertThat(received).containsExactly(false, false);
        frame.resume();
        assertThat(received).containsExactly(false, false, true);
        assertThat(payload.toByteArray()).isEqualTo(ByteBuffer.allocate(3 * fragmentSize)
            .put(data).put(data).put(data).array());
        verifyNoInteractions(channel);

        frame.resume();
        verify(channel).read(any(ByteBuffer.class), isNull(), any(CompletionHandler.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void waitsForMoreBytesWhenTheNextHeaderOrPayloadIsIncomplete(int available) {
        byte[] next = {(byte) 128, 2, 3, 4};
        var input = ByteBuffer.allocate(4 + available);
        input.put(new byte[] {2, 2, 1, 2}).put(next, 0, available).flip();
        frame = new WsFrameClient(input, channel, session, null);
        frame.startInputProcessing();

        frame.resume();
        assertThat(received).containsExactly(false);
        verify(channel).read(any(ByteBuffer.class), isNull(), any(CompletionHandler.class));
        pendingRead.put(next, available, next.length - available);
        completion.completed(next.length - available, null);

        assertThat(received).containsExactly(false, true);
        assertThat(payload.toByteArray()).containsExactly(1, 2, 3, 4);
        // The final callback suspended processing again; no second read is started.
        verifyNoMoreInteractions(channel);
    }

    @Test
    void resumesWhenSuspensionHappensBeforeInputProcessingStarts() {
        frame = new WsFrameClient(ByteBuffer.wrap(new byte[] {(byte) 130, 2, 1, 2}), channel, session, null);
        frame.suspend();
        frame.startInputProcessing();
        assertThat(received).isEmpty();
        verifyNoInteractions(channel);

        frame.resume();
        assertThat(received).containsExactly(true);
        assertThat(payload.toByteArray()).containsExactly(1, 2);
        verifyNoInteractions(channel);
    }

    @Test
    void retriesBufferedInputWhenResumeWinsBeforeTheReaderObservesSuspension() {
        // Force the interleaving where the writer resumes after the parser stops, but
        // before processSocketRead can observe the suspended state.
        frame = new WsFrameClient(ByteBuffer.wrap(new byte[] {2, 2, 1, 2, (byte) 128, 2, 3, 4}),
            channel, session, null) {
            @Override
            protected boolean processAvailableInput() throws IOException {
                boolean needsInput = super.processAvailableInput();
                if (!needsInput) resume();
                return needsInput;
            }
        };
        frame.startInputProcessing();

        assertThat(received).containsExactly(false, true);
        assertThat(payload.toByteArray()).containsExactly(1, 2, 3, 4);
        verify(channel).read(any(ByteBuffer.class), isNull(), any(CompletionHandler.class));
    }

    private static void appendFrame(ByteBuffer buffer, int opcode, byte[] data) {
        buffer.put((byte) opcode);
        if (data.length < 126) {
            buffer.put((byte) data.length);
        } else {
            buffer.put((byte) 126).putShort((short) data.length);
        }
        buffer.put(data);
    }
}
