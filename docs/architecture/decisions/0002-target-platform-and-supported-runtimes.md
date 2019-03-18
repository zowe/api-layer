# 2. Target platform and supported runtimes

Date: 2018-06-08 (Updated: 2019-02-14)

## Status

Accepted 

## Context

Our target platform is IBM Java on z/OS. There are several versions of Java and we need to decide what versions will be supported.

Options:
  1. Support multiple versions of Java that are supported by IBM (Java 7 and Java 8) and all addressing modes
      * Pros: 
         * Customer does not need to install Java 8 if they have only Java 7
           * But support for Java 7 will end in September 2019 and there is other software that requires Java 8
         * 31-bit Java has lower memory requirements
      * Cons:
         * We cannot use features from Java 8
         * We need to do testing on multiple version of Java
         * 31-bit native code will need to be coded and supported if we will need some in future

  2. Support only Java 8 64-bit
      * Cons:
         * There can be some customers that will have to install Java 8 64-bit because they are using different one
      * Pros:
         * Supported until April 2022
         * Java 8 64-bit can be used for applications that have large memory requirements
         * Only one version that needs to be supported and used for testing
      
The pros of #2 are bigger than the cons because installation of Java on z/OS is not difficult.

## Decision

We have decided to use Java 8 as the runtime for the production runtime on z/OS, and build and development environments as well.

## Consequences

We are using Java 8 64-bit as the target platform on z/OS. It needs to be listed in requirements of our product.

Java SDK 8 should be used for building of our software. We can use Oracle Java on workstations and build machine (Jenkins). 
Oracle will not support Java 8 in 2019 for free. We can continue using Oracle Java 8 for local development and building. 
We will not get public updates after January 2019. Since we are not using it in production, it should not be a big deal. 
In case of troubles, we have several options:
- use IBM Java 8 SDK for Windows (development), and Linux (builds). This will be supported until April 2022. 
- use Amazon Corretto 8 (No-cost, multiplatform, production-ready distribution of OpenJDK) - https://aws.amazon.com/corretto/. It supports Windows, Mac, and Amazon Linux

We need to care about support for IBM’s Java vNext which will correspond to Oracle Java 11 (LTS).
Oracle’s Java 11 will be GA at September 2018. IBM has not published any dates yet. We have a plenty of time before Java 8 will 
not be supported but we should plan for estimating the size and benefits of the upgrade when IBM Java vNext will be closer to GA. 
