/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.model;

import lombok.NonNull;

public class SemanticVersion implements Comparable<SemanticVersion> {
    @NonNull
    private final int[] numbers;

    public SemanticVersion(@NonNull String version) {
        final String[] split = version.split("-")[0].split("\\.");
        numbers = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            numbers[i] = Integer.valueOf(split[i]);
        }
    }

    @Override
    public int compareTo(@NonNull SemanticVersion another) {
        final int maxLength = Math.max(numbers.length, another.numbers.length);
        for (int i = 0; i < maxLength; i++) {
            final int left = i < numbers.length ? numbers[i] : 0;
            final int right = i < another.numbers.length ? another.numbers[i] : 0;
            if (left != right) {
                return left < right ? -1 : 1;
            }
        }
        return 0;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.apicatalog.model.SemanticVersion)) return false;
        final com.ca.mfaas.apicatalog.model.SemanticVersion other = (com.ca.mfaas.apicatalog.model.SemanticVersion) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!java.util.Arrays.equals(this.numbers, other.numbers)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.apicatalog.model.SemanticVersion;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.util.Arrays.hashCode(this.numbers);
        return result;
    }
}
