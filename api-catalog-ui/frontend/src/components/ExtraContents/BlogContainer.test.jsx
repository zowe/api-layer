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
import React from 'react';
import BlogContainer from './BlogContainer';

describe('>>> BlogContainer component tests', () => {
    it('should render medium blog', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce(),
        });

        const blogContainer = shallow(<BlogContainer user="user" url="https://medium.com/some/medium" title="title" />);

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(true);

        global.fetch.mockRestore();
    });

    it('should not render medium blog if url missing', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce(),
        });

        const blogContainer = shallow(<BlogContainer user="user" title="title" />);

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(false);

        global.fetch.mockRestore();
    });

    it('should render other blog (non-Medium and non-Zowe)', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce(),
        });

        const blogContainer = shallow(<BlogContainer user="user" url="https://example.com" title="title" />);

        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        global.fetch.mockRestore();
    });

    it('should render correctly for Zowe documentation URL', () => {
        const blogContainer = shallow(<BlogContainer user="user" url="https://docs.zowe.org/doc" title="title" />);

        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);
    });

    it('should handle missing items in the fetched data', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce('HTML content with missing items'),
        });

        const blogContainer = shallow(<BlogContainer user="user" url="https://example.com" title="title" />);
        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        global.fetch.mockRestore();
    });

    it('should handle empty content and description', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce('<div class="shortdesc"></div>'),
        });

        const blogContainer = shallow(<BlogContainer user="user" url="https://example.com" title="title" />);

        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        global.fetch.mockRestore();
    });

    it('should handle missing RSS feed items', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            json: jest.fn().mockResolvedValueOnce({}),
        });

        const blogContainer = shallow(<BlogContainer user="user" url="https://medium.com/some/medium" title="title" />);

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(true);

        global.fetch.mockRestore();
    });

    it('should render zowe blogs', async () => {
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            json: jest.fn().mockResolvedValueOnce({
                items: [
                    {
                        link: 'https://docs.zowe.org/some',
                        title: 'Zowe Blog Title',
                        description: 'Zowe Blog Description',
                        content: 'Zowe Blog Content',
                    },
                ],
            }),
        });

        const wrapper = mount(<BlogContainer user="user" url="https://docs.zowe.org/some" title="title" />);

        await wrapper.update();

        expect(wrapper.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        expect(wrapper.find('BlogTile').exists()).toEqual(true);

        global.fetch.mockRestore();
    });

    it('should fetch data and render blog correctly', async () => {
        const mockFetch = jest.spyOn(global, 'fetch');
        mockFetch.mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce('<div class="shortdesc"><h1 class="title">Blog content</h1></div>'),
        });

        const wrapper = shallow(<BlogContainer user="user" url="https://example.com/hello" title="title" />);
        expect(wrapper.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        // Restore the original fetch function
        mockFetch.mockRestore();
    });

    it('should use description from diffeent element', async () => {
        const mockFetch = jest.spyOn(global, 'fetch');
        mockFetch.mockResolvedValueOnce({
            text: jest.fn().mockResolvedValueOnce('<div class="p"><h1 class="title">Blog content</h1></div>'),
        });

        const wrapper = shallow(<BlogContainer user="user" url="https://example.com/hello" title="title" />);
        expect(wrapper.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        // Restore the original fetch function
        mockFetch.mockRestore();
    });

    it('should render multiple medium blogs', async () => {
        const myBlogData = {
            items: [{ link: 'https://medium.com/blog1' }, { link: 'https:///medium.com/blog2' }],
        };
        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            json: jest.fn().mockResolvedValueOnce(myBlogData),
        });

        const useStateSpy = jest.spyOn(React, 'useState');
        const setMyBlog = jest.fn();
        useStateSpy.mockImplementation((init) => [init, setMyBlog]);

        const blogContainer = mount(<BlogContainer user="user" url="https://medium.com/some/medium" title="title" />);

        blogContainer.update();

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(true);

        // Clean up mocks
        global.fetch.mockRestore();
        useStateSpy.mockRestore();
    });

    it('should not render a blog when items do not contain matching URLs', async () => {
        const myBlogData = {
            items: [{ link: 'https://someother.com/blog1' }],
        };

        jest.spyOn(global, 'fetch').mockResolvedValueOnce({
            json: jest.fn().mockResolvedValueOnce(myBlogData),
        });

        const setMyBlog = jest.fn();
        jest.spyOn(React, 'useState').mockImplementation((init) => [init, setMyBlog]);

        const blogContainer = mount(<BlogContainer user="user" url="https://medium.com/some/medium" title="title" />);

        expect(blogContainer.find('[data-testid="medium-blog-container"]').exists()).toEqual(true);

        expect(blogContainer.find('BlogTile').exists()).toEqual(false);

        // Clean up mocks
        global.fetch.mockRestore();
        React.useState.mockRestore();
    });

    it('should handle errors during data fetching inside fetchDataEffect and return null', async () => {
        jest.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Fetch error'));

        jest.spyOn(console, 'error').mockImplementation(() => {});

        const setMyBlog = jest.fn();
        jest.spyOn(React, 'useState').mockImplementation((init) => [init, setMyBlog]);

        const blogContainer = mount(<BlogContainer user="user" url="https://example.com" title="title" />);
        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(true);

        expect(blogContainer.find('BlogTile').exists()).toEqual(true);
        expect(blogContainer.find('.blog-description').exists()).toEqual(false);

        // Clean up mocks
        global.fetch.mockRestore();
        React.useState.mockRestore();
    });

    it('should handle errors during data fetching when url is medium', async () => {
        jest.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Fetch error'));

        jest.spyOn(console, 'error').mockImplementation(() => {});

        const setMyBlog = jest.fn();
        jest.spyOn(React, 'useState').mockImplementation((init) => [init, setMyBlog]);

        const blogContainer = mount(<BlogContainer user="user" url="https://medium.com" title="title" />);
        expect(blogContainer.find('[data-testid="tech-blog-container"]').exists()).toEqual(false);

        expect(blogContainer.find('BlogTile').exists()).toEqual(false);

        // Clean up mocks
        global.fetch.mockRestore();
        React.useState.mockRestore();
    });
});
