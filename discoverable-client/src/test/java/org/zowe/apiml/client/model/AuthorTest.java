/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.model;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.client.model.graphql.Author;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AuthorTest {

    @Test
    public void testGetByIdFound() {
        Author author = Author.getById("author-1");
        assertEquals("Joshua", author.firstName());
        assertEquals("Bloch", author.lastName());
    }

    @Test
    public void testGetByIdNotFound() {
        Author author = Author.getById("non-existent-id");
        assertNull(author);
    }
}
