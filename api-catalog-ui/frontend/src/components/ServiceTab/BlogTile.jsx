/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import './_serviceTab.scss';
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
            <a target="_blank" rel="noopener noreferrer" href={`${link}`}>
                {thumbnail && (
                    <img src={`${thumbnail}`} className="blogs-image" alt={truncateText(cleanTitle(title), 0, 60)} />
                )}
                <h3>{truncateText(cleanTitle(title), 0, 60)}</h3>
                <br />
                <Typography>{`${truncateText(toText(description), 0, 300)}...`}</Typography>
                <br />
                <h4>{author}</h4>
                {pubDate && <h4>{convertDate(pubDate)}</h4>}
            </a>
        );
    }

    return <div className="PostContainer">{blogPost()}</div>;
}

export default BlogTile;
