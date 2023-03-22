/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zowe.apiml.gateway.security.mapping.OIDCExternalMapperAuthException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapperResponse {
    @JsonProperty("userid")
    private String userId;
    @JsonProperty("returnCode")
    private int rc;
    @JsonProperty("safReturnCode")
    private int safRc;
    @JsonProperty("racfReturnCode")
    private int racfRc;
    @JsonProperty("racfReasonCode")
    private int racfRs;

    public String toString() {
        return "User: " + userId + ", rc=" + rc + ", safRc=" + safRc + ", racfRc=" + racfRc + ", racfRs=" + racfRs;
    }

    public void validateOIDCResults() {
        if (rc == 8 || safRc == 8 || racfRc == 8) {
            if(racfRs == 44) {
                throw new OIDCExternalMapperAuthException("The Registry Name length is not valid, or the Registry Name" +
                    " string is all blanks (x'20'), all nulls (x'00'), or a combination of blanks and nulls.", this);
            }
            if(racfRs == 48) {
                throw new OIDCExternalMapperAuthException("There is no distributed identity filter mapping the supplied" +
                    " distributed identity to a SAF user ID, or the IDIDMAP SAF general resource class is not active or not" +
                    " RACLISTed.", this);
            }
        }

        if (rc > 0 || safRc > 0 || racfRc > 0 || racfRs > 0) {
            throw new OIDCExternalMapperAuthException("Failed to map distributed to mainframe identity.", this);
        }
    }

}
