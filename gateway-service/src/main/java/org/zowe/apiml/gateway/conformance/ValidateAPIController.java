/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

/**
 * Controller offered methods for validating serviceID under conformance criteria, it offer methods to 
 * check the validation of the given serviceID
 */
@Controller
public class ValidateAPIController {

    /**
     * Accept serviceID and return the JSON file with appropirate message to show if it is valid
     * 
     * @param serviceID accepted serviceID to check validation 
     * @return return the JSON file message of whether the serviceID is valid
     */
    @PostMapping(
        value = "/validate",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, String> checkValidate(@RequestBody String serviceID) {
        HashMap<String, String> statusCodeMap = new HashMap<>();
        
        if (checkValidPatternAPI(serviceID) && checkServiceIDLength(serviceID)) {
            statusCodeMap.put("statusCode", "200");
            statusCodeMap.put("message", "The service id is validate under conformance criteria.");
        }
        else {
            statusCodeMap.put("statusCode", "400");
            statusCodeMap.put("message", "The service id is not validate under conformance criteria.");
        }
        
        return statusCodeMap;
    }

    /**
     * Accept serviceID and check if it is longer than 64 characters, return true if it meets the criteria
     * otherwise return false
     * 
     * @param serviceID accept serviceID to check if it is longer than 64 characters
     * @return return boolean variable True if it is shorter or equal to 64 characters
     */
    private boolean checkServiceIDLength(String serviceID) {

        boolean serviceIdIdentify = false;
        if (serviceID.length() <= 64) {
            serviceIdIdentify = true;
        }

        return serviceIdIdentify;
    }

    /**
     * Accept serviceID and check if it contains only lower case characters without symbols, return true if it meets the criteria
     * otehr wise return false
     * 
     * @param serviceID accept serviceID to check 
     * @return return boolean variable True if it only contains lower case characters without symbols
     */
    private boolean checkValidPatternAPI(String serviceID) {
        
        boolean symbolUpperIdentify = false;
        Pattern symbolPattern = Pattern.compile("[^a-z0-9]");
        Matcher findSymbol = symbolPattern.matcher(serviceID);

        if (!findSymbol.matches()) {
            symbolUpperIdentify = true;
        }

        return symbolUpperIdentify;
    }

}
