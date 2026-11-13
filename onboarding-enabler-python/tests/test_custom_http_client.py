"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""
"""
Copyright 2026 Contributors to the Zowe Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import pytest
from unittest.mock import patch, MagicMock, AsyncMock
import ssl
import yaml
from src.zowe_apiml_onboarding_enabler_python.custom_http_client import HttpResponse, HttpClient
from src.zowe_apiml_onboarding_enabler_python.registration import PythonEnabler

mock_yaml_content = """
eureka:
  ssl: true
  host: localhost
  ipAddress: 127.0.0.1
  port: 10011

instance:
  app: pythonservice

ssl:
  certificate: "../keystore/service/service.cer"
  keystore: "../keystore/service/service.key"
  caFile: "../keystore/service/service.pem"
  keyPassword: "password"
"""

@pytest.fixture
def mock_config_loader():
    mocked_yaml = yaml.safe_load(mock_yaml_content)

    def mock_init(self, *args, **kwargs):
        self.config = mocked_yaml

    with patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.ConfigLoader.__init__", mock_init), \
        patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.config_loader.config", mocked_yaml):
        yield

@pytest.fixture
def mock_ssl_context():
    mock_ssl = MagicMock(spec=ssl.SSLContext)

    with patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.ssl.create_default_context", return_value=mock_ssl):
        yield mock_ssl


@pytest.fixture
def mock_aiohttp_session():
    with patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.aiohttp.ClientSession") as mock_session_cls:
        mock_session = MagicMock()
        mock_response_context = MagicMock()
        mock_response = MagicMock()

        mock_response.status = 200
        mock_response.text = AsyncMock(return_value="success")
        mock_response.headers = {'Content-Type': 'application/xml'}

        mock_response_context.__aenter__.return_value = mock_response
        mock_session.request.return_value = mock_response_context
        mock_session_cls.return_value.__aenter__.return_value = mock_session

        yield mock_session

@pytest.mark.asyncio
async def test_http_client_ssl_setup(mock_config_loader, mock_ssl_context, mock_aiohttp_session):
    client = HttpClient()
    request_mock = MagicMock()
    request_mock.url = "https://example.com"

    response = await client.urlopen(request_mock, data=None)

    mock_ssl_context.load_cert_chain.assert_called_once_with(
        certfile="../keystore/service/service.cer",
        keyfile="../keystore/service/service.key",
        password="password"
    )

    assert isinstance(response, HttpResponse)
    assert response.raw_response.status == 200
    assert response.body_text == "success"

    mock_aiohttp_session.request.assert_called_once_with(
        method="GET", url="https://example.com", data=None,
        ssl=mock_ssl_context, headers={"Content-Type": "application/xml"}
    )


@pytest.mark.asyncio
async def test_enabler_uses_custom_config_context_for_requests(
    tmp_path, mock_ssl_context, mock_aiohttp_session
):
    config_file = tmp_path / "custom-config.yml"
    config_file.write_text(mock_yaml_content)
    with patch(
        "src.zowe_apiml_onboarding_enabler_python.registration.http_client.set_http_client"
    ) as set_client, patch(
        "src.zowe_apiml_onboarding_enabler_python.custom_http_client.config_loader.config", {}
    ), patch(
        "src.zowe_apiml_onboarding_enabler_python.custom_http_client.ssl.create_default_context",
        return_value=mock_ssl_context,
    ) as create_context:
        enabler = PythonEnabler(config_file=str(config_file))
        client = set_client.call_args.args[0]

        await client.urlopen(enabler.discovery_service)
        await client.urlopen(enabler.discovery_service)

    create_context.assert_called_once_with(
        ssl.Purpose.SERVER_AUTH, cafile="../keystore/service/service.pem"
    )
    mock_ssl_context.load_cert_chain.assert_called_once_with(
        certfile="../keystore/service/service.cer",
        keyfile="../keystore/service/service.key",
        password="password",
    )
    assert enabler.ssl_context is mock_ssl_context
    assert all(
        call.kwargs["ssl"] is mock_ssl_context
        for call in mock_aiohttp_session.request.call_args_list
    )


@pytest.mark.asyncio
async def test_http_request_does_not_require_certificates(mock_aiohttp_session):
    with patch(
        "src.zowe_apiml_onboarding_enabler_python.custom_http_client.config_loader.config", {}
    ):
        response = await HttpClient().urlopen("http://example.com")

    assert response.body_text == "success"
    assert mock_aiohttp_session.request.call_args.kwargs["ssl"] is None


@pytest.mark.asyncio
async def test_https_request_rejects_missing_certificates(mock_aiohttp_session):
    with patch(
        "src.zowe_apiml_onboarding_enabler_python.custom_http_client.config_loader.config", {}
    ), pytest.raises(ValueError, match="SSL configuration is incomplete"):
        await HttpClient().urlopen("https://example.com")

    mock_aiohttp_session.request.assert_not_called()

def test_http_response():
    raw_response_mock = MagicMock()
    raw_response_mock.status = 200
    response = HttpResponse(raw_response_mock, "success")

    assert response.raw_response.status == 200
    assert response.body_text == "success"
