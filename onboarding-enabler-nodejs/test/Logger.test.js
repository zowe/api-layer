/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
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

import sinon from 'sinon';
import { expect } from 'chai';
import Logger from '../src/Logger.js';

const DEFAULT_LEVEL = 30;

describe('Logger', () => {
  it('should construct with no args', () => {
    expect(() => new Logger()).to.not.throw();
  });

  describe('Logger Instance', () => {
    let logger;
    beforeEach(() => {
      logger = new Logger();
    });

    it('should return the current log level from the "level" method', () => {
      expect(logger.level()).to.equal(DEFAULT_LEVEL);
    });

    it('should update the log level if passed a number', () => {
      logger.level(100);
      expect(logger.level()).to.equal(100);
      logger.level(15);
      expect(logger.level()).to.equal(15);
    });

    it('should update the log level if a valid string is passed', () => {
      logger.level('warn');
      expect(logger.level()).to.equal(40);
      logger.level('error');
      expect(logger.level()).to.equal(50);
    });

    it('should use the default log level is an invalid string is passed', () => {
      logger.level('invalid');
      expect(logger.level()).to.equal(DEFAULT_LEVEL);
    });

    it('should only log a message if the log level is higher than the level', () => {
      logger.level(100);
      const stub = sinon.stub(console, 'error');
      logger.error('Some Error');
      expect(stub.callCount).to.equal(0);
      logger.level(50);
      logger.error('Other Error');
      expect(stub.callCount).to.equal(1);
      stub.restore();
    });

    describe('Log Methods', () => {
      beforeEach(() => {
        // Log everything:
        logger.level(-1);
      });

      const stubConsole = method => sinon.stub(console, method);

      it('should call console.log with debug', () => {
        const stub = stubConsole('log');
        logger.debug('test');
        expect(stub.callCount).to.equal(1);
        stub.restore();
      });

      it('should call console.info with info', () => {
        const stub = stubConsole('info');
        logger.info('test');
        expect(stub.callCount).to.equal(1);
        stub.restore();
      });

      it('should call console.warn with warn', () => {
        const stub = stubConsole('warn');
        logger.warn('test');
        expect(stub.callCount).to.equal(1);
        stub.restore();
      });

      it('should call console.error with error', () => {
        const stub = stubConsole('error');
        logger.error('test');
        expect(stub.callCount).to.equal(1);
        stub.restore();
      });
    });
  });
});
