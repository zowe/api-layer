/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
export default function countAdditionalContent(service) {
    let useCasesCounter = 0;
    let tutorialsCounter = 0;
    let videosCounter = 0;
    if (service) {
        if ('useCases' in service && service.useCases) {
            useCasesCounter = service.useCases.length;
        }
        if ('tutorials' in service && service.tutorials) {
            tutorialsCounter = service.tutorials.length;
        }
        if ('videos' in service && service.videos) {
            videosCounter = service.videos.length;
        }
    }
    return { useCasesCounter, tutorialsCounter, videosCounter };
}
