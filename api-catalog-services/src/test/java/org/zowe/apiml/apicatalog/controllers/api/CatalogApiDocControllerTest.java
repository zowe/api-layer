/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.controllers.api;

import org.zowe.apiml.apicatalog.services.status.APIServiceStatusService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;

public class CatalogApiDocControllerTest {

    @Test
    public void testCreationOfClass() {
        APIServiceStatusService apiServiceStatusService = Mockito.mock(APIServiceStatusService.class);
        CatalogApiDocController catalogApiDocController = new CatalogApiDocController(apiServiceStatusService);
        Assert.assertNotNull(catalogApiDocController);
    }

    @Test
    public void testGetApiDocInfo() {
        APIServiceStatusService apiServiceStatusService = Mockito.mock(APIServiceStatusService.class);
        CatalogApiDocController catalogApiDocController = new CatalogApiDocController(apiServiceStatusService);
        ResponseEntity<String> response = new ResponseEntity<>("Some API Doc", HttpStatus.OK);

        when(apiServiceStatusService.getServiceCachedApiDocInfo("service", "1.0.0")).thenReturn(response);
        ResponseEntity<String> res = catalogApiDocController.getApiDocInfo("service", "1.0.0");
        Assert.assertNotNull(res);
        Assert.assertEquals("Some API Doc", res.getBody());
    }
}
