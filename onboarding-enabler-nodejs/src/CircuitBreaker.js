/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-underscore-dangle */
import { EventEmitter } from 'events';

const STATES = {
  CLOSED: 'CLOSED',
  OPEN: 'OPEN',
  HALF_OPEN: 'HALF_OPEN',
};

/**
 * Circuit breaker state machine for the Node.js onboarding enabler.
 *
 * Three states: CLOSED (normal), OPEN (failing, no requests), HALF_OPEN (probing).
 * Transitions:
 *   CLOSED → OPEN when failureCount >= maxFailures
 *   OPEN  → HALF_OPEN when cooldown expires (checked via allowRequest())
 *   HALF_OPEN → CLOSED on recordSuccess()
 *   HALF_OPEN → OPEN  on recordFailure()
 *
 * OPEN cooldown uses exponential backoff: cooldownTime × 2^(openCycleCount-1),
 * capped at backoffMax. CLOSED backoff uses cooldownTime × 2^(failureCount-1).
 *
 * Emits events: 'circuitOpen', 'circuitHalfOpen', 'circuitClose'
 */
export default class CircuitBreaker extends EventEmitter {
  /**
   * @param {Object} options
   * @param {number} [options.maxFailures=5] Consecutive failures before circuit opens
   * @param {number} [options.cooldownTime=60000] Base cooldown time in ms (used for
   *   both CLOSED exponential backoff and OPEN exponential cooldown base)
   * @param {number} [options.backoffMax=300000] Maximum backoff cap in ms
   */
  constructor({
    maxFailures = 5,
    cooldownTime = 60000,
    backoffMax = 300000,
  } = {}) {
    super();
    this.maxFailures = maxFailures;
    this.cooldownTime = cooldownTime;
    this.backoffMax = backoffMax;

    this._state = STATES.CLOSED;
    this.failureCount = 0;
    this._openedAt = null;
    this._openCycleCount = 0;
  }

  /** @returns {string} Current state: CLOSED, OPEN, or HALF_OPEN */
  get state() {
    return this._state;
  }

  /** @returns {boolean} True if circuit is OPEN (not accepting requests) */
  isOpen() {
    return this._state === STATES.OPEN;
  }

  /**
   * Check whether a request should be allowed.
   * If circuit is OPEN but cooldown has expired, transitions to HALF_OPEN and returns true.
   *
   * @returns {boolean} True if a request may proceed
   */
  allowRequest() {
    if (this._state === STATES.CLOSED) {
      return true;
    }
    if (this._state === STATES.OPEN) {
      if (this._cooldownExpired()) {
        this._transitionTo(STATES.HALF_OPEN);
        return true;
      }
      return false;
    }
    // HALF_OPEN: allow probe request
    return true;
  }

  /**
   * Record a successful request. Resets failureCount.
   * If transitioning from HALF_OPEN to CLOSED, resets openCycleCount and emits 'circuitClose'.
   *
   * @returns {{ transition: string|null }} Transition if state changed
   */
  recordSuccess() {
    const prevState = this._state;
    this.failureCount = 0;
    if (prevState === STATES.HALF_OPEN) {
      this._openCycleCount = 0;
      this._transitionTo(STATES.CLOSED);
      return { transition: STATES.CLOSED };
    }
    return { transition: null };
  }

  /**
   * Record a failed request. Increments failureCount.
   * May transition to OPEN if threshold reached.
   * OPEN delay returned is the exponential cooldown .
   *
   * @returns {{ transition: string|null, delay: number }}
   *   transition — state change if any; delay — suggested wait before next attempt (ms)
   */
  recordFailure() {
    this.failureCount += 1;
    const prevState = this._state;

    // HALF_OPEN probe failure → immediately re-open with exponential delay
    if (prevState === STATES.HALF_OPEN) {
      this._transitionTo(STATES.OPEN);
      return { transition: STATES.OPEN, delay: this._computeOpenCooldown() };
    }

    // CLOSED + threshold reached → open circuit with exponential delay
    if (prevState === STATES.CLOSED && this.failureCount >= this.maxFailures) {
      this._transitionTo(STATES.OPEN);
      return { transition: STATES.OPEN, delay: this._computeOpenCooldown() };
    }

    // Still CLOSED, below threshold — return exponential backoff delay
    return { transition: null, delay: this.getNextCooldown() };
  }

  /**
   * Compute the cooldown/delay for the next scheduling cycle.
   * OPEN state: exponential cooldown based on openCycleCount.
   * CLOSED state: exponential backoff based on failureCount.
   * Both use cooldownTime as base, capped at backoffMax.
   *
   * @returns {number} Delay in milliseconds
   */
  getNextCooldown() {
    if (this._state === STATES.OPEN) {
      return this._computeOpenCooldown();
    }
    if (this.failureCount === 0) {
      return this.cooldownTime;
    }
    const backoff = this.cooldownTime * (2 ** (this.failureCount - 1));
    return Math.min(backoff, this.backoffMax);
  }

  /** Reset breaker to CLOSED, zero failures and open cycle count. */
  reset() {
    this._state = STATES.CLOSED;
    this.failureCount = 0;
    this._openedAt = null;
    this._openCycleCount = 0;
  }

  // ---- internal ----

  /**
   * Compute the OPEN cooldown with exponential backoff.
   * cooldownTime × 2^(openCycleCount-1), capped at backoffMax.
   * @returns {number} Cooldown in milliseconds
   */
  _computeOpenCooldown() {
    if (this._openCycleCount === 0) return this.cooldownTime;
    const backoff = this.cooldownTime * (2 ** (this._openCycleCount - 1));
    return Math.min(backoff, this.backoffMax);
  }

  _transitionTo(newState) {
    const oldState = this._state;
    this._state = newState;

    if (newState === STATES.OPEN) {
      this._openedAt = Date.now();
      this._openCycleCount += 1;
      this.emit('circuitOpen', { from: oldState, to: newState });
    } else if (newState === STATES.HALF_OPEN) {
      this.emit('circuitHalfOpen', { from: oldState, to: newState });
    } else if (newState === STATES.CLOSED) {
      this._openedAt = null;
      this.emit('circuitClose', { from: oldState, to: newState });
    }
  }

  _cooldownExpired() {
    if (this._openedAt === null) {
      return false;
    }
    return (Date.now() - this._openedAt) >= this._computeOpenCooldown();
  }
}
