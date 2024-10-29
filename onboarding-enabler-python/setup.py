"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""
from setuptools import setup

with open("README.md", "r") as read_me:
    long_description = read_me.read()

with open("requirements.txt", "r") as requirements:
    required = requirements.read().splitlines()

setup(
    name="zowe/apiml-onboarding-enabler-python",
    version="3.0.0",
    description="Python enabler for Zowe API Mediation Layer",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/zowe/api-layer",
    author="",
    license="EPL-2.0",
    classifiers=[
        "License :: OSI Approved :: Eclipse Public License 2.0 (EPL-2.0)",
        "Programming Language :: Python :: 3.10",
        "Operating System :: OS Independent",
    ],
    install_requires=required,
    python_requires=">=3.10"
)
