"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""
import unittest
import yaml
from unittest.mock import patch, mock_open
from src.config import ConfigLoader  # Use absolute import


class TestConfigLoader(unittest.TestCase):

    @patch("builtins.open", new_callable=mock_open, read_data="instance:\n  scheme: http\n  port: 8080\n")
    def test_load_config_valid_yaml(self, _mock_file):
        with patch("yaml.safe_load", return_value={"instance": {"scheme": "http", "port": 8080}}):
            config_loader = ConfigLoader("dummy.yml")
            self.assertEqual(config_loader.config["instance"]["scheme"], "http")
            self.assertEqual(config_loader.config["instance"]["port"], 8080)

    @patch("builtins.open", side_effect=FileNotFoundError)
    def test_load_config_file_not_found(self, _mock_file):
        with self.assertLogs("src.config", level="ERROR") as log:
            config_loader = ConfigLoader("dummy.yml")
            self.assertEqual(config_loader.config, {})
            self.assertIn("Configuration file not found.", log.output[0])

    @patch("builtins.open", new_callable=mock_open, read_data="invalid_yaml: [")
    @patch("yaml.safe_load", side_effect=yaml.YAMLError)
    def test_load_config_invalid_yaml(self, _mock_file, _mock_yaml_load):
        with self.assertLogs("src.config", level="ERROR") as log:
            config_loader = ConfigLoader("dummy.yml")
            self.assertEqual(config_loader.config, {})
            self.assertTrue(any("Error parsing YAML file" in msg for msg in log.output))


if __name__ == '__main__':
    unittest.main()
