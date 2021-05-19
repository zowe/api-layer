export const SEND_ERROR = 'SEND_ERROR';
export const CLEAR_ALL_ERRORS = 'CLEAR_ALL_ERRORS';

export class ApiError {
    key = '';
    number = '';
    text = '';
    messageType = {};

    constructor(key, number, messageType, text) {
        this.key = key;
        this.number = number;
        this.messageType = messageType;
        this.text = text;
    }
}

export class MessageType {
    levelInt = 0;
    levelStr = '';
    levelChar = '';

    constructor(levelInt, levelStr, levelChar) {
        this.levelInt = levelInt;
        this.levelStr = levelStr;
        this.levelChar = levelChar;
    }
}
