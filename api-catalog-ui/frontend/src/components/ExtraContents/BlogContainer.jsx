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
import BlogTile from './BlogTile';
import { isValidUrl } from '../../utils/utilFunctions';

function BlogContainer({ user, url, title }) {
    if (!isValidUrl(url)) {
        return null;
    }
    if (url.includes('medium.com') && !user) {
        return null;
    }
    const rss2json = `https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Fmedium.com%2Ffeed%2F%40${user}`;
    const [myBlog, setMyBlog] = useState([]);
    const fetchData = async () => {
        fetch(url)
            .then((res) => res.text())
            .then((data) => {
                const parser = new DOMParser();
                const doc = parser.parseFromString(data, 'text/html');
                const divs = doc.querySelector('.linklist.relatedlinks');
                if (divs) {
                    divs.parentNode.removeChild(divs);
                }
                const content = doc.querySelector('.shortdesc');
                const tutorialTitle = doc.querySelector('h1.title');
                const blogTitle = tutorialTitle.textContent;
                const blogContent = content.textContent;
                const blogData = {
                    content: blogContent,
                    description: blogContent,
                    title: blogTitle,
                    link: url,
                };
                setMyBlog(blogData);
            });
    };
    useEffect(() => {
        if (!url.includes('medium.com') && !url.includes('docs.zowe.org')) {
            fetchData();
        } else if (url.includes('docs.zowe.org')) {
            const blogData = {
                content: '',
                description: `Tutorial from the Zowe documentation related to ${user}`,
                title,
                link: url,
            };
            setMyBlog(blogData);
        } else {
            fetch(rss2json)
                .then((res) => res.json())
                .then((data) => {
                    setMyBlog(data);
                });
        }
    }, [rss2json]);

    function displayBlogs() {
        if (myBlog?.items) {
            const correctBlog = myBlog.items.find((blog) => blog?.link.includes(url));
            return correctBlog && <BlogTile blogData={correctBlog} />;
        }
    }
    if (url.includes('medium.com')) {
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

export default BlogContainer;
