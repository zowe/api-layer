package org.zowe.apiml.util.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorConfiguration {

    private String id;
    private String firstName;
    private String lastName;
}
