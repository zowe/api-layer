/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

// envConfig.js — maps EUREKA_CLIENT_* env vars to eureka config properties
import Logger from './Logger.js';

const logger = new Logger();

const ENV_MAP = [
  {
    env: 'EUREKA_CLIENT_REGISTRYFETCHINTERVALSECONDS',
    key: 'registryFetchInterval',
    unit: 'seconds',
  },
  {
    env: 'EUREKA_CLIENT_INSTANCEINFOREPLICATIONINTERVALSECONDS',
    key: 'heartbeatInterval',
    unit: 'seconds',
  },
  // Future: add entries here for circuit breaker properties (#4775)
  // { env: 'EUREKA_CLIENT_MAXFAILURES',   key: 'maxFailures',   unit: 'milliseconds' },
  // { env: 'EUREKA_CLIENT_COOLDOWNTIME',  key: 'cooldownTime',  unit: 'seconds' },
  // { env: 'EUREKA_CLIENT_BACKOFFMAX',    key: 'backoffMax',    unit: 'seconds' },
];

function parsePositiveInt(envName) {
  const raw = process.env[envName];
  if (raw === undefined || raw === '') return undefined;
  const parsed = parseInt(raw, 10);
  if (isNaN(parsed) || parsed <= 0) {
    const msg = `Invalid value for ${envName}: "${raw}". `
      + 'Expected a positive integer. Using default.';
    logger.warn(msg);
    return undefined;
  }
  return parsed;
}

export default function envConfig() {
  const result = { eureka: {} };
  for (const { env, key, unit } of ENV_MAP) {
    const value = parsePositiveInt(env);
    if (value !== undefined) {
      result.eureka[key] = unit === 'seconds' ? value * 1000 : value;
    }
  }
  return result;
}
