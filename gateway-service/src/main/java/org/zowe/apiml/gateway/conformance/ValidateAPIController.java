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
import java.util.function.Predicate;
import java.util.regex.Matcher;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.zowe.apiml.message.core.MessageService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;


/**
 * Controller offered methods for validating serviceID under conformance criteria, it offer methods to 
 * check the validation of the given serviceID
 */
@Controller
@RequiredArgsConstructor
public class ValidateAPIController {

    private static final Pattern symbolPattern = Pattern.compile("[^a-z0-9]");
    private static final Predicate<String> isTooLong = serviceId -> (serviceId).length() <= 64;
    private final MessageService messageService;

    /**
     * Accept serviceID and return the JSON file with appropirate message to show if it is valid
     * 
     * @param serviceID accepted serviceID to check validation 
     * @return return the JSON file message of whether the serviceID is valid
     */
    @PostMapping(
        value = "/validate",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> checkValidate(@RequestBody String serviceID) {
        
        if (!isTooLong.test(serviceID)) {
            String invalidLength = "The serviceid is longer than 64 characters";
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.verifier.wrongServiceId", invalidLength).mapToApiMessage(), HttpStatus.BAD_REQUEST);
        }
        else if (checkValidPatternAPI(serviceID)) {
            String invalidPattern = "The serviceid contains symbols or upper case letters";
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.verifier.wrongServiceId", invalidPattern).mapToApiMessage(), HttpStatus.BAD_REQUEST);
        }
      
        return new ResponseEntity<>(HttpStatus.OK);
        
    }
    
    /**
     * Accept serviceID and check if it contains only lower case characters without symbols, return true if it meets the criteria
     * otherwise return false
     * 
     * @param serviceID accept serviceID to check 
     * @return return boolean variable False if it only contains lower case characters without symbols
     */
    private boolean checkValidPatternAPI(String serviceID) {
        
        Matcher findSymbol = symbolPattern.matcher(serviceID);

        return findSymbol.find();
    }


}
