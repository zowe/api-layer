/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { isValidUrl } from '../../utils/utilFunctions';

function VideoWrapper({ url }) {
    if (!isValidUrl(url)) return null;
    return (
        <div data-testid="video-container" className="video-responsive">
            <iframe
                width="355"
                height="200"
                src={url}
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                title="Embedded youtube"
            />
            <br />
        </div>
    );
}

VideoWrapper.propTypes = {
    url: PropTypes.string.isRequired,
};

export default VideoWrapper;
