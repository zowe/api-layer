/* eslint-disable no-undef */
import * as enzyme from 'enzyme';
import Shield from './Shield';
import React from "react";
import { render } from "react-dom";
import { act } from "react-dom/test-utils";

const Child = () => {
    throw 'error';
};

const pauseErrorLogging = codeToRun => {
    const logger = console.error;
    console.error = () => {};

    codeToRun();

    console.error = logger;
};
describe('>>> Shield component tests', () => {
    it('Should catches error and renders message', () => {
        const errorMessage = 'Display the error stack';
        let container = document.createElement("div");
        act(()=>{
            render( <Shield>
                <Child />
            </Shield>,container)
        })
        expect(container.textContent).toBe(errorMessage);
    });
});
