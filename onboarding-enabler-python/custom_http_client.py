import json
import ssl
from typing import Union

import aiohttp
import py_eureka_client.http_client as http_client


# 1. A class inherited `py_eureka_client.http_client.HttpResponse`
class MyHttpResponse(http_client.HttpResponse):
    def __init__(self, raw_response, body_text):
        super().__init__(raw_response)
        self.raw_response = raw_response
        self._body_text = body_text  # Cache body text when it's read

    @property
    def body_text(self):
        """Read the body text from `self.raw_response`."""
        return self._body_text


# 2. A class inherited `py_eureka_client.http_client.HttpClient`.
class MyHttpClient(http_client.HttpClient):
    async def urlopen(self, request: Union[str, http_client.HttpRequest] = None,
                      data: bytes = None, timeout: float = None) -> http_client.HttpResponse:
        # Determine the URL and method (POST or GET) based on the request and data
        url = request if isinstance(request, str) else request.url
        method = "POST" if data else "GET"

        # Prepare the headers
        headers = {
            'Content-Type': 'application/json' if method == "POST" else 'application/xml'
        }

        # Serialize data to JSON if it's a POST request
        # if data and method == "POST":
        #     data = json.dumps(data)

        # Prepare the timeout setting
        client_timeout = aiohttp.ClientTimeout(total=timeout) if timeout else None

        # Create an SSLContext using your certificate
        ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
        ssl_context.load_cert_chain(
            certfile='../keystore/localhost/localhost.keystore.cer',
            keyfile='../keystore/localhost/localhost.keystore.key',
            password='password'  # Password for the key file if it's encrypted
        )

        async with aiohttp.ClientSession(timeout=client_timeout) as session:
            try:
                if data and isinstance(data, dict):
                    data = json.dumps(data)  # Convert dict to JSON string
                    data = data.encode('utf-8')  # Encode JSON string to bytes
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
                    # print("Response Body:")
                    # print(body_text)

                    # Accept 200 (OK) or 201 (Created) as valid status codes for successful registration
                    if status_code not in [200, 201]:
                        raise ValueError(f"Unexpected status code: {status_code}. URL: {url} Method: {method}")

                    if 'application/xml' not in content_type and method == "GET":
                        raise ValueError("Received non-XML response from Eureka server.")
                    return MyHttpResponse(response, body_text)
            except aiohttp.ClientError as e:
                raise http_client.URLError(f"Error connecting to {url}: {e}")


# 4. Set your class to `py_eureka_client.http_client`.
http_client.set_http_client(MyHttpClient())
