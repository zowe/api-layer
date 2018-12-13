#!/bin/sh

#
# APIML Certificate Management
# ============================
#
# User guide: https://github.com/zowe/docs-site/blob/apiml-https/docs/guides/api-mediation-security.md
#
# IBM Java keytool documentation:
# https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/keytoolDocs/keytool_overview.html
#

BASE_DIR=$(dirname "$0")
PARAMS="$@"
PWD=`pwd`

function usage {
    echo "APIML Certificate Management"
    echo "usage: apiml_cm.sh --action <action>"
    echo ""
    echo "  <action> action to be done:"
    echo "     - setup - setups APIML certificate management"
    echo "     - new-service-csr - creates CSR for new service to be signed by external CA"
    echo "     - new-service - adds new service signed by local CA or external CA"
    echo "     - trust - adds a public certificate of a service to APIML truststore"
    echo "     - trust-zosmf - adds public certificates from z/OSMF keyring to APIML truststore"
    echo "     - clean - removes files created by setup"
    echo ""
    echo "  See ${BASE_DIR}/keystore/README.md for more details"
    echo ""
    echo "  Called with: ${PARAMS}"
}

ACTION=
V=
LOG=

LOCAL_CA_ALIAS="localca"
LOCAL_CA_FILENAME="keystore/local_ca/localca"
LOCAL_CA_DNAME="CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
LOCAL_CA_PASSWORD="local_ca_password"
LOCAL_CA_VALIDITY=3650
EXTERNAL_CA_FILENAME="keystore/local_ca/extca"
EXTERNAL_CA=

SERVICE_ALIAS="localhost"
SERVICE_PASSWORD="password"
SERVICE_KEYSTORE="keystore/localhost/localhost.keystore"
SERVICE_TRUSTSTORE="keystore/localhost/localhost.truststore"
SERVICE_DNAME="CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
SERVICE_EXT="SAN=dns:localhost.localdomain,dns:localhost"
SERVICE_VALIDITY=3650
EXTERNAL_CERTIFICATE=
EXTERNAL_CERTIFICATE_ALIAS=

ZOSMF_KEYRING="IZUKeyring.IZUDFLT"
ZOSMF_USERID="IZUSVR"

ALIAS="alias"
CERTIFICATE="no-certificate-specified"

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

function add_external_ca {
    echo "Adding external Certificate Authorities:"
    if [ -n "${EXTERNAL_CA}" ]; then
        I=1
        for FILE in ${EXTERNAL_CA}; do
            cp -v ${FILE} ${EXTERNAL_CA_FILENAME}.${I}.cer
            I=$((I+1))
        done
        if [ `uname` = "OS/390" ]; then
            for FILENAME in ${EXTERNAL_CA_FILENAME}.*.cer; do
                iconv -f ISO8859-1 -t IBM-1047 ${FILENAME} > ${FILENAME}-ebcdic
            done
        fi
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

function create_self_signed_service {
    if [ ! -e "${SERVICE_KEYSTORE}.p12" ];
    then
        echo "Generate service private key and service:"
        pkeytool -genkeypair $V -alias ${SERVICE_ALIAS} -keyalg RSA -keysize 2048 -keystore ${SERVICE_KEYSTORE}.p12 -keypass ${SERVICE_PASSWORD} -storepass ${SERVICE_PASSWORD} \
            -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY} \
            -ext ${SERVICE_EXT} -ext KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment -ext ExtendedKeyUsage=clientAuth,serverAuth
    fi
}

function sign_csr_using_local_ca {
    echo "Sign the CSR using the Certificate Authority:"
    pkeytool -gencert $V -infile ${SERVICE_KEYSTORE}.csr -outfile ${SERVICE_KEYSTORE}_signed.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -alias ${LOCAL_CA_ALIAS} -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 \
        -ext ${SERVICE_EXT} -ext KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment -ext ExtendedKeyUsage=clientAuth,serverAuth -rfc \
        -validity ${SERVICE_VALIDITY}
}

function import_local_ca_certificate {
    echo "Import the local Certificate Authority to the truststore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_TRUSTSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
}

function import_external_ca_certificates {
    echo "Import the external Certificate Authorities to the truststore:"
    I=1
    ls ${EXTERNAL_CA_FILENAME}.*.cer
    for FILENAME in ${EXTERNAL_CA_FILENAME}.*.cer; do
        [ -e "$FILENAME" ] || continue
        pkeytool -importcert $V -trustcacerts -noprompt -file ${FILENAME} -alias "extca${I}" -keystore ${SERVICE_TRUSTSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
        I=$((I+1))
    done
}

function import_signed_certificate {
    echo "Import the Certificate Authority to the keystore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_KEYSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the signed CSR to the keystore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${SERVICE_KEYSTORE}_signed.cer -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
}

function import_external_certificate {
    echo "Import the external Certificate Authorities to the keystore:"
    I=1
    for FILENAME in ${EXTERNAL_CA_FILENAME}.*.cer; do
        [ -e "$FILENAME" ] || continue
        pkeytool -importcert $V -trustcacerts -noprompt -file ${FILENAME} -alias "extca${I}" -keystore ${SERVICE_KEYSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
        I=$((I+1))
    done

    if [ -n "${EXTERNAL_CERTIFICATE}" ]; then
        echo "Import the signed certificate and its private key to the keystore:"
        pkeytool -importkeystore $V -deststorepass ${SERVICE_PASSWORD} -destkeypass ${SERVICE_PASSWORD} -destkeystore ${SERVICE_KEYSTORE}.p12 -deststoretype PKCS12 -destalias ${SERVICE_ALIAS} \
          -srckeystore ${EXTERNAL_CERTIFICATE} -srcstoretype PKCS12 -srcstorepass ${SERVICE_PASSWORD} -keypass ${SERVICE_PASSWORD} -srcalias ${EXTERNAL_CERTIFICATE_ALIAS}
    fi
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
    echo "TEMP_DIR=$TEMP_DIR"
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
    echo "cat returned $?"
    javac ${TEMP_DIR}/ExportPrivateKey.java
    echo "javac returned $?"
    java -cp ${TEMP_DIR} ExportPrivateKey ${SERVICE_KEYSTORE}.p12 PKCS12 ${SERVICE_PASSWORD} ${SERVICE_ALIAS} ${SERVICE_PASSWORD} ${SERVICE_KEYSTORE}.key
    echo "java returned $?"
    rm ${TEMP_DIR}/ExportPrivateKey.java ${TEMP_DIR}/ExportPrivateKey.class
}

function setup_local_ca {
    clean_local_ca
    create_certificate_authority
    add_external_ca
    echo "Listing generated files for local CA:"
    ls -l ${LOCAL_CA_FILENAME}*
}

function new_service_csr {
    clean_service
    create_service_certificate_and_csr
    echo "Listing generated files for service:"
    ls -l ${SERVICE_KEYSTORE}* ${SERVICE_TRUSTSTORE}*
}

function new_service {
    clean_service
    if [ -n "${EXTERNAL_CERTIFICATE}" ]; then
        import_external_certificate
    else
        create_service_certificate_and_csr
        sign_csr_using_local_ca
        import_signed_certificate
    fi
    import_local_ca_certificate
    import_external_ca_certificates
    export_service_certificate
    export_service_private_key
    echo "Listing generated files for service:"
    ls -l ${SERVICE_KEYSTORE}* ${SERVICE_TRUSTSTORE}*
}

function new_self_signed_service {
    clean_service
    create_self_signed_service
    import_local_ca_certificate
    export_service_certificate
    export_service_private_key
    echo "Listing generated files for self-signed service:"
    ls -l ${SERVICE_KEYSTORE}*
}

function trust {
    echo "Import a certificate to the truststore:"
    pkeytool -importcert $V -trustcacerts -noprompt -file ${CERTIFICATE} -alias ${ALIAS} -keystore ${SERVICE_TRUSTSTORE}.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
}

function trust_zosmf {
    ALIASES_FILE=${TEMP_DIR}/aliases.txt
    rm -f ${ALIASES_FILE}
    echo "Listing entries in the z/OSMF keyring (${ZOSMF_KEYRING}):"
    _BPX_USERID=${ZOSMF_USERID} keytool -list -keystore safkeyring:///${ZOSMF_KEYRING} -storetype JCERACFKS -J-Djava.protocol.handler.pkgs=com.ibm.crypto.provider
    RC=$?
    if [ "$RC" -ne "0" ]; then
    SWITCHED_USERID=`_BPX_USERID=${ZOSMF_USERID} whoami`
    echo "It is not possible to execute commands as z/OSMF user ID ${ZOSMF_USERID}. The effective user ID was: ${SWITCHED_USERID}. You need to run this command as user that has access to the z/OSMF keyring or as a superuser:"
    echo "  cd ${PWD}"
    echo "  scripts/apiml_cm.sh --action trust-zosmf --zosmf-keyring IZUKeyring.IZUDFLT --zosmf-userid IZUSVR"
    exit 1
    fi
    _BPX_USERID=${ZOSMF_USERID} keytool -list -keystore safkeyring:///${ZOSMF_KEYRING} -storetype JCERACFKS -J-Djava.protocol.handler.pkgs=com.ibm.crypto.provider | grep "Entry," | cut -f 1 -d , > ${ALIASES_FILE}
    CERT_PREFIX=${TEMP_DIR}/zosmf_cert_
    for ALIAS in `cat ${ALIASES_FILE}`; do
        echo "Exporting certificate ${ALIAS} from z/OSMF:"
        CERTIFICATE=${CERT_PREFIX}${ALIAS}.cer
        _BPX_USERID=${ZOSMF_USERID} keytool -exportcert -alias ${ALIAS} -keystore safkeyring:///${ZOSMF_KEYRING} -storetype JCERACFKS -J-Djava.protocol.handler.pkgs=com.ibm.crypto.provider -file ${CERTIFICATE}
        trust
        rm ${CERTIFICATE}
    done
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
        --external-certificate ) shift
                                EXTERNAL_CERTIFICATE=$1
                                ;;
        --external-certificate-alias ) shift
                                EXTERNAL_CERTIFICATE_ALIAS=$1
                                ;;
        --external-ca )         shift
                                EXTERNAL_CA="${EXTERNAL_CA} $1"
                                ;;
        --external-ca-filename ) shift
                                EXTERNAL_CA_FILENAME=$1
                                ;;
        --zosmf-keyring )       shift
                                ZOSMF_KEYRING=$1
                                ;;
        --zosmf-userid )        shift
                                ZOSMF_USERID=$1
                                ;;
        --certificate )         shift
                                CERTIFICATE=$1
                                ;;
        --alias )               shift
                                ALIAS=$1
                                ;;
        * )                     echo "Unexpected parameter: $1"
                                usage
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
    add-external-ca)
        add_external_ca
        ;;
    new-service-csr)
        new_service_csr
        ;;
    new-service)
        new_service
        ;;
    new-self-signed-service)
        new_self_signed_service
        ;;
    trust)
        trust
        ;;
    trust-zosmf)
        trust_zosmf
        ;;
    cert-key-export)
        export_service_certificate
        export_service_private_key
        ;;
    *)
        usage
        exit 1
esac
