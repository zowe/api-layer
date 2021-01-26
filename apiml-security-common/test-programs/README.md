# PlatformAccessControl test programs

The files in this directory can be used to test PlatformAccessControl setup on different systems.

Source code can be built using `build.sh`. You need to have zosmf and ssh profiles setup in Zowe CLI and change the workspace directory in `build.sh`.

On a system with ZOWE class and APIML.SERVICES resource you can use this to validate the results:

```sh
java -cp . -Xquickstart CheckPerm IBMUSER SPATNY ZOWE BADZOWE APIML.SERVICES BADAPIML 1 4
```

If you do not have the ZOWE class defined you can use an existing class and resource:

```sh
java -cp . -Xquickstart CheckPerm SDKBLD SPATNY FACILITY BADZOWE BPX.SERVER BADAPIML 1 4
```

Values for the access level are following:

- READ(1)
- UPDATE(2)
- CONTROL(3)
- ALTER(4)

Expected output on a RACF system:

```txt
> java -cp . -Xquickstart CheckPerm IBMUSER SPATNY ZOWE BADZOWE APIML.SERVICES BADAPIML 1 4
PlatformAccessControl.checkPermission(IBMUSER, ZOWE, APIML.SERVICES, 1) = null
OK - null expected
PlatformAccessControl.checkPermission(IBMUSER, BADZOWE, APIML.SERVICES, 1) = PlatformReturned(rc=0, errno=143, errno2=0x93800cf, errnoMsg=EDC5143I No such process., stringRet=null, success=false)
OK - expected errno and errno2 returned
PlatformAccessControl.checkPermission(IBMUSER, ZOWE, BADAPIML, 1) = PlatformReturned(rc=0, errno=143, errno2=0x93800cf, errnoMsg=EDC5143I No such process., stringRet=null, success=false)
OK - expected errno and errno2 returned
PlatformAccessControl.checkPermission(IBMUSER, ZOWE, APIML.SERVICES, 4) = PlatformReturned(rc=0, errno=139, errno2=0x93800d9, errnoMsg=EDC5139I Operation not permitted., stringRet=null, success=false)
OK - expected errno and errno2 returned
PlatformAccessControl.checkPermission(SPATNY, ZOWE, APIML.SERVICES, 1) = PlatformReturned(rc=0, errno=143, errno2=0x93800f9, errnoMsg=EDC5143I No such process., stringRet=null, success=false)
OK - expected errno and errno2 returned
PlatformAccessControl.checkPermission(SPATNY, ZOWE, BADAPIML, 1) = PlatformReturned(rc=0, errno=143, errno2=0x93800f9, errnoMsg=EDC5143I No such process., stringRet=null, success=false)
OK - expected errno and errno2 returned
PlatformAccessControl.checkPermission(SPATNY, BADZOWE, BADAPIML, 1) = PlatformReturned(rc=0, errno=143, errno2=0x93800f9, errnoMsg=EDC5143I No such process., stringRet=null, success=false)
OK - expected errno and errno2 returned
Total failures: 0
```

The ZOWE class and resourced can be defined by the following TSO commands on a RACF system:

```txt
RDEFINE CDT ZOWE UACC(NONE) CDTINFO(DEFAULTUACC(NONE) FIRST(ALPHA) OTHER(ALPHA,NATIONAL,NUMERIC,SPECIAL) MAXLENGTH(246) POSIT(607) RACLIST(DISALLOWED))
SETROPTS RACLIST(CDT) REFRESH
SETROPTS CLASSACT(ZOWE)
RDEFINE ZOWE APIML.SERVICES UACC(NONE)
PERMIT APIML.SERVICES CLASS(ZOWE) ID(user) ACCESS(READ)
```

In a Top Secret system:

```txt
TSS ADDTO(RDT) RESCLASS(ZOWE) ACLST(NONE,READ,UPDATE,CONTROL) DEFACC(NONE)
TSS ADDTO(zoweDept) ZOWE(APIML.SERVICES)
TSS PERMIT(user) ZOWE(APIML.SERVICES) ACCESS(READ)
```

In an ACF2 system:

```txt
SET CONTROL(GSO)
INSERT CLASMAP.ZOWE RESOURCE(ZOWE) RSRCTYPE(ZWE)
F ACF2,REFRESH(CLASMAP),TYPE(GSO)
CHANGE INFODIR TYPES(R-RZWE)
F ACF2,REFRESH(INFODIR)
SET CONTROL(GSO)
LIST INFODIR
SET RESOURCE(ZWE)
RECKEY APIML ADD(SERVICES -
UID(*************user) SERVICE(READ) ALLOW)
F ACF2,REBUILD(ZWE)
```
