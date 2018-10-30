import { Text } from 'mineral-ui';
import React from 'react';
import renderHTML from 'react-render-html';
import uuidv4 from 'uuid/v4';

const colorDanger = '#de1b1b';
const colorWarning = '#ad5f00';

function extractAjaxError(error) {
    let clr = colorDanger;
    let msg = '';
    if (error.response !== undefined && error.response !== null && error.response.messages !== undefined) {
        const apiError = error.response.messages[0];
        msg = `<b>Message Code:</b> ${apiError.messageNumber}<br /><b>Message:</b> ${apiError.messageContent}`;
        switch (apiError.messageType.levelStr) {
            case 'ERROR':
                clr = colorDanger;
                break;
            case 'WARNING':
                clr = colorWarning;
                break;
            default:
                clr = colorDanger;
        }
    } else if (error.status !== undefined && error.response !== undefined && error.response.message !== undefined) {
        msg = `${error.status} : ${error.response.message}`;
        clr = colorDanger;
    } else {
        return null;
    }
    return { msg, clr };
}

const formatError = error => {
    let message = '';
    let color = colorDanger;
    if (error === null || error === undefined) {
        message = 'Could not determine error';
    } else if (error.id !== undefined && error.timestamp !== undefined) {
        const extractedAjaxError = extractAjaxError(error.error);
        if (extractedAjaxError) {
            const { msg, clr } = extractedAjaxError;
            message = msg;
            color = clr;
        } else if (error.key !== null && error.key !== undefined) {
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
        } else {
            message = error.error;
            color = colorDanger;
        }
    } else if (error.name !== undefined && error.name === 'AjaxError') {
        const extractedAjaxError = extractAjaxError(error);
        const { msg, clr } = extractedAjaxError;
        message = msg;
        color = clr;
    } else {
        message = error.message === undefined ? 'Could not determine error' : error.message;
        color = colorDanger;
    }
    return (
        <Text key={uuidv4()} element="h5" fontWeight="semiBold" color={color}>
            {renderHTML(message)}
        </Text>
    );
};

export default formatError;
