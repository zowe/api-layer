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
 * Emits events: 'circuitOpen', 'circuitHalfOpen', 'circuitClose'
 */
export default class CircuitBreaker extends EventEmitter {
  /**
   * @param {Object} options
   * @param {number} [options.maxFailures=5] Consecutive failures before circuit opens
   * @param {number} [options.cooldownTime=60000] Time in ms circuit stays OPEN
   * @param {number} [options.baseCooldown=30000] Base cooldown for exponential backoff
   * @param {number} [options.backoffMax=300000] Maximum backoff cap in ms
   */
  constructor({
    maxFailures = 5,
    cooldownTime = 60000,
    baseCooldown = 30000,
    backoffMax = 300000,
  } = {}) {
    super();
    this.maxFailures = maxFailures;
    this.cooldownTime = cooldownTime;
    this.baseCooldown = baseCooldown;
    this.backoffMax = backoffMax;

    this._state = STATES.CLOSED;
    this.failureCount = 0;
    this._openedAt = null;
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
   * If transitioning from HALF_OPEN to CLOSED, emits 'circuitClose'.
   *
   * @returns {{ transition: string|null }} Transition if state changed
   */
  recordSuccess() {
    const prevState = this._state;
    this.failureCount = 0;
    if (prevState === STATES.HALF_OPEN) {
      this._transitionTo(STATES.CLOSED);
      return { transition: STATES.CLOSED };
    }
    return { transition: null };
  }

  /**
   * Record a failed request. Increments failureCount.
   * May transition to OPEN if threshold reached.
   *
   * @returns {{ transition: string|null, delay: number }}
   *   transition — state change if any; delay — suggested wait before next attempt (ms)
   */
  recordFailure() {
    this.failureCount += 1;
    const prevState = this._state;

    // HALF_OPEN probe failure → immediately re-open
    if (prevState === STATES.HALF_OPEN) {
      this._transitionTo(STATES.OPEN);
      return { transition: STATES.OPEN, delay: this.cooldownTime };
    }

    // CLOSED + threshold reached → open circuit
    if (prevState === STATES.CLOSED && this.failureCount >= this.maxFailures) {
      this._transitionTo(STATES.OPEN);
      return { transition: STATES.OPEN, delay: this.cooldownTime };
    }

    // Still CLOSED, below threshold — return backoff delay
    return { transition: null, delay: this.getNextCooldown() };
  }

  /**
   * Compute the cooldown/delay for the next scheduling cycle.
   * Uses exponential backoff: baseCooldown × 2^(failureCount-1), capped at backoffMax.
   *
   * @returns {number} Delay in milliseconds
   */
  getNextCooldown() {
    if (this._state === STATES.OPEN) {
      return this.cooldownTime;
    }
    if (this.failureCount === 0) {
      return this.baseCooldown;
    }
    const backoff = this.baseCooldown * (2 ** (this.failureCount - 1));
    return Math.min(backoff, this.backoffMax);
  }

  /** Reset breaker to CLOSED, zero failures. */
  reset() {
    this._state = STATES.CLOSED;
    this.failureCount = 0;
    this._openedAt = null;
  }

  // ---- internal ----

  _transitionTo(newState) {
    const oldState = this._state;
    this._state = newState;

    if (newState === STATES.OPEN) {
      this._openedAt = Date.now();
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
    return (Date.now() - this._openedAt) >= this.cooldownTime;
  }
}
