/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gradle.keystore;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Registers the {@code generateKeystores} task.
 *
 * <p>Keystores are generated in-process, through JCA and BouncyCastle, so the build needs no
 * openssl, no keytool and no shell. Generation used to shell out to a script that needed all three,
 * which was awkward to rely on for developers using the Git that ships with IntelliJ or VS Code on
 * Windows.
 */
public class KeystorePlugin implements Plugin<Project> {

    /** Set this project property to leave the keystores alone — CI jobs that receive them as an artifact do. */
    private static final String SKIP_PROPERTY = "skipKeystoreGeneration";

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateKeystores", GenerateKeystoresTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Generate TLS keystores for local development and testing");
            task.getRepoRoot().set(project.getLayout().getProjectDirectory());
            task.getKeystoreDirectory().set(project.getLayout().getProjectDirectory().dir("keystore"));
            task.onlyIf(unused -> shouldGenerate(project));
        });
    }

    private boolean shouldGenerate(Project project) {
        if (project.hasProperty(SKIP_PROPERTY)) {
            project.getLogger().lifecycle(
                "Keystore generation skipped: {} property is set", SKIP_PROPERTY);
            return false;
        }
        String reason;
        try {
            reason = GenerateKeystoresTask.reasonToRegenerate(project.getProjectDir().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not inspect the existing keystores", e);
        }
        if (reason == null) {
            project.getLogger().lifecycle("Keystore generation skipped: keystores are present and up to date");
            return false;
        }
        project.getLogger().lifecycle("Generating keystores: {}", reason);
        return true;
    }
}
