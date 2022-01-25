/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { connect } from 'react-redux';
import ServiceVersionDiff from './ServiceVersionDiff';
import { getDiff } from '../../actions/service-version-diff-actions';

const mapSateToProps = (state) => ({
    diffText: state.serviceVersionDiff.diffText,
    version1: state.serviceVersionDiff.oldVersion,
    version2: state.serviceVersionDiff.newVersion,
});

const mapDispatchToProps = {
    getDiff,
};

export default connect(mapSateToProps, mapDispatchToProps)(ServiceVersionDiff);
