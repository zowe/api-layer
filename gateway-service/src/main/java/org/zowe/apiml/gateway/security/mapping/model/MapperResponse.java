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
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapperResponse {

    private static final String OIDC_FAILED_MESSAGE_KEY = "org.zowe.apiml.security.common.OIDCMappingFailed";

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

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public String toString() {
        return "User: " + userId + ", rc=" + rc + ", safRc=" + safRc + ", racfRc=" + racfRc + ", racfRs=" + racfRs;
    }

    public boolean isOIDCResultValid() {
        // Some codes may be 4 and the result is still valid. But deny unless we know it for sure
        // https://www.ibm.com/docs/en/zos/2.5.0?topic=user-return-reason-codes
        if (rc == 0 || safRc == 0 || racfRc == 0 || racfRs == 0) {
            return true;
        }

        if (rc == 8 && safRc == 8 && racfRc == 8) {
            switch (racfRs) {
                case 20:
                    apimlLog.log(OIDC_FAILED_MESSAGE_KEY,
                        "Not authorized to use this service. Make sure that the Zowe user has READ access to" +
                            " the IRR.IDIDMAP.QUERY resource in the FACILITY class.");
                    return false;
                case 44:
                    apimlLog.log(OIDC_FAILED_MESSAGE_KEY,
                        "The Registry Name or supplied distributed identity is all blanks (x'20'), all nulls" +
                            " (x'00'), or a combination of blanks and nulls.");
                    return false;
                case 48:
                    apimlLog.log(OIDC_FAILED_MESSAGE_KEY,
                        "There is no distributed identity filter mapping of the supplied distributed identity to" +
                            " a SAF user ID, or the IDIDMAP SAF general resource class is not active or not RACLISTed.");
                    return false;
                default:
            }
        }

        apimlLog.log(OIDC_FAILED_MESSAGE_KEY, "SAF response: " + this + " Review the return codes in the IBM" +
            " documentation for R_usermap (IRRSIM00) callable service.");
        return false;
    }

}
