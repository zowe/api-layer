package org.zowe.apiml.gateway.security.mapping;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperResponseTest {

    private static final String USER = "ZOSUSER";
    private static final int RC = 1;
    private static final int SAFRC = 2;
    private static final int RACFRC = 3;
    private static final int RACFREASON = 4;

    @Test
    void testMapperResponseToString() {
        MapperResponse response = new MapperResponse(USER, RC, SAFRC, RACFRC, RACFREASON);
        String expected = "User: ZOSUSER, rc=1, safRc=2, racfRc=3, racfRs=4";
        assertEquals(expected, response.toString());
    }
}
