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
import BlogTile from './BlogTile';

describe('>>> BlogTile component tests', () => {
    let props;
    beforeEach(() => {
        process.env.REACT_APP_API_PORTAL = false;
        props = {
            blogData: {
                title: 'title',
                link: 'link',
                thumbnail: 'img',
                description: 'desc',
                pubDate: '123343',
                author: 'author',
            },
        };
    });
    it('should render blog tile', () => {
        const blogTile = shallow(<BlogTile blogData={props.blogData} />);
        expect(blogTile.find('[data-testid="blogs-image"]').exists()).toEqual(true);
        expect(blogTile.find('[data-testid="blog-title"]').first().prop('children')).toEqual('title');
        expect(blogTile.find('[data-testid="blog-description"]').exists()).toEqual(true);
        expect(blogTile.find('[data-testid="author"]').first().prop('children')).toEqual('author');
        expect(blogTile.find('[data-testid="pub-date"]').first().prop('children')).toEqual('Published: 123343');
    });

    it('should truncate text with space', () => {
        props.blogData.title =
            'long  title to text that word are not truncated in the middle eheheqwdqwdwqdqwdwqdwqdqw dwqdwqdwqdq dwqdqwdwq dwqdwqdwqdqwdwqdwqdqwdqwdqw ';
        const blogTile = shallow(<BlogTile blogData={props.blogData} />);
        expect(blogTile.find('[data-testid="blog-title"]').first().prop('children')).toEqual(
            'long  title to text that word are not truncated in the'
        );
    });

    it('should truncate if no space', () => {
        props.blogData.title =
            'ThisisaverylongstringwithnospacesanditisusedtodemonstratetheconceptoflastSpaceIndexwithintherange.';
        const blogTile = shallow(<BlogTile blogData={props.blogData} />);
        expect(blogTile.find('[data-testid="blog-title"]').first().prop('children')).toEqual(
            'Thisisaverylongstringwithnospacesanditisusedtodemonstratethe...'
        );
    });

    it('should render non-truncated text in BlogTile component when length is less than maxLength', () => {
        const shortTitle = 'short title';
        props.blogData.title = shortTitle;

        const blogTile = shallow(<BlogTile blogData={props.blogData} />);
        const renderedText = blogTile.find('[data-testid="blog-title"]').first().prop('children');

        expect(renderedText).toEqual(shortTitle);
    });
});
