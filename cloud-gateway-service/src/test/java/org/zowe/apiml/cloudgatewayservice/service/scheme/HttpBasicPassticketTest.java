/*
 * Copyright (c) 2022 Broadcom.  All Rights Reserved.  The term
 * "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This software and all information contained therein is
 * confidential and proprietary and shall not be duplicated,
 * used, disclosed, or disseminated in any way except as
 * authorized by the applicable license agreement, without the
 * express written permission of Broadcom.  All authorized
 * reproductions must be marked with this language.
 *
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO
 * THE EXTENT PERMITTED BY APPLICABLE LAW, BROADCOM PROVIDES THIS
 * SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL BROADCOM
 * BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR
 * DAMAGE, DIRECT OR INDIRECT, FROM THE USE OF THIS SOFTWARE,
 * INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS
 * INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF BROADCOM IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
 */

package org.zowe.apiml.cloudgatewayservice.service.scheme;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpBasicPassticketTest {

    @Test
    void givenInstance_whenGetAuthenticationScheme_thenReturnProperType() {
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, new HttpBasicPassticket().getAuthenticationScheme());
    }

    @Test
    void givenInstance_whenApply_thenFulfillFilterFactorArgs() {
        RouteDefinition routeDefinition = new RouteDefinition();
        Authentication authentication = new Authentication();
        authentication.setApplid("applid");

        new HttpBasicPassticket().apply(routeDefinition, authentication);

        assertEquals(1, routeDefinition.getFilters().size());
        FilterDefinition filterDefinition = routeDefinition.getFilters().get(0);
        assertEquals("applid", filterDefinition.getArgs().get("applicationName"));
        assertEquals("PassticketFilterFactory", filterDefinition.getName());
    }

}