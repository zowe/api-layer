/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SecureConnectionFilter extends OncePerRequestFilter {

    static {
        try {
            extractLib(System.getProperty("java.library.path"), "lib" + AttlsContext.ATTLS_LIBRARY_NAME + ".so");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("z/os".equalsIgnoreCase(System.getProperty("os.name"))) {
            try {
                if (InboundAttls.getStatConn() != StatConn.SECURE) {
                    response.setStatus(500);
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.writeValue(response.getWriter(), "Connection is not secure");
                } else {
                    filterChain.doFilter(request, response);
                }
            } catch (ContextIsNotInitializedException e) {
                e.printStackTrace();
            } catch (UnknownEnumValueException e) {
                e.printStackTrace();
            } catch (IoctlCallException e) {
                e.printStackTrace();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    public static void extractLib(String directory, String fileName) throws IOException {
        File library = new File(directory, fileName);
        try (InputStream inputStream = AttlsContext.class.getResourceAsStream("/lib/" + fileName)) {
            Files.copy(inputStream, library.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            library.delete();
            throw e;
        } catch (NullPointerException e) {
            library.delete();
            throw new FileNotFoundException(fileName + " does not exist in JAR.");
        }
    }
}
