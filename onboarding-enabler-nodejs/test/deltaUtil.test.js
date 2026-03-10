/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
 * Copyright 2026 Contributors to the Zowe Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Jacob Quatier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import { expect } from 'chai';
import { arrayOrObj, findInstance, normalizeDelta } from '../src/deltaUtils.js';

describe('deltaUtils', () => {
  describe('arrayOrObj', () => {
    it('should return same array if passed an array', () => {
      const arr = ['foo'];
      expect(arrayOrObj(arr)).to.equal(arr);
    });
    it('should return an array containing obj', () => {
      const obj = {};
      expect(arrayOrObj(obj)[0]).to.equal(obj);
    });
  });
  describe('findInstance', () => {
    it('should return true if objects match', () => {
      const obj1 = { hostName: 'foo', port: { $: '6969' } };
      const obj2 = { hostName: 'foo', port: { $: '6969' } };
      expect(findInstance(obj1)(obj2)).to.equal(true);
    });
    it('should return false if objects do not match', () => {
      const obj1 = { hostName: 'foo', port: { $: '6969' } };
      const obj2 = { hostName: 'bar', port: { $: '1111' } };
      expect(findInstance(obj1)(obj2)).to.equal(false);
    });
  });
  describe('normalizeDelta', () => {
    it('should normalize nested objs to arrays', () => {
      const delta = {
        instance: {
          hostName: 'foo', port: { $: '6969' },
        },
      };
      const normalized = normalizeDelta(delta);
      expect(normalized).to.be.an('array');
      expect(normalized[0].instance).to.be.an('array');
    });
  });
});
