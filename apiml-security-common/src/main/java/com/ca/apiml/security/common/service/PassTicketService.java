/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.service;

import com.ca.mfaas.util.ClassOrDefaultProxyUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * This method allow to get a passTicket from RAC.
 */
@Service
public class PassTicketService {

    private IRRPassTicket irrPassTicket;

    @PostConstruct
    public void init() {
        this.irrPassTicket = ClassOrDefaultProxyUtils.createProxy(
            IRRPassTicket.class,
            "com.ibm.eserver.zos.racf.IRRPassTicket",
            DefaultPassTicketImpl::new,
            new ClassOrDefaultProxyUtils.ByMethodName<IRRPassTicketEvaluationException>(
                "com.ibm.eserver.zos.racf.IRRPassTicketEvaluationException",
                IRRPassTicketEvaluationException.class,
                "getSafRc", "getRacfRsn", "getRacfRc"
            ),
            new ClassOrDefaultProxyUtils.ByMethodName<IRRPassTicketGenerationException>(
                "com.ibm.eserver.zos.racf.IRRPassTicketGenerationException",
                IRRPassTicketGenerationException.class,
                "getSafRc", "getRacfRsn", "getRacfRc"
            )
        );
    }

    public void evaluate(String userId, String applId, String passTicket) throws IRRPassTicketEvaluationException {
        irrPassTicket.evaluate(userId, applId, passTicket);
    }

    public String generate(String userId, String applId) throws IRRPassTicketGenerationException {
        return irrPassTicket.generate(userId, applId);
    }

    public boolean isUsingSafImplementation() {
        ClassOrDefaultProxyUtils.ClassOrDefaultProxyState stateInterface = (ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) irrPassTicket;
        return stateInterface.isUsingBaseImplementation();
    }

    public static class DefaultPassTicketImpl implements IRRPassTicket {
        public static final String ZOWE_DUMMY_PASSTICKET = "ZoweDummyPassTicket";

        @Override
        public void evaluate(String userId, String applId, String passTicket) {
            if (userId == null) throw new IllegalArgumentException("Parameter userId is empty");
            if (applId == null) throw new IllegalArgumentException("Parameter applId is empty");
            if (passTicket == null) throw new IllegalArgumentException("Parameter passTicket is empty");
            if (!passTicket.equals(ZOWE_DUMMY_PASSTICKET)) {
                throw new IllegalArgumentException("Invalid PassTicket");
            }
        }

        @Override
        public String generate(String userId, String applId) {
            return ZOWE_DUMMY_PASSTICKET;
        }

    }

}
