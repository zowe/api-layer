/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
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
