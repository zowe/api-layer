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

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

@RestController
@Slf4j
public class StaticController {

    @GetMapping(value = "/robots.txt")
    @ResponseStatus(code = HttpStatus.OK)
    public void robots(HttpServletRequest request, HttpServletResponse response) {
        URL robots = Resources.getResource("robots.txt");
        try {
            String content = getContent(robots);
            response.getWriter().write(content);
        } catch (IOException e) {
            log.error("Failed getting robots.txt", e);
        }
    }

    @GetMapping(value = "/sitemap.xml")
    @ResponseStatus(code = HttpStatus.OK)
    public void sitemap(HttpServletRequest request, HttpServletResponse response) {
        URL sitemap = Resources.getResource("sitemap.xml");
        try {
            String content = getContent(sitemap);
            response.getWriter().write(content);
        } catch (IOException e) {
            log.error("Failed getting sitemap.xml", e);
        }
    }

    @GetMapping(value = "/feedback")
    @ResponseStatus(code = HttpStatus.OK)
    public void contactUs(HttpServletRequest request, HttpServletResponse response) {
        URL htmlContactUsForm = Resources.getResource("MSD_FY23_GEN_API-Feedback_0_Contact-Us_MKT_FM_281.html");
        try {
            String content = getContent(htmlContactUsForm);
            response.addHeader(HttpHeaders.CONTENT_TYPE, "text/html");
            response.getWriter().write(content);
        } catch (IOException e) {
            log.error("Failed getting the feedback html", e);
        }
    }

    private String getContent(URL source) throws IOException {
        return IOUtils.toString(source.openStream(), Charset.defaultCharset());
    }

}
