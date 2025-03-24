package org.zowe.apiml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Application"
        )
    }
)
public class ApimlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApimlApplication.class, args);
    }
}
