/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.security.token;

public class CookieConfiguration {
    private final String name;
    private final boolean secure;
    private final String path;
    private final String comment;
    private final Integer maxAge;

    @java.beans.ConstructorProperties({"name", "secure", "path", "comment", "maxAge"})
    CookieConfiguration(String name, boolean secure, String path, String comment, Integer maxAge) {
        this.name = name;
        this.secure = secure;
        this.path = path;
        this.comment = comment;
        this.maxAge = maxAge;
    }

    public static CookieConfigurationBuilder builder() {
        return new CookieConfigurationBuilder();
    }

    public String getName() {
        return this.name;
    }

    public boolean isSecure() {
        return this.secure;
    }

    public String getPath() {
        return this.path;
    }

    public String getComment() {
        return this.comment;
    }

    public Integer getMaxAge() {
        return this.maxAge;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CookieConfiguration)) return false;
        final CookieConfiguration other = (CookieConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        if (this.isSecure() != other.isSecure()) return false;
        final Object this$path = this.getPath();
        final Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final Object this$comment = this.getComment();
        final Object other$comment = other.getComment();
        if (this$comment == null ? other$comment != null : !this$comment.equals(other$comment)) return false;
        final Object this$maxAge = this.getMaxAge();
        final Object other$maxAge = other.getMaxAge();
        if (this$maxAge == null ? other$maxAge != null : !this$maxAge.equals(other$maxAge)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CookieConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        result = result * PRIME + (this.isSecure() ? 79 : 97);
        final Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final Object $comment = this.getComment();
        result = result * PRIME + ($comment == null ? 43 : $comment.hashCode());
        final Object $maxAge = this.getMaxAge();
        result = result * PRIME + ($maxAge == null ? 43 : $maxAge.hashCode());
        return result;
    }

    public String toString() {
        return "CookieConfiguration(name=" + this.getName() + ", secure=" + this.isSecure() + ", path=" + this.getPath() + ", comment=" + this.getComment() + ", maxAge=" + this.getMaxAge() + ")";
    }

    public CookieConfigurationBuilder toBuilder() {
        return new CookieConfigurationBuilder().name(this.name).secure(this.secure).path(this.path).comment(this.comment).maxAge(this.maxAge);
    }

    public static class CookieConfigurationBuilder {
        private String name;
        private boolean secure;
        private String path;
        private String comment;
        private Integer maxAge;

        CookieConfigurationBuilder() {
        }

        public CookieConfiguration.CookieConfigurationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CookieConfiguration.CookieConfigurationBuilder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public CookieConfiguration.CookieConfigurationBuilder path(String path) {
            this.path = path;
            return this;
        }

        public CookieConfiguration.CookieConfigurationBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public CookieConfiguration.CookieConfigurationBuilder maxAge(Integer maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public CookieConfiguration build() {
            return new CookieConfiguration(name, secure, path, comment, maxAge);
        }

        public String toString() {
            return "CookieConfiguration.CookieConfigurationBuilder(name=" + this.name + ", secure=" + this.secure + ", path=" + this.path + ", comment=" + this.comment + ", maxAge=" + this.maxAge + ")";
        }
    }
}
