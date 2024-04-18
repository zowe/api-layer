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
import '@testing-library/jest-dom';
import HeaderDetail from './HeaderDetail';

describe('>>> HeaderDetail component tests', () => {
    it('should display a Detail', () => {
        const sample = enzyme.shallow(<HeaderDetail />);
        expect(sample.find('Header Detail')).toBeDefined();
    });
});
