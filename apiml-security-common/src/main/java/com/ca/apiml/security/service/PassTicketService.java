/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.service;

import com.ca.mfaas.util.ClassOrDefaultProxyUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * This method allow to get a passticket from RAC.
 */
@Service
public class PassTicketService {

    private IRRPassTicket irrPassTicket;

    @PostConstruct
    public void init() {
        this.irrPassTicket = ClassOrDefaultProxyUtils.createProxy(
            IRRPassTicket.class,
            "com.ibm.eserver.zos.racf.IRRPassTicket",
            () -> new IRRPassTicket() {
                @Override
                public void evaluate(String userId, String applId, String passTicket) {
                    if (userId == null) throw new IllegalArgumentException("Parameter userId is empty");
                    if (applId == null) throw new IllegalArgumentException("Parameter applId is empty");
                    if (passTicket == null) throw new IllegalArgumentException("Parameter passTicket is empty");

                    throw new IllegalStateException("This implementation only for testing purpose");
                }

                @Override
                public String generate(String userId, String applId) {
                    return null;
                }
            }
        );
    }

    public void evaluate(String userId, String applId, String passTicket) {
        irrPassTicket.evaluate(userId, applId, passTicket);
    }

    public String generate(String userId, String applId) {
        return irrPassTicket.generate(userId, applId);
    }

    public boolean isUsingRacImplementation() {
        ClassOrDefaultProxyUtils.ClassOrDefaultProxyState stateInterface = (ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) irrPassTicket;
        return stateInterface.isUsingBaseImplementation();
    }

}
