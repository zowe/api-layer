/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import '../ServiceTab/_serviceTab.scss';
import { Typography } from '@material-ui/core';

function BlogTile(props) {
    const { title, link, thumbnail, description, pubDate, author } = props.blogData;
    // eslint-disable-next-line no-console
    console.log(props.blogData);
    function cleanTitle(checkTitle) {
        // eslint-disable-next-line no-console
        console.log(checkTitle);
        return checkTitle?.replace('amp;', '');
    }

    function truncateText(text, start, len) {
        return text?.length > len ? text?.slice(start, len) : text;
    }

    function toText(block) {
        const tag = document.createElement('div');
        tag.innerHTML = block;
        return tag.innerText;
    }

    function convertDate(date) {
        const dateArray = date?.slice(0, 10).split('-');
        const year = dateArray?.shift();
        dateArray?.push(year);
        // eslint-disable-next-line no-console
        console.log(date);
        return `Published: ${dateArray?.join('/')}`;
    }
    function blogPost() {
        return (
            <a className="blog_content_link" target="_blank" rel="noopener noreferrer" href={`${link}`}>
                {thumbnail && (
                    <img
                        data-testid="blogs-image"
                        src={`${thumbnail}`}
                        className="blogs-image"
                        alt={truncateText(cleanTitle(title), 0, 60)}
                    />
                )}
                {title && (
                    <h3 data-testid="blog-title" className="blog-title">
                        {truncateText(cleanTitle(title), 0, 60)}
                    </h3>
                )}
                {description && (
                    <Typography data-testid="blog-description" className="blog-description">{`${truncateText(
                        toText(description),
                        0,
                        180
                    )}...`}</Typography>
                )}
                <br />
                <h4 data-testid="author" className="author">
                    {author}
                </h4>
                {pubDate && (
                    <Typography data-testid="pub-date" className="pub-date" variant="subtitle2">
                        {convertDate(pubDate)}
                    </Typography>
                )}
            </a>
        );
    }

    return <div className="PostContainer">{blogPost()}</div>;
}

export default BlogTile;
