/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import BlogTile from './BlogTile';

export default function BlogContainer({ user, url, title }) {
    const RSSUrl = `https://medium.com/feed/@${user}`;
    const rss2json = `https://api.rss2json.com/v1/api.json?rss_url=${RSSUrl}`;
    const [myBlog, setMyBlog] = useState([]);

    const fetchData = async () => {
        try {
            const res = await fetch(url);
            const data = await res.text();

            const parser = new DOMParser();
            const doc = parser.parseFromString(data, 'text/html');
            const divs = doc.querySelector('.linklist.relatedlinks');
            if (divs) {
                divs.parentNode.removeChild(divs);
            }

            let content = doc.querySelector('.shortdesc');
            if (!content?.textContent) {
                content = doc.querySelector('.p');
            }
            const tutorialTitle = doc.querySelector('h1.title');
            const blogTitle = tutorialTitle?.textContent;
            const blogContent = content?.textContent;

            const blogData = {
                content: blogContent,
                description: blogContent,
                title: blogTitle,
                link: url,
            };

            setMyBlog(blogData);
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error('Error fetching data:', error);
            return null;
        }
    };

    useEffect(() => {
        const fetchDataEffect = async () => {
            if (!url?.includes('medium.com') && !url?.includes('docs.zowe.org')) {
                await fetchData();
            } else if (url?.includes('docs.zowe.org')) {
                const blogData = {
                    content: '',
                    description: `Tutorial from the Zowe documentation related to ${title}`,
                    title,
                    link: url,
                };
                setMyBlog(blogData);
            } else {
                try {
                    const res = await fetch(rss2json);
                    const data = await res.json();
                    setMyBlog(data);
                } catch (error) {
                    // eslint-disable-next-line no-console
                    console.error('Error fetching data:', error);
                    return null;
                }
            }
        };

        fetchDataEffect();
    }, [rss2json]);

    function displayBlogs() {
        if (myBlog?.items) {
            const correctBlog = myBlog.items.find((blog) => blog?.link.includes(url));
            return correctBlog && <BlogTile blogData={correctBlog} />;
        }
        const blogData = {
            content: '',
            description: 'Blog preview not available',
            title: url,
            link: url,
        };
        return <BlogTile blogData={blogData} />;
    }
    if (url?.includes('medium.com')) {
        return (
            <div data-testid="medium-blog-container" className="BlogsContainer">
                {displayBlogs()}
            </div>
        );
    }

    return (
        myBlog && (
            <div data-testid="tech-blog-container" className="BlogsContainer">
                <BlogTile blogData={myBlog} />
            </div>
        )
    );
}

BlogContainer.propTypes = {
    url: PropTypes.shape({
        includes: PropTypes.func.isRequired,
    }).isRequired,
    user: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
};
