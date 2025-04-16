"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""

import pytest
from src.zowe_apiml_onboarding_enabler_python.registration import PythonEnabler

@pytest.fixture
def mock_config_loader(mocker):
    """Fixture to mock ConfigLoader globally for all tests."""
    mock_loader = mocker.patch("src.zowe_apiml_onboarding_enabler_python.registration.ConfigLoader")
    mock_loader.return_value.config = {
        "eureka": {
            "ssl": False,
            "host": "localhost",
            "port": "10011",
            "servicePath": "/eureka"
        },
        "instance": {
            "scheme": "http",
            "hostName": "localhost",
            "ipAddr": "127.0.0.1",
            "port": 8080,
            "app": "TestApp"
        }
    }
    return mock_loader


@pytest.mark.usefixtures("mock_config_loader")
def test_python_enabler_register(mocker):
    """Test if PythonEnabler correctly registers with Eureka"""

    mock_eureka_init = mocker.patch("py_eureka_client.eureka_client.init")

    enabler = PythonEnabler()
    enabler.register()

    print(f"Call count: {mock_eureka_init.call_count}")

    mock_eureka_init.assert_called_once()
    kwargs = mock_eureka_init.call_args.kwargs
    assert kwargs["app_name"] == "TestApp"
    assert kwargs["instance_port"] == 8080
    assert "http://localhost:10011/eureka" in kwargs["eureka_server"]


@pytest.mark.usefixtures("mock_config_loader")
def test_python_enabler_register_failure(mocker):
    """Test if PythonEnabler handles registration failure correctly"""

    mocker.patch("py_eureka_client.eureka_client.init", side_effect=Exception("Test error"))

    mock_logger_error = mocker.patch("logging.Logger.error")

    enabler = PythonEnabler()
    enabler.register()

    print(f"Logger error calls: {mock_logger_error.call_args_list}")

    mock_logger_error.assert_any_call("Error during registration: Test error")


def test_python_enabler_unregister(mocker):
    """Test if PythonEnabler correctly unregisters from Eureka"""

    mock_eureka_stop = mocker.patch("py_eureka_client.eureka_client.stop")

    mock_logger_info = mocker.patch("logging.Logger.info")

    enabler = PythonEnabler()
    enabler.unregister()

    mock_eureka_stop.assert_called_once()
    mock_logger_info.assert_called_with("Service unregistered successfully.")
