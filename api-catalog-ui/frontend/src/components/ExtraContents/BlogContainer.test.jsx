/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { shallow, mount } from 'enzyme';
import BlogContainer from './BlogContainer';

describe('>>> BlogContainer component tests', () => {
    it('should render medium blogs', () => {
        const blogContainer = shallow(<BlogContainer user="user" url="https://medium.com/some/medium" title="title" />);

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(true);
    });

    it('should render other blogs', () => {
        const blogContainer = shallow(<BlogContainer user="user" url="https://docs.zowe.org/doc" title="title" />);

        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);
    });

    it('should return null if URL is not valid', () => {
        const blogContainer = mount(<BlogContainer user="user" url="wrong_url" title="title" />);

        expect(blogContainer.isEmptyRender()).toBe(true);
    });

    it('should return null if medium user is null', () => {
        const blogContainer = mount(<BlogContainer user="" url="https://medium.com/some/medium" title="title" />);

        expect(blogContainer.isEmptyRender()).toBe(true);
    });
    it('should return null if medium user is not provided', () => {
        const blogContainer = mount(<BlogContainer url="https://medium.com/some/medium" title="title" />);

        expect(blogContainer.isEmptyRender()).toBe(true);
    });
});
