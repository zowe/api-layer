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
import ConfirmDialog from './ConfirmDialog';

describe('>>> ConfirmDialog tests', () => {
   it('should override the static definition', () => {
       const overrideStaticDef = jest.fn();
       const confirmStaticDefOverride = jest.fn();
       const wrapper = enzyme.shallow(
           <ConfirmDialog
               overrideStaticDef={overrideStaticDef}
               confirmStaticDefOverride={confirmStaticDefOverride}
           />
       );
       wrapper.instance().override();
       expect(overrideStaticDef).toHaveBeenCalled();
       expect(overrideStaticDef).toHaveBeenCalled();
   })
});
