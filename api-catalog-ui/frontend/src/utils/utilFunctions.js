/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/**
 * Counts the additional contents
 * @param service
 * @returns {{videosCounter: number, useCasesCounter: number, tutorialsCounter: number}}
 */
export default function countAdditionalContents(service) {
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

function setMultipleElements(uiConfig) {
    if (uiConfig.headerColor) {
        const divider = document.getElementById('separator2');
        const logoutButton = document.getElementById('go-back-button');
        const title1 = document.getElementById('title');
        const swaggerLabel = document.getElementById('swagger-label');
        const header = document.getElementsByClassName('header');
        if (header && header.length > 0) {
            header[0].style.setProperty('background-color', uiConfig.headerColor);
        }
        if (divider) {
            divider.style.setProperty('background-color', uiConfig.headerColor);
        }
        if (title1) {
            title1.style.setProperty('color', uiConfig.headerColor);
        }
        if (swaggerLabel) {
            swaggerLabel.style.setProperty('color', uiConfig.headerColor);
        }
        if (logoutButton) {
            logoutButton.style.setProperty('color', uiConfig.headerColor);
        }
    }
}

/**
 * Retrieve the logo set in the configuration
 * @returns {Promise<T>}
 */
function fetchImagePath() {
    const getImgUrl = `${process.env.REACT_APP_GATEWAY_URL}/apicatalog/api/v1/custom-logo`;

    return fetch(getImgUrl)
        .then((response) => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            const contentType = response.headers.get('Content-Type');
            return response.blob().then((blob) => {
                const blobWithContentType = new Blob([blob], { type: contentType });
                return URL.createObjectURL(blobWithContentType);
            });
        })
        .catch((error) => {
            throw new Error(`Error fetching image path: ${error.message}`);
        });
}

/**
 * Custom the UI look to match the setup from the service metadata
 * @param uiConfig the configuration to customize the UI
 */
export const customUIStyle = async (uiConfig) => {
    const root = document.documentElement;
    const logo = document.getElementById('logo');
    if (logo && uiConfig.logo) {
        logo.src = await fetchImagePath();
    }

    if (uiConfig.backgroundColor) {
        const homepage = document.getElementsByClassName('apis');
        if (homepage[0]) {
            homepage[0].style.backgroundColor = uiConfig.backgroundColor;
            homepage[0].style.backgroundImage = 'none';
        }

        const detailPage = document.getElementsByClassName('content');
        root.style.backgroundColor = uiConfig.backgroundColor;
        detailPage[0].style.backgroundColor = uiConfig.backgroundColor;
    }
    setMultipleElements(uiConfig);
    if (uiConfig.fontFamily) {
        document.body.style.fontFamily = uiConfig.fontFamily;
    }
    if (uiConfig.textColor) {
        const description = document.getElementById('description');
        if (description) {
            description.style.color = uiConfig.textColor;
        }
    }
};
