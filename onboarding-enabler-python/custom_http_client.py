"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""

import json
import ssl
from typing import Union

import aiohttp
import py_eureka_client.http_client as http_client


class MyHttpResponse(http_client.HttpResponse):
    def __init__(self, raw_response, body_text):
        super().__init__(raw_response)
        self.raw_response = raw_response
        self._body_text = body_text  # Cache body text when it's read

    @property
    def body_text(self):
        """Read the body text from `self.raw_response`."""
        return self._body_text


class MyHttpClient(http_client.HttpClient):
    async def urlopen(self, request: Union[str, http_client.HttpRequest] = None,
                      data: bytes = None, timeout: float = None) -> http_client.HttpResponse:
        # Determine the URL and method (POST or GET) based on the request and data
        url = request if isinstance(request, str) else request.url
        method = "POST" if data else "GET"

        headers = {
            'Content-Type': 'application/json' if method == "POST" else 'application/xml'
        }

        client_timeout = aiohttp.ClientTimeout(total=timeout) if timeout else None

        # Create an SSLContext using SSL material
        ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
        ssl_context.load_cert_chain(
            certfile='../keystore/localhost/localhost.keystore.cer',
            keyfile='../keystore/localhost/localhost.keystore.key',
            password='password'
        )

        async with aiohttp.ClientSession(timeout=client_timeout) as session:
            try:
                if data and isinstance(data, dict):
                    data = json.dumps(data)
                    data = data.encode('utf-8')
                if data:
                    print("Payload (data):", data.decode("utf-8"))
                async with session.request(
                    method=method,
                    url=url,
                    data=data,
                    ssl=ssl_context,
                    headers=headers
                ) as response:
                    content_type = response.headers.get('Content-Type', '').lower()
                    status_code = response.status

                    body_text = await response.text()
                    print(f"Status Code: {status_code}")
                    print(f"Content-Type: {content_type}")

                    # Accept 200 (OK), 201 (Created) or 204 (No Content) as valid status codes for successful
                    # registration
                    if status_code not in [200, 201, 204]:
                        raise ValueError(f"Unexpected status code: {status_code}. URL: {url} Method: {method}")

                    if 'application/xml' not in content_type and method == "GET":
                        raise ValueError("Received non-XML response from Eureka server.")
                    return MyHttpResponse(response, body_text)
            except aiohttp.ClientError as e:
                raise http_client.URLError(f"Error connecting to {url}: {e}")


# Set the custom HTTP client to override the one used in `py_eureka_client.http_client`
http_client.set_http_client(MyHttpClient())
