/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import * as enzyme from 'enzyme';
import React from 'react';
import YAMLVisualizer from './YAMLVisualizer';

describe('>>> YAML Visualizer tests', () => {
    it('should generate YAML on mount', () => {
        const createYamlObject = jest.fn();
        const dummyData = {
            text: 'Basic info',
            content: {
                testInput: {
                    value: 'input',
                    question: '',
                },
            },
            multiple: false,
        };
        enzyme.shallow(
            <YAMLVisualizer createYamlObject={createYamlObject} inputData={dummyData} />
        );
        expect(createYamlObject).toHaveBeenCalledWith(dummyData);
    });

    it('should copy the right value to clipboard', () => {
        const createYamlObject = jest.fn();
        document.execCommand = jest.fn();
        jest.spyOn(document, 'createRange').mockImplementation(() => ({ selectNodeContents: jest.fn() }));
        jest.spyOn(window, 'getSelection').mockImplementation(() => ({
            removeAllRanges: jest.fn(),
            addRange: jest.fn(),
        }));
        const wrapper = enzyme.shallow(<YAMLVisualizer createYamlObject={createYamlObject} />);

        const copyButton = wrapper.instance();
        copyButton.copy();

        expect(document.execCommand).toHaveBeenCalledWith('copy');
    });

    it('should copy the right value to clipboard with Clipboard API', () => {
        Object.assign(navigator, {
            clipboard: {
                writeText: () => {},
            },
        });
        jest.spyOn(navigator.clipboard, "writeText");
        const wrapper = enzyme.shallow(<YAMLVisualizer createYamlObject={jest.fn()} />);
        wrapper.instance().copy();
        expect(navigator.clipboard.writeText).toHaveBeenCalled();
    });

    it('should display YAML from props', () => {
        const wrapper = enzyme.shallow(
            <YAMLVisualizer createYamlObject={jest.fn()} yamlObject="test:yaml" />
        );
        expect(wrapper.find('.yamlContainer code').text()).toEqual('test:yaml\n');
    });
});
