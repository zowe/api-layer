/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { shallow } from 'enzyme';
import VideoWrapper from './VideoWrapper';

describe('>>> BlogTile component tests', () => {
    it('should not render videos if url is not valid', () => {
        const video = shallow(<VideoWrapper url="" />);
        expect(video.find('[data-testid="video-container"]').exists()).toEqual(false);
    });

    it('should render videos', () => {
        const video = shallow(<VideoWrapper url="https://localhost.com/hfehf" />);
        expect(video.find('[data-testid="video-container"]').exists()).toEqual(true);
    });
});
