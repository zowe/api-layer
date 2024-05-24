/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { Typography } from '@material-ui/core';
import PropTypes from 'prop-types';

export default function BlogTile(props) {
    const { title, link, thumbnail, description, pubDate, author } = props.blogData;
    function cleanTitle(checkTitle) {
        return checkTitle?.replace('amp;', '');
    }

    function truncateText(text, maxLength) {
        if (text?.length > maxLength) {
            const lastSpaceIndex = text?.lastIndexOf(' ', maxLength);

            if (lastSpaceIndex !== -1) {
                return `${text?.slice(0, lastSpaceIndex)}`;
            }
            return `${text?.slice(0, maxLength)}...`;
        }
        return text;
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
                        alt={truncateText(cleanTitle(title), 60)}
                    />
                )}
                {title && (
                    <h3 data-testid="blog-title" className="blog-title">
                        {truncateText(cleanTitle(title), 60)}
                    </h3>
                )}
                {description && (
                    <Typography data-testid="blog-description" className="blog-description">{`${truncateText(
                        toText(description),
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

BlogTile.propTypes = {
    blogData: PropTypes.oneOfType([
        PropTypes.shape({
            author: PropTypes.string,
            title: PropTypes.string.isRequired,
            link: PropTypes.string.isRequired,
            thumbnail: PropTypes.string,
            description: PropTypes.string.isRequired,
            pubDate: PropTypes.string,
        }),
        PropTypes.arrayOf(
            PropTypes.shape({
                author: PropTypes.string,
                title: PropTypes.string.isRequired,
                link: PropTypes.string.isRequired,
                thumbnail: PropTypes.string,
                description: PropTypes.string.isRequired,
                pubDate: PropTypes.string,
            })
        ),
    ]).isRequired,
};
