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
import yaml
from unittest.mock import patch, mock_open
from src.zowe_apiml_onboarding_enabler_python.config import ConfigLoader


@pytest.fixture
def mock_valid_yaml():
    """Mock valid YAML file content."""
    return "instance:\n  scheme: http\n  port: 8080\n"


@pytest.fixture
def mock_invalid_yaml():
    """Mock invalid YAML file content."""
    return "invalid_yaml: ["  # Malformed YAML


@patch("builtins.open", new_callable=mock_open, read_data="instance:\n  scheme: http\n  port: 8080\n")
@patch("yaml.safe_load", return_value={"instance": {"scheme": "http", "port": 8080}})
def test_load_config_valid_yaml(mock_yaml_load, mock_file):
    """Test loading a valid YAML configuration."""
    config_loader = ConfigLoader("dummy.yml")
    assert config_loader.config["instance"]["scheme"] == "http"
    assert config_loader.config["instance"]["port"] == 8080


@patch("builtins.open", side_effect=FileNotFoundError)
def test_load_config_file_not_found(mock_file, caplog):
    """Test handling when the configuration file is missing."""
    with caplog.at_level("ERROR"):
        config_loader = ConfigLoader("dummy.yml")
        assert config_loader.config == {}
        assert "Configuration file not found." in caplog.text


@patch("builtins.open", new_callable=mock_open, read_data="invalid_yaml: [")
@patch("yaml.safe_load", side_effect=yaml.YAMLError)
def test_load_config_invalid_yaml(mock_yaml_load, mock_file, caplog):
    """Test handling of invalid YAML parsing errors."""
    with caplog.at_level("ERROR"):
        config_loader = ConfigLoader("dummy.yml")
        assert config_loader.config == {}
        assert "Error parsing YAML file" in caplog.text
