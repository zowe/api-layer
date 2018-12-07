package io.apiml.security.gateway.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class QueryResponse {
    private String username;
    private String domain;
    private long creation;
    private long expiration;
}
