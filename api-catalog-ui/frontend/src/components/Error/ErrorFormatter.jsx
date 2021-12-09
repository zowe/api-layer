import { Typography } from '@material-ui/core';
import renderHTML from 'react-render-html';
import uuidv4 from 'uuid/v4';

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
            {renderHTML(message)}
        </Typography>
    );
}

const formatError = error => {
    let message = 'Could not determine error';
    let color = colorDanger;

    if (error === null || error === undefined) {
        return formaHtmlError(message, color);
    }

    if (error.id !== undefined && error.timestamp !== undefined) {
        message = error.error;
        color = colorDanger;
        const extractedAjaxError = extractAjaxError(error.error);
        if (extractedAjaxError) {
            const { msg, clr } = extractedAjaxError;
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

    if (error.name === 'AjaxError') {
        const { msg, clr } = extractAjaxError(error);
        return formaHtmlError(msg, clr);
    }

    if (error.message !== undefined) {
        return formaHtmlError(error.message, colorDanger);
    }

    return formaHtmlError(message, color);
};

export default formatError;
