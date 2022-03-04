/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.passticket;

import com.ibm.eserver.zos.racf.IRRPassTicket;
import com.ibm.eserver.zos.racf.IRRPassTicketEvaluationException;
import com.ibm.eserver.zos.racf.IRRPassTicketGenerationException;

/**
 * This class allows to get a PassTicket from SAF.
 */
public class PassTicketService {

    private IRRPassTicket irrPassTicket;

    public PassTicketService() {
        this.irrPassTicket = new IRRPassTicket();
    }

    public void evaluate(String userId, String applId, String passTicket) throws IRRPassTicketEvaluationException {
        irrPassTicket.evaluate(userId.toUpperCase(), applId.toUpperCase(), passTicket.toUpperCase());
    }

    public String generate(String userId, String applId) throws IRRPassTicketGenerationException {
        return irrPassTicket.generate(userId.toUpperCase(), applId.toUpperCase());
    }
}
