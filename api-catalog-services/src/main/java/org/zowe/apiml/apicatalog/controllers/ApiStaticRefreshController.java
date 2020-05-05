/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class ApiStaticRefreshController {

    @RequestMapping(
        value = "/discovery/api/v1/staticApi",
        method = RequestMethod.POST)
    public String refreshStaticApis() {
        // TODO find the instances of discovery sercice and redirect to one of them
        return "redirect:https://localhost:10011/discovery/api/v1/staticApi";
    }

}
