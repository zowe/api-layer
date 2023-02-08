# Zowe CLI Sample Plug-in

This repository contains a sample Zowe CLI plug-in that adheres to the contribution guidelines for the project. Use this project and the associated tutorials as a starting point for creating Zowe CLI plug-ins.

- [Zowe CLI Sample Plug-in](#zowe-cli-sample-plug-in)
  - [Why Create a Zowe CLI Plug-in?](#why-create-a-zowe-cli-plug-in)
  - [Tutorials, Documentation, and Guidelines](#tutorials-documentation-and-guidelines)
    - [Tutorials](#tutorials)
    - [Contribution Guidelines](#contribution-guidelines)
    - [Imperative CLI Framework Documentation](#imperative-cli-framework-documentation)
    - [Jenkinsfile Guidelines](#jenkinsfile-guidelines)
  - [Prerequisites](#prerequisites)
  - [Create a Local Development Space](#create-a-local-development-space)
    - [Clone zowe-cli-sample-plugin and Build From Source](#clone-zowe-cli-sample-plugin-and-build-from-source)
    - [Run the Automated Tests](#run-the-automated-tests)
    - [Install the zowe-cli-sample-plugin to Zowe CLI](#install-the-zowe-cli-sample-plugin-to-zowe-cli)

## Why Create a Zowe CLI Plug-in?

You might want to create a Zowe CLI plug-in to accomplish the following:

* Provide new scriptable functionality for yourself, your organization, or to a broader community.
* Make use of Zowe CLI infrastructure (profiles and programmatic APIs).
* Participate in the Zowe CLI community space.

## Tutorials, Documentation, and Guidelines

We also provide the following tutorials, guidelines, and documentation to assist you during development:

### Tutorials

To learn about how to work with this sample plug-in, build new commands, or build a new Zowe CLI plug-in, see [Develop for Zowe CLI](https://zowe.github.io/docs-site/active-development/extend/extend-cli/cli-devTutorials.html).

### Contribution Guidelines

The Zowe CLI [contribution guidelines](CONTRIBUTING.md) contain standards and conventions for developing Zowe CLI plug-ins. 

The guidelines contain critical information about working with the code, running/writing/maintaining automated tests, developing consistent syntax in your plug-in, and ensuring that your plug-in integrates with Zowe CLI properly.

### Imperative CLI Framework Documentation

[Imperative CLI Framework](https://github.com/zowe/imperative/wiki) documentation is a key source of information to learn about the features of Imperative CLI Framework (the code framework that you use to build plug-ins for Zowe CLI). Refer to these documents during development.

### Jenkinsfile Guidelines

Reference the [Jenkinsfile Guidelines](CICD-TEMPLATE.md) for information about setting up and maintaining automated testing/deployment for your plug-in with Jenkins automation server.

## Prerequisites

Before you work with the Zowe CLI sample plug-in, [install Zowe CLI globally.](https://docs.zowe.org/active-development/user-guide/cli-installcli.html)

## Create a Local Development Space

To create your development space, clone and build the Zowe CLI sample plug-in from source.

Create a local development folder named `zowe-tutorial`. You will clone and build all projects in this folder.

Clone the repositories into your development folder to match the following structure:
```
zowe-tutorial
└── zowe-cli-sample-plugin
```

### Clone zowe-cli-sample-plugin and Build From Source

See [setup](docs/tutorials/Setup.md).

### Run the Automated Tests

**Note:** If you don't have access to a z/OSMF instance at your site, run `npm run server:start` to launch a mock server at http://localhost:3000.

1. `cd __tests__/__resources__/properties`
2. Copy `example_properties.yaml` to `custom_properties.yaml`.
3. Edit the properties within `custom_properties.yaml` to contain valid system information for your site.
4. `cd` to your `zowe-cli-sample-plugin` folder
5. `npm run test`

### Install the zowe-cli-sample-plugin to Zowe CLI

This process assumes that you already installed Zowe CLI on your PC in the previous steps.

1. `cd` to your `zowe-tutorial` folder.
2. `zowe plugins install ./zowe-cli-sample-plugin`
3. `zowe zowe-cli-sample`
   You should see help text displayed if the installation was successful.
