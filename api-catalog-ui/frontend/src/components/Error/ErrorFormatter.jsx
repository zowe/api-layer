/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import {Typography} from '@material-ui/core';
import htmr from 'htmr';
import {v4 as uuidv4} from 'uuid';

const colorDanger = '#de1b1b';
const colorWarning = '#ad5f00';

function extractAjaxError(error) {
    if (error.response !== undefined && error.response !== null && error.response.messages !== undefined) {
        const apiError = error.response.messages[0];
        const msg = `<b>Message Code:</b> ${apiError.messageNumber}<br /><b>Message:</b> ${apiError.messageContent}`;
        let clr = colorDanger;
        if (apiError.messageType.levelStr === 'WARNING') {
            clr = colorWarning;
        }
        return { msg, clr };
    }
    if (error.status !== undefined && error.response !== undefined && error.response.message !== undefined) {
        const msg = `${error.status} : ${error.response.message}`;
        return { msg, colorDanger };
    }
    return null;
}

function formaHtmlError(message, color) {
    return (
        <Typography key={uuidv4()} variant="h5" style={{ color, fontWeight: 'semiBold' }}>
            {htmr(message)}
        </Typography>
    );
}

function handleValidError(error) {
    let message = error.error;
    let color;
    const extractedAjaxError = extractAjaxError(error.error);
    if (extractedAjaxError) {
        const {msg, clr} = extractedAjaxError;
        return formaHtmlError(msg, clr);
    }
    if (error.key !== null && error.key !== undefined) {
        message = `${error.key} : ${error.text}`;
        switch (error.messageType.levelStr) {
            case 'ERROR':
                color = colorDanger;
                break;
            case 'WARNING':
                color = colorWarning;
                break;
            default:
                color = colorDanger;
        }
    }
    return formaHtmlError(message, color);
}

const formatError = (error) => {
    const message = 'Could not determine error';

    if (error === null || error === undefined) {
        return formaHtmlError(message, colorDanger);
    }

    if (error.id !== undefined && error.timestamp !== undefined) {
        return handleValidError(error, colorDanger);
    }

    if (error.name === 'AjaxError') {
        const extractedAjaxError = extractAjaxError(error);
        if (extractedAjaxError) {
            const { msg, clr } = extractedAjaxError;
            return formaHtmlError(msg, clr);
        }
    }

    if (error.message !== undefined) {
        return formaHtmlError(error.message, colorDanger);
    }

    return formaHtmlError(message, colorDanger);
};

export default formatError;
