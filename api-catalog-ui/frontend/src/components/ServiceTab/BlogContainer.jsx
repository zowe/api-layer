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
import './_serviceTab.scss';

function BlogContainer({ mediumUser, mediumBlogUrl }) {
    // const rss2json =
    //     'https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Fmedium.com%2Ffeed%2F%40joshuagauthreaux';
    if (!isValidUrl(mediumBlogUrl) || !mediumUser) {
        return null;
    }
    const rss2json = `https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Fmedium.com%2Ffeed%2F%40${mediumUser}`;
    const [myBlog, setMyBlog] = useState([]);

    useEffect(() => {
        fetch(rss2json)
            .then((res) => res.json())
            .then((data) => {
                setMyBlog(data);
                // eslint-disable-next-line no-console
                console.log(data);
            });
    }, [rss2json]);

    function displayBlogs() {
        // eslint-disable-next-line no-console
        console.log(myBlog);
        if (myBlog?.items) {
            const correctBlog = myBlog.items.find((blog) => blog?.link.includes(mediumBlogUrl));
            return correctBlog && <BlogTile blogData={correctBlog} />;
        }
    }

    return <div className="BlogsContainer">{displayBlogs()}</div>;
}

export default BlogContainer;
