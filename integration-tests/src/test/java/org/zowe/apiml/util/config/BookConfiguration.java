package org.zowe.apiml.util.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookConfiguration {
    private String id;
    private String name;
    private int pageCount;
    private String authorId;
}
