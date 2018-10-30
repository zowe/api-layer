# Supported Java Versions on z/OS

## Context and Problem Statement

Our target platform is IBM Java on z/OS. There are several versions of Java and we need to decide what versions will be supported.

## Decision

Options:
  1. Support multiple versions of Java that are supported by IBM (Java 7 and Java 8) and all addressing modes
    * Pros: 
      * Customer does not need to install Java 8 if they have only Java 7
        * But support for Java 7 will end in September 2019 and there is other software that requires Java 8
      * 31-bit Java has lower memory requirements
    * Cons:
      * We cannnot use features from Java 8
      * We need to do testing on multiple version of Java
      * 31-bit native code will need to be coded and supported if we will need some in future
  2. Support only Java 8 64-bit
    * Cons:
      * There can be some customers that will have to install Java 8 64-bit because they are using different one
    * Pros:
      * Supported until April 2022
      * Java 8 64-bit can be used for applications that have large memory requirements
      * Only one version that needs to be supported and used for testing
      
The pros of #2 are bigger than the cons because installation of Java on z/OS is not difficult 

## Consequences 

We are using Java 8 64-bit as the target platform on z/OS. It needs to be listed in requirements of our product.

Java SDK 8 should be used for building of our software. We can use Oracle Java on workstations and build machine (Jenkins). 
Oracle will not support Java 8 in 2019 for free. We can continue using Oracle Java 8 for local development and building. 
We will not get public updates after January 2019. Since we are not using it in production, it should not be a big deal. 
In case of troubles, we can use IBM Java 8 SDK for Windows (development), Mac OS (development), and Linux (builds). 
This will be supported until April 2022. 

We need to care about support for IBM’s Java vNext which will correspond to Oracle Java 11 (LTS).
Oracle’s Java 11 will be GA at September 2018. IBM has not published any dates yet. We have a plenty of time before Java 8 will 
not be supported but we should plan for estimating the size and benefits of the upgrade when IBM Java vNext will be closer to GA. 

### Links:
  
   * IBM support dates - https://developer.ibm.com/javasdk/support/lifecycle/
   * Oracle support datas - http://www.oracle.com/technetwork/java/javase/eol-135779.html
   * IBM Java vNext - https://www.ibm.com/developerworks/community/wikis/home?lang=en#!/wiki/W0f473c0e23e2_435b_9c7d_7f4de7f136a4
