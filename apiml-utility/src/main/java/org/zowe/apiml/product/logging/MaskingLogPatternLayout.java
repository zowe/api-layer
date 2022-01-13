package org.zowe.apiml.product.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class MaskingLogPatternLayout extends PatternLayout {
    private static final String MASK_VALUE = "***";
    private static final Pattern maskPatterns = new MaskPatternBuilder()
        .addJsonValue("password")
        .addJsonValue("newPassword")
        .build();

    @Override
    public String doLayout(ILoggingEvent event) {
        return maskMessage(super.doLayout(event));
    }

    private String maskMessage(String message) {
        StringBuilder sb = new StringBuilder(message);
        Matcher matcher = maskPatterns.matcher(sb);
        while (matcher.find()) {
            IntStream.rangeClosed(1, matcher.groupCount()).forEach(group -> {
                if (matcher.group(group) != null) {
                    sb.replace(matcher.start(group), matcher.end(group), MASK_VALUE);
                }
            });
        }
        return sb.toString();
    }

    private static class MaskPatternBuilder {
        private static final String JSON_VALUE = "\\\".*?\\\"";
        private final List<String> maskPatterns = new ArrayList<>();

        public MaskPatternBuilder add(String prefix, String capture) {
            return add(prefix, capture, "");
        }

        public MaskPatternBuilder add(String prefix, String capture, String postfix) {
            maskPatterns.add(prefix + "(" + capture + ")" + postfix);
            return this;
        }

        public MaskPatternBuilder addJsonValue(String jsonKey, String... keys) {
            // add \" around key and value
            add("\\\"" + jsonKey + "\\\"s*:\\s*", JSON_VALUE);
            for (String k : keys) {
                add(k, JSON_VALUE);
            }
            return this;
        }

        public Pattern build() {
            return Pattern.compile(String.join("|", maskPatterns), Pattern.MULTILINE);
        }
    }
}
