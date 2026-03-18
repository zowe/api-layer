/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const React = require('react');

function GraphiQL() {
    return React.createElement('div', { 'data-testid': 'graphiql-mock' });
}
GraphiQL.displayName = 'GraphiQL';

function GraphiQLLogo() {
    return null;
}
GraphiQLLogo.displayName = 'GraphiQL.Logo';
GraphiQL.Logo = GraphiQLLogo;

function GraphiQLToolbar() {
    return null;
}
GraphiQLToolbar.displayName = 'GraphiQL.Toolbar';
GraphiQL.Toolbar = GraphiQLToolbar;

function GraphiQLFooter() {
    return null;
}
GraphiQLFooter.displayName = 'GraphiQL.Footer';
GraphiQL.Footer = GraphiQLFooter;

module.exports = { GraphiQL };
