package org.zowe.apiml.zaasclient.passTicket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZaasPassTicketResponse {
    private String token;
    private String userId;
    private String applicationName;
    private String ticket;
}

