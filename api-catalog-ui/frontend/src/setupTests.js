/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/* eslint-disable import/no-extraneous-dependencies */
import * as enzyme from 'enzyme';
import 'jest-enzyme';
import Adapter from '@cfaester/enzyme-adapter-react-18';

enzyme.configure({ adapter: new Adapter() });
