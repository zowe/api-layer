/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import gulp from 'gulp';
import babel from 'gulp-babel';
import mocha from 'gulp-mocha';
import eslint from 'gulp-eslint';
import { Instrumenter } from 'babel-istanbul';
import istanbul from 'gulp-istanbul';
import env from 'gulp-env';

gulp.task('build', () => (
  gulp.src('src/**/*.js')
    .pipe(babel())
    .pipe(gulp.dest('lib'))
));

gulp.task('lint', () => (
  gulp.src(['src/**/*.js', 'test/**/*.js'])
    .pipe(eslint())
    .pipe(eslint.format())
    .pipe(eslint.failOnError())
));

gulp.task('mocha', (cb) => {
  const envs = env.set({
    NODE_ENV: 'test',
  });

  return gulp.src('src/**/*.js')
    .pipe(envs)
    .pipe(istanbul({
      instrumenter: Instrumenter,
    })) // Covering files
    .pipe(istanbul.hookRequire()) // Force `require` to return covered files
    .on('finish', () => {
      gulp.src(['test/**/*.js', '!test/integration.test.js'])
        .pipe(mocha())
        .pipe(istanbul.writeReports())
        .pipe(istanbul.enforceThresholds({ thresholds: { global: 0 } }))
        .pipe(envs.reset)
        .on('end', cb);
    });
});

gulp.task('test:integration', () => (
  gulp.src('test/integration.test.js')
    .pipe(mocha({ timeout: 120000 }))
));

gulp.task('test', gulp.series('lint', 'mocha'));

gulp.task('test:watch', () => (
  gulp.watch(['src/**/*.js', 'test/**/*.test.js'], ['test'])
));

gulp.task('default', gulp.series('build'));
