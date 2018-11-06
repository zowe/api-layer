/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class BuildInfoDetails {
    private final Properties build;
    private final Properties git;

    public BuildInfoDetails(Properties build, Properties git) {
        this.build = build;
        this.git = git;
    }

    public String getArtifact() {
        return build.get("build.artifact") == null ? "Unknown" : String.valueOf(build.get("build.artifact"));
    }

    public String getVersion() {
        return build.get("build.version") == null ? "Unknown" : String.valueOf(build.get("build.version"));
    }

    public String getNumber() {
        if (String.valueOf(build.get("build.number")).equals("n/a")) {
            return "n/a";
        } else {
            return String.valueOf(build.get("build.number"));
        }
    }

    public Date getTime() {
        DateFormat dateFormat = new SimpleDateFormat();
        Date date = null;
        try {
            date = dateFormat.parse(String.valueOf(build.get("build.time")));
        } catch (ParseException ignore) {
        }
        return date;
    }

    public String getMachine() {
        return build.get("build.machine") == null ? "Unknown" : String.valueOf(build.get("build.machine"));
    }

    public String getCommitId() {
        return git.get("git.commit.id.abbrev") == null ? "Unknown" : String.valueOf(git.get("git.commit.id.abbrev"));
    }
}
