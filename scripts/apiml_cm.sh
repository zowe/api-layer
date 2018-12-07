#!/bin/sh

BASE_DIR=$(dirname "$0")

function usage {
    echo "APIML Certificate Management"
    echo "usage: apiml_cm.sh --action <action>"
    echo ""
    echo "  <action> action to be done:"
    echo "     - setup - setups APIML certificate management"
    echo "     - new-service - adds new service"
    echo "     - clean - removes files created by setup"
    echo ""
    echo "  See ${BASE_DIR}/keystore/README.md for more details"
}

ACTION=
V=
LOG=

LOCAL_CA_ALIAS="localca"
LOCAL_CA_FILENAME="keystore/local_ca/localca"
LOCAL_CA_DNAME="CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
LOCAL_CA_PASSWORD="local_ca_password"
LOCAL_CA_VALIDITY=3650

SERVICE_ALIAS="localhost"
SERVICE_PASSWORD="password"
SERVICE_KEYSTORE="keystore/localhost/localhost.keystore"
SERVICE_TRUSTSTORE="keystore/localhost/localhost.truststore"
SERVICE_DNAME="CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
SERVICE_EXT="SAN=dns:localhost.localdomain,dns:localhost"
SERVICE_VALIDITY=3650

if [ -z ${TEMP_DIR+x} ]; then
    TEMP_DIR=/tmp
fi

function pkeytool {
    ARGS=$@
    echo "Calling keytool $ARGS"
    if [ "$LOG" != "" ]; then
        keytool "$@" >> $LOG 2>&1
    else
        keytool "$@"
    fi
    RC=$?
    echo "keytool returned: $RC"
    if [ "$RC" -ne "0" ]; then
        exit 1
    fi
}

function clean_local_ca {
    rm -f ${LOCAL_CA_FILENAME}.keystore.p12 ${LOCAL_CA_FILENAME}.cer
}

function clean_service {
    rm -f ${SERVICE_KEYSTORE}.p12 ${SERVICE_KEYSTORE}.csr ${SERVICE_KEYSTORE}_signed.cer ${SERVICE_TRUSTSTORE}.p12
}

function create_certificate_authority {
    echo "Generate keystore with the local CA private key and local CA public certificate:"
    pkeytool -genkeypair $V -alias ${LOCAL_CA_ALIAS} -keyalg RSA -keysize 2048 -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -dname "${LOCAL_CA_DNAME}" -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 -validity ${LOCAL_CA_VALIDITY} \
        -ext KeyUsage="keyCertSign" -ext BasicConstraints:"critical=ca:true"
    chmod 600 ${LOCAL_CA_FILENAME}.keystore.p12

    echo "Export local CA public certificate:"
    pkeytool -export $V -alias ${LOCAL_CA_ALIAS} -file ${LOCAL_CA_FILENAME}.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 -rfc \
        -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12

    if [ `uname` = "OS/390" ]; then
        iconv -f ISO8859-1 -t IBM-1047 ${LOCAL_CA_FILENAME}.cer > ${LOCAL_CA_FILENAME}.cer-ebcdic
    fi
}

function create_service_certificate_and_csr {
    if [ ! -e "${SERVICE_KEYSTORE}.p12" ];
    then
        echo "Generate service private key and service:"
        pkeytool -genkeypair $V -alias ${SERVICE_ALIAS} -keyalg RSA -keysize 2048 -keystore ${SERVICE_KEYSTORE}.p12 -keypass ${SERVICE_PASSWORD} -storepass ${SERVICE_PASSWORD} \
            -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY}

        echo "Generate CSR for the the service certificate:"
        pkeytool -certreq $V -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE}.p12 -storepass ${SERVICE_PASSWORD} -file ${SERVICE_KEYSTORE}.csr \
            -keyalg RSA -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY}
    fi
}

function sign_csr_using_local_ca {
    echo "Sign the CSR using the Certificate Authority:"
    pkeytool -gencert $V -infile ${SERVICE_KEYSTORE}.csr -outfile ${SERVICE_KEYSTORE}_signed.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -alias ${LOCAL_CA_ALIAS} -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 \
        -ext ${SERVICE_EXT} -ext KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment -ext ExtendedKeyUsage=clientAuth,serverAuth -rfc \
        -validity ${SERVICE_VALIDITY}
}

function import_signed_certificate_and_ca_certificate {
    echo "Import the Certificate Authority to the truststore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_TRUSTSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the Certificate Authority to the keystore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_KEYSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the signed CSR to the keystore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${SERVICE_KEYSTORE}_signed.cer -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
}

function export_service_certificate {
    echo "Export service certificate to the PEM format"
    pkeytool -exportcert -alias localhost -keystore ${SERVICE_KEYSTORE}.p12 -storetype PKCS12 -storepass ${SERVICE_PASSWORD} -rfc -file ${SERVICE_KEYSTORE}.cer

    if [ `uname` = "OS/390" ]; then
        iconv -f ISO8859-1 -t IBM-1047 ${SERVICE_KEYSTORE}.cer > ${SERVICE_KEYSTORE}.cer-ebcdic
    fi
}

function export_service_private_key {
    echo "Exporting service private key"
    cat <<EOF >$TEMP_DIR/ExportPrivateKey.java

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;

public class ExportPrivateKey {
    private File keystoreFile;
    private String keyStoreType;
    private char[] keyStorePassword;
    private char[] keyPassword;
    private String alias;
    private File exportedFile;

    public void export() throws Exception {
        KeyStore keystore = KeyStore.getInstance(keyStoreType);
        keystore.load(new FileInputStream(keystoreFile), keyStorePassword);
        Key key = keystore.getKey(alias, keyPassword);
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        FileWriter fw = new FileWriter(exportedFile);
        fw.write("-----BEGIN PRIVATE KEY-----");
        for (int i = 0; i < encoded.length(); i++) {
            if (((i % 64) == 0) && (i != (encoded.length() - 1))) {
                fw.write("\n");
            }
            fw.write(encoded.charAt(i));
        }
        fw.write("\n");
        fw.write("-----END PRIVATE KEY-----\n");
        fw.close();
    }

    public static void main(String args[]) throws Exception {
        ExportPrivateKey export = new ExportPrivateKey();
        export.keystoreFile = new File(args[0]);
        export.keyStoreType = args[1];
        export.keyStorePassword = args[2].toCharArray();
        export.alias = args[3];
        export.keyPassword = args[4].toCharArray();
        export.exportedFile = new File(args[5]);
        export.export();
    }
}
EOF
    javac ${TEMP_DIR}/ExportPrivateKey.java
    java -cp ${TEMP_DIR} ExportPrivateKey ${SERVICE_KEYSTORE}.p12 PKCS12 ${SERVICE_PASSWORD} ${SERVICE_ALIAS} ${SERVICE_PASSWORD} ${SERVICE_KEYSTORE}.key
    rm ${TEMP_DIR}/ExportPrivateKey.java ${TEMP_DIR}/ExportPrivateKey.class
}

function setup_local_ca {
    clean_local_ca
    create_certificate_authority
    echo "Listing generated files for local CA:"
    ls -l ${LOCAL_CA_FILENAME}*
}

function new_service {
    clean_service
    create_service_certificate_and_csr
    sign_csr_using_local_ca
    import_signed_certificate_and_ca_certificate
    export_service_certificate
    export_service_private_key
    echo "Listing generated files for service:"
    ls -l ${SERVICE_KEYSTORE}*
}

while [ "$1" != "" ]; do
    case $1 in
        -a | --action )         shift
                                ACTION=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        -v | --verbose )        V="-v"
                                ;;
        --local-ca-alias )      shift
                                LOCAL_CA_ALIAS=$1
                                ;;
        --log )                 shift
                                export LOG=$1
                                exec >> $LOG
                                ;;
        --local-ca-filename )   shift
                                LOCAL_CA_FILENAME=$1
                                ;;
        --local-ca-dname )      shift
                                LOCAL_CA_DNAME=$1
                                ;;
        --local-ca-password )   shift
                                LOCAL_CA_PASSWORD=$1
                                ;;
        --local-ca-validity )   shift
                                LOCAL_CA_VALIDITY=$1
                                ;;
        --service-alias )       shift
                                SERVICE_ALIAS=$1
                                ;;
        --service-ext )         shift
                                SERVICE_EXT=$1
                                ;;
        --service-keystore )    shift
                                SERVICE_KEYSTORE=$1
                                ;;
        --service-truststore )  shift
                                SERVICE_TRUSTSTORE=$1
                                ;;
        --service-dname )       shift
                                SERVICE_DNAME=$1
                                ;;
        --service-password )    shift
                                SERVICE_PASSWORD=$1
                                ;;
        --service-validity )    shift
                                SERVICE_VALIDITY=$1
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

case $ACTION in
    clean)
        clean_local_ca
        clean_service
        ;;
    setup)
        setup_local_ca
        new_service
        ;;
    new-service)
        new_service
        ;;
    *)
        usage
        exit 1
esac
