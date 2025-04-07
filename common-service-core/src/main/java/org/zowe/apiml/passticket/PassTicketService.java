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

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import org.zowe.apiml.util.ObjectUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class allows to get a PassTicket from SAF.
 */
@Slf4j
public class PassTicketService {

    private final IRRPassTicket irrPassTicket;

    public PassTicketService() {
        this.irrPassTicket = ClassOrDefaultProxyUtils.createProxy(IRRPassTicket.class,
            "com.ibm.eserver.zos.racf.IRRPassTicket", DefaultPassTicketImpl::new,
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.eserver.zos.racf.IRRPassTicketEvaluationException",
                IRRPassTicketEvaluationException.class, "getSafRc", "getRacfRc", "getRacfRsn"),
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.eserver.zos.racf.IRRPassTicketGenerationException",
                IRRPassTicketGenerationException.class, "getSafRc", "getRacfRc", "getRacfRsn"));
    }

    // IRRPassTicket is not thread-safe, must be synchronized
    public synchronized void evaluate(String userId, String applId, String passTicket) throws PassTicketException {
        validateUserIdAndApplId(userId, applId);
        irrPassTicket.evaluate(userId.toUpperCase(), applId.toUpperCase(), passTicket.toUpperCase());
    }

    // IRRPassTicket is not thread-safe, must be synchronized
    public synchronized String generate(String userId, String applId) throws PassTicketException {
        try {
            validateUserIdAndApplId(userId, applId);
            return irrPassTicket.generate(userId.toUpperCase(), applId.toUpperCase());
        } catch (PassTicketException | RuntimeException e) {
            log.debug("Error during pass ticket generation, userId={}, applid={}, exception={}", userId, applId, e);
            throw e;
        }
    }

    public boolean isUsingSafImplementation() {
        ClassOrDefaultProxyUtils.ClassOrDefaultProxyState stateInterface = (ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) irrPassTicket;
        return stateInterface.isUsingBaseImplementation();
    }

    private void validateUserIdAndApplId(String userId, String applId) throws ApplicationNameNotProvidedException, UsernameNotProvidedException {
        if (StringUtils.isBlank(applId)) {
           throw new ApplicationNameNotProvidedException();
        }

        if (StringUtils.isBlank(userId)) {
            throw new UsernameNotProvidedException();
        }
    }

    public static class DefaultPassTicketImpl implements IRRPassTicket {

        private static int id = 0;

        public static final String ZOWE_DUMMY_USERID = "USER";
        public static final String ZOWE_DUMMY_PASS_TICKET_PREFIX = "ZOWE_DUMMY_PASS_TICKET";

        public static final String DUMMY_USER = "USER";
        public static final String UNKNOWN_USER = "UNKNOWN_USER";
        public static final String UNKNOWN_APPLID = "XBADAPPL";

        private final Map<UserApp, Set<String>> userAppToPasstickets = new HashMap<>();

        @Override
        public void evaluate(String userId, String applId, String passTicket) throws IRRPassTicketEvaluationException {
            ObjectUtil.requireNotNull(userId, "Parameter userId is empty");
            ObjectUtil.requireNotNull(applId, "Parameter applId is empty");
            ObjectUtil.requireNotNull(passTicket, "Parameter passTicket is empty");

            if (StringUtils.equalsIgnoreCase(UNKNOWN_APPLID, applId)) {
                throw new IRRPassTicketEvaluationException(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28);
            }

            if (userId.equals(ZOWE_DUMMY_USERID) && passTicket.startsWith(ZOWE_DUMMY_PASS_TICKET_PREFIX)) {
                return;
            }

            final Set<String> passTickets = userAppToPasstickets.get(new UserApp(userId, applId));

            if ((passTickets == null) || !passTickets.contains(passTicket)) {
                throw new IRRPassTicketEvaluationException(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_32);
            }
        }

        @Override
        public String generate(String userId, String applId) throws IRRPassTicketGenerationException {
            if (StringUtils.equalsIgnoreCase(UNKNOWN_USER, userId)) {
                throw new IRRPassTicketGenerationException(AbstractIRRPassTicketException.ErrorCode.ERR_8_8_16);
            }

            if (StringUtils.equalsIgnoreCase(UNKNOWN_APPLID, applId)) {
                throw new IRRPassTicketGenerationException(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28);
            }

            if (StringUtils.equalsIgnoreCase(DUMMY_USER, userId)) {
                return ZOWE_DUMMY_PASS_TICKET_PREFIX;
            }

            final UserApp userApp = new UserApp(userId, applId);
            final int currentId;
            synchronized (DefaultPassTicketImpl.class) {
                currentId = DefaultPassTicketImpl.id++;
            }
            final String passTicket = ZOWE_DUMMY_PASS_TICKET_PREFIX + "_" + applId + "_" + userId + "_" + currentId;

            final Set<String> passTickets = userAppToPasstickets.computeIfAbsent(userApp, x -> new HashSet<>());
            passTickets.add(passTicket);

            return passTicket;
        }

        @AllArgsConstructor
        @Value
        private static class UserApp {

            String userId;
            String applId;

        }
    }

}
