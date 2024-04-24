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
import { ReactComponent as CustomDoc } from '../../assets/images/ExternalLink.svg';

export default function BlogTile(props) {
    const { title, link, description, isMedia } = props.blogData;
    function cleanTitle(checkTitle) {
        return checkTitle?.replace('amp;', '');
    }

    function truncateText(text, maxLength) {
        if (text?.length > maxLength) {
            const lastSpaceIndex = text?.lastIndexOf(' ', maxLength);

            if (lastSpaceIndex !== -1) {
                return `${text?.slice(0, lastSpaceIndex)}...`;
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

    function blogPost() {
        return (
            <a
                className={`blog_content_link ${title ? '' : 'no_title'}`}
                target="_blank"
                rel="noopener noreferrer"
                href={`${link}`}
            >
                {title && (
                    <h3 data-testid="blog-title" className="blog-title">
                        {truncateText(cleanTitle(title), 130)}
                    </h3>
                )}
                {description && (
                    <Typography
                        data-testid="blog-description"
                        className={`blog-description ${title ? '' : 'no_title'}`}
                    >{`${truncateText(toText(description), 800)}`}</Typography>
                )}
                <h4 data-testid="blog-learn" className="blog-learn">
                    {`Learn more ${isMedia ? '' : 'from TechDocs'}`}
                    <CustomDoc />
                </h4>
            </a>
        );
    }

    return blogPost();
}

BlogTile.defaultProps = {
    isMedia: false,
};

BlogTile.propTypes = {
    blogData: PropTypes.oneOfType([
        PropTypes.shape({
            author: PropTypes.string,
            title: PropTypes.string,
            link: PropTypes.string.isRequired,
            thumbnail: PropTypes.string,
            description: PropTypes.string.isRequired,
            pubDate: PropTypes.string,
            isMedia: PropTypes.bool,
        }),
        PropTypes.arrayOf(
            PropTypes.shape({
                author: PropTypes.string,
                title: PropTypes.string,
                link: PropTypes.string.isRequired,
                thumbnail: PropTypes.string,
                description: PropTypes.string.isRequired,
                pubDate: PropTypes.string,
                isMedia: PropTypes.bool,
            })
        ),
    ]).isRequired,
};
