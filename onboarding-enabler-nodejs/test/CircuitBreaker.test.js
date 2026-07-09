/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-unused-expressions, max-len, no-underscore-dangle */
import sinon from 'sinon';
import * as chai from 'chai';

const { expect } = chai;
import sinonChai from 'sinon-chai';
import { EventEmitter } from 'events';

import CircuitBreaker from '../src/CircuitBreaker.js';

chai.use(sinonChai);

describe('CircuitBreaker', () => {
  let breaker;
  let clock;

  beforeEach(() => {
    clock = sinon.useFakeTimers();
    breaker = new CircuitBreaker({
      maxFailures: 3,
      cooldownTime: 10000,
      backoffMax: 60000,
    });
  });

  afterEach(() => {
    clock.restore();
  });

  describe('construction and initial state', () => {
    it('should extend EventEmitter', () => {
      expect(breaker).to.be.instanceof(EventEmitter);
    });

    it('should start in CLOSED state', () => {
      expect(breaker.state).to.equal('CLOSED');
    });

    it('should have failureCount of 0', () => {
      expect(breaker.failureCount).to.equal(0);
    });

    it('should have openCycleCount of 0', () => {
      expect(breaker._openCycleCount).to.equal(0);
    });

    it('should use default values when none provided', () => {
      const defaultBreaker = new CircuitBreaker();
      expect(defaultBreaker.maxFailures).to.equal(5);
      expect(defaultBreaker.cooldownTime).to.equal(60000);
      expect(defaultBreaker.backoffMax).to.equal(300000);
    });
  });

  describe('isOpen()', () => {
    it('should return false when CLOSED', () => {
      expect(breaker.isOpen()).to.be.false;
    });

    it('should return true when OPEN', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.isOpen()).to.be.true;
    });

    it('should return false when HALF_OPEN', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // transitions to HALF_OPEN
      expect(breaker.isOpen()).to.be.false;
    });
  });

  describe('allowRequest()', () => {
    it('should return true when CLOSED', () => {
      expect(breaker.allowRequest()).to.be.true;
    });

    it('should return false when OPEN and cooldown has not expired', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');
      expect(breaker.allowRequest()).to.be.false;
    });

    it('should transition OPEN → HALF_OPEN when cooldown has expired', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');
      clock.tick(10001);
      expect(breaker.allowRequest()).to.be.true;
      expect(breaker.state).to.equal('HALF_OPEN');
    });

    it('should return true while HALF_OPEN (probe allowed)', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // transitions to HALF_OPEN
      expect(breaker.allowRequest()).to.be.true;
      expect(breaker.state).to.equal('HALF_OPEN');
    });

    it('should not transition OPEN → HALF_OPEN before cooldown expires', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(9999); // 1ms short of cooldown
      expect(breaker.allowRequest()).to.be.false;
      expect(breaker.state).to.equal('OPEN');
    });
  });

  describe('recordSuccess()', () => {
    it('should reset failureCount to 0 in CLOSED', () => {
      breaker.recordFailure();
      breaker.recordFailure();
      expect(breaker.failureCount).to.equal(2);
      breaker.recordSuccess();
      expect(breaker.failureCount).to.equal(0);
    });

    it('should transition HALF_OPEN → CLOSED and reset openCycleCount', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker._openCycleCount).to.equal(1);
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      const result = breaker.recordSuccess();
      expect(result.transition).to.equal('CLOSED');
      expect(breaker.state).to.equal('CLOSED');
      expect(breaker._openCycleCount).to.equal(0);
    });

    it('should not transition when already CLOSED', () => {
      const result = breaker.recordSuccess();
      expect(result.transition).to.be.null;
      expect(breaker.state).to.equal('CLOSED');
    });

    it('should return transition=null when CLOSED and no state change needed', () => {
      breaker.recordFailure();
      expect(breaker.failureCount).to.equal(1);
      const result = breaker.recordSuccess();
      expect(result.transition).to.be.null;
      expect(breaker.failureCount).to.equal(0);
    });
  });

  describe('recordFailure()', () => {
    it('should increment failureCount', () => {
      breaker.recordFailure();
      expect(breaker.failureCount).to.equal(1);
      breaker.recordFailure();
      expect(breaker.failureCount).to.equal(2);
    });

    it('should transition CLOSED → OPEN when failureCount reaches maxFailures', () => {
      breaker.recordFailure(); // 1
      expect(breaker.state).to.equal('CLOSED');
      breaker.recordFailure(); // 2
      expect(breaker.state).to.equal('CLOSED');
      const result = breaker.recordFailure(); // 3 → OPEN
      expect(result.transition).to.equal('OPEN');
      expect(result.delay).to.equal(10000); // cooldownTime (first open cycle)
      expect(breaker.state).to.equal('OPEN');
    });

    it('should return exponential backoff delay when below threshold', () => {
      const result = breaker.recordFailure(); // failureCount=1
      expect(result.transition).to.be.null;
      expect(result.delay).to.equal(10000); // cooldownTime × 2^(1-1) = 10000
    });

    it('should transition HALF_OPEN → OPEN on probe failure with exponential delay', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      // First open: openCycleCount=1, cooldown=10000
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      const result = breaker.recordFailure();
      expect(result.transition).to.equal('OPEN');
      // Probe failure starts a NEW open cycle (openCycleCount=2),
      // so delay = cooldownTime * 2^(2-1) = 20000
      expect(result.delay).to.equal(20000);
      expect(breaker.state).to.equal('OPEN');
      expect(breaker._openCycleCount).to.equal(2);
    });
  });

  describe('getNextCooldown()', () => {
    it('should return cooldownTime when failureCount is 0', () => {
      expect(breaker.getNextCooldown()).to.equal(10000);
    });

    it('should double with each failure (exponential backoff)', () => {
      breaker.recordFailure(); // 1 failure → 10000 × 2^0 = 10000
      expect(breaker.getNextCooldown()).to.equal(10000);

      breaker.recordFailure(); // 2 failures → 10000 × 2^1 = 20000
      expect(breaker.getNextCooldown()).to.equal(20000);

      // 3rd failure would open circuit, test with maxFailures=10 instead
      const bigBreaker = new CircuitBreaker({ maxFailures: 10, cooldownTime: 1000, backoffMax: 60000 });
      for (let i = 0; i < 5; i += 1) bigBreaker.recordFailure();
      // 5 failures → 1000 × 2^(5-1) = 1000 × 16 = 16000
      expect(bigBreaker.getNextCooldown()).to.equal(16000);

      bigBreaker.recordFailure(); // 6 failures → 1000 × 2^5 = 32000
      expect(bigBreaker.getNextCooldown()).to.equal(32000);
    });

    it('should cap at backoffMax', () => {
      const cappedBreaker = new CircuitBreaker({
        maxFailures: 20,
        cooldownTime: 1000,
        backoffMax: 5000,
      });
      for (let i = 0; i < 10; i += 1) cappedBreaker.recordFailure();
      expect(cappedBreaker.getNextCooldown()).to.equal(5000); // capped
    });

    it('should return exponential cooldown when OPEN (AC6)', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');
      expect(breaker._openCycleCount).to.equal(1);
      expect(breaker.getNextCooldown()).to.equal(10000); // cooldownTime × 2^0

      // Simulate a second open cycle
      breaker._openCycleCount = 2;
      expect(breaker.getNextCooldown()).to.equal(20000); // cooldownTime × 2^1

      breaker._openCycleCount = 3;
      expect(breaker.getNextCooldown()).to.equal(40000); // cooldownTime × 2^2
    });

    it('should cap OPEN cooldown at backoffMax', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      breaker._openCycleCount = 10; // 10000 × 2^9 = 5120000 > backoffMax(60000)
      expect(breaker.getNextCooldown()).to.equal(60000); // capped
    });

    it('should return cooldownTime after success resets failureCount', () => {
      breaker.recordFailure();
      breaker.recordFailure();
      expect(breaker.getNextCooldown()).to.be.above(10000);
      breaker.recordSuccess();
      expect(breaker.getNextCooldown()).to.equal(10000);
    });
  });

  describe('OPEN cooldown — exponential backoff (AC5+AC6)', () => {
    it('should use cooldownTime for first OPEN cycle', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker._openCycleCount).to.equal(1);
      expect(breaker.getNextCooldown()).to.equal(10000);
    });

    it('should double cooldown on second OPEN cycle', () => {
      // First OPEN
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      breaker.recordFailure(); // → OPEN again, openCycleCount=2
      expect(breaker._openCycleCount).to.equal(2);
      expect(breaker.getNextCooldown()).to.equal(20000);
    });

    it('should double cooldown again on third OPEN cycle', () => {
      // First OPEN
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest();
      breaker.recordFailure(); // openCycleCount=2
      clock.tick(20001);
      breaker.allowRequest();
      breaker.recordFailure(); // openCycleCount=3
      expect(breaker._openCycleCount).to.equal(3);
      expect(breaker.getNextCooldown()).to.equal(40000);
    });

    it('should reset openCycleCount on successful HALF_OPEN probe', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker._openCycleCount).to.equal(1);
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      breaker.recordSuccess(); // → CLOSED
      expect(breaker._openCycleCount).to.equal(0);
    });

    it('should use longer cooldown after multiple OPEN cycles', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      // openCycleCount=1, cooldown=10000
      expect(breaker._cooldownExpired()).to.be.false;
      clock.tick(5000);
      expect(breaker._cooldownExpired()).to.be.false;
      clock.tick(5001); // total 10001ms > 10000
      expect(breaker._cooldownExpired()).to.be.true;
    });
  });

  describe('state transitions — events', () => {
    it('should emit "circuitOpen" on CLOSED → OPEN', () => {
      const spy = sinon.spy();
      breaker.on('circuitOpen', spy);
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(spy).to.have.been.calledOnce;
      expect(spy).to.have.been.calledWith({ from: 'CLOSED', to: 'OPEN' });
    });

    it('should emit "circuitHalfOpen" on OPEN → HALF_OPEN', () => {
      const spy = sinon.spy();
      breaker.on('circuitHalfOpen', spy);
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest();
      expect(spy).to.have.been.calledOnce;
      expect(spy).to.have.been.calledWith({ from: 'OPEN', to: 'HALF_OPEN' });
    });

    it('should emit "circuitClose" on HALF_OPEN → CLOSED', () => {
      const spy = sinon.spy();
      breaker.on('circuitClose', spy);
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      breaker.recordSuccess(); // → CLOSED
      expect(spy).to.have.been.calledOnce;
      expect(spy).to.have.been.calledWith({ from: 'HALF_OPEN', to: 'CLOSED' });
    });

    it('should emit "circuitOpen" on HALF_OPEN → OPEN (probe fail)', () => {
      const spy = sinon.spy();
      breaker.on('circuitOpen', spy);
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      breaker.recordFailure(); // → OPEN
      // Already called once for the first OPEN, and now a second time
      expect(spy).to.have.been.calledTwice;
      expect(spy.secondCall).to.have.been.calledWith({ from: 'HALF_OPEN', to: 'OPEN' });
    });
  });

  describe('reset()', () => {
    it('should reset to CLOSED with failureCount=0 and openCycleCount=0', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');
      expect(breaker.failureCount).to.equal(3);
      expect(breaker._openCycleCount).to.equal(1);
      breaker.reset();
      expect(breaker.state).to.equal('CLOSED');
      expect(breaker.failureCount).to.equal(0);
      expect(breaker._openCycleCount).to.equal(0);
    });

    it('should reset from HALF_OPEN to CLOSED', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      breaker.reset();
      expect(breaker.state).to.equal('CLOSED');
      expect(breaker.failureCount).to.equal(0);
      expect(breaker._openCycleCount).to.equal(0);
    });
  });

  describe('complete lifecycle', () => {
    it('should cycle through CLOSED → OPEN → HALF_OPEN → CLOSED', () => {
      // CLOSED → OPEN (3 failures)
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');

      // OPEN → HALF_OPEN (cooldown passes)
      clock.tick(10001);
      breaker.allowRequest();
      expect(breaker.state).to.equal('HALF_OPEN');

      // HALF_OPEN → CLOSED (success)
      breaker.recordSuccess();
      expect(breaker.state).to.equal('CLOSED');
      expect(breaker.failureCount).to.equal(0);
    });

    it('should cycle through CLOSED → OPEN → HALF_OPEN → OPEN (probe fails)', () => {
      // CLOSED → OPEN
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');

      // OPEN → HALF_OPEN
      clock.tick(10001);
      breaker.allowRequest();
      expect(breaker.state).to.equal('HALF_OPEN');

      // HALF_OPEN → OPEN (probe fails)
      breaker.recordFailure();
      expect(breaker.state).to.equal('OPEN');

      // Another cooldown cycle with exponential (20000ms now)
      clock.tick(20001);
      breaker.allowRequest();
      expect(breaker.state).to.equal('HALF_OPEN');
    });
  });

  describe('edge cases', () => {
    it('should handle zero failures gracefully', () => {
      expect(breaker.failureCount).to.equal(0);
      expect(breaker.allowRequest()).to.be.true;
      expect(breaker.state).to.equal('CLOSED');
    });

    it('should handle rapid success after many failures', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure(); // OPEN
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      const result = breaker.recordSuccess(); // CLOSED
      expect(result.transition).to.equal('CLOSED');
      expect(breaker.failureCount).to.equal(0);
      expect(breaker.allowRequest()).to.be.true;
    });

    it('should handle single failure without opening circuit', () => {
      const result = breaker.recordFailure();
      expect(result.transition).to.be.null;
      expect(breaker.state).to.equal('CLOSED');
      expect(breaker.failureCount).to.equal(1);
    });

    it('should resume normal interval after success resets failures', () => {
      breaker.recordFailure();
      breaker.recordFailure();
      expect(breaker.getNextCooldown()).to.equal(20000); // 10000 × 2^1
      breaker.recordSuccess();
      expect(breaker.getNextCooldown()).to.equal(10000); // back to base
    });

    it('should keep same OPEN timestamp on repeated failures while OPEN', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      const firstOpenAt = breaker._openedAt;

      // Additional recordFailure while OPEN shouldn't reset the clock
      breaker.recordFailure();
      breaker.recordFailure();
      expect(breaker._openedAt).to.equal(firstOpenAt);
    });

    it('should reset failureCount in HALF_OPEN after success', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN, failureCount still 3
      expect(breaker.failureCount).to.equal(3);
      breaker.recordSuccess(); // → CLOSED
      expect(breaker.failureCount).to.equal(0);
    });

    it('should allow new failure count to accumulate after circuit closes', () => {
      // Open circuit
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      clock.tick(10001);
      breaker.allowRequest(); // HALF_OPEN
      breaker.recordSuccess(); // CLOSED, failureCount=0

      // New failures should count from 0 again
      breaker.recordFailure();
      expect(breaker.failureCount).to.equal(1);
      breaker.recordFailure();
      expect(breaker.failureCount).to.equal(2);
      expect(breaker.state).to.equal('CLOSED'); // still below threshold
    });

    it('should increment openCycleCount only once per CLOSED→OPEN transition', () => {
      for (let i = 0; i < 3; i += 1) breaker.recordFailure();
      expect(breaker._openCycleCount).to.equal(1);

      // HALF_OPEN→OPEN should increment again
      clock.tick(10001);
      breaker.allowRequest();
      breaker.recordFailure();
      expect(breaker._openCycleCount).to.equal(2);
    });

    it('should handle many rapid OPEN cycles without overflow', () => {
      for (let i = 0; i < 50; i += 1) {
        breaker.recordFailure();
        clock.tick(10001);
        breaker.allowRequest();
      }
      // Should not crash, and cooldown should be capped
      expect(breaker.getNextCooldown()).to.be.at.most(breaker.backoffMax);
    });
  });
});
