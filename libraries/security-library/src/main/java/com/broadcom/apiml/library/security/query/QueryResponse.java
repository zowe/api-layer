/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.security.query;

import java.util.Date;

public class QueryResponse {
    private String domain;
    private String userId;
    private Date creation;
    private Date expiration;

    @java.beans.ConstructorProperties({"domain", "userId", "creation", "expiration"})
    public QueryResponse(String domain, String userId, Date creation, Date expiration) {
        this.domain = domain;
        this.userId = userId;
        this.creation = creation;
        this.expiration = expiration;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCreation() {
        return this.creation;
    }

    public void setCreation(Date creation) {
        this.creation = creation;
    }

    public Date getExpiration() {
        return this.expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof QueryResponse)) return false;
        final QueryResponse other = (QueryResponse) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$domain = this.getDomain();
        final Object other$domain = other.getDomain();
        if (this$domain == null ? other$domain != null : !this$domain.equals(other$domain)) return false;
        final Object this$userId = this.getUserId();
        final Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final Object this$creation = this.getCreation();
        final Object other$creation = other.getCreation();
        if (this$creation == null ? other$creation != null : !this$creation.equals(other$creation)) return false;
        final Object this$expiration = this.getExpiration();
        final Object other$expiration = other.getExpiration();
        if (this$expiration == null ? other$expiration != null : !this$expiration.equals(other$expiration))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof QueryResponse;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $domain = this.getDomain();
        result = result * PRIME + ($domain == null ? 43 : $domain.hashCode());
        final Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final Object $creation = this.getCreation();
        result = result * PRIME + ($creation == null ? 43 : $creation.hashCode());
        final Object $expiration = this.getExpiration();
        result = result * PRIME + ($expiration == null ? 43 : $expiration.hashCode());
        return result;
    }

    public String toString() {
        return "QueryResponse(domain=" + this.getDomain() + ", userId=" + this.getUserId() + ", creation=" + this.getCreation() + ", expiration=" + this.getExpiration() + ")";
    }
}
