package org.zowe.apiml.zaasclient.passTicket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZaasClientTicketRequest {
    private String applicationName;
}
