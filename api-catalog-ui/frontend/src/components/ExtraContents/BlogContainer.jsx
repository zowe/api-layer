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

function BlogContainer({ mediumUser, mediumBlogUrl }) {
    if (!isValidUrl(mediumBlogUrl)) {
        return null;
    }
    if (mediumBlogUrl.includes('medium.com') && !mediumUser) {
        return null;
    }
    const rss2json = `https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Fmedium.com%2Ffeed%2F%40${mediumUser}`;
    const [myBlog, setMyBlog] = useState([]);
    const fetchData = async () => {
        fetch(mediumBlogUrl)
            .then((res) => res.text())
            .then((data) => {
                const parser = new DOMParser();
                const doc = parser.parseFromString(data, 'text/html');
                const divs = doc.querySelector('.linklist.relatedlinks');
                if (divs) {
                    divs.parentNode.removeChild(divs);
                }
                const content = doc.querySelector('.shortdesc');
                const title = doc.querySelector('h1.title');
                const blogTitle = title.textContent;
                const blogContent = content.textContent;
                const blogData = {
                    content: blogContent,
                    description: blogContent,
                    title: blogTitle,
                    link: mediumBlogUrl,
                };
                setMyBlog(blogData);
            });
    };
    useEffect(() => {
        if (!mediumBlogUrl.includes('medium.com')) {
            fetchData();
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
            const correctBlog = myBlog.items.find((blog) => blog?.link.includes(mediumBlogUrl));
            return correctBlog && <BlogTile blogData={correctBlog} />;
        }
    }
    if (mediumBlogUrl.includes('medium.com')) {
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
