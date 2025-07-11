/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import lombok.NonNull;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.exceptions.ApiVersionNotFoundException;

import java.util.List;

public interface ApiDocRetrievalService {


    ApiDocInfo retrieveApiDoc(@NonNull String serviceId, String apiVersion);

}
