##Certificate validation tool

### Usage

java -jar certificate-analyser-<version>.jar --help
```
Usage: <main class> [-hl] [-kp[=<keyPasswd>]] [-tp[=<trustPasswd>]]
                    [-a=<keyAlias>] [-k=<keyStore>] [-kt=<keyStoreType>]
                    [-r=<remoteUrl>] [-t=<trustStore>] [-tt=<trustStoreType>]
  -a, --keyalias=<keyAlias>
                Alias under which this key is stored
  -h, --help    display a help message
  -k, --keystore=<keyStore>
                Path to keystore file or keyring. When using keyring, pass
                                  -Djava.protocol.handler.pkgs=com.ibm.crypto.provider in
                                  command line.
      -kp, --keypasswd[=<keyPasswd>]
                Keystore password
      -kt, --keystoretype=<keyStoreType>
                Keystore type, default is PKCS12
  -l, --local   Do SSL handshake on localhost
  -r, --remoteurl=<remoteUrl>
                URL of service to be verified
  -t, --truststore=<trustStore>
                Path to truststore file or keyring
      -tp, --trustpasswd[=<trustPasswd>]
                Truststore password
      -tt, --truststoretype=<trustStoreType>
                Truststore type, default is PKCS12
```
*NOTE*

keypasswd - if you specify this parameter without a value(e.g. java -jar <file.jar> --keypasswd), you will be asked to enter the password 

trustpasswd - if you specify this parameter without a value(e.g. java -jar <file.jar> --trustpasswd), you will be asked to enter the password
            -  if this parameter is omitted completely, value from keypasswd will be used

truststoretype - if this parameter is omitted completely, value from keystoretype will be used

### Do local handshake

java -jar -Djavax.net.debug=ssl:handshake:verbose certificate-analyser-<version>.jar --keystore ../../../keystore/localhost/localhost.keystore.p12 --truststore ../../../keystore/localhost/localhost.truststore.p12 --keypasswd password --keyalias localhost --local 

### Keyring

If you are using SAF keyrings, you need to provide an additional parameter in command line `-Djava.protocol.handler.pkgs=com.ibm.crypto.provider`.

### Possible issues:

Keystore/truststore is owned by different user - permission error. Temporarily Change read permission to all.


