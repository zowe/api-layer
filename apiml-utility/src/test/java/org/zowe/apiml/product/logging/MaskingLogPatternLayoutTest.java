package org.zowe.apiml.product.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaskingLogPatternLayoutTest {
    @Nested
    class WhenBuildMask {
        private MaskingLogPatternLayout.MaskPatternBuilder underTest;

        @BeforeEach
        void setup() {
            underTest = new MaskingLogPatternLayout.MaskPatternBuilder();
        }

        @Test
        void givenMultiplePatterns_thenReturnMaskRegexForBoth() {
            String expectedRegex = "one(cap1)|two(cap2)post";
            underTest.add("one", "cap1").add("two", "cap2", "post");

            assertEquals(expectedRegex, underTest.build().pattern());
        }

        @Test
        void givenJsons_thenMaskRegexWithJsonFormat() {
            String expectedRegex = "\\\"key1\\\"\\s*:\\s*(\\\".*?\\\")|\\\"key2\\\"\\s*:\\s*(\\\".*?\\\")";
            underTest.addJsonValue("key1", "key2");

            assertEquals(expectedRegex, underTest.build().pattern());
        }
    }

    @Nested
    class WhenMaskMessage {
        private MaskingLogPatternLayout underTest;

        @BeforeEach
        void setup() {
            underTest = new MaskingLogPatternLayout();
        }

        @Test
        void givenNothingToMask_thenNoMask() {
            String message = "expected log";
            String actual = underTest.maskMessage(message);
            assertEquals(message, actual);
        }

        @Test
        void givenSensitiveData_thenMaskData() {
            String actual = underTest.maskMessage("\"password\":\"mypassword\"");
            assertEquals("\"password\":***", actual);
        }
    }
}
