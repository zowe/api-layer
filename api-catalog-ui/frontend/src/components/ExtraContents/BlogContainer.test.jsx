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
import BlogContainer from './BlogContainer';

describe('>>> BlogContainer component tests', () => {
    it('should render medium blogs', () => {
        const blogContainer = shallow(
            <BlogContainer mediumUser="user" mediumBlogUrl="https://medium.com/some/medium" />
        );

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(true);
    });

    it('should render other blogs', () => {
        const blogContainer = shallow(
            <BlogContainer mediumUser="user" mediumBlogUrl="https://someother.com/some/medium" />
        );

        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);
    });
});
