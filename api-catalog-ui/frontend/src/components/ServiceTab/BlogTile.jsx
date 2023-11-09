/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
function BlogTile(props) {
    const { title, link, thumbnail, content, pubDate } = props.blogData;
    function cleanTitle(checkTitle) {
        return checkTitle.replace('amp;', '');
    }

    function truncateText(text, start, len) {
        return text.length > len ? text.slice(start, len) : text;
    }

    function toText(block) {
        const tag = document.createElement('div');
        tag.innerHTML = block;
        return tag.innerText;
    }

    function convertDate(date) {
        const dateArray = date.slice(0, 10).split('-');
        const year = dateArray.shift();
        dateArray.push(year);
        // eslint-disable-next-line no-console
        console.log(date);
        return `Published: ${dateArray.join('/')}`;
    }
    function blogPost() {
        return (
            <a target="_blank" rel="noopener noreferrer" href={`${link}`}>
                <div className="ImageContainer">
                    <img src={`${thumbnail}`} className="Image" alt={truncateText(cleanTitle(title), 0, 60)} />
                </div>
                <div className="TDContainer">
                    <h3>{truncateText(cleanTitle(title), 0, 60)}</h3>
                    <br />
                    <p>{`${truncateText(toText(content), 48, 300)}...`}</p>
                    <br />
                    <h4>{convertDate(pubDate)}</h4>
                </div>
            </a>
        );
    }

    return <div className="PostContainer">{blogPost()}</div>;
}

export default BlogTile;
