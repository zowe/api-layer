/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import java.util.HashMap;
import java.util.Map;

import lombok.Generated;
import lombok.RequiredArgsConstructor;

/**
 * Provides explanation for secondary error codes for as described at:
 * https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.4.0/com.ibm.zos.v2r4.bpxa800/errnojrs.htm
 */
@Generated
@RequiredArgsConstructor
public enum PlatformErrno2 {
    // Sonar exclusion - The names of the values are same as the original IDs on z/OS

    JROK("JROK", 0x0000, "The return code value describes the error"),  // NOSONAR
    JRNotKey8("JRNotKey8", 0x0011, "The task is not in PSW key 8"),  // NOSONAR
    JRMaxChild("JRMaxChild", 0x0012, "The maximum number of processes for this user ID has been exceeded"),  // NOSONAR
    JRMaxUIDs("JRMaxUIDs", 0x0013, "The maximum number of OpenMVS user IDs is exceeded"),  // NOSONAR
    JRAppcErrAlloc("JRAppcErrAlloc", 0x0014, "An APPC/MVS Allocate Conversation error occurred"),  // NOSONAR
    JRAppcErrSend("JRAppcErrSend", 0x0015, "An APPC/MVS Send_Data error occurred"),  // NOSONAR
    JRAppcErrRecv("JRAppcErrRecv", 0x0016, "An APPC/MVS Receive_and_Wait error occurred"),  // NOSONAR
    JRAppcErrAccept("JRAppcErrAccept", 0x0017, "An APPC/MVS Accept_Conversation error occurred"),  // NOSONAR
    JRAleservErr("JRAleservErr", 0x0019, "The ALESERV macro failed"),  // NOSONAR
    JRStgInUse("JRStgInUse", 0x001a, "The requested storage area has already been allocated (explicit STORAGE request)"),  // NOSONAR
    JRInitPgmErr("JRInitPgmErr", 0x001b, "The initialization (INIT) program failed"),  // NOSONAR
    JRSyseventErr("JRSyseventErr", 0x001c, "A SYSEVENT DONTSWAP/OKSWAP failure occurred"),  // NOSONAR
    JREstaeErr("JREstaeErr", 0x001d, "The ESTAE macro failed"),  // NOSONAR
    JRForkExitRcParentNoRoom("JRForkExitRcParentNoRoom", 0x001e, "Not enough work space exists for a parent fork exit"),  // NOSONAR
    JRForkExitRcChildNoStorage("JRForkExitRcChildNoStorage", 0x001f, "The system cannot obtain the storage needed for the child process"),  // NOSONAR
    JRForkExitRcParentBadEnv("JRForkExitRcParentBadEnv", 0x0020, "Forking is not supported under the current parent environment. An EAGAIN failure with this reason may be due to a temporary condition that can be resolved by reissuing the fork request"),  // NOSONAR
    JRForkExitRcMiscProblem("JRForkExitRcMiscProblem", 0x0021, "A fork exit failure occurred due to miscellaneous problems"),  // NOSONAR
    JRForkVsmListError("JRForkVsmListError", 0x0022, "The VSMLIST macro failed"),  // NOSONAR
    JRForkVsmListTooLarge("JRForkVsmListTooLarge", 0x0023, "The storage is insufficient to hold VSMLIST output"),  // NOSONAR
    JRForkRsmCopyError("JRForkRsmCopyError", 0x0024, "The fork real storage manager (RSM) copy service failed"),  // NOSONAR
    JRUnexpectedErr("JRUnexpectedErr", 0x0025, "An unexpected error occurred"),  // NOSONAR
    JRKernelReady("JRKernelReady", 0x0026, "The system is not in a ready state"),  // NOSONAR
    JRMaxProc("JRMaxProc", 0x0028, "The maximum number of processes was exceeded"),  // NOSONAR
    JRPidBad("JRPidBad", 0x0029, "The process ID (PID) indicates an internal error"),  // NOSONAR
    JRPidNotFound("JRPidNotFound", 0x002a, "A find, delete, or move did not happen"),  // NOSONAR
    JRNoSecurityProduct("JRNoSecurityProduct", 0x002b, "A security product is not installed"),  // NOSONAR
    JRBldlErr("JRBldlErr", 0x002c, "The BLDL macro failed"),  // NOSONAR
    JRCsvQueryErr("JRCsvQueryErr", 0x002d, "The CSVQUERY macro failed"),  // NOSONAR
    JRFilesysNotThere("JRFilesysNotThere", 0x002e, "The file system named does not exist"),  // NOSONAR
    JRFilesysWasReset("JRFilesysWasReset", 0x002f, "The file system named was reset by another user"),  // NOSONAR
    JRNegativeValueInvalid("JRNegativeValueInvalid", 0x0030, "A negative value cannot be supplied for one of the parameters"),  // NOSONAR
    JRUnlMountRO("JRUnlMountRO", 0x0032, "The unlink call was on a read-only file system"),  // NOSONAR
    JRRFileWrOnly("JRRFileWrOnly", 0x0033, "A call tried to read a file opened as write-only"),  // NOSONAR
    JRWFileRdOnly("JRWFileRdOnly", 0x0034, "A call tried to write to a file opened as read-only"),  // NOSONAR
    JRNegFileDes("JRNegFileDes", 0x0036, "A negative file descriptor was requested"),  // NOSONAR
    JRFileDesNotInUse("JRFileDesNotInUse", 0x0037, "The requested file descriptor is not in use"),  // NOSONAR
    JRMkDirExist("JRMkDirExist", 0x0038, "The requested file directory already exists"),  // NOSONAR
    JRPathTooLong("JRPathTooLong", 0x0039, "The pathname is too long"),  // NOSONAR
    JRNullInPath("JRNullInPath", 0x003a, "The pathname or file name contains a null"),  // NOSONAR
    JRNotSysRoot("JRNotSysRoot", 0x003b, "A relative pathname is allowed only for processes"),  // NOSONAR
    JRCompNotDir("JRCompNotDir", 0x003c, "A node in the pathname is not a directory"),  // NOSONAR
    JRDirNotFound("JRDirNotFound", 0x003d, "A directory in the pathname was not found"),  // NOSONAR
    JRCompNameTooLong("JRCompNameTooLong", 0x003e, "A component in the pathname was too long"),  // NOSONAR
    JRInvOpenFlags("JRInvOpenFlags", 0x003f, "The open call detected incorrect open flags"),  // NOSONAR
    JRTrNotRegFile("JRTrNotRegFile", 0x0041, "Truncate is valid only on a regular file"),  // NOSONAR
    JRClNeedClose("JRClNeedClose", 0x0042, "The closedir call was for a file that was opened with the open call"),  // NOSONAR
    JRPfsDead("JRPfsDead", 0x0043, "The file system owning the file is no longer active"),  // NOSONAR
    JRMkDir("JRMkDir", 0x0044, "The mkdir service is not supported by this file system"),  // NOSONAR
    JRClose("JRClose", 0x0045, "The vnode operation CLOSE is not supported by this file system"),  // NOSONAR
    JRRdwr("JRRdwr", 0x0046, "Vnode operation RDWR is not supported by this file system"),  // NOSONAR
    JRLookup("JRLookup", 0x0047, "Lookup is not supported by this file system"),  // NOSONAR
    JRForkChildErr("JRForkChildErr", 0x0048, "The fork child process returned an error code"),  // NOSONAR
    JRVnodGet("JRVnodGet", 0x0049, "A cell pool get for a vnode failed"),  // NOSONAR
    JRAppcCheckState("JRAppcCheckState", 0x004a, "An APPC/MVS receive error occurred while the APPC/MVS status was being checked"),  // NOSONAR
    JROpen("JROpen", 0x004b, "The open service is not supported because the file system is not locally mounted"),  // NOSONAR
    JRCreate("JRCreate", 0x004c, "The create service is not supported by this file system"),  // NOSONAR
    JRNoPath("JRNoPath", 0x004d, "The path length is not greater than 0"),  // NOSONAR
    JRNotActive("JRNotActive", 0x004e, "The OpenMVS kernel is not active"),  // NOSONAR
    JRChdNotDir("JRChdNotDir", 0x004f, "The chdir, fchdir, or chroot service is valid only for directory files"),  // NOSONAR
    JRChdNoEnt("JRChdNoEnt", 0x0050, "The chdir or chroot service was invoked with the name of a nonexisting file"),  // NOSONAR
    JRInvalidName("JRInvalidName", 0x0051, "File system name characters must be greater than 40 (hex) and less than FF (hex)"),  // NOSONAR
    JRMkDirROnly("JRMkDirROnly", 0x0055, "The directory cannot be created in a read-only file system"),  // NOSONAR
    JRLnkDir("JRLnkDir", 0x0056, "Hard links cannot be made to directory files"),  // NOSONAR
    JRLskOnPipe("JRLskOnPipe", 0x0057, "The lseek service cannot be performed on a pipe or socket"),  // NOSONAR
    JRLskOffsetIsInvalid("JRLskOffsetIsInvalid", 0x0058, "The offset given for lseek service is incorrect"),  // NOSONAR
    JRLskWhenceIsInvalid("JRLskWhenceIsInvalid", 0x0059, "The whence given for the lseek service is incorrect"),  // NOSONAR
    JRFSNotStart("JRFSNotStart", 0x005a, "The file system specified was not started"),  // NOSONAR
    JRIsMounted("JRIsMounted", 0x005b, "The file system is already mounted"),  // NOSONAR
    JRMountPt("JRMountPt", 0x005c, "A problem was found with the mount point specified"),  // NOSONAR
    JRUnlNoEnt("JRUnlNoEnt", 0x005d, "The file to be unlinked does not exist"),  // NOSONAR
    JRUnlDir("JRUnlDir", 0x005e, "The unlink service was requested on a directory file"),  // NOSONAR
    JROutOfOfteCells("JROutOfOfteCells", 0x005f, "The system was unable to obtain a cell from the OFTE cell pool"),  // NOSONAR
    JRReadOnlyFileSetWriteReq("JRReadOnlyFileSetWriteReq", 0x0060, "An open request for write was entered for a file system that was mounted read-only"),  // NOSONAR
    JRReadOnlyFileSetCreatReq("JRReadOnlyFileSetCreatReq", 0x0061, "A file cannot be created in a read-only file system"),  // NOSONAR
    JRNoFileNoCreatFlag("JRNoFileNoCreatFlag", 0x0062, "A service tried to open a nonexistent file without O_CREAT"),  // NOSONAR
    JRFileExistsExclFlagSet("JRFileExistsExclFlagSet", 0x0063, "The file exists, but O_EXCL is specified on the open call"),  // NOSONAR
    JRDirWriteRequest("JRDirWriteRequest", 0x0064, "The service tried to open a directory for write access"),  // NOSONAR
    JROpenFlagConflict("JROpenFlagConflict", 0x0065, "The call tried to open a file with O_RDONLY and O_TRUNC specified"),  // NOSONAR
    JRParmTooLong("JRParmTooLong", 0x0067, "On the mount, a parameter field longer than 1024 was specified"),  // NOSONAR
    JRRemove("JRRemove", 0x0068, "Vn_Remove is not supported by the physical file system"),  // NOSONAR
    JRBothMode("JRBothMode", 0x0069, "On the mount service, both read and read/write were specified"),  // NOSONAR
    JRNeitherMode("JRNeitherMode", 0x006a, "On the mount service, neither read nor read/write were specified"),  // NOSONAR
    JRBuffTooSmall("JRBuffTooSmall", 0x006b, "The buffer for return information is too small"),  // NOSONAR
    JRFileNotThere("JRFileNotThere", 0x006c, "The requested file does not exist"),  // NOSONAR
    JRReadDir("JRReadDir", 0x006d, "The readdir service vnode operation is not supported"),  // NOSONAR
    JRGetAttr("JRGetAttr", 0x006e, "GetAttr is not supported by this file system"),  // NOSONAR
    JROutOfVFSCells("JROutOfVFSCells", 0x006f, "The system is unable to obtain a cell from the VFS cell pool"),  // NOSONAR
    JRRddFileNotDir("JRRddFileNotDir", 0x0070, "The readdir service request was on a file that was not opened as a directory"),  // NOSONAR
    JRTargetNotDir("JRTargetNotDir", 0x0071, "The opendir service did not specify a directory"),  // NOSONAR
    JROpenDirNotFound("JROpenDirNotFound", 0x0072, "The directory specified on the opendir service did not exist"),  // NOSONAR
    JRNotPRB("JRNotPRB", 0x0073, "The current request block is not a program request block"),  // NOSONAR
    JRSpFileExists("JRSpFileExists", 0x0075, "The file specified on the mknod service already existed"),  // NOSONAR
    JRReadOnlyFileSetMknodReq("JRReadOnlyFileSetMknodReq", 0x0076, "A special file cannot be created on a read-only file system"),  // NOSONAR
    JRRmDir("JRRmDir", 0x0077, "The rmdir service vnode operation is not supported"),  // NOSONAR
    JRPathNotDir("JRPathNotDir", 0x0078, "The pathname does not specify a directory"),  // NOSONAR
    JRReadOnlyFS("JRReadOnlyFS", 0x0079, "This operation does not work on a read-only file system"),  // NOSONAR
    JRDirInUse("JRDirInUse", 0x007a, "The requested directory is in use"),  // NOSONAR
    JRDiffFileSets("JRDiffFileSets", 0x007b, "The rename service is not supported across file systems"),  // NOSONAR
    JRNewNotDir("JRNewNotDir", 0x007c, "The new name specified on the rename service is not a directory"),  // NOSONAR
    JRNewIsDir("JRNewIsDir", 0x007d, "The new name specified on the rename service is a directory"),  // NOSONAR
    JROldNoExist("JROldNoExist", 0x007e, "The old name specified on the rename service does not exist"),  // NOSONAR
    JRIsFSRoot("JRIsFSRoot", 0x007f, "The name specified is in use as a file system root"),  // NOSONAR
    JRRename("JRRename", 0x0080, "The rename service vnode operation is not supported"),  // NOSONAR
    JRTermReq("JRTermReq", 0x0081, "The termination request does not point to PPRP or PPRT"),  // NOSONAR
    JRDotOrDotDot("JRDotOrDotDot", 0x0082, "The requested function cannot be performed against dot (.) or dot-dot (..)"),  // NOSONAR
    JRKernelDown("JRKernelDown", 0x0083, "The kernel has ended during this service"),  // NOSONAR
    JRInternalError("JRInternalError", 0x0084, "An internal error was detected"),  // NOSONAR
    JRBadEntryCode("JRBadEntryCode", 0x0086, "An incorrect entry code was specified on this request"),  // NOSONAR
    JRFdAllocErr("JRFdAllocErr", 0x0088, "An error occurred while trying to allocate a file descriptor page"),  // NOSONAR
    JRBytes2RWZero("JRBytes2RWZero", 0x008a, "The number of bytes requested to read or write is negative"),  // NOSONAR
    JRRwdFileNotDir("JRRwdFileNotDir", 0x008b, "The rewinddir service was on a file that is not a directory"),  // NOSONAR
    JRRootNode("JRRootNode", 0x008c, "The requested operation cannot be done on a root"),  // NOSONAR
    JRInvalidSignal("JRInvalidSignal", 0x008d, "A signal number specified is incorrect"),  // NOSONAR
    JRInvalidSigAct("JRInvalidSigAct", 0x008e, "The action is incorrect for the specified signal"),  // NOSONAR
    JRInvalidSigHow("JRInvalidSigHow", 0x008f, "The how operand specified is incorrect"),  // NOSONAR
    JRNotForDir("JRNotForDir", 0x0090, "The system cannot perform the requested function on a directory"),  // NOSONAR
    JROldPartOfNew("JROldPartOfNew", 0x0091, "The old name specified on the rename service is part of the new name"),  // NOSONAR
    JRParmBadSyntax("JRParmBadSyntax", 0x0092, "A parmlib parameter has a syntax error"),  // NOSONAR
    JRParmLibIOError("JRParmLibIOError", 0x0093, "An I/O error occurred in reading parmlib"),  // NOSONAR
    JRParmLibOpenFailure("JRParmLibOpenFailure", 0x0094, "A parmlib data set cannot be opened"),  // NOSONAR
    JRParmLibNotFound("JRParmLibNotFound", 0x0095, "A parmlib member could not be found"),  // NOSONAR
    JRParmLibBadData("JRParmLibBadData", 0x0096, "A parmlib member contains incorrect data"),  // NOSONAR
    JRParmLibNoAccess("JRParmLibNoAccess", 0x0097, "A parmlib data set cannot be accessed at this time"),  // NOSONAR
    JRParmBufNoStorage("JRParmBufNoStorage", 0x0098, "Storage could not be obtained for the parameter buffer"),  // NOSONAR
    JRPgserError("JRPgserError", 0x009a, "The page service (PGSER) failed"),  // NOSONAR
    JRTrOpenedRO("JRTrOpenedRO", 0x009c, "The ftruncate service was for a file opened in read-only mode"),  // NOSONAR
    JRTrMountedRO("JRTrMountedRO", 0x009d, "The ftruncate service was for a file on a file system mounted in read-only mode"),  // NOSONAR
    JRTrNegOffset("JRTrNegOffset", 0x009f, "A negative offset was given to a file truncate service"),  // NOSONAR
    JROutOfLocks("JROutOfLocks", 0x00a0, "The file system has run out of locks"),  // NOSONAR
    JRMount("JRMount", 0x00a1, "The mount service VFS operation is not supported"),  // NOSONAR
    JRUMount("JRUMount", 0x00a2, "The unmount service VFS operation is not supported"),  // NOSONAR
    JRSync("JRSync", 0x00a3, "The Sync VFS operation is not supported"),  // NOSONAR
    JRRoot("JRRoot", 0x00a4, "The Root VFS operation is not supported"),  // NOSONAR
    JRStatFS("JRStatFS", 0x00a5, "The StatFS VFS operation is not supported"),  // NOSONAR
    JRFID("JRFID", 0x00a6, "The Get Fid VFS operation is not supported"),  // NOSONAR
    JRVget("JRVget", 0x00a7, "The VGet VFS operation is not supported"),  // NOSONAR
    JRInvalidVnode("JRInvalidVnode", 0x00a8, "The vnode returned is not valid"),  // NOSONAR
    JRInvalidParms("JRInvalidParms", 0x00a9, "An incorrect combination of parameters was specified"),  // NOSONAR
    JRFsParentFs("JRFsParentFs", 0x00aa, "The file system has file systems mounted on it"),  // NOSONAR
    JRFsUnmountInProgress("JRFsUnmountInProgress", 0x00ab, "An unmount service is already in progress"),  // NOSONAR
    JRFsMustReset("JRFsMustReset", 0x00ac, "An unmount service must specify reset when already unmounting"),  // NOSONAR
    JRFsForceUmount("JRFsForceUmount", 0x00ad, "Reset fails when the file system is forced to unmount"),  // NOSONAR
    JRAudit("JRAudit", 0x00ae, "The audit vnode operation is not supported"),  // NOSONAR
    JRLockErr("JRLockErr", 0x00af, "The file system had a lock error"),  // NOSONAR
    JRUserNotPrivileged("JRUserNotPrivileged", 0x00b0, "The requester of the service is not privileged"),  // NOSONAR
    JRUnexpectedError("JRUnexpectedError", 0x00b1, "An unexpected return value was received"),  // NOSONAR
    JRMustUmountImmed("JRMustUmountImmed", 0x00b2, "An immediate unmount must precede a forced unmount"),  // NOSONAR
    JRNotQuiesced("JRNotQuiesced", 0x00b3, "A quiesce service must precede an unquiesce"),  // NOSONAR
    JRQuiesced("JRQuiesced", 0x00b4, "There was a previous quiesce request"),  // NOSONAR
    JRInvalidRequester("JRInvalidRequester", 0x00b5, "The requester of the function cannot make the request"),  // NOSONAR
    JRPfsSuspend("JRPfsSuspend", 0x00b6, "The PFS is waiting to restart"),  // NOSONAR
    JRPfsAbend("JRPfsAbend", 0x00b7, "The physical file system abended"),  // NOSONAR
    JRNoStorage("JRNoStorage", 0x00b8, "Insufficient storage to copy user parameters"),  // NOSONAR
    JRActiveProcess("JRActiveProcess", 0x00b9, "Multiple processes in the address space prevent the termination of the job step process"),  // NOSONAR
    JRPfsctl("JRPfsctl", 0x00ba, "The Pfsctl operation is not supported"),  // NOSONAR
    JRPfsInitFailed("JRPfsInitFailed", 0x00bb, "The file system did not initialize"),  // NOSONAR
    JRSpawnNoCommonStg("JRSpawnNoCommonStg", 0x00bc, "Spawn cannot be completed because not enough common storage is available to complete the request"),  // NOSONAR
    JRSpawnArgsTooBig("JRSpawnArgsTooBig", 0x00bd, "Spawn cannot be completed because the total length of arguments specified by the caller exceeds the system imposed limit of 1&finespace;000&finespace;000 bytes"),  // NOSONAR
    JRFuncUndefined("JRFuncUndefined", 0x00be, "The oe_env_np (BPX1ENV) service cannot be completed because the Function_code specified is undefined"),  // NOSONAR
    JRBadArgCount("JRBadArgCount", 0x00bf, "The oe_env_np (BPX1ENV) service cannot be completed because the number of input or output arguments are incorrect for the Function_code specified"),  // NOSONAR
    JRBadInputValue("JRBadInputValue", 0x00c0, "The oe_env_np (BPX1ENV) service cannot be completed because an input argument contained an undefined value"),  // NOSONAR
    JRNoUserExit("JRNoUserExit", 0x00c1, "A pthread_quiesce (BPX1PTQ) service FREEZE_EXIT request cannot complete because no user exit has been registered with the oe_env_np (BPX1ENV) service"),  // NOSONAR
    JRProcessNotFrozen("JRProcessNotFrozen", 0x00c2, "A pthread_quiesce (BPX1PTQ) service FREEZE_EXIT request cannot complete because the process is not frozen"),  // NOSONAR
    JRFreezeExitTooSlow("JRFreezeExitTooSlow", 0x00c3, "A pthread_quiesce (BPX1PTQ) service FREEZE_EXIT request cannot complete because user exits did not return in the expected time"),  // NOSONAR
    JRResrPortSpecified("JRResrPortSpecified", 0x00c4, "During a Socket Bind request, a Reserved Port was incorrectly specified"),  // NOSONAR
    JRResrPortNotAvail("JRResrPortNotAvail", 0x00c5, "All port zero, INADDR_ANY, reserved ports are in use"),  // NOSONAR
    JRResrPortUsed("JRResrPortUsed", 0x00c6, "A sockets port assignment conflict exists between OMVS and a Transport Provider"),  // NOSONAR
    JRTLSRequestInvalid("JRTLSRequestInvalid", 0x00c7, "An invalid request has been passed for task level security"),  // NOSONAR
    JRNoReservedPorts("JRNoReservedPorts", 0x00c8, "A bind() syscall that specified port number zero and IP address INADDR_ANY, failed because no ports were reserved"),  // NOSONAR
    JRTLSIDTypeInvalid("JRTLSIDTypeInvalid", 0x00c9, "An invalid ID type has been passed for task level security"),  // NOSONAR
    JRTLSIDLengthInvalid("JRTLSIDLengthInvalid", 0x00ca, "An invalid ID length has been passed for task level security"),  // NOSONAR
    JRTLSAddressLengthInvalid("JRTLSAddressLengthInvalid", 0x00cb, "An invalid address length has been passed for task level security"),  // NOSONAR
    JRTLSCallerIsIPT("JRTLSCallerIsIPT", 0x00cc, "The calling task is IPT"),  // NOSONAR
    JRTLSNotDoneByOE("JRTLSNotDoneByOE", 0x00cd, "Task level security already exists, but it was not created by a previous call to pthread_security_np"),  // NOSONAR
    JRNoPtraceTaskSec("JRNoPtraceTaskSec", 0x00ce, "Ptrace is active in the caller’s address space, task level security not allowed concurrently with Ptrace"),  // NOSONAR
    JRSAFResourceUndefined("JRSAFResourceUndefined", 0x00cf, "The resource specified by the caller is not defined to RACF"),  // NOSONAR
    JRSAFParmListErr("JRSAFParmListErr", 0x00d0, "A SAF service was unsuccessful because an error was detected by SAF in the parameter list"),  // NOSONAR
    JRSAFNoUUIDtoUser("JRSAFNoUUIDtoUser", 0x00d1, "No mapping to a RACF userid exists for the DCE UUID specified"),  // NOSONAR
    JRSAFNoUsertoUUID("JRSAFNoUsertoUUID", 0x00d2, "No mapping to a DCE UUID exists for the RACF Userid specified"),  // NOSONAR
    JRSAFNoDCEClass("JRSAFNoDCEClass", 0x00d3, "The RACF DCEUUIDS class is not active"),  // NOSONAR
    JRSAFNoCellUUID("JRSAFNoCellUUID", 0x00d4, "The local cell UUID could not be determined for this RACF userid to DCE UUID conversion request"),  // NOSONAR
    JRClassLenErr("JRClassLenErr", 0x00d5, "The length of the RACF class name is outside of the allowable range of 1 to 8"),  // NOSONAR
    JREntityLenErr("JREntityLenErr", 0x00d6, "The length of the RACF entity name is outside of the allowable range of 1 to 246"),  // NOSONAR
    JRAccessUndefined("JRAccessUndefined", 0x00d7, "The access type specified is undefined"),  // NOSONAR
    JRNotServerAuthorized("JRNotServerAuthorized", 0x00d8, "The calling address space is not permitted to the BPX.SERVER Facility class or the BPX.SERVER Facility class is undefined and caller not a superuser (UID=0)"),  // NOSONAR
    JRNoResourceAccess("JRNoResourceAccess", 0x00d9, "The user specified by the caller does not have the access specified to the resource"),  // NOSONAR
    JRSurrogateUndefined("JRSurrogateUndefined", 0x00da, "The RACF SURROGAT class has not been activated or no SURROGAT class profile has been defined for the client"),  // NOSONAR
    JRNoSurrogatePerm("JRNoSurrogatePerm", 0x00db, "The server is not permitted to the SURROGAT class profile defined for the client"),  // NOSONAR
    JRSAFNotAuthorized("JRSAFNotAuthorized", 0x00dc, "The calling address space is not authorized to use this service"),  // NOSONAR
    JRNoBufStorage("JRNoBufStorage", 0x00dd, "Storage could not be obtained for I/O buffers"),  // NOSONAR
    JRNoVsmList("JRNoVsmList", 0x00f6, "Fork cannot be completed because the parent process ended prematurely"),  // NOSONAR
    JRForkNoResource("JRForkNoResource", 0x00f7, "Fork cannot be processed for lack of resource"),  // NOSONAR
    JRSWAREQ("JRSWAREQ", 0x00f8, "The SWAREQ macro failed"),  // NOSONAR
    JRSAFNoUser("JRSAFNoUser", 0x00f9, "The user ID is not defined to the security product"),  // NOSONAR
    JRSAFGroupNoOMVS("JRSAFGroupNoOMVS", 0x00fa, "The current group does not have a GID defined in the OMVS segment"),  // NOSONAR
    JRSAFUserNoOMVS("JRSAFUserNoOMVS", 0x00fb, "The user ID is not authorized to use OMVS"),  // NOSONAR
    JRSAFNoUID("JRSAFNoUID", 0x00fc, "The user ID has no UID"),  // NOSONAR
    JRSAFNoGID("JRSAFNoGID", 0x00fd, "The user ID is in a group that has no GID"),  // NOSONAR
    JRSAFInternal("JRSAFInternal", 0x00fe, "An internal error occurred in the security product"),  // NOSONAR
    JRStatusPosted("JRStatusPosted", 0x00ff, "A request was received to dub a thread for a process that is stopped or has ended"),  // NOSONAR
    JRTrunc("JRTrunc", 0x0100, "Vnode operation trunc is not supported by this file system"),  // NOSONAR
    JRFsync("JRFsync", 0x0101, "Vnode operation fsync is not supported by this file system"),  // NOSONAR
    JRSetAttr("JRSetAttr", 0x0102, "Vnode operation setattr is not supported by this file system"),  // NOSONAR
    JRSymFileAlreadyExists("JRSymFileAlreadyExists", 0x0103, "The file requested for creation as a symbolic link already exists"),  // NOSONAR
    JRSymlink("JRSymlink", 0x0104, "The symbolic link vnode operation is not supported"),  // NOSONAR
    JRFileNotSymLink("JRFileNotSymLink", 0x0105, "The file requested for readlink service is not a symbolic link"),  // NOSONAR
    JRReadlink("JRReadlink", 0x0106, "The readlink vnode operation is not supported"),  // NOSONAR
    JRMknodInvalidType("JRMknodInvalidType", 0x0107, "The mknod service invoked with incorrect file type parameter"),  // NOSONAR
    JREndingSlashMknod("JREndingSlashMknod", 0x0108, "The pathname ended with a slash on the mknod service"),  // NOSONAR
    JREndingSlashOCreat("JREndingSlashOCreat", 0x0109, "The pathname in the open service, with the O_CREAT option, ended with slash"),  // NOSONAR
    JRLnkNoEnt("JRLnkNoEnt", 0x010a, "The service tried to link to nonexistent file"),  // NOSONAR
    JRLnkNewPathExists("JRLnkNewPathExists", 0x010b, "The service tried to add a link whose name already exists"),  // NOSONAR
    JRLnkAcrossFilesets("JRLnkAcrossFilesets", 0x010c, "The service tried to link across file systems"),  // NOSONAR
    JRLnkROFileset("JRLnkROFileset", 0x010d, "The service tried to add a directory entry on a read-only file system"),  // NOSONAR
    JRLink("JRLink", 0x010e, "Vn_Link is not supported by this physical file system"),  // NOSONAR
    JRExecNmLenZero("JRExecNmLenZero", 0x010f, "The length of the executable name passed was zero"),  // NOSONAR
    JRParmlibSuffixTooLong("JRParmlibSuffixTooLong", 0x0111, "The parmlib member name suffix is more than two characters"),  // NOSONAR
    JRFsFailStorage("JRFsFailStorage", 0x0112, "Dub or fork failed due to unavailable file system storage"),  // NOSONAR
    JRFsFailQuiesce("JRFsFailQuiesce", 0x0113, "Dub or fork cannot complete, because the working directory is unmounted"),  // NOSONAR
    JRNotPermitted("JRNotPermitted", 0x0114, "You are not permitted to signal to the specified process ID (PID)"),  // NOSONAR
    JRBuffLenInvalid("JRBuffLenInvalid", 0x0115, "The length of the buffer is less than or equal to zero or less than a minimum length"),  // NOSONAR
    JRNotRegistered("JRNotRegistered", 0x0117, "The call is not registered for signals"),  // NOSONAR
    JRNotSupportedForFileType("JRNotSupportedForFileType", 0x0119, "The requested service is not supported for this file type"),  // NOSONAR
    JRInvalidSymLinkLen("JRInvalidSymLinkLen", 0x011a, "The contents specified for the symbolic link has an incorrect length"),  // NOSONAR
    JRInvalidSymLinkComp("JRInvalidSymLinkComp", 0x011b, "The contents specified for symbolic link has an incorrect component"),  // NOSONAR
    JRFileNotOpen("JRFileNotOpen", 0x011c, "The file is not opened"),  // NOSONAR
    JRTooManySymlinks("JRTooManySymlinks", 0x011d, "Too many symbolic links were encountered in the pathname"),  // NOSONAR
    JRMVSArgTooBig("JRMVSArgTooBig", 0x011e, "The execMVS argument string was too long"),  // NOSONAR
    JRExecNotRegFile("JRExecNotRegFile", 0x011f, "The filename specified on the exec is not a regular file"),  // NOSONAR
    JRAppcErrRecvIncomp("JRAppcErrRecvIncomp", 0x0120, "An APPC/MVS Receive_and_Wait error occurred. The data is incomplete"),  // NOSONAR
    JRForkNoAccess("JRForkNoAccess", 0x0121, "The call tried an unauthorized access to a fork child transaction program"),  // NOSONAR
    JRInactive("JRInactive", 0x0122, "The vnode operation inactive is not supported by the file system"),  // NOSONAR
    JRInvalidMajorNumber("JRInvalidMajorNumber", 0x0123, "Character special file system detected an incorrect device major number"),  // NOSONAR
    JROutofPnodeCells("JROutofPnodeCells", 0x0124, "No pnode/pnwt cells are available"),  // NOSONAR
    JRRdandWRTforPipe("JRRdandWRTforPipe", 0x0125, "The open call on a pipe was for read/write"),  // NOSONAR
    JRQueueManPutFailed("JRQueueManPutFailed", 0x0126, "The queue manager returned an incorrect return code"),  // NOSONAR
    JRQueueManGetFailed("JRQueueManGetFailed", 0x0127, "The queue manager returned an incorrect return code"),  // NOSONAR
    JROpenforWriteNoReaders("JROpenforWriteNoReaders", 0x0128, "Open for write was done before any open for read"),  // NOSONAR
    JRNoReaders("JRNoReaders", 0x0129, "The service tried to write before any open for reads"),  // NOSONAR
    JRInvParmLength("JRInvParmLength", 0x012a, "The parameter length is incorrect or negative"),  // NOSONAR
    JRForkAbend("JRForkAbend", 0x012b, "The program abended in the fork module"),  // NOSONAR
    JRForkExitAbend("JRForkExitAbend", 0x012c, "An abend occurred in a fork exit"),  // NOSONAR
    JRSyscallAbend("JRSyscallAbend", 0x012d, "An abend occurred in a system call"),  // NOSONAR
    JRBadAddress("JRBadAddress", 0x012e, "An incorrect address was encountered when the system tried to access data"),  // NOSONAR
    JRL16Jump("JRL16Jump", 0x012f, "An unexpected error occurred in load 16 and jump"),  // NOSONAR
    JRSigDuringWait("JRSigDuringWait", 0x0130, "A signal occurred during a wait"),  // NOSONAR
    JRQueueManOpenFailed("JRQueueManOpenFailed", 0x0131, "The queue manager was unable to create a queue"),  // NOSONAR
    JRQueueManCloseFailed("JRQueueManCloseFailed", 0x0132, "The queue manager returned a nonzero return code on a close service"),  // NOSONAR
    JRRdnorWRTforPipe("JRRdnorWRTforPipe", 0x0133, "The open service on a pipe was for neither read nor write"),  // NOSONAR
    JROutofVnodeCells("JROutofVnodeCells", 0x0134, "No vnode cells are available"),  // NOSONAR
    JRNoData("JRNoData", 0x0135, "There is no data in this pipe"),  // NOSONAR
    JRUserNotAuthorized("JRUserNotAuthorized", 0x0136, "The user is not authorized for the requested file descriptor"),  // NOSONAR
    JRFileIsBlocked("JRFileIsBlocked", 0x0138, "The file is blocked"),  // NOSONAR
    JRIoctl("JRIoctl", 0x0139, "The ioctl service is not supported by this file system"),  // NOSONAR
    JRInvalidPid("JRInvalidPid", 0x013a, "The process ID (PID) was not found, so the signal was not sent"),  // NOSONAR
    JRVfsIoctl("JRVfsIoctl", 0x013b, "The Vfsioctl service is not supported by this file system"),  // NOSONAR
    JRInvRbState("JRInvRbState", 0x013c, "Callable services cannot be nested"),  // NOSONAR
    JRWrongInstance("JRWrongInstance", 0x013d, "The process is not known to current kernel instance"),  // NOSONAR
    JRVfsInact("JRVfsInact", 0x013e, "The InAct VFS operation is not supported"),  // NOSONAR
    JRInvTermStat("JRInvTermStat", 0x013f, "An incorrect process termination status was passed to BPX1MPC"),  // NOSONAR
    JRActiveThreads("JRActiveThreads", 0x0140, "The process could not be terminated because there are multiple threads still running in the process"),  // NOSONAR
    JRBadExitStatusAddr("JRBadExitStatusAddr", 0x0141, "An incorrect exit status address was passed to the wait call"),  // NOSONAR
    JRWaitError("JRWaitError", 0x0142, "An unexpected error occurred in the wait service"),  // NOSONAR
    JRProcessEnding("JRProcessEnding", 0x0143, "The current process is ending"),  // NOSONAR
    JRSignalsNotBlocked("JRSignalsNotBlocked", 0x0144, "The service did not complete because signals are not blocked"),  // NOSONAR
    JRFdTooBig("JRFdTooBig", 0x0145, "The requested file descriptor exceeds the Open_max limit"),  // NOSONAR
    JROpenMax("JROpenMax", 0x0146, "The maximum number of open files for this process was reached"),  // NOSONAR
    JRBadUidtSlot("JRBadUidtSlot", 0x0147, "The slot passed to BPXPRCHK is not a valid index for the UIDT table"),  // NOSONAR
    JRResMgr("JRResMgr", 0x0148, "The RESMGR macro returned a negative status"),  // NOSONAR
    JRIOBufLengthInvalid("JRIOBufLengthInvalid", 0x0149, "The input argument buffer length was incorrect"),  // NOSONAR
    JRInvalidAmode("JRInvalidAmode", 0x014a, "An incorrect access mode was specified on the access service"),  // NOSONAR
    JRAccess("JRAccess", 0x014b, "The access vnode operation is not supported"),  // NOSONAR
    JRUIDchanged("JRUIDchanged", 0x014c, "The slot passed to BPXPRCHK is not valid for the UID specified"),  // NOSONAR
    JRFsFailChdir("JRFsFailChdir", 0x014d, "The dub failed, due to an error with the initial home directory"),  // NOSONAR
    JRFsFailLock("JRFsFailLock", 0x014e, "The dub failed, due to an error getting necessary file system locks"),  // NOSONAR
    JRUpdateUidtFailure("JRUpdateUidtFailure", 0x014f, "The update to UIDT in BPXPRCHK failed"),  // NOSONAR
    JRBadAuditOption("JRBadAuditOption", 0x0150, "An incorrect option code was specified for the chaudit service"),  // NOSONAR
    JRExecFileTooBig("JRExecFileTooBig", 0x0151, "The size of the specified file exceeds the private region of the caller"),  // NOSONAR
    JRInvalidCursor("JRInvalidCursor", 0x0152, "The cursor value passed to the w_getmntent call is incorrect"),  // NOSONAR
    JRPtySlaveOpened("JRPtySlaveOpened", 0x0153, "The open of the main pseudo-TTY failed, because the associated secondary pseudo-TTY is still open"),  // NOSONAR
    JRPtyMinorInvalid("JRPtyMinorInvalid", 0x0154, "The device minor number is larger than the MAXPTYS parameter in the BPXPRMxx member"),  // NOSONAR
    JRPtyAlreadyActive("JRPtyAlreadyActive", 0x0155, "The device minor number is already active"),  // NOSONAR
    JRSignalReceived("JRSignalReceived", 0x0156, "The call was interrupted by a signal"),  // NOSONAR
    JRPtyDifferentUID("JRPtyDifferentUID", 0x0157, "The process UID is different from the UID of the process that opened the master pseudo-TTY"),  // NOSONAR
    JRPtyMasterClosed("JRPtyMasterClosed", 0x0158, "There is no corresponding master pseudo-TTY file open"),  // NOSONAR
    JRPtyDifferentFile("JRPtyDifferentFile", 0x0159, "A dependent pseudo-TTY file for this minor number with a different filename is already open"),  // NOSONAR
    JRPtySlaveNotInit("JRPtySlaveNotInit", 0x015b, "The dependent pseudo-TTY support did not complete successfully"),  // NOSONAR
    JRPtyInputStopped("JRPtyInputStopped", 0x015c, "The nonblocked write failed, because input is stopped"),  // NOSONAR
    JREOFAlreadySent("JREOFAlreadySent", 0x015e, "The write to the master pseudo-TTY failed, because all dependents are closed and HUPCL was set"),  // NOSONAR
    JRPtyOrphanedWrite("JRPtyOrphanedWrite", 0x0160, "The write service is processing in a background, orphaned process group"),  // NOSONAR
    JRPtyOutputStopped("JRPtyOutputStopped", 0x0161, "Write cannot be processed, because output has stopped"),  // NOSONAR
    JRPtyNoData("JRPtyNoData", 0x0163, "Data or room is not available on the queue"),  // NOSONAR
    JRPtyOrphanedRead("JRPtyOrphanedRead", 0x0164, "The read service is processing in a background, orphaned process group"),  // NOSONAR
    JRPtySIGTTINBlocked("JRPtySIGTTINBlocked", 0x0165, "The process is in a background process group and SIGTTIN is blocked or ignored"),  // NOSONAR
    JRPtyNoBufStorage("JRPtyNoBufStorage", 0x0166, "Storage is not available for pseudo-TTY buffers"),  // NOSONAR
    JRFuncNotSupported("JRFuncNotSupported", 0x0167, "The function is not supported by device driver"),  // NOSONAR
    JRConv2TicksFailed("JRConv2TicksFailed", 0x0168, "The time value is too large to convert to ticks"),  // NOSONAR
    JRPtAttemptedCRStore("JRPtAttemptedCRStore", 0x0169, "Ptrace attempted to store into a control register"),  // NOSONAR
    JRPtAttemptedPSW0Store("JRPtAttemptedPSW0Store", 0x016a, "Ptrace attempted to store into the left half of PSW"),  // NOSONAR
    JRPtCellNotAvail("JRPtCellNotAvail", 0x016b, "There is not enough storage for ptrace"),  // NOSONAR
    JRPtDbdEqualsDbr("JRPtDbdEqualsDbr", 0x016c, "The ptrace debugger process ID (PID) is the same as the debugged PID"),  // NOSONAR
    JRPtDbdParentTerm("JRPtDbdParentTerm", 0x016d, "The ptrace debugged parent (debugger) ended"),  // NOSONAR
    JRPtDbdPidNotFound("JRPtDbdPidNotFound", 0x016e, "The ptrace target debugged process ID (PID) is incorrect"),  // NOSONAR
    JRPtDbrPidNotFound("JRPtDbrPidNotFound", 0x016f, "The ptrace debugger ended"),  // NOSONAR
    JRPtDbrZombie("JRPtDbrZombie", 0x0170, "Ptrace debugger is ending"),  // NOSONAR
    JRPtInvCallingMode("JRPtInvCallingMode", 0x0171, "The ptrace caller mode is incorrect"),  // NOSONAR
    JRPtInvDbdAddress("JRPtInvDbdAddress", 0x0172, "An incorrect address was supplied for the debugged process"),  // NOSONAR
    JRPtInvDbrAddress("JRPtInvDbrAddress", 0x0173, "An incorrect address was supplied for the debugger process"),  // NOSONAR
    JRPtInvFPRNumber("JRPtInvFPRNumber", 0x0174, "The ptrace call has an incorrect floating point register number"),  // NOSONAR
    JRPtInvGPRNumber("JRPtInvGPRNumber", 0x0175, "The ptrace call has an incorrect general register number"),  // NOSONAR
    JRPtInvLength("JRPtInvLength", 0x0176, "The ptrace length is incorrect"),  // NOSONAR
    JRPtInvNumberThreads("JRPtInvNumberThreads", 0x0177, "The ptrace target process has no threads"),  // NOSONAR
    JRPtInvPtraceState("JRPtInvPtraceState", 0x0178, "The ptrace mode of target process is incorrect"),  // NOSONAR
    JRPtInvRequest("JRPtInvRequest", 0x0179, "The ptrace request was not valid"),  // NOSONAR
    JRPtInvSignalNumber("JRPtInvSignalNumber", 0x017a, "The ptrace service does not have a valid signal number"),  // NOSONAR
    JRPtInvUareaOffset("JRPtInvUareaOffset", 0x017b, "The ptrace service does not have a valid offset into the user area"),  // NOSONAR
    JRPtOldDbrPidNotFound("JRPtOldDbrPidNotFound", 0x017c, "The ptrace original debugger ended"),  // NOSONAR
    JRPtStateError("JRPtStateError", 0x017d, "The ptrace service has detected an internal state error"),  // NOSONAR
    JRPtProcessNotPtraced("JRPtProcessNotPtraced", 0x017e, "The ptrace target process is not in ptrace mode"),  // NOSONAR
    JRPtProcessNotStopped("JRPtProcessNotStopped", 0x017f, "The ptrace target process was not stopped for ptrace"),  // NOSONAR
    JRPtProcessTerm("JRPtProcessTerm", 0x0180, "The ptrace target process ended"),  // NOSONAR
    JRPtRecoveryEntered("JRPtRecoveryEntered", 0x0181, "Ptrace error recovery was entered"),  // NOSONAR
    JRPtRestrictedProcess("JRPtRestrictedProcess", 0x0182, "The ptrace target process is restricted from debugging"),  // NOSONAR
    JRPtSigInterrupt("JRPtSigInterrupt", 0x0183, "The ptrace request was interrupted by a signal for the debugger"),  // NOSONAR
    JRTIMUnexpectedErr("JRTIMUnexpectedErr", 0x0184, "An error occurred in the times call"),  // NOSONAR
    JRExecUnexpectedErr("JRExecUnexpectedErr", 0x0185, "An error occurred in exec and execmvs services"),  // NOSONAR
    JRExecParmErr("JRExecParmErr", 0x0186, "An error occurred when copying parameters passed to the exec service"),  // NOSONAR
    JRChowntoPipe("JRChowntoPipe", 0x0187, "The fchown service was issued against a pipe"),  // NOSONAR
    JRChaudtoPipe("JRChaudtoPipe", 0x0188, "The fchaudit service was issued against a pipe"),  // NOSONAR
    JRExecKernErr("JRExecKernErr", 0x0189, "An error occurred in the exec call"),  // NOSONAR
    JRBadRBState("JRBadRBState", 0x018a, "The caller&apos;s request block state was incorrect"),  // NOSONAR
    JRSignalError("JRSignalError", 0x018b, "A signal error occurred"),  // NOSONAR
    JRInvalidSigProc("JRInvalidSigProc", 0x018c, "The mask address was incorrect"),  // NOSONAR
    JRBadAlet("JRBadAlet", 0x018d, "An incorrect ALET was given as input"),  // NOSONAR
    JRRMGUnexpectedErr("JRRMGUnexpectedErr", 0x018e, "An error occurred in the resource call"),  // NOSONAR
    JRQuiescing("JRQuiescing", 0x018f, "The call did not complete. The file system is not in an active state. This may be a temporary condition. One of the following conditions apply: (1) The file system is in the process of being unmounted or is unmounted. (2) An attempt to unmount the file system occurred, but failed. The file system is in the IMMEDIATE UNMOUNT ATTEMPTED state. (3) The file system is in the process of remounting. (4) The file system ownership is in the process of moving to another system. (5) The file system is NOT ACTIVE or UNOWNED"),  // NOSONAR
    JRPtyInvalidAction("JRPtyInvalidAction", 0x0190, "The action code is incorrect"),  // NOSONAR
    JRPtyInvalidCcflag("JRPtyInvalidCcflag", 0x0191, "The c_cflag bits are incorrect"),  // NOSONAR
    JRPtyInvalidClflag("JRPtyInvalidClflag", 0x0192, "The c_lflag bits are incorrect"),  // NOSONAR
    JRPtyInvalidCiflag("JRPtyInvalidCiflag", 0x0193, "The c_iflag bits are incorrect"),  // NOSONAR
    JRPtyInvalidCoflag("JRPtyInvalidCoflag", 0x0194, "The c_oflag bits are incorrect"),  // NOSONAR
    JRPtyInvalidOutBaud("JRPtyInvalidOutBaud", 0x0195, "The output baud rate is incorrect"),  // NOSONAR
    JRPtyInvalidInBaud("JRPtyInvalidInBaud", 0x0196, "The input baud rate is incorrect"),  // NOSONAR
    JRPtyBgCall("JRPtyBgCall", 0x0197, "This is a background process"),  // NOSONAR
    JRInvIoctlCmd("JRInvIoctlCmd", 0x0198, "The input command value is incorrect"),  // NOSONAR
    JRPtyNoCntlTerm("JRPtyNoCntlTerm", 0x0199, "The caller has no controlling terminal"),  // NOSONAR
    JRPtyDiffSession("JRPtyDiffSession", 0x019a, "This is not the caller&apos;s controlling terminal"),  // NOSONAR
    JRecoveryInvoked("JRecoveryInvoked", 0x019b, "Recovery code was invoked"),  // NOSONAR
    JRPtKillFailed("JRPtKillFailed", 0x019c, "The ptrace kill signal failed"),  // NOSONAR
    JRPtPtrbNotAvail("JRPtPtrbNotAvail", 0x019e, "The ptrace service cannot complete the request due to synchronization error"),  // NOSONAR
    JRPtBadEnvironment("JRPtBadEnvironment", 0x019f, "The ptrace request handler environment is not valid"),  // NOSONAR
    JRPtEdIsAuthorized("JRPtEdIsAuthorized", 0x01a0, "The ptrace debugged process is running in supervisor state"),  // NOSONAR
    JRPtCsvinfoFailed("JRPtCsvinfoFailed", 0x01a1, "Ptrace received an error from CSVINFO"),  // NOSONAR
    JRPtLDBufferTooSmall("JRPtLDBufferTooSmall", 0x01a2, "The ptrace loader information request buffer is too small"),  // NOSONAR
    JRPtLDRMODE64LoadModAn("JRPtLDRMODE64LoadModAn", 0x01a3, "An RMODE64 load module was encountered but some information is missing"),  // NOSONAR
    JRPtDbrParentEqualsDbdThe("JRPtDbrParentEqualsDbdThe", 0x01a4, "The ptrace debugger parent PID is the same as debugged PID"),  // NOSONAR
    JRPtyNotPGLeaderThe("JRPtyNotPGLeaderThe", 0x01a5, "The process is not a process group leader"),  // NOSONAR
    JRPtyNotSlaveUnsupported("JRPtyNotSlaveUnsupported", 0x01a6, "Unsupported function against master TTY"),  // NOSONAR
    JRPtyBadQueSel("JRPtyBadQueSel", 0x01a7, "The queue selector is not valid"),  // NOSONAR
    JRPtyNoSessLeader("JRPtyNoSessLeader", 0x01a8, "The system is unable to locate the session leader"),  // NOSONAR
    JRNoCTTY("JRNoCTTY", 0x01a9, "There is no controlling terminal for this process"),  // NOSONAR
    JRPtyHupclClose("JRPtyHupclClose", 0x01aa, "The depndent pseudo-TTY file was previously closed with the termios HUPCL flag set"),  // NOSONAR
    JRFsInUse("JRFsInUse", 0x01ab, "The requested file system is still in use"),  // NOSONAR
    JRPtyInvalidPgid("JRPtyInvalidPgid", 0x01ac, "The requested process group ID is not valid"),  // NOSONAR
    JRPtyNotInSession("JRPtyNotInSession", 0x01ad, "The process group ID (PGID) does not exist in the caller&apos;s session"),  // NOSONAR
    JRBrlmNotActive("JRBrlmNotActive", 0x01ae, "The byte-range lock manager is not active"),  // NOSONAR
    JRBrlmFileLockRecycling("JRBrlmFileLockRecycling", 0x01af, "File lock is being recycled. Do not use until the file is closed by all users"),  // NOSONAR
    JRBrlmBadFileType("JRBrlmBadFileType", 0x01b0, "Byte-range locking can be performed only on regular files"),  // NOSONAR
    JRBrlmNoReadAccess("JRBrlmNoReadAccess", 0x01b1, "Shared byte-range locks are only for files open for read"),  // NOSONAR
    JRBrlmNoWriteAccess("JRBrlmNoWriteAccess", 0x01b2, "Exclusive byte-range locks are only for files open for write"),  // NOSONAR
    JRBrlmBadL_Type("JRBrlmBadL_Type", 0x01b3, "A byte-range lock request specified an l_type that is not valid"),  // NOSONAR
    JRBrlmInvalidRange("JRBrlmInvalidRange", 0x01b4, "A byte-range lock extends to before the start of the file"),  // NOSONAR
    JRBrlmBadL_Whence("JRBrlmBadL_Whence", 0x01b5, "A byte-range lock request specified an l_whence that is not valid"),  // NOSONAR
    JRSecurityInternalError("JRSecurityInternalError", 0x01b6, "Internal error in security product"),  // NOSONAR
    JRBrlmRangeNotAvailable("JRBrlmRangeNotAvailable", 0x01b7, "All or part of requested range is held by another user"),  // NOSONAR
    JRBrlmDeadLockDetected("JRBrlmDeadLockDetected", 0x01b8, "Waiting on the specified range causes a deadlock"),  // NOSONAR
    JRBrlmSignalPosted("JRBrlmSignalPosted", 0x01b9, "While the process was waiting for a byte-range lock, a signal was posted"),  // NOSONAR
    JRPtSigactionFailed("JRPtSigactionFailed", 0x01bb, "Ptrace sigaction failed"),  // NOSONAR
    JRPtSigprocmaskFailed("JRPtSigprocmaskFailed", 0x01bc, "Ptrace sigprocmask failed"),  // NOSONAR
    JRBrlmBadL_Len("JRBrlmBadL_Len", 0x01bd, "A byte-range lock request specified an incorrect l_len"),  // NOSONAR
    JRReadUserStorageFailed("JRReadUserStorageFailed", 0x01bf, "A read error occurred on the user data area passed to the service"),  // NOSONAR
    JRWriteUserStorageFailed("JRWriteUserStorageFailed", 0x01c0, "A write error occurred on the user data area passed to the service"),  // NOSONAR
    JRBrlmAlreadyWaiting("JRBrlmAlreadyWaiting", 0x01c2, "Request includes a range already being waited on"),  // NOSONAR
    JRBrlmPromotePending("JRBrlmPromotePending", 0x01c3, "Another user is waiting to promote the requested range"),  // NOSONAR
    JRPtyNoPtyrStorage("JRPtyNoPtyrStorage", 0x01c4, "There is not enough storage in the kernel address space"),  // NOSONAR
    JRBrlmProcessBroken("JRBrlmProcessBroken", 0x01c5, "This process has been marked broken for byte locking"),  // NOSONAR
    JRPtyConnectionInop("JRPtyConnectionInop", 0x01c6, "The pseudo-TTY connection is inoperative"),  // NOSONAR
    JRDtuErr("JRDtuErr", 0x01c8, "An error occurred during process signal initialization"),  // NOSONAR
    JRBrlmUnlockWhileWait("JRBrlmUnlockWhileWait", 0x01c9, "The unlock service is not valid while the process is waiting for an intersecting lock"),  // NOSONAR
    JRBrlmObjAndProcBroken("JRBrlmObjAndProcBroken", 0x01ca, "The object and process are marked broken for byte locking"),  // NOSONAR
    JROutOfCells("JROutOfCells", 0x01cb, "Out of nonexpandable cell pool cells"),  // NOSONAR
    JRBadTree("JRBadTree", 0x01cc, "The session or group tree is broken"),  // NOSONAR
    JRFd2TooSmall("JRFd2TooSmall", 0x01cd, "The second file descriptor cannot be smaller than the first"),  // NOSONAR
    JRPtCreateError("JRPtCreateError", 0x01ce, "An unexpected error occurred in the BPX1PTC service"),  // NOSONAR
    JRNotAuthorized("JRNotAuthorized", 0x01cf, "Unauthorized caller of BPX1PTC in an authorized environment"),  // NOSONAR
    JRPtExitError("JRPtExitError", 0x01d0, "An unexpected error occurred in the BPXPTEXT service"),  // NOSONAR
    JRPtCancelError("JRPtCancelError", 0x01d1, "An unexpected error occurred in the BPX1PTB service"),  // NOSONAR
    JRPtDetachError("JRPtDetachError", 0x01d2, "An unexpected error occurred in the BPX1PTD service"),  // NOSONAR
    JRPtatEye("JRPtatEye", 0x01d3, "The pthread attribute area contains an incorrect eyecatcher"),  // NOSONAR
    JRPtatAddrError("JRPtatAddrError", 0x01d4, "The pthread attribute area address is incorrect"),  // NOSONAR
    JRPTCNotSupp("JRPTCNotSupp", 0x01d5, "BPX1PTC is not supported from the calling task"),  // NOSONAR
    JRAllFilesNotClosed("JRAllFilesNotClosed", 0x01d6, "All requested files were not closed"),  // NOSONAR
    JRExitRtnError("JRExitRtnError", 0x01d7, "An error occurred in the user exit called by the exec"),  // NOSONAR
    JRThreadTerm("JRThreadTerm", 0x01d8, "The service was rejected because the requesting thread is terminating"),  // NOSONAR
    JRLightWeightThid("JRLightWeightThid", 0x01da, "The thread specified is a lightweight thread"),  // NOSONAR
    JRAlreadyDetached("JRAlreadyDetached", 0x01db, "The thread specified is already detached"),  // NOSONAR
    JRThreadNotFound("JRThreadNotFound", 0x01dc, "The thread specified was not found"),  // NOSONAR
    JRHeavyWeight("JRHeavyWeight", 0x01de, "The new thread was not started and the existing thread is a heavyweight thread"),  // NOSONAR
    JRGetFirst("JRGetFirst", 0x01df, "The first call did not specify PTGetNewThread"),  // NOSONAR
    JRAlreadyJoined("JRAlreadyJoined", 0x01e0, "The thread specified was already joined by another thread"),  // NOSONAR
    JRPTJoinError("JRPTJoinError", 0x01e1, "An error occurred in the BPX1PTJ service"),  // NOSONAR
    JRJoinExitStatPtr("JRJoinExitStatPtr", 0x01e2, "The address of the exit status parameter is not correct"),  // NOSONAR
    JRJoinToSelf("JRJoinToSelf", 0x01e3, "The thread attempted to join to itself"),  // NOSONAR
    JRJoinLoop("JRJoinLoop", 0x01e4, "The connection would result in thread waiting for itself"),  // NOSONAR
    JRJoinIPTExited("JRJoinIPTExited", 0x01e5, "The thread attempted to connect to IPT after IPT had already exited"),  // NOSONAR
    JRJoinAsyncNoFreeTasks("JRJoinAsyncNoFreeTasks", 0x01e6, "The connection ended with an asynchronous thread and no tasks available"),  // NOSONAR
    JRAlreadyPtexited("JRAlreadyPtexited", 0x01e7, "The calling thread has already been exited through a call to the BPX1PTX service"),  // NOSONAR
    JRAlreadyTerminated("JRAlreadyTerminated", 0x01e8, "The thread specified has already ended"),  // NOSONAR
    JRCallRtmErr("JRCallRtmErr", 0x01e9, "The CALLRTM macro returned a return code with an error"),  // NOSONAR
    JRBrokenBrlmRecycling("JRBrokenBrlmRecycling", 0x01ea, "The byte-range-lock manager is broken and is currently recycling"),  // NOSONAR
    JRPtatSysOff("JRPtatSysOff", 0x01eb, "The system offset value in the pthread attribute area is incorrect"),  // NOSONAR
    JRPtatSysLen("JRPtatSysLen", 0x01ec, "The system length value in the pthread attribute area is incorrect"),  // NOSONAR
    JRPtatLen("JRPtatLen", 0x01ed, "The total length value in the pthread attribute area is incorrect"),  // NOSONAR
    JRRMGWrongDataLen("JRRMGWrongDataLen", 0x01ee, "Resource data area length is not correct for this release"),  // NOSONAR
    JRInvOption("JRInvOption", 0x01ef, "Incorrect option specified on call to BPX1PTX"),  // NOSONAR
    JRInitRtn("JRInitRtn", 0x01f0, "The initialization routine is not valid for the current environment"),  // NOSONAR
    JRPtatWeight("JRPtatWeight", 0x01f1, "The pthread attribute area contains an incorrect weight value"),  // NOSONAR
    JRPtatSyncType("JRPtatSyncType", 0x01f2, "The pthread attribute area contains an incorrect Sync Type value"),  // NOSONAR
    JRPtatDetachState("JRPtatDetachState", 0x01f3, "The pthread attribute area contains an incorrect detach state value"),  // NOSONAR
    JRNoSuchPid("JRNoSuchPid", 0x01f4, "The process ID is incorrect"),  // NOSONAR
    JRPidEQSessLeader("JRPidEQSessLeader", 0x01f5, "The process ID is a session leader"),  // NOSONAR
    JRTooMany("JRTooMany", 0x01f6, "The event list specified contained more than one event"),  // NOSONAR
    JRPidDifferentSession("JRPidDifferentSession", 0x01f7, "The process ID is in a session different from the caller"),  // NOSONAR
    JRExecAfterFork("JRExecAfterFork", 0x01f8, "The process ID was called by the exec service after the fork service"),  // NOSONAR
    JRTimeOutNotAuth("JRTimeOutNotAuth", 0x01f9, "The caller to BPX1CPO service specified the CW_TIMEOUT event but is not authorized"),  // NOSONAR
    JRNotDescendant("JRNotDescendant", 0x01fa, "The process ID is not an immediate descendant of the caller"),  // NOSONAR
    JRPgidDifferentSession("JRPgidDifferentSession", 0x01fb, "Process group ID is in a session different from the caller"),  // NOSONAR
    JRCallerIsPgLeader("JRCallerIsPgLeader", 0x01fc, "The caller is already a process group leader"),  // NOSONAR
    JRNullMask("JRNullMask", 0x01fd, "The caller specified a null signal mask"),  // NOSONAR
    JRRdlBuffLenInvalid("JRRdlBuffLenInvalid", 0x01fe, "The length of the buffer is less than zero"),  // NOSONAR
    JRPswKeyNotValid("JRPswKeyNotValid", 0x0200, "The PSW key of the caller is not a valid key"),  // NOSONAR
    JRAlreadySigSetUp("JRAlreadySigSetUp", 0x0201, "BPX1MSS found the process already set up for signals"),  // NOSONAR
    JRNotSigSetUp("JRNotSigSetUp", 0x0202, "The service found the current task was not set up for signals"),  // NOSONAR
    JREndingSlashSymlink("JREndingSlashSymlink", 0x0203, "The pathname ended with slash on the symlink service"),  // NOSONAR
    JRUndefEvents("JRUndefEvents", 0x0204, "The specified event list contains undefined events"),  // NOSONAR
    JRNoEvents("JRNoEvents", 0x0205, "The specified event list is zero"),  // NOSONAR
    JRIPTCannotLeave("JRIPTCannotLeave", 0x0206, "The caller has daughter tasks. Termination is denied"),  // NOSONAR
    JRNotSetup("JRNotSetup", 0x0207, "The thread is not set up for cond_wait or cond_timed_wait"),  // NOSONAR
    JRAlreadySetup("JRAlreadySetup", 0x0208, "The thread is already set up for cond_setup, cond_wait, or cond_timed_wait"),  // NOSONAR
    JROutOfRange("JROutOfRange", 0x0209, "The value specified for a parameter is outside the allowable range"),  // NOSONAR
    JRNanoSecondsTooBig("JRNanoSecondsTooBig", 0x0210, "The value specified for nanoseconds is outside the allowable range"),  // NOSONAR
    JRTimeOut("JRTimeOut", 0x0211, "The time for the service to wait has expired"),  // NOSONAR
    JRDup2Error("JRDup2Error", 0x0212, "A problem occurred with the requested file descriptor"),  // NOSONAR
    JRAccept("JRAccept", 0x0213, "Vnode operation accept is not supported by this file system"),  // NOSONAR
    JRBind("JRBind", 0x0214, "Vnode operation bind is not supported by this file system"),  // NOSONAR
    JRConnect("JRConnect", 0x0215, "Vnode operation connect is not supported by this file system"),  // NOSONAR
    JRGetHost("JRGetHost", 0x0216, "Vnode operation gethost is not supported by this file system"),  // NOSONAR
    JRGetName("JRGetName", 0x0217, "Vnode operation getname is not supported by this file system"),  // NOSONAR
    JRSockOpt("JRSockOpt", 0x0218, "Vnode operation sockopt is not supported by this file system"),  // NOSONAR
    JRListen("JRListen", 0x0219, "Vnode operation listen is not supported by this file system"),  // NOSONAR
    JRReadWriteV("JRReadWriteV", 0x021a, "Vnode operation readwritev is not supported by this file system"),  // NOSONAR
    JRSndRcv("JRSndRcv", 0x021b, "Vnode operation sndrcv is not supported by this file system"),  // NOSONAR
    JRSndToRcvFm("JRSndToRcvFm", 0x021c, "Vnode operation sndtorcvfm is not supported by this file system"),  // NOSONAR
    JRSrMsg("JRSrMsg", 0x021d, "Vnode operation srmsg is not supported by this file system"),  // NOSONAR
    JRSelect("JRSelect", 0x021e, "Vnode operation select is not supported by this file system"),  // NOSONAR
    JRSetPeer("JRSetPeer", 0x021f, "Vnode operation setpeer is not supported by this file system"),  // NOSONAR
    JRShutdown("JRShutdown", 0x0220, "Vnode operation shutdown is not supported by this file system"),  // NOSONAR
    JRSocket("JRSocket", 0x0221, "VFS operation socket is not supported by this file system"),  // NOSONAR
    JRNoSocket("JRNoSocket", 0x0222, "The requested operation cannot be performed on a socket file descriptor"),  // NOSONAR
    JRMustBeSocket("JRMustBeSocket", 0x0223, "The requested operation is only valid on a socket file descriptor"),  // NOSONAR
    JRTargetEnding("JRTargetEnding", 0x0224, "The target process is ending"),  // NOSONAR
    JRQuiesceTypeInvalid("JRQuiesceTypeInvalid", 0x0225, "The quiescetype specified by the caller is invalid"),  // NOSONAR
    JRQuiesceInProgress("JRQuiesceInProgress", 0x0226, "Another thread in the process has already requested quiescing of all threads"),  // NOSONAR
    JRLastThread("JRLastThread", 0x0227, "The last pthread is exiting when the PTFAILIFLASTTHREAD option is specified"),  // NOSONAR
    JRDomainNotSupported("JRDomainNotSupported", 0x0228, "The requested domain is not supported"),  // NOSONAR
    JRNetwork("JRNetwork", 0x0229, "Vfs operation network is not supported by this file system"),  // NOSONAR
    JROutofVdeCells("JROutofVdeCells", 0x022a, "All Vde cells have been allocated"),  // NOSONAR
    JRTokenMax("JRTokenMax", 0x022b, "The maximum number of Vnode tokens have been allocated for this process"),  // NOSONAR
    JRVTokenFreed("JRVTokenFreed", 0x022c, "The Vnode token has already been released"),  // NOSONAR
    JRWrongPID("JRWrongPID", 0x022d, "The process does not own this Vde"),  // NOSONAR
    JRStaleVnodeTok("JRStaleVnodeTok", 0x022e, "The Vnode token is stale"),  // NOSONAR
    JRInvalidVnodeTok("JRInvalidVnodeTok", 0x022f, "The Vnode token does not point to a Vde"),  // NOSONAR
    JRNotRegisteredServer("JRNotRegisteredServer", 0x0230, "The process is not a registered server"),  // NOSONAR
    JRInvalidRegType("JRInvalidRegType", 0x0231, "The server type supplied in NRegSType is not valid"),  // NOSONAR
    JRNameTooLong("JRNameTooLong", 0x0232, "The name supplied is longer than the allowed maximum"),  // NOSONAR
    JRAlreadyRegistered("JRAlreadyRegistered", 0x0233, "The process is already registered as a server"),  // NOSONAR
    JRInvalidNReg("JRInvalidNReg", 0x0234, "An incorrect NReg parameter list was passed"),  // NOSONAR
    JRNoLeadingSlash("JRNoLeadingSlash", 0x0235, "The pathname does not begin with /"),  // NOSONAR
    JRStaleVfsTok("JRStaleVfsTok", 0x0236, "The VFS token is stale"),  // NOSONAR
    JRSmallAttr("JRSmallAttr", 0x0237, "The Attr length parameter was too small"),  // NOSONAR
    JRSmallMnte("JRSmallMnte", 0x0238, "The Mnte length parameter was too small"),  // NOSONAR
    JRRwNotRegFile("JRRwNotRegFile", 0x0239, "The rdwr call is valid only on a regular file"),  // NOSONAR
    JRDubSetting("JRDubSetting", 0x0240, "The dub setting value specified on the BPX1SDD service call is not correct"),  // NOSONAR
    JRInvalidAtt("JRInvalidAtt", 0x0241, "The Attribute structure passed to BPX1CHR or BPX1FCR was not valid"),  // NOSONAR
    JRInvalidOSS("JRInvalidOSS", 0x0242, "The OSS is not valid"),  // NOSONAR
    JRSmallFSAttr("JRSmallFSAttr", 0x0243, "The FSAttr length parameter was too small"),  // NOSONAR
    JRAPFAuthChange("JRAPFAuthChange", 0x0244, "A local process exec or a local spawn, running in an authorized state attempted to load and execute an unauthorized program"),  // NOSONAR
    JRIDChange("JRIDChange", 0x0245, "A local process exec tried to change the UID / GID"),  // NOSONAR
    JROtherProcesses("JROtherProcesses", 0x0246, "An exec was attempted that would terminate the other processes in the address space"),  // NOSONAR
    JRPtRequestDenied("JRPtRequestDenied", 0x0247, "The ptrace request is not allowed for the current debugged program environment"),  // NOSONAR
    JRInvalidFUio("JRInvalidFUio", 0x0248, "An incorrect FUio area was passed"),  // NOSONAR
    JRTokDir("JRTokDir", 0x0249, "The VNODE token specifies a directory"),  // NOSONAR
    JRTokNotDir("JRTokNotDir", 0x024a, "The VNODE token does not specify a directory"),  // NOSONAR
    JRInvalidAttr("JRInvalidAttr", 0x024b, "The supplied Attribute structure was not valid"),  // NOSONAR
    JRMaxTasks("JRMaxTasks", 0x024c, ""),  // NOSONAR
    JRMaxSockets("JRMaxSockets", 0x024d, "The number of active sockets is equal to the value specified on the MAXSOCKETS parmlib statement"),  // NOSONAR
    JROutofLatches("JROutofLatches", 0x024e, "All latches in the socket latch set pool are assigned to Snodes"),  // NOSONAR
    JROutofSocketCells("JROutofSocketCells", 0x024f, "The system was unable to obtain a cell from the sockets cell pool"),  // NOSONAR
    JRNotDir("JRNotDir", 0x0250, "The name does not specify a directory"),  // NOSONAR
    JROutofSocketsNodeCells("JROutofSocketsNodeCells", 0x0251, "The system was unable to obtain a cell from the sockets node cell pool"),  // NOSONAR
    JRExternalLink("JRExternalLink", 0x0252, "An external symbolic link was found but is not supported"),  // NOSONAR
    JRSocketNotFound("JRSocketNotFound", 0x0253, "The requested socket was not found, or is not active"),  // NOSONAR
    JRSocketNamed("JRSocketNamed", 0x0254, "A Bind request was received for a socket that was previously named"),  // NOSONAR
    JRInvalidCallingState("JRInvalidCallingState", 0x0255, "The caller is not supervisor state and key 0"),  // NOSONAR
    JRPidIsCaller("JRPidIsCaller", 0x0256, "Pid specifies the PID for the calling process"),  // NOSONAR
    JRPidNoLatch("JRPidNoLatch", 0x0257, "Unable to obtain latch for PID"),  // NOSONAR
    JROutofSocketDataCells("JROutofSocketDataCells", 0x0258, "The system was unable to obtain a cell from the sockets data buffer cell pool"),  // NOSONAR
    JRSocaNwkBitOn("JRSocaNwkBitOn", 0x0259, "Duplicate NETWORK parmlib statements exist"),  // NOSONAR
    JRISGLCRTFailed("JRISGLCRTFailed", 0x025a, "Latch set service ISGLCRT failed"),  // NOSONAR
    JRListenNotAccepted("JRListenNotAccepted", 0x025b, "A listen syscall was issued for a socket that has not been bound, for a socket that is already a server, or for a socket that is already connected"),  // NOSONAR
    JRSocketClosed("JRSocketClosed", 0x025c, "An attempt was made to read, write or connect to a socket that is closed"),  // NOSONAR
    JRTooManyThds("JRTooManyThds", 0x025d, "An attempt was made to create another thread but the process limit for pthreads has already been reached"),  // NOSONAR
    JRSocketCallParmError("JRSocketCallParmError", 0x025e, "A socket syscall contains incorrect parameters"),  // NOSONAR
    JRRecovery("JRRecovery", 0x025f, "The recovery VFS operation is not supported"),  // NOSONAR
    JRInvalidRoutine("JRInvalidRoutine", 0x0260, "An incorrect routine address was passed"),  // NOSONAR
    JRRoutineError("JRRoutineError", 0x0261, "An error occurred while the user-provided routine was in control"),  // NOSONAR
    JRNoLists("JRNoLists", 0x0262, "A Select request was issued without a read, write or exception list"),  // NOSONAR
    JRListTooLong("JRListTooLong", 0x0263, "The read, write or exception list is too long"),  // NOSONAR
    JRListTooShort("JRListTooShort", 0x0264, "The read, write or exception list is too short to contain the specified number of file descriptors and message queue identifiers"),  // NOSONAR
    JRMSOutOfRange("JRMSOutOfRange", 0x0265, "The value specified for microseconds is outside the allowable range"),  // NOSONAR
    JRSecOutOfRange("JRSecOutOfRange", 0x0266, "The value specified for seconds is outside the allowable range"),  // NOSONAR
    JRNoFds("JRNoFds", 0x0267, "The read, write or exception list did not contain any file descriptors, or the Number_FDs parameter was not greater than 0"),  // NOSONAR
    JRPtQuiesceFailed("JRPtQuiesceFailed", 0x0268, "Ptrace quiesce failed"),  // NOSONAR
    JRIncorrectSocketType("JRIncorrectSocketType", 0x0269, "The socket type is incorrect for the request"),  // NOSONAR
    JRWouldBlock("JRWouldBlock", 0x026a, "The O_NONBLOCK flag is set and this request would block"),  // NOSONAR
    JRExceedsBackLogCount("JRExceedsBackLogCount", 0x026b, "This connect request exceeds the connect backlog count that was specified on the Listen request"),  // NOSONAR
    JRLevelNotSupp("JRLevelNotSupp", 0x026c, "The value specified for Level is not supported by the physical file system"),  // NOSONAR
    JRSetNotSupp("JRSetNotSupp", 0x026d, "The UNIX Domain Socket File System does not support the setting of socket options"),  // NOSONAR
    JRInvOpOpt("JRInvOpOpt", 0x026e, "The Option name specified is not valid"),  // NOSONAR
    JRBuff("JRBuff", 0x026f, "The buffer for return information is too small"),  // NOSONAR
    JROptNotSupp("JROptNotSupp", 0x0270, "The Option name specified is not supported"),  // NOSONAR
    JRPtAsyncThread("JRPtAsyncThread", 0x0271, "The ptrace request is not allowed because the target thread is asynchronous"),  // NOSONAR
    JRSocketNotCon("JRSocketNotCon", 0x0272, "The requested socket is not connected"),  // NOSONAR
    JRPtyNoPtysStorage("JRPtyNoPtysStorage", 0x0273, "There is insufficient storage in the kernel address space"),  // NOSONAR
    JRUnknownKPRC("JRUnknownKPRC", 0x0274, "Kernpost returned an invalid return code"),  // NOSONAR
    JRSockRdwrSignal("JRSockRdwrSignal", 0x0275, "Signal interrupt during socket read or write processing"),  // NOSONAR
    JRSockBufMax("JRSockBufMax", 0x0276, "There is insufficient storage for the socket message"),  // NOSONAR
    JRInvalidMsgH("JRInvalidMsgH", 0x0277, "The socket message header is not correct"),  // NOSONAR
    JRSockNoName("JRSockNoName", 0x0278, "The request requires a socket name structure"),  // NOSONAR
    JRInvalidServerNameLen("JRInvalidServerNameLen", 0x0279, "The server name length supplied in NRegSNameLen is too long or negative"),  // NOSONAR
    JRNoOOBDataAvail("JRNoOOBDataAvail", 0x027a, "The MSG_OOB flag is set but no OOB data is available or OOB data is inline"),  // NOSONAR
    JRIncorrectTypeForFlag("JRIncorrectTypeForFlag", 0x027b, "MSG_OOB flag is set but socket is not a stream socket"),  // NOSONAR
    JRSockShutDown("JRSockShutDown", 0x027c, "Socket has been shutdown"),  // NOSONAR
    JRMSGHInvalid("JRMSGHInvalid", 0x027d, "msg_controllen or a cmsg_len has an incorrect length"),  // NOSONAR
    JRNoAddrSpace("JRNoAddrSpace", 0x027e, "The caller&apos;s address space name cannot be determined"),  // NOSONAR
    JRNoName("JRNoName", 0x0280, "The name length is zero"),  // NOSONAR
    JRListLenBad("JRListLenBad", 0x0281, "The length of one or all of the input bit lists is not a multiple of four or is not more than 256 bytes"),  // NOSONAR
    JRInvUserOp("JRInvUserOp", 0x0282, "The value specifed for the User Option was not 0 or 1"),  // NOSONAR
    JRSocketProtocolInvalid("JRSocketProtocolInvalid", 0x0283, "The protocol argument on the socket or socketpair syscall was not 0. The physical file system only supports a value of 0 for the protocol"),  // NOSONAR
    JRSteplibAllocateBad("JRSteplibAllocateBad", 0x0284, "Dynamic allocation failed for a data set while attempting to build the STEPLIB concatenation"),  // NOSONAR
    JRSteplibConcatBad("JRSteplibConcatBad", 0x0285, "Dynamic concatenation failed while attempting to build the STEPLIB concatenation"),  // NOSONAR
    JRSteplibOpenBad("JRSteplibOpenBad", 0x0286, "Open of the steplib dd failed while attempting to build the STEPLIB concatenation"),  // NOSONAR
    JRSteplibDcbObtainBad("JRSteplibDcbObtainBad", 0x0287, "Storage could not be obtained for the STEPLIB dcb while attempting to build the STEPLIB concatenation"),  // NOSONAR
    JRNameExists("JRNameExists", 0x0288, "The name specified in the request is already in use"),  // NOSONAR
    JRListenNotDone("JRListenNotDone", 0x0289, "The socket is not ready to accept connections"),  // NOSONAR
    JRSteplibDSORGBad("JRSteplibDSORGBad", 0x028a, "A nonpartitioned data set was specified in the STEPLIB concatenation"),  // NOSONAR
    JRSteplibTooBig("JRSteplibTooBig", 0x028b, "More than 255 data sets were specified in the STEPLIB concatenation"),  // NOSONAR
    JRListenNotStream("JRListenNotStream", 0x028c, "A listen syscall was issued for a socket that is not a stream socket. Listen is only valid for stream sockets"),  // NOSONAR
    JRListenAlreadyDone("JRListenAlreadyDone", 0x028d, "A listen request has already been completed"),  // NOSONAR
    JRSTLActionInvalid("JRSTLActionInvalid", 0x028e, "The value specified for the action parameter is not valid"),  // NOSONAR
    JRSTLTasksInvalid("JRSTLTasksInvalid", 0x028f, "The value specified for the task limit is not valid"),  // NOSONAR
    JRSTLThreadsInvalid("JRSTLThreadsInvalid", 0x0290, "The value specified for the thread limit is not valid"),  // NOSONAR
    JRTcpError("JRTcpError", 0x0291, "Tcp returned an error identified by the return code"),  // NOSONAR
    JRNoSpace("JRNoSpace", 0x0292, "Pthread_create failed, due to unavailable user address space storage"),  // NOSONAR
    JRMaxTcpPathIds("JRMaxTcpPathIds", 0x0293, "The maximum number of Tcp/Ip path IDs has been exceeded"),  // NOSONAR
    JRPtNotXtdEvent("JRPtNotXtdEvent", 0x0294, "The ptrace request is not allowed because the target process is not stopped for an extended event"),  // NOSONAR
    JRPtTooManyEvents("JRPtTooManyEvents", 0x0295, "The ptrace PT_EVENTS request attempted to add more events than the specified maximum"),  // NOSONAR
    JRTcpNotActive("JRTcpNotActive", 0x0296, "No AF_INET socket provider is active"),  // NOSONAR
    JRMaxInetSockets("JRMaxInetSockets", 0x0297, "The number of active INET sockets is equal to the value specified on the MAXSOCKETS parmlib statement"),  // NOSONAR
    JRECBerror("JRECBerror", 0x0298, "The last ECB pointer in the list of ECB pointers does not have the high order bit (&apos;80000000&apos;x) set on to indicate that it is the last ECB pointer in the list"),  // NOSONAR
    JRECBListBad("JRECBListBad", 0x0299, "An error occurred accessing the list of pointers to ECBs on a call to the MVSpauseInit service"),  // NOSONAR
    JRECBstateBad("JRECBstateBad", 0x029a, "An error occurred accessing one or more of the input ECBs on a call to the MVSpauseInit or MVSpause service"),  // NOSONAR
    JRECBListNotSetup("JRECBListNotSetup", 0x029b, "The MVSpause service was called but the MVSpauseInit service was never called to prepare for an MVSpause"),  // NOSONAR
    JRSocketTypeNotSupported("JRSocketTypeNotSupported", 0x029c, "The requested socket type is not supported"),  // NOSONAR
    JREcbWaitBitOn("JREcbWaitBitOn", 0x029d, "The wait bit (the high order bit) was on in the specified event control block (ECB)"),  // NOSONAR
    JRInvalidVlok("JRInvalidVlok", 0x029e, "The supplied VLock structure was not valid"),  // NOSONAR
    JRInvalidServerPID("JRInvalidServerPID", 0x029f, "The supplied VLokServerPID value was not valid"),  // NOSONAR
    JRNoLockerToken("JRNoLockerToken", 0x02a0, "No Locker token was specified in the Vlock structure"),  // NOSONAR
    JRBrlmObjectMissing("JRBrlmObjectMissing", 0x02a1, "No Object was specified in the Vlock structure"),  // NOSONAR
    JRBrlmWrongLevel("JRBrlmWrongLevel", 0x02a2, "The byte-range lock manager does not support the request"),  // NOSONAR
    JRBrlmLockerNotRegistered("JRBrlmLockerNotRegistered", 0x02a3, "The Locker token specified in the Vlock structure is not registered"),  // NOSONAR
    JRMultiProc("JRMultiProc", 0x02a4, "The specified function is not supported in an address space running multiple processes"),  // NOSONAR
    JRTaskAcee("JRTaskAcee", 0x02a5, "The specified function is not supported when a task level ACEE is active for the calling task"),  // NOSONAR
    JRUserNameLenError("JRUserNameLenError", 0x02a6, "The user name length value was incorrect"),  // NOSONAR
    JRPasswordLenError("JRPasswordLenError", 0x02a7, "The pass length value was incorrect"),  // NOSONAR
    JRNewPasswordLenError("JRNewPasswordLenError", 0x02a8, "The new pass length value was incorrect"),  // NOSONAR
    JRMixedSecurityEnv("JRMixedSecurityEnv", 0x02a9, "The specified function is not supported after a seteuid has changed the security environment of the caller"),  // NOSONAR
    JRQuiesceTerm("JRQuiesceTerm", 0x02aa, "A pthread_quiesce(term) is already in progress on another thread in the caller&apos;s process"),  // NOSONAR
    JRQuiesceForce("JRQuiesceForce", 0x02ab, "A pthread_quiesce(force) is already in progress on another thread in the caller&apos;s process"),  // NOSONAR
    JRQuiesceFreeze("JRQuiesceFreeze", 0x02ac, "A pthread_quiesce(freeze) is already in progress on another thread in the caller&apos;s process"),  // NOSONAR
    JRQuiesceFreezeForce("JRQuiesceFreezeForce", 0x02ad, "A pthread_quiesce(freeze_force) is already in progress on another thread in the caller&apos;s process"),  // NOSONAR
    JRTso("JRTso", 0x02ae, "The specified function is not supported in a TSO space running multiple processes"),  // NOSONAR
    JREnvDirty("JREnvDirty", 0x02af, "The specified function is not supported in an address space where a load was done that is not program controlled"),  // NOSONAR
    JRIpcBadID("JRIpcBadID", 0x0302, "The ID is not valid or has been removed from the system"),  // NOSONAR
    JRIpcDenied("JRIpcDenied", 0x0303, "Access was denied because the caller does not have the correct permission"),  // NOSONAR
    JRIpcExists("JRIpcExists", 0x0304, "The caller issued CREATE, EXCL and the key was already defined to InterProcess Communications"),  // NOSONAR
    JRIpcMaxIDs("JRIpcMaxIDs", 0x0305, "The number of IDs exceeds the system limit and the create fails"),  // NOSONAR
    JRIpcNoExist("JRIpcNoExist", 0x0306, "The caller tried to locate a member for the key specified but it does not exist"),  // NOSONAR
    JRIpcRetry("JRIpcRetry", 0x0307, "NOWAIT was specified but the operation could not be performed immediately"),  // NOSONAR
    JRIpcSignaled("JRIpcSignaled", 0x0308, "An IPC wait was interrupted by a signal"),  // NOSONAR
    JRIpcBadFlags("JRIpcBadFlags", 0x0309, "Extraneous bits were set in the flags word parameter or in the mode flag bit field"),  // NOSONAR
    JRMsqBadType("JRMsqBadType", 0x030a, "Message type must be greater than zero"),  // NOSONAR
    JRMsqBadSize("JRMsqBadSize", 0x030b, "The message length exceeds the system limit or is less than zero"),  // NOSONAR
    JRMsqNoMsg("JRMsqNoMsg", 0x030c, "No message of the type requested was found"),  // NOSONAR
    JRMsq2Big("JRMsq2Big", 0x030d, "The message to receive was too large for the buffer and MSG_NOERROR was not specified"),  // NOSONAR
    JRSema4BadAdj("JRSema4BadAdj", 0x030e, "The value specified would exceed the system limit for semadj"),  // NOSONAR
    JRSema4BadNOps("JRSema4BadNOps", 0x030f, "Number of semaphore operation exceeds the system limit"),  // NOSONAR
    JRSema4BadNSems("JRSema4BadNSems", 0x0310, "Semaphore ID already exists for the KEY, but the number of semaphores is less than requested"),  // NOSONAR
    JRTypeNotPID("JRTypeNotPID", 0x0311, "A msgrcv or msgsnd did not have its process ID as type"),  // NOSONAR
    JRSema4BadSemN("JRSema4BadSemN", 0x0312, "The semaphore number is invalid"),  // NOSONAR
    JRSema4BadValue("JRSema4BadValue", 0x0313, "The value specified would exceed the system limit"),  // NOSONAR
    JRSema4BigNSems("JRSema4BigNSems", 0x0314, "The number of semaphores exceeds the system maximum"),  // NOSONAR
    JRSema4ZeroNSems("JRSema4ZeroNSems", 0x0315, "The number of semaphores specified was zero and the semaphore does not exist"),  // NOSONAR
    JRShmBadSize("JRShmBadSize", 0x0316, "The shared memory segment size is incorrect or outside the system defined range of valid segment sizes"),  // NOSONAR
    JRShmMaxAttach("JRShmMaxAttach", 0x0317, "The number of shared memory attaches for the current process exceeds the system defined maximum"),  // NOSONAR
    JRIpcRemoved("JRIpcRemoved", 0x0318, "During a wait, the IPC member ID was removed from the system"),  // NOSONAR
    JRMsqQBytes("JRMsqQBytes", 0x0319, "Not permitted to increase message qbytes or attempt by superuser to set message qbytes exceeds system limit"),  // NOSONAR
    JRBadPerfGroup("JRBadPerfGroup", 0x031a, "The specified priority or nice value represents a performance group that could not be used"),  // NOSONAR
    JRBadServClass("JRBadServClass", 0x031b, "The specified priority or nice value represents a service class that could not be used"),  // NOSONAR
    JRMsqQueueFullMessages("JRMsqQueueFullMessages", 0x031c, "IPC_NOWAIT was specified but the operation was not done because there was no room in the message queue due to the number of messages in the message queue"),  // NOSONAR
    JRMsqQueueFullBytes("JRMsqQueueFullBytes", 0x031d, "IPC_NOWAIT was specified and the operation was not done because there was no room in the message queue due to the number of bytes in the message queue"),  // NOSONAR
    JRRFileNoRead("JRRFileNoRead", 0x031e, "A call tried to read a file opened without read access"),  // NOSONAR
    JRSemStorageLimit("JRSemStorageLimit", 0x031f, "The semget or semop failed because the semaphore storage limit was reached"),  // NOSONAR
    JRInheEye("JRInheEye", 0x0320, "The inheritance area contains an incorrect eyecatcher value"),  // NOSONAR
    JRInheLength("JRInheLength", 0x0321, "The length specified for the inheritance area contains an incorrect value"),  // NOSONAR
    JRInheVersion("JRInheVersion", 0x0322, "The inheritance area contains an incorrect version number"),  // NOSONAR
    JRSpawnTooManyFds("JRSpawnTooManyFds", 0x0323, "The count of file descriptors specified is greater than the maximum supported by the system"),  // NOSONAR
    JRSmNoStorage("JRSmNoStorage", 0x0324, "There is no storage available to allocate"),  // NOSONAR
    JRSmDspservErr("JRSmDspservErr", 0x0325, "The DSPSERV macro failed"),  // NOSONAR
    JRSmInvalidDsID("JRSmInvalidDsID", 0x0326, "The data space group ID is not valid"),  // NOSONAR
    JRSmInvalidDsSToken("JRSmInvalidDsSToken", 0x0327, "The data space SToken is not valid"),  // NOSONAR
    JRSmOutOfMasterCells("JRSmOutOfMasterCells", 0x0328, "A Master Cell Pool is out of extent storage cells"),  // NOSONAR
    JRShmMaxSpages("JRShmMaxSpages", 0x0329, "The operation was not done because the system wide limit for shared memory segment pages was exceeded"),  // NOSONAR
    JRNoAccess("JRNoAccess", 0x032a, "Caller does not have access to function"),  // NOSONAR
    JRSmInvalidLength("JRSmInvalidLength", 0x032b, "The length the requested storage exceeds maximum"),  // NOSONAR
    JRFdListTooBig("JRFdListTooBig", 0x032c, "The size of the file descriptor list is larger than can be currently supported"),  // NOSONAR
    JRBadFdList("JRBadFdList", 0x032d, "The file descriptor list supplied on the call to BPX1SPN is not accessible by the caller"),  // NOSONAR
    JRNotMapped("JRNotMapped", 0x032e, "One or more specified pages are not mapped"),  // NOSONAR
    JRClnyASCREFailed("JRClnyASCREFailed", 0x032f, "The ASCRE macro issued to start a colony address space returned a failing return code"),  // NOSONAR
    JRClnyStartFailed("JRClnyStartFailed", 0x0330, "A colony address space failed to initialize"),  // NOSONAR
    JRClnyNotStopped("JRClnyNotStopped", 0x0331, "The colony address space could not be stopped"),  // NOSONAR
    JRClnyNoCommonStorage("JRClnyNoCommonStorage", 0x0332, "The system was unable to obtain storage in common for a control block to represent a colony address space"),  // NOSONAR
    JRPfsNotDubbed("JRPfsNotDubbed", 0x0333, "The PFS task calling an OSI service is not dubbed"),  // NOSONAR
    JRClnyNotStarted("JRClnyNotStarted", 0x0334, "An attempt was made to start a PFS within a colony address space. The colony was either not completely initialized or it was being terminated"),  // NOSONAR
    JRPtyChgFromSlave("JRPtyChgFromSlave", 0x0335, "An attempt was made to change a termios flag from the dependent pty, which is only allowed from the master"),  // NOSONAR
    JRClnyPfsNotStarted("JRClnyPfsNotStarted", 0x0336, "An attempt was made to stop or clean up a colony PFS that was not previously started"),  // NOSONAR
    JRClnyPfsNotDone("JRClnyPfsNotDone", 0x0337, "An attempt was made to clean up a colony PFS that was not completely terminated"),  // NOSONAR
    JRCpbNotFound("JRCpbNotFound", 0x0338, "No Cpb was found on the Cpb chain representing this colony PFS"),  // NOSONAR
    JRDevConfigTypeError("JRDevConfigTypeError", 0x0339, "An attempt was made to configure a device driver that was not defined"),  // NOSONAR
    JRPtyNeedPKT3270("JRPtyNeedPKT3270", 0x033a, "An attempt was made to set 3270 Passthru mode without 3270 Packet mode"),  // NOSONAR
    JRMmapOverEOF("JRMmapOverEOF", 0x033b, "The extended file cannot be mapped over its EOF point"),  // NOSONAR
    JRRaiseHardLimit("JRRaiseHardLimit", 0x033c, "An attempt was made to raise a hard limit without superuser authority"),  // NOSONAR
    JRInvalidResource("JRInvalidResource", 0x033d, "The input resource value is not valid"),  // NOSONAR
    JRSoftExceedsHard("JRSoftExceedsHard", 0x033e, "An attempt was made to raise a soft limit above its hard limit"),  // NOSONAR
    JRSoftBelowUsage("JRSoftBelowUsage", 0x033f, "An attempt was made to lower a soft limit below the current usage for the resource"),  // NOSONAR
    JRInvalidWho("JRInvalidWho", 0x0340, "The input who value is not valid"),  // NOSONAR
    JRCPUTimeObtainFailed("JRCPUTimeObtainFailed", 0x0341, "Failure obtaining CPU time usage"),  // NOSONAR
    JRVsmListError("JRVsmListError", 0x0342, "The VSMLIST macro failed"),  // NOSONAR
    JRPtyMutuallyExclusive("JRPtyMutuallyExclusive", 0x0343, "An attempt was made to set mutually exclusive bits"),  // NOSONAR
    JRMmapStgExceeded("JRMmapStgExceeded", 0x0344, "The system-wide limit on the amount of memory consumed by memory mapped areas is exceeded"),  // NOSONAR
    JRPathconf("JRPathconf", 0x0345, "The Vnode operation pathconf is not supported by this file system"),  // NOSONAR
    JRNotPage("JRNotPage", 0x0346, "A location specified or generated is not on a page boundary"),  // NOSONAR
    JRMmapBadType("JRMmapBadType", 0x0347, "The value of the map_type is not valid"),  // NOSONAR
    JRHardware("JRHardware", 0x0348, "A request was made for a hardware that is not available"),  // NOSONAR
    JRAddressNotAvailable("JRAddressNotAvailable", 0x0349, "A request was made for a storage address, but it could not be satisfied"),  // NOSONAR
    JRProcMaxMmap("JRProcMaxMmap", 0x034a, "The process has exceeded the maximum number of mmaps"),  // NOSONAR
    JRMmapFileAddress("JRMmapFileAddress", 0x034b, "File_offset + map_length exceeds file size"),  // NOSONAR
    JRIarvServ("JRIarvServ", 0x034c, "An invocation of IARVSERV service failed"),  // NOSONAR
    JRPtyPendingControlInfo("JRPtyPendingControlInfo", 0x034d, "An attempt was made to write to a master pty in 3270 packet mode when control information was pending"),  // NOSONAR
    JRZeroOrNegative("JRZeroOrNegative", 0x034e, "An input parameter must be greater than zero"),  // NOSONAR
    JRNoUserStorage("JRNoUserStorage", 0x034f, "The service could not obtain enough storage in user address space subpool 129"),  // NOSONAR
    JRAsynchMount("JRAsynchMount", 0x0350, "The request to mount a file system will complete asynchronously. The system rejects all vnode (file) operations against the file system"),  // NOSONAR
    JRPfsOpNotSupported("JRPfsOpNotSupported", 0x0351, "The pfsctl command is not supported by this PFS"),  // NOSONAR
    JRPfsOpNotPermitted("JRPfsOpNotPermitted", 0x0352, "Not authorized to perform this pfsctl operation"),  // NOSONAR
    JRPfsArgLenBad("JRPfsArgLenBad", 0x0353, "The argument length is not valid for this pfsctl operation"),  // NOSONAR
    JRDdBadConfigOpt("JRDdBadConfigOpt", 0x0354, "The CONFIGURE operation is not supported by this device driver"),  // NOSONAR
    JRDdConfigInbuf2Big("JRDdConfigInbuf2Big", 0x0355, "The CONFIGURE input buffer too large"),  // NOSONAR
    JRDdConfigOutbuf2Big("JRDdConfigOutbuf2Big", 0x0356, "The CONFIGURE output buffer too large"),  // NOSONAR
    JRDdBadDdType("JRDdBadDdType", 0x0357, "The device driver TYPE is not known to the system"),  // NOSONAR
    JRDdNoDdConfigure("JRDdNoDdConfigure", 0x0358, "The device driver TYPE does not support a dd_configure routine"),  // NOSONAR
    JRDdwtTaskTerm("JRDdwtTaskTerm", 0x0359, "The operation could not be completed because the device driver work thread terminated"),  // NOSONAR
    JRDdConfigAbend("JRDdConfigAbend", 0x035a, "An abend occurred during dd_config processing"),  // NOSONAR
    JRCaptureFailure("JRCaptureFailure", 0x035b, "A memory capture (IARVSERV) failed"),  // NOSONAR
    JRHotCCreateFailure("JRHotCCreateFailure", 0x035c, "An attempt to create a Hot C environment for a physical file system failed"),  // NOSONAR
    JRVdacError("JRVdacError", 0x035d, "The Vdac macro failed"),  // NOSONAR
    JRBadSiginfoAddr("JRBadSiginfoAddr", 0x035e, "An incorrect siginfo_t address was passed to the waitid call"),  // NOSONAR
    JRBadRUsageAddr("JRBadRUsageAddr", 0x035f, "An incorrect rusage address was passed to the wait3 call"),  // NOSONAR
    JRPtyNoTRTStorage("JRPtyNoTRTStorage", 0x0360, "Storage is not available for pseudo-TTY scan tables"),  // NOSONAR
    JRBadStDev("JRBadStDev", 0x0361, "The device number specified to osi_mountstatus does not refer to a mounted file system"),  // NOSONAR
    JRDdNoDdwt("JRDdNoDdwt", 0x0362, "The device driver task is not active"),  // NOSONAR
    JRBadInputBufAddr("JRBadInputBufAddr", 0x0363, "Bad input buffer address"),  // NOSONAR
    JROpenFileLimitMax("JROpenFileLimitMax", 0x0364, "The Open file limit cannot exceed 524287"),  // NOSONAR
    JRFdOpenAboveLimit("JRFdOpenAboveLimit", 0x0365, "A file descriptor is open above requested limit"),  // NOSONAR
    JRWriteBeyondLimit("JRWriteBeyondLimit", 0x0366, "Cannot write beyond the file size limit"),  // NOSONAR
    JRSyscallFailAll("JRSyscallFailAll", 0x0367, "An attempt to process a syscall for a socket failed on all of the transport providers supporting the socket"),  // NOSONAR
    JRSyscallFailOne("JRSyscallFailOne", 0x0368, "An attempt to process a syscall for a socket failed on one of the transport providers supporting the socket"),  // NOSONAR
    JRSyscallFailSome("JRSyscallFailSome", 0x0369, "An attempt to process a syscall for a socket failed on some of the transport providers supporting the socket"),  // NOSONAR
    JRSetSockOptFailAll("JRSetSockOptFailAll", 0x036a, "An attempt to set socket options failed on all of the transport providers supporting the socket"),  // NOSONAR
    JRSetSockOptFailOne("JRSetSockOptFailOne", 0x036b, "An attempt to set socket options failed on one of the transport providers supporting the socket"),  // NOSONAR
    JRSetSockOptFailSome("JRSetSockOptFailSome", 0x036c, "An attempt to set socket options failed on some of the transport providers supporting the socket"),  // NOSONAR
    JRRlimitCantCreate("JRRlimitCantCreate", 0x036d, "You cannot create files when RLIMIT_FSIZE is 0"),  // NOSONAR
    JRBadOutputBufAddr("JRBadOutputBufAddr", 0x036e, "Bad output buffer address"),  // NOSONAR
    JRNotStdFile("JRNotStdFile", 0x036f, "Only standard files can be mmap"),  // NOSONAR
    JRBadIDType("JRBadIDType", 0x0370, "An invalid ID type was passed"),  // NOSONAR
    JRBadOptions("JRBadOptions", 0x0371, "Incorrect options were passed on the options parameter"),  // NOSONAR
    JRCdstAlreadyAdded("JRCdstAlreadyAdded", 0x0372, "Character special service routine detected an ADD request for a previously completed CDST entry"),  // NOSONAR
    JRDdConfigNoResource("JRDdConfigNoResource", 0x0373, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRDdNotConfigured("JRDdNotConfigured", 0x0374, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRDdConfigBadOpt("JRDdConfigBadOpt", 0x0375, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRTbmStorageFailure("JRTbmStorageFailure", 0x0376, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRTbmAttachFailure("JRTbmAttachFailure", 0x0377, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRTbmLatchSetFailure("JRTbmLatchSetFailure", 0x0378, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JROcsNotConfigured("JROcsNotConfigured", 0x0379, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JROcsDevNotConfigured("JROcsDevNotConfigured", 0x037a, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRDevUnfigPnd("JRDevUnfigPnd", 0x037b, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRRtyDevConfigChange("JRRtyDevConfigChange", 0x037c, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRPtyNotCntlTerm("JRPtyNotCntlTerm", 0x037d, "The device is not associated with a controlling terminal"),  // NOSONAR
    JROcsAdminBufferExceeded("JROcsAdminBufferExceeded", 0x037e, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRRtyBadMultiByteCodePageName("JRRtyBadMultiByteCodePageName", 0x037f, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JROcsRtyOutBufferExceeded("JROcsRtyOutBufferExceeded", 0x0380, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRPrevSockError("JRPrevSockError", 0x0381, "A previous error caused this socket to become unusable"),  // NOSONAR
    JROcsNotConnected("JROcsNotConnected", 0x0382, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRRtyDifferentFIle("JRRtyDifferentFIle", 0x0383, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRBadOsi("JRBadOsi", 0x0384, "The Osi structure passed is not valid"),  // NOSONAR
    JRBadPfsId("JRBadPfsId", 0x0385, "The value of the OsiPfsId field is not valid"),  // NOSONAR
    JRFRRActive("JRFRRActive", 0x0386, "An FRR is established"),  // NOSONAR
    JRPtyQueueChange("JRPtyQueueChange", 0x0387, "A termios option change requires all output be read or flushed before further writes"),  // NOSONAR
    JRIntervalTypeInvalid("JRIntervalTypeInvalid", 0x0388, "The IntervalType is not valid"),  // NOSONAR
    JRRtyNoResource("JRRtyNoResource", 0x0389, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRRtyTermSyscall("JRRtyTermSyscall", 0x038a, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRRtyNoReply("JRRtyNoReply", 0x038b, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JROcsErrno("JROcsErrno", 0x038c, "OBSOLETE - DO NOT USE"),  // NOSONAR
    JRPtyOrphaned("JRPtyOrphaned", 0x038d, "The syscall is processing in an orphaned process group"),  // NOSONAR
    JRTransportDriverNotAccessible("JRTransportDriverNotAccessible", 0x038e, "The transport driver specified is not being used by the socket specified"),  // NOSONAR
    JRInvSignalForProcess("JRInvSignalForProcess", 0x038f, "The specified signal number is incorrect"),  // NOSONAR
    JRUserNameBad("JRUserNameBad", 0x0390, "The user name is not a valid MVS user name"),  // NOSONAR
    JRTooManyFds("JRTooManyFds", 0x039e, "Too many Fds were specified"),  // NOSONAR
    JRMicroSecondsTooBig("JRMicroSecondsTooBig", 0x039f, "The value specified for microseconds is outside the allowable range"),  // NOSONAR
    JRSockPrerouterErr("JRSockPrerouterErr", 0x03a0, "The Common Inet Sockets Prerouter returned an error"),  // NOSONAR
    JROsiAbend("JROsiAbend", 0x03a1, "An abend occurred in an Operating System Interface routine"),  // NOSONAR
    JRSTIMERMMax("JRSTIMERMMax", 0x03a2, "A 32E system abend has been intercepted by the syscall. See the appropriate MVS System Codes manual"),  // NOSONAR
    JRNoClnyThreadSppt("JRNoClnyThreadSppt", 0x03a3, "Colony thread support has not been built"),  // NOSONAR
    JRSHSPMASK("JRSHSPMASK", 0x03a4, "The shared subpool mask is not valid for the current environment"),  // NOSONAR
    JROWaitSetupErr("JROWaitSetupErr", 0x03a5, "An error occurred attempting OsiWait setup"),  // NOSONAR
    JRBadOptnFlags("JRBadOptnFlags", 0x03a6, "Extraneous bits were set in the option flags parameter"),  // NOSONAR
    JRNegFileSizeLimit("JRNegFileSizeLimit", 0x03a7, "One of the file size limits specified is negative"),  // NOSONAR
    JRNoSAFsupport("JRNoSAFsupport", 0x03a8, "The installed Security product does not support this function"),  // NOSONAR
    JRPtNoStorage("JRPtNoStorage", 0x03a9, "Not enough storage is available for ptrace"),  // NOSONAR
    JRPtBufNotFound("JRPtBufNotFound", 0x03aa, "A Pt_Uncapture ptrace request was issued for a specific buffer but the buffer was not previously captured with a Pt_Capture request"),  // NOSONAR
    JRPtTso("JRPtTso", 0x03ab, "The specified ptrace function is not supported in a TSO address space"),  // NOSONAR
    JRAuthCaller("JRAuthCaller", 0x03ac, "The caller of this service is authorized. Authorized callers are not permitted to load or call unauthorized programs or programs residing in a file system mounted with the NOSETUID parameter"),  // NOSONAR
    JRSingleTDReqd("JRSingleTDReqd", 0x03ad, "The Common Inet Sockets ioctl command requires that a single transport driver be connected to the socket"),  // NOSONAR
    JRBatSel("JRBatSel", 0x03ae, "The batch-select VFS operation is not supported"),  // NOSONAR
    JRRealPageNotSupported("JRRealPageNotSupported", 0x03af, "A Fuio area containing a real page address was passed"),  // NOSONAR
    JRBadMVSPgmName("JRBadMVSPgmName", 0x03b0, "A call to the exec or loadHFS service specified a file that resolves to an MVS program name that is not valid"),  // NOSONAR
    JRMVSLoadFailure("JRMVSLoadFailure", 0x03b1, "A call to the loadHFS service resulted in a failure in the MVS Load service"),  // NOSONAR
    JRMVSPgmNotFound("JRMVSPgmNotFound", 0x03b2, "A call to the exec or loadHFS service specified a file that resolves to an MVS program that cannot be found"),  // NOSONAR
    JRNoConsoleBuffers("JRNoConsoleBuffers", 0x03b3, "The write to /dev/console cannot complete"),  // NOSONAR
    JRPtMaxCapture("JRPtMaxCapture", 0x03b4, "The ptrace capture request would exceed maximum allowed"),  // NOSONAR
    JRCPCNnotEnabled("JRCPCNnotEnabled", 0x03b5, "Code Page Change Notification is not enabled"),  // NOSONAR
    JRExitAbend("JRExitAbend", 0x03b6, "An abend occurred in a File Exporter Exit"),  // NOSONAR
    JRShrStgShortage("JRShrStgShortage", 0x03b7, "Request for shared storage exceeds amount available"),  // NOSONAR
    JRPtyNeedPKTXTND("JRPtyNeedPKTXTND", 0x03b8, "An attempt was made to set 3270 Passthru mode without 3270 Packet mode"),  // NOSONAR
    JRWaitForever("JRWaitForever", 0x03b9, "The timeout value specified wait forever, but there were no events to wait for"),  // NOSONAR
    JRInvalidNfds("JRInvalidNfds", 0x03ba, "The NFDS parameter is larger than the OPEN_MAX (MAXFILEPROC) value"),  // NOSONAR
    JRClnyPfsNotAllowed("JRClnyPfsNotAllowed", 0x03bb, "The requested operation is not allowed for a PFS that is running in a Colony Address Space"),  // NOSONAR
    JRPtyNotMaster("JRPtyNotMaster", 0x03bc, "Unsupported function against dependent TTY"),  // NOSONAR
    JRFsUnAuthClnt("JRFsUnAuthClnt", 0x03bd, "An unauthenticated client is denied access"),  // NOSONAR
    JRBadBufLen("JRBadBufLen", 0x03be, "The length of the buffer is not valid"),  // NOSONAR
    JRBadStgKey("JRBadStgKey", 0x03bf, "The message data could not be fetched using the specified storage key"),  // NOSONAR
    JRIxcMsgo("JRIxcMsgo", 0x03c0, "An unexpected error occurred in the IXCMSGO macro"),  // NOSONAR
    JRNoFdsTooManyQIds("JRNoFdsTooManyQIds", 0x03c1, "The number of Fds specified is negative or too many Msg Q Ids specified on select or poll service"),  // NOSONAR
    JRInvHdr("JRInvHdr", 0x03c2, "Invalid parameter list header"),  // NOSONAR
    JRIxcMsgi("JRIxcMsgi", 0x03c3, "An unexpected error occurred in the IXCMSGI macro"),  // NOSONAR
    JRIXCXCDSIfail("JRIXCXCDSIfail", 0x03c4, "An unexpected error occurred in the IXCXCDSI macro"),  // NOSONAR
    JRIXCXCDSIenv("JRIXCXCDSIenv", 0x03c5, "An environmental error occurred in the IXCXCDSI macro"),  // NOSONAR
    JRIXCXCDSInoCDS("JRIXCXCDSInoCDS", 0x03c6, "An environmental error occurred in the IXCXCDSI macro, OMVS CDS not available"),  // NOSONAR
    JRIXCXCDSIinvparm("JRIXCXCDSIinvparm", 0x03c7, "An invparm error occurred in the IXCXCDSI macro"),  // NOSONAR
    JRIXCXCDSIaccess("JRIXCXCDSIaccess", 0x03c8, "An invparm error occurred in the IXCXCDSI macro, not able to access dataarea or token"),  // NOSONAR
    JRIXCXCDSIvalidate("JRIXCXCDSIvalidate", 0x03c9, "An invparm error occurred in the IXCXCDSI macro, validate failed for dataarea or token"),  // NOSONAR
    JROutOfMountEntries("JROutOfMountEntries", 0x03ca, "The system is unable to obtain an entry in the file system mount table in the OMVS couple data set"),  // NOSONAR
    JRPtyUnsupportedAttr("JRPtyUnsupportedAttr", 0x03cb, "Attempt to change attribute to unsupported value"),  // NOSONAR
    JRTgtMemberInactive("JRTgtMemberInactive", 0x03cc, "The XCF member represented by the input member token is not active. For a Shared File System configuration, this can occur when a system is attempting to send a message to another system and that target system is no longer active in the configuration"),  // NOSONAR
    JRnoSavedToken("JRnoSavedToken", 0x03cd, "The saved IXCXCDSI token is zero for a request that expects a valid token"),  // NOSONAR
    JRStaleVfs("JRStaleVfs", 0x03ce, "The Vfs passed to complete asynchronous mount does not represent an outstanding asynchronous mount"),  // NOSONAR
    JRNoArea("JRNoArea", 0x03cf, "The State Area has not been established"),  // NOSONAR
    JRBadSubField("JRBadSubField", 0x03d0, "A subfield of the Argument is not valid"),  // NOSONAR
    JRNoChangeIdentity("JRNoChangeIdentity", 0x03d1, "The invoker is not authorized to change MVS userids"),  // NOSONAR
    JRBadId("JRBadId", 0x03d2, "An incorrect ID value was passed to the BPX1WTE service"),  // NOSONAR
    JRNoWorkUnit("JRNoWorkUnit", 0x03d3, "Attempt to transfer work via BPX1SPW call failed"),  // NOSONAR
    JRNoWLMConn("JRNoWLMConn", 0x03d4, "Attempt to refresh work via BPX1SPW call failed"),  // NOSONAR
    JRMgcreErr("JRMgcreErr", 0x03d5, "MGCRE macro invocation failed on BPX1SPW call"),  // NOSONAR
    JRNoMulti("JRNoMulti", 0x03d6, "Attempt to issue multiple BPX1CCS calls"),  // NOSONAR
    JRMsgLengthErr("JRMsgLengthErr", 0x03d7, "Message length to the console was exceeded"),  // NOSONAR
    JRInvalidClassify("JRInvalidClassify", 0x03d8, "The classification area is not accessible to the current caller"),  // NOSONAR
    JRNoEnclave("JRNoEnclave", 0x03d9, "No enclave is associated with the calling thread or process"),  // NOSONAR
    JRSubSysNotFnd("JRSubSysNotFnd", 0x03da, "No process was found with the associated subsystem type and subsystem name"),  // NOSONAR
    JRApplDataLenErr("JRApplDataLenErr", 0x03db, "The application data area is too large to be processed"),  // NOSONAR
    JRBadOptCode("JRBadOptCode", 0x03dc, "Bad entry code to process work unit"),  // NOSONAR
    JRInvalidApplData("JRInvalidApplData", 0x03dd, "The application data area passed was not accessible"),  // NOSONAR
    JRInvalidApplData2("JRInvalidApplData2", 0x03de, "The application data area is not accessible to the current caller"),  // NOSONAR
    JRInvalidSFDL("JRInvalidSFDL", 0x03df, "The file descriptor list passed was not accessible"),  // NOSONAR
    JRNewLocationErr("JRNewLocationErr", 0x03e0, "The new tag data area passed was not accessible"),  // NOSONAR
    JROldLocationErr("JROldLocationErr", 0x03e1, "The old tag data area passed was not accessible"),  // NOSONAR
    JRNewLenBad("JRNewLenBad", 0x03e2, "The new tag data length was not valid"),  // NOSONAR
    JRMsgAttrErr("JRMsgAttrErr", 0x03e3, "Message attribute error detected"),  // NOSONAR
    JRNoAck("JRNoAck", 0x03e4, "No acknowledgement signal from remote system"),  // NOSONAR
    JRIPAddrNotAllowed("JRIPAddrNotAllowed", 0x03e5, "The input IP address is not valid"),  // NOSONAR
    JRPrevBound("JRPrevBound", 0x03e6, "The requested port number is in use"),  // NOSONAR
    JRInaccessible("JRInaccessible", 0x03e7, "The user storage is inaccessible"),  // NOSONAR
    JRIefddsrvFailed("JRIefddsrvFailed", 0x03e8, "The macro IEFDDSRV failed"),  // NOSONAR
    JRSvc99Failed("JRSvc99Failed", 0x03e9, "SVC 99 (Allocate) failed"),  // NOSONAR
    JRDevNotOnline("JRDevNotOnline", 0x03ea, "Specified device is not online"),  // NOSONAR
    JRDevNotFound("JRDevNotFound", 0x03eb, "Specified device is not found"),  // NOSONAR
    JRAlreadyInProgress("JRAlreadyInProgress", 0x03ec, "Another config request is in progress"),  // NOSONAR
    JRInvalidVersion("JRInvalidVersion", 0x03ed, "Config request has invalid version number"),  // NOSONAR
    JRNotAuthWLM("JRNotAuthWLM", 0x03ee, "Server_init service call failed permission check for BPX.WLMSERVER facility class profile"),  // NOSONAR
    JRTargetIPNotFound("JRTargetIPNotFound", 0x03ef, "Target IP address cannot be found"),  // NOSONAR
    JRESCONNotConfigured("JRESCONNotConfigured", 0x03f0, "The ESCON connection is not configured"),  // NOSONAR
    JRPtySlaveLocked("JRPtySlaveLocked", 0x03f1, "A grantpt() was issued against the master pty but an unlockpt() has not yet been issued"),  // NOSONAR
    JRPtySlaveNotLocked("JRPtySlaveNotLocked", 0x03f2, "The dependent pty is not locked either because grantpt was not done or because grantpt has already been issued"),  // NOSONAR
    JRPtyGrantptDone("JRPtyGrantptDone", 0x03f3, "grantpt() has already been issued. This grantpt is redundant"),  // NOSONAR
    JRSRBSNotAllowed("JRSRBSNotAllowed", 0x03f4, "Issuing syscalls from an SRB is not allowed"),  // NOSONAR
    JRNotSRBSyscall("JRNotSRBSyscall", 0x03f5, "The syscall requested is not supported in SRB mode"),  // NOSONAR
    JRRTSSConnErr("JRRTSSConnErr", 0x03f6, "Fork child processing failed due to a RTSS connection error"),  // NOSONAR
    JRRTSSEnvErr("JRRTSSEnvErr", 0x03f7, "Fork parent processing failed due to a RTSS environment error"),  // NOSONAR
    JRAsyncAuthErr("JRAsyncAuthErr", 0x03f8, "User is not authorized for asynchronous i/o"),  // NOSONAR
    JRAsyncRWLenZero("JRAsyncRWLenZero", 0x03f9, "Zero length asynchronous read or write not permitted"),  // NOSONAR
    JRAsyncBadMsgHdrLen("JRAsyncBadMsgHdrLen", 0x03fa, "AioBuffSize is not set to correct message header length"),  // NOSONAR
    JRAsyncBadCmd("JRAsyncBadCmd", 0x03fb, "AioCmd is not set to a supported value"),  // NOSONAR
    JRAsyncBadNotifyType("JRAsyncBadNotifyType", 0x03fc, "AioNotifyType is not set to a supported value"),  // NOSONAR
    JRAsyncBadOffset("JRAsyncBadOffset", 0x03fd, "AioOffset is a negative value"),  // NOSONAR
    JRAsyncBadAiocbLen("JRAsyncBadAiocbLen", 0x03fe, "The input length for the AioCb is not a supported length"),  // NOSONAR
    JRAsyncBadSockAddr("JRAsyncBadSockAddr", 0x03ff, "The AioSockAddrPtr, AioSockAddrLen, AioLocSockAddrPtr or AioLocSockAddrLen contains a bad value"),  // NOSONAR
    JRSchedSrbErr("JRSchedSrbErr", 0x0400, "The Srb was not scheduled"),  // NOSONAR
    JRBadArq("JRBadArq", 0x0401, "An invalid Arq was encountered"),  // NOSONAR
    JRCancel("JRCancel", 0x0402, "Vnode operation CANCEL is not supported by this file system"),  // NOSONAR
    JRDuplicateCancel("JRDuplicateCancel", 0x0403, "A cancel operation is already in progress for the target asyncio request"),  // NOSONAR
    JRAsyncNotSingleTd("JRAsyncNotSingleTd", 0x0404, "The AsyncIO operation can not be performed because this socket does not have a chosen transport stack"),  // NOSONAR
    JRSMFNotAuthorized("JRSMFNotAuthorized", 0x0405, "The __smf_record function can not be performed because the caller is not permitted to the BPX.SMF facility class and is not APF authorized. The caller must either be permitted to the facility class or APF authorized"),  // NOSONAR
    JRSMFNotAccepting("JRSMFNotAccepting", 0x0406, "SMF is not recording SMF records of the type and subtype requested"),  // NOSONAR
    JRSMFNotActive("JRSMFNotActive", 0x0407, "SMF is not active"),  // NOSONAR
    JRSMFError("JRSMFError", 0x0408, "An error occurred in the SMFEWTM SMF macro"),  // NOSONAR
    JRSMFBadRecordLength("JRSMFBadRecordLength", 0x0409, "The length of the SMF record passed to __smf_record is either too large or too small"),  // NOSONAR
    JREnclavesExist("JREnclavesExist", 0x040a, "The calling work manager has enclaves that it created that have yet to be serviced"),  // NOSONAR
    JRBindNotDone("JRBindNotDone", 0x040b, "Bind() must be issued before issuing read/write"),  // NOSONAR
    JRQhitRecovery("JRQhitRecovery", 0x040c, "BPXXQHIT macro reportedly entered recovery"),  // NOSONAR
    JRPingSelf("JRPingSelf", 0x040d, "Ping to self is not supported"),  // NOSONAR
    JRDuplicateReq("JRDuplicateReq", 0x040e, "Requested connection already exists"),  // NOSONAR
    JRTrleNotFound("JRTrleNotFound", 0x040f, "Target TRLE name cannot be found"),  // NOSONAR
    JRBpxxuiwrErr("JRBpxxuiwrErr", 0x0410, "The BPXXUIWR macro failed"),  // NOSONAR
    JRFastPathRange("JRFastPathRange", 0x0411, "A new socket descriptor is too large for Fastpath"),  // NOSONAR
    JRRosAlreadyReg("JRRosAlreadyReg", 0x0412, "An error occurred during process signal initialization"),  // NOSONAR
    JRAsyncSigKey0Err("JRAsyncSigKey0Err", 0x0413, "A caller in key 0 cannot request signals for async i/o"),  // NOSONAR
    JRAsyncExitModeTcb("JRAsyncExitModeTcb", 0x0414, "A request for ExitMode of TCB was requested for a user who is either not running on a TCB or running in key 0"),  // NOSONAR
    JRAsyncOpNotSupp("JRAsyncOpNotSupp", 0x0415, "The AsyncIO operation can not be performed because the socket transport does not support asynchronous I/O or asynchronous select included a physical file system that could not support this operation"),  // NOSONAR
    JRAsyncBadSigNo("JRAsyncBadSigNo", 0x0416, "The AsyncIO operation specified a signal number that does not fall within the valid range of values"),  // NOSONAR
    JRPriviligedFile("JRPriviligedFile", 0x0417, "The operation is not allowed on a priviliged file"),  // NOSONAR
    JRRouteExists("JRRouteExists", 0x0418, "The oeifconfig contained a duplicate destination IP address"),  // NOSONAR
    JRHomeExists("JRHomeExists", 0x0419, "The oeifconfig contained a duplicate home IP address"),  // NOSONAR
    JRAPFNotAuthorized("JRAPFNotAuthorized", 0x041a, "The chattr function can not be performed to change the APF attribute because the caller is not permitted to the BPX.FILEATTR.APF facility class"),  // NOSONAR
    JRPGMNotAuthorized("JRPGMNotAuthorized", 0x041b, "The chattr function can not be performed to change the program control attribute because the caller is not permitted to the BPX.FILEATTR.PROGCTL facility class"),  // NOSONAR
    JRProgCntl("JRProgCntl", 0x041c, "A request to load an HFS executable that is not program controlled was made into an environment that must be kept clean"),  // NOSONAR
    JRFileSzExcdLimit("JRFileSzExcdLimit", 0x041d, "A request to load a file into the LFS Cache was made for a file that exceeds the size limit of the cache"),  // NOSONAR
    JRFileIsEmpty("JRFileIsEmpty", 0x041e, "A request to load a file into the LFS Cache was made for a file that is empty"),  // NOSONAR
    JRCacheNotRegFile("JRCacheNotRegFile", 0x041f, "Requests to load a file into the LFS Cache are valid for only regular files"),  // NOSONAR
    JRNotMVSLocalFile("JRNotMVSLocalFile", 0x0420, "Requests to load a file into the LFS Cache are valid for only local files"),  // NOSONAR
    JRFileInUse("JRFileInUse", 0x0421, "The file cannot be loaded into the LFS Cache because the file is currently in use or the cleanup daemon has not completed"),  // NOSONAR
    JRNoFreeEntry("JRNoFreeEntry", 0x0422, "No free entries in the interface control array"),  // NOSONAR
    JRInheUserId("JRInheUserId", 0x0423, "User Id specified in Inheritance structure is not valid"),  // NOSONAR
    JRInheRegion("JRInheRegion", 0x0424, "Region size specified in Inheritance structure is not valid"),  // NOSONAR
    JRInheCPUTime("JRInheCPUTime", 0x0425, "CPU Time specified in Inheritance structure is not valid"),  // NOSONAR
    JRInheAcctDataPtr("JRInheAcctDataPtr", 0x0426, "The account data pointer in the Inheritance structure points to a location not addressable by the caller"),  // NOSONAR
    JRInheCWD("JRInheCWD", 0x0427, "CWD specified in Inheritance structure is not valid"),  // NOSONAR
    JRTooManyIntf("JRTooManyIntf", 0x0428, "Too many configured interfaces already exist"),  // NOSONAR
    JRBadIpAddr("JRBadIpAddr", 0x0429, "An IP address in the configured interface is incorrect"),  // NOSONAR
    JRDataNotAvail("JRDataNotAvail", 0x042a, "No data exists to return to the caller"),  // NOSONAR
    JRReqDenied("JRReqDenied", 0x042b, "The oeifconfig request is not accepted"),  // NOSONAR
    JRBadMtuSize("JRBadMtuSize", 0x042c, "The specified MTU size for the interface is incorrect"),  // NOSONAR
    JRNotOsa("JRNotOsa", 0x042d, "Current® interface is not an OSA adapter"),  // NOSONAR
    JRBadOsaPort("JRBadOsaPort", 0x042e, "Incorrect OSA port number specified"),  // NOSONAR
    JRJsrCIErr("JRJsrCIErr", 0x042f, "A request to convert JCL to SWA control blocks failed"),  // NOSONAR
    JRJsrRsErr("JRJsrRsErr", 0x0430, "A request to create a JSAB for a Forked space failed"),  // NOSONAR
    JRJsrRacXtr("JRJsrRacXtr", 0x0431, "A call to RACROUTE failed"),  // NOSONAR
    JRJsrUavXit("JRJsrUavXit", 0x0432, "The IEFUAV exit rejected account data"),  // NOSONAR
    JRJsrItjt("JRJsrItjt", 0x0433, "The IEFITJT routine had an internal error"),  // NOSONAR
    JRJsrInt("JRJsrInt", 0x0434, "Internal error from BPXPRJSR"),  // NOSONAR
    JRJsrSetUp("JRJsrSetUp", 0x0435, "Error setting up running environment"),  // NOSONAR
    JRWlmWonErr("JRWlmWonErr", 0x0436, "An IWMUWON request to create a child address space failed"),  // NOSONAR
    JRForkExitRcOverlayPgmNotValid("JRForkExitRcOverlayPgmNotValid", 0x0437, "Contents Supervisor Fork Exit cannot fork overlay programs"),  // NOSONAR
    JRMsgMaxLines("JRMsgMaxLines", 0x0438, "Maximum number of lines was exceeded"),  // NOSONAR
    JRRddPlusNoCursorSupp("JRRddPlusNoCursorSupp", 0x0439, "FuioRddPlus is only supported with the index protocol, not the cursor protocol"),  // NOSONAR
    JRBadMaxSendRcvSize("JRBadMaxSendRcvSize", 0x043a, "Incorrect Maximum send/receive size value specified"),  // NOSONAR
    JRPtLDTooManyExtents("JRPtLDTooManyExtents", 0x043b, "The ptrace loader information request failed because a load module had more than 16 extents"),  // NOSONAR
    JRBadHandle("JRBadHandle", 0x043c, "The lock handle on the lock parameter is not 0"),  // NOSONAR
    JRNoDefault("JRNoDefault", 0x043d, "Common Inet is running, but there are no transport providers active to run as a Default"),  // NOSONAR
    JRBlocksInFlux("JRBlocksInFlux", 0x043e, "Unable to access internal blocks without lock"),  // NOSONAR
    JRLinetFail("JRLinetFail", 0x043f, "Local inet not correctly set up"),  // NOSONAR
    JRNoEphemeralPorts("JRNoEphemeralPorts", 0x0440, "The supply of ephemeral port numbers is exhausted"),  // NOSONAR
    JRSocketSynReceived("JRSocketSynReceived", 0x0441, "A SYN was received on the socket connection"),  // NOSONAR
    JRSocketRstReceived("JRSocketRstReceived", 0x0442, "A RST was received on the socket connection"),  // NOSONAR
    JRSelfConnect("JRSelfConnect", 0x0443, "Connect to self is not allowed"),  // NOSONAR
    JRAlreadyConn("JRAlreadyConn", 0x0444, "The socket is already connected"),  // NOSONAR
    JRNotStream("JRNotStream", 0x0445, "The socket operation is only valid for a stream socket"),  // NOSONAR
    JRSocketConDropped("JRSocketConDropped", 0x0446, "The socket connection was severed"),  // NOSONAR
    JRMpMuProcess("JRMpMuProcess", 0x0447, "Operation not permitted in a Multiproc/Multiuser process"),  // NOSONAR
    JRTLSCertIDLenInvalid("JRTLSCertIDLenInvalid", 0x0448, "The certificate structure has an incorrect length associated with it"),  // NOSONAR
    JRTLSCertTypeInvalid("JRTLSCertTypeInvalid", 0x0449, "The type of certificate used is not valid"),  // NOSONAR
    JRTLSCertLengthInvalid("JRTLSCertLengthInvalid", 0x044a, "The length of the certificate is not valid"),  // NOSONAR
    JRNoINITACEE("JRNoINITACEE", 0x044b, "There is no SAF service available to manage a certificate authorization"),  // NOSONAR
    JRNoCertforUser("JRNoCertforUser", 0x044c, "There is no userid defined for this certificate"),  // NOSONAR
    JRCertInvalid("JRCertInvalid", 0x044d, "The certificate is not valid to the security service"),  // NOSONAR
    JRKeepaliveTO("JRKeepaliveTO", 0x044e, "The socket connection was severed"),  // NOSONAR
    JRInetRecycled("JRInetRecycled", 0x044f, "One of the Transport Providers was activated after the socket was opened"),  // NOSONAR
    JRSwapMismatch("JRSwapMismatch", 0x0450, "An attempt was made to make the address space swappable when no previous attempt to make the address space non-swappable was made"),  // NOSONAR
    JRNoBacklogQ("JRNoBacklogQ", 0x0451, "An accept() request was issued for a server socket that does not have a backlog queue"),  // NOSONAR
    JRFunctionCode("JRFunctionCode", 0x0452, "The function code for syscall is not valid"),  // NOSONAR
    JRIdentityType("JRIdentityType", 0x0453, "The identity type for the syscall is not valid"),  // NOSONAR
    JRCertificate("JRCertificate", 0x0454, "The security certificate was either not specified or the length was not valid"),  // NOSONAR
    JRCwdPLusFileName("JRCwdPLusFileName", 0x0455, "The combined length of the CWD specified in the INHE and the filename parameter exceeded 1023 bytes"),  // NOSONAR
    JRFileChangedDuringLoad("JRFileChangedDuringLoad", 0x0456, "The file or its attributes changed while the file was in the process of being loaded"),  // NOSONAR
    JRSecurityEnv("JRSecurityEnv", 0x0457, "The syscall is prohibited with the caller’s current security environment"),  // NOSONAR
    JRStorNotAvail("JRStorNotAvail", 0x0458, "An address was specified which is not available"),  // NOSONAR
    JRNotSegment("JRNotSegment", 0x0459, "A location specified or generated is not on a segment boundary"),  // NOSONAR
    JRMmapTypeMismatch("JRMmapTypeMismatch", 0x045a, "A request specified the MAP_MEGA option but was already mapped without the option or did not specify the MAP_MEGA option but was already mapped with that option"),  // NOSONAR
    JRWFileMapRDonly("JRWFileMapRDonly", 0x045b, "A request tried to write to a file that is mapped as read-only"),  // NOSONAR
    JRPtTypeNotTried("JRPtTypeNotTried", 0x045c, "The entry in a ptrace PT_BlockReq request containing this value in the status field was not processed because of an unexpected error"),  // NOSONAR
    JRPtBadBlkReqStruc("JRPtBadBlkReqStruc", 0x045d, "The PtBRInfo block or related structure passed as input for a ptrace Pt_BlockReq request is not valid"),  // NOSONAR
    JRPtSomeBlkedFailed("JRPtSomeBlkedFailed", 0x045e, "One or more entries in a Pt_BlockReq request was not processed"),  // NOSONAR
    JRPtTypeNotBlockable("JRPtTypeNotBlockable", 0x045f, "The entry in a ptrace PT_BlockReq request containing this value in the status field was not processed because the type specified is not allowed in a blocked request"),  // NOSONAR
    JRTargetPid("JRTargetPid", 0x0460, "A target pid specified is incorrect"),  // NOSONAR
    JRSignalPid("JRSignalPid", 0x0461, "A signal pid specified is incorrect"),  // NOSONAR
    JRNoCallerPid("JRNoCallerPid", 0x0462, "The Signal_Pid or Target_Pid did not contain the pid of the caller"),  // NOSONAR
    JRAnr("JRAnr", 0x0463, "Vnode operation ACCEPT_AND_RECEIVE is not supported by this file system"),  // NOSONAR
    JRSrx("JRSrx", 0x0464, "Vnode operation SR_CSM is not supported by this file system"),  // NOSONAR
    JRWrongKey("JRWrongKey", 0x0465, "An error occurred during process signal initialization"),  // NOSONAR
    JRPtBadBlkOffset("JRPtBadBlkOffset", 0x0466, "A PtBRInfo entry has an offset that either points to within the PtBRInfo block or past the end of the area provided by the user"),  // NOSONAR
    JRSsetTooSmall("JRSsetTooSmall", 0x0467, "The OldCount value supplied was too small to accomodate the number of built entries"),  // NOSONAR
    JRInvalidRange("JRInvalidRange", 0x0468, "The NewCount or OldCount was greater than the maximum number of signals allowed"),  // NOSONAR
    JRInvalidOption("JRInvalidOption", 0x0469, "The option specified is not supported"),  // NOSONAR
    JRInvalidBinSemUndo("JRInvalidBinSemUndo", 0x046a, "The requested operation violates binary semaphore rules by specifying UNDO option"),  // NOSONAR
    JRInvalidBinSemNumSemOps("JRInvalidBinSemNumSemOps", 0x046b, "The requested operation violates binary semaphore rules by specifying more than one operation per semop"),  // NOSONAR
    JRInvalidBinSemNotBinOp("JRInvalidBinSemNotBinOp", 0x046c, "The requested operation violates binary semaphore rules by specifying too large a semop or semctl value for the semval"),  // NOSONAR
    JRInvalidBinSemFlag("JRInvalidBinSemFlag", 0x046d, "A semgt request matched an existing key or ID but the __IPC_BINSEM flag operand does not match"),  // NOSONAR
    JRInvalidBinSemSetAll("JRInvalidBinSemSetAll", 0x046e, "A requested semctl SETALL is being done after a semop for a __IPC_BINSEM semaphore set"),  // NOSONAR
    JRNegativeLength("JRNegativeLength", 0x046f, "A negative length was specified for either the header length or trailer length on the SEND_FILE Syscall"),  // NOSONAR
    JRInValidOffset("JRInValidOffset", 0x0470, "The offset parameter specified on the SEND_FILE syscall is not correct. It is either a negative number or it specifies an offset past the end of the file"),  // NOSONAR
    JRTooManyBytes("JRTooManyBytes", 0x0471, "The FILE_BYTES parameter specified on the SEND_FILE syscall is larger than the file size"),  // NOSONAR
    JRSocketNonBlock("JRSocketNonBlock", 0x0472, "The socket descriptor specified on the SEND_FILE syscall is a non-blocking socket. SEND_FILE requires a blocking socket"),  // NOSONAR
    JRLocalSpawnNotAllowed("JRLocalSpawnNotAllowed", 0x0473, "A request to spawn a local child process could not be completed because of conflicting inheritance attributes. A local spawn request with options that affect the attributes of the address space is not allowed because this would affect the attributes of the current address space where the new process would be created. The spawn request specified an inheritance structure or environment variable settings that would have changed one or more of the following attributes of the address space: the region size, the memory limit, the time limit, accounting information, the user ID"),  // NOSONAR
    JRInValidSFPLLen("JRInValidSFPLLen", 0x0474, "The SFPL_LENGTH parameter specified on the SEND_FILE syscall is not correct"),  // NOSONAR
    JRPtInvFPCWrite("JRPtInvFPCWrite", 0x0475, "The value specified for writing into the Floating Point Control Register is not valid"),  // NOSONAR
    JRMaxAiocbEcb("JRMaxAiocbEcb", 0x0476, "The maximum number of AIOCBs with user defined ECBs was exceeded on the requested function"),  // NOSONAR
    JRCertAlreadyDefined("JRCertAlreadyDefined", 0x0477, "The certificate being registered/deregistered is already defined for another user"),  // NOSONAR
    JRCertDoesNotMeetReq("JRCertDoesNotMeetReq", 0x0478, "The certificate being registered/deregistered does not meet RACF requirements"),  // NOSONAR
    JRLockFcnCode("JRLockFcnCode", 0x0479, "The value specified for the LockFcnCode parameter for the BPX1SLK service is not valid"),  // NOSONAR
    JRLockReqType("JRLockReqType", 0x047a, "The value specified for the LockReqType parameter for the BPX1SLK service is not valid"),  // NOSONAR
    JRLockType("JRLockType", 0x047b, "The value specified for the LockType parameter for the BPX1SLK service is not valid"),  // NOSONAR
    JRLockAddr("JRLockAddr", 0x047c, "The address specified for the LockAddr parameter for the BPX1SLK service is not a valid shared memory address"),  // NOSONAR
    JRLockToken("JRLockToken", 0x047d, "The value specified for the LockToken parameter for the BPX1SLK service is not valid"),  // NOSONAR
    JRLockedAlready("JRLockedAlready", 0x047e, "The lock represented by the specified lock token is already in a held state"),  // NOSONAR
    JRLockInUse("JRLockInUse", 0x047f, "The lock represented by the specified lock token is in use for a condition wait"),  // NOSONAR
    JRLockShmAcc("JRLockShmAcc", 0x0480, "The caller does not have read/write access to the shared memory in which the specified lock resides"),  // NOSONAR
    JRLockNotOwner("JRLockNotOwner", 0x0481, "The calling thread does not own the lock represented by the specified lock token"),  // NOSONAR
    JRLockMaxCntSys("JRLockMaxCntSys", 0x0482, "The maximum number of shared memory locks for the system have been initialized"),  // NOSONAR
    JRLockMaxCntThd("JRLockMaxCntThd", 0x0483, "The maximum number of shared memory locks for the calling thread have been obtained"),  // NOSONAR
    JRLockMaxCntRecurse("JRLockMaxCntRecurse", 0x0484, "The maximum number of recursive lock obtains for a given lock has been reached"),  // NOSONAR
    JRLockShmRemoved("JRLockShmRemoved", 0x0485, "The specified lock cannot be obtained because it is in shared memory that has been removed"),  // NOSONAR
    JRBadAioEcb("JRBadAioEcb", 0x0486, "An Ecb represented by AioEcbPtr in one of the input Aiocbs was found to be bad"),  // NOSONAR
    JRNoPtSecEnv("JRNoPtSecEnv", 0x0487, "Only a thread level identity created with pthread_security_np (BPX1TLS) can be propagated over a socket"),  // NOSONAR
    JRMultiThreaded("JRMultiThreaded", 0x0488, "The requested service cannot be performed in an address space with multiple user threads"),  // NOSONAR
    JRActAcceptUserid("JRActAcceptUserid", 0x0489, "The active identity for the caller /process was propagated over a socket via accept(). The propagated identity must be cleaned up before another identity can be propagted"),  // NOSONAR
    JRLockTokenAddr("JRLockTokenAddr", 0x048a, "The address specified in the LockTokenAddr parameter for the BPX1SLK service is not accessible"),  // NOSONAR
    JREnclaveErr("JREnclaveErr", 0x048b, "While changing to an identity propagated by a socket an error occurred while attempting to join/leave the propagated enclave"),  // NOSONAR
    JRNotInWLMEnclave("JRNotInWLMEnclave", 0x048c, "The unit of work is not in a WLM enclave. BPX1WLM did not return an enclave token "),  // NOSONAR
    JRCSMfailure("JRCSMfailure", 0x048d, "The get_buffer request to get a CSM buffer failed"),  // NOSONAR
    JRMaxQueuedSigs("JRMaxQueuedSigs", 0x048e, "Maximum number of queued signal exceeded by the invoking process"),  // NOSONAR
    JRSigInfoLen("JRSigInfoLen", 0x048f, "The value specified for the SigInfo_Len parameter on a BPX1STW (sigtimedwait) syscall was not valid"),  // NOSONAR
    JRLevelTooHigh("JRLevelTooHigh", 0x0490, "The load module format of the target executable file is at a level higher than the current system supports"),  // NOSONAR
    JRBpxoinitStarted("JRBpxoinitStarted", 0x0491, "The OMVS initial process must be started by the system. Do not use the START operator command to start the OMVS initial process"),  // NOSONAR
    JRBpxoinitNotUid0("JRBpxoinitNotUid0", 0x0492, "The userid associated with system procedure, BPXOINIT, must have uid=0 in the OMVS segment in the security database"),  // NOSONAR
    JRStickyBit("JRStickyBit", 0x0493, "A program with the sticky bit was found but it is not supported on this call"),  // NOSONAR
    JRMaxAsyncIO("JRMaxAsyncIO", 0x0494, "The maximum number of outstanding async I/O requests has been exceeded by the invoking process"),  // NOSONAR
    JRNotPtCreated("JRNotPtCreated", 0x0495, "Invoking task is not pthread created"),  // NOSONAR
    JRNotPtSecurityThe("JRNotPtSecurityThe", 0x0496, "The current task security environment is not set up via pthread_security_np"),  // NOSONAR
    JRAlreadyActive("JRAlreadyActive", 0x0497, "Requested function is already active"),  // NOSONAR
    JRSecActive("JRSecActive", 0x0498, "Task level security environment already active"),  // NOSONAR
    JRInvOsenvTok("JRInvOsenvTok", 0x0499, "Input osenv token is incorrect"),  // NOSONAR
    JRNoPersist("JRNoPersist", 0x049a, "Unpersist requested but persist count is 0"),  // NOSONAR
    JROsenvWlmMismatch("JROsenvWlmMismatch", 0x049b, "osenv WLM Enclave membership does not match the current pthread WLM Enclave membership"),  // NOSONAR
    JROsenvWrongEnclave("JROsenvWrongEnclave", 0x049c, "Current task is not associated with the osenv WLM enclave"),  // NOSONAR
    JROsenvBeginEnvOutstanding("JROsenvBeginEnvOutstanding", 0x049d, "Current task is operating under an outstanding WLM Begin environment. Enclave leave is not allowed"),  // NOSONAR
    JROsenvNotEjoinedTcb("JROsenvNotEjoinedTcb", 0x049e, "Current task did not issue WLM Enclave Join, but only inherited Enclave attribute from mother task"),  // NOSONAR
    JROsenvEnclaveSubTaskExists("JROsenvEnclaveSubTaskExists", 0x049f, "Current task has residual subtasks propagated to the enclave which are still associated with the Enclave"),  // NOSONAR
    JROsenvSecurityMismatch("JROsenvSecurityMismatch", 0x04a0, "Current security environment does not match the value specified in the osenv token"),  // NOSONAR
    JROsenvNotActive("JROsenvNotActive", 0x04a1, "Osenv environment is not active"),  // NOSONAR
    JROsenvPersistCntBad("JROsenvPersistCntBad", 0x04a2, "There are no outstanding persist requests"),  // NOSONAR
    JRWlmJoinError("JRWlmJoinError", 0x04a3, "Bad return code from IWMEJOIN macro"),  // NOSONAR
    JRIwmeleavError("JRIwmeleavError", 0x04a4, "Bad return code from IWMELEAV macro"),  // NOSONAR
    JRNotSupInSysplex("JRNotSupInSysplex", 0x04a5, "In sysplex, for remount to be supported, all systems must be at a release that provides remount support. In sysplex, unmount drain is tried as unmount normal, but if it cannot complete, an error is returned"),  // NOSONAR
    JRSysplexRecoveryInProg("JRSysplexRecoveryInProg", 0x04a6, "An unmount or some file system command was attempted while sysplex system recovery was in progress"),  // NOSONAR
    JRMustBeImmed("JRMustBeImmed", 0x04a7, "An unmount was attempted in a sysplex for a filesystem that is unowned, and Immed was not specified"),  // NOSONAR
    JRResetAlreadyInProg("JRResetAlreadyInProg", 0x04a8, "Unmount reset is already in progress"),  // NOSONAR
    JRInvalidSize("JRInvalidSize", 0x04a9, "Buffer received by mount was too small"),  // NOSONAR
    JRUnknownBlock("JRUnknownBlock", 0x04aa, "Control block received is unrecognized"),  // NOSONAR
    JRBadCombo("JRBadCombo", 0x04ab, "Conflicting values have been received"),  // NOSONAR
    JRNoWildFromSys("JRNoWildFromSys", 0x04ac, "Conflicting values have been received"),  // NOSONAR
    JRNoValues("JRNoValues", 0x04ad, "No values for the chmount request have been given"),  // NOSONAR
    JRMustBeSysplex("JRMustBeSysplex", 0x04ae, "This request only possible in a sysplex enivornment"),  // NOSONAR
    JRAttemptsExhausted("JRAttemptsExhausted", 0x04af, "Every attempt to move the filesystem has failed"),  // NOSONAR
    JRLostState("JRLostState", 0x04b0, "The filesystem can no longer be moved by this process"),  // NOSONAR
    JRNoFROMSYS("JRNoFROMSYS", 0x04b1, "The system specified as the FROMSYS can not be found"),  // NOSONAR
    JRNoFSFound("JRNoFSFound", 0x04b2, "The filesystem requested to be moved can not be found"),  // NOSONAR
    JRInRecovery("JRInRecovery", 0x04b3, "A required file system is being recovered"),  // NOSONAR
    JRNoMountPointFound("JRNoMountPointFound", 0x04b4, "The mountpoint requested to be moved can not be found"),  // NOSONAR
    JRBlackHole("JRBlackHole", 0x04b5, "Filesystem is unowned"),  // NOSONAR
    JRNoPlace("JRNoPlace", 0x04b6, "This filesystem can not be placed"),  // NOSONAR
    JRNoSystemFound("JRNoSystemFound", 0x04b7, "There is no system with that name"),  // NOSONAR
    JRSysplexDataSyncLost("JRSysplexDataSyncLost", 0x04b8, "The I/O request is rejected because the file integrity was lost due to the failure of the file system server"),  // NOSONAR
    JRTdGone("JRTdGone", 0x04b9, "The socket Transport Driver has terminated and restarted"),  // NOSONAR
    JRNoDeviceFound("JRNoDeviceFound", 0x04ba, "There is no longer any device with that device number"),  // NOSONAR
    JRNotMoveable("JRNotMoveable", 0x04bc, "This filesystem can not be moved"),  // NOSONAR
    JRNoLongerServer("JRNoLongerServer", 0x04bd, "The server detected it is no longer the server"),  // NOSONAR
    JRLockCtl("JRLockCtl", 0x04be, "The lockctl service vnode operation is not supported"),  // NOSONAR
    JRCPLNotAuth("JRCPLNotAuth", 0x04bf, "__cpl service call failed permission check for BPX.CF facility class profile"),  // NOSONAR
    JRCPLInvFcnCode("JRCPLInvFcnCode", 0x04c0, "__cpl service call failed because the supplied function code is not valid"),  // NOSONAR
    JRCPLInvBuffLen("JRCPLInvBuffLen", 0x04c1, "__cpl service call failed because the supplied buffer length is not valid"),  // NOSONAR
    JRCPLBuffTooSmall("JRCPLBuffTooSmall", 0x04c2, "__cpl service call failed because the supplied buffer was too small for the data to be returned"),  // NOSONAR
    JRSysplexBlackHoleIO("JRSysplexBlackHoleIO", 0x04c3, "The I/O request is rejected because access to the owning file system was lost. This is a temporary condition"),  // NOSONAR
    JRSysplexRecoveryTO("JRSysplexRecoveryTO", 0x04c4, "Filesystem recovery timeout. One or multiple file systems may not have been recovered, and are thus black holes"),  // NOSONAR
    JRCPLInvStrucType("JRCPLInvStrucType", 0x04c5, "__cpl service call failed because one of the supplied structure entries is defined with an incorrect type"),  // NOSONAR
    JRCPLCFNotFound("JRCPLCFNotFound", 0x04c6, "__cpl service call failed because a Coupling Facility at the appropriate level (level 8 or greater) could not be found in the sysplex where this __cpl callable service routine is running"),  // NOSONAR
    JRShrLibNotAuthorized("JRShrLibNotAuthorized", 0x04c7, "The chattr function can not be performed to change the shared library attribute because the caller is not permitted to the BPX.FILEATTR.SHARELIB facility class"),  // NOSONAR
    JROutOfAutomountEntries("JROutOfAutomountEntries", 0x04c8, "The system is unable to obtain an entry in the file system automount table in the OMVS couple data set"),  // NOSONAR
    JRNeedAbsPath("JRNeedAbsPath", 0x04c9, "An absolute path name must be used when performing a mount command from a userid that has changed roots"),  // NOSONAR
    JRMapBadFunction("JRMapBadFunction", 0x04ca, "The __Map function or sub-function code was not valid"),  // NOSONAR
    JRNotAuthMAP("JRNotAuthMAP", 0x04cb, "__map_init call failed permission check for BPX.MAP facility class profile"),  // NOSONAR
    JRBadBlkAddr("JRBadBlkAddr", 0x04cc, "__map service was passed a bad block address. The block address specified was not within the map area, or was not on a block boundary"),  // NOSONAR
    JRMapAlreadyActive("JRMapAlreadyActive", 0x04cd, "__map_init requested for a process that already had an __map environment active"),  // NOSONAR
    JRMapOutOfBlocks("JRMapOutOfBlocks", 0x04ce, "__map service, processing for a new block or connect request, was asked to select a map block that was not currently in use, but none was available as all blocks were currently in use"),  // NOSONAR
    JRMapNotActive("JRMapNotActive", 0x04cf, "__map service was requested but either no map environment is active or the current map environment is being shut down"),  // NOSONAR
    JRMapTokenNotFound("JRMapTokenNotFound", 0x04d0, "__map service data block token not defined"),  // NOSONAR
    JRMapBlockNotInUse("JRMapBlockNotInUse", 0x04d1, "__map service disconnect is issued for a map block that is not currently in use"),  // NOSONAR
    JRMapBlockInUse("JRMapBlockInUse", 0x04d2, "__map service new block or connect specified a map block that is currently in use"),  // NOSONAR
    JRMapBlockFreePending("JRMapBlockFreePending", 0x04d3, "__map service connect request specified a data block that is currently being freed"),  // NOSONAR
    JRMapBadStorage("JRMapBadStorage", 0x04d4, "The parameter list either could not be accessed or was in read only storage and could not be updated"),  // NOSONAR
    JRMapUnexpectedErr("JRMapUnexpectedErr", 0x04d5, "An error occurred in an __map service"),  // NOSONAR
    JRReservedValueInvalid("JRReservedValueInvalid", 0x04d6, "A reserved field contained a value other than zero"),  // NOSONAR
    JRMapArrayCountErr("JRMapArrayCountErr", 0x04d7, "The array count was outside the valid values"),  // NOSONAR
    JRNotClient("JRNotClient", 0x04d8, "This system is not a client of the requested filesystem"),  // NOSONAR
    JRJointMsgSent("JRJointMsgSent", 0x04d9, "The joint LFS/PFS message requested for osi_xmsg to send has already been sent"),  // NOSONAR
    JRNoWildAutoMove("JRNoWildAutoMove", 0x04da, "The automove setting can not be changed while moving a collection of filesystems"),  // NOSONAR
    JRInvalidFSP("JRInvalidFSP", 0x04db, "The file does not have a valid FSP"),  // NOSONAR
    JRNoRoot("JRNoRoot", 0x04dc, "The user home directory did not exist or there is no system root mounted"),  // NOSONAR
    JRMoveInProgress("JRMoveInProgress", 0x04dd, "The filesystem is in the process of being moved to a different server"),  // NOSONAR
    JRBadBodyLength("JRBadBodyLength", 0x04de, "The MNTE2 does not have a correct body length coded into the body length field in the MNTE header"),  // NOSONAR
    JRXcfNoStorage("JRXcfNoStorage", 0x04df, "I/O buffer shortage for writing data through XCF"),  // NOSONAR
    JRPFSFailed("JRPFSFailed", 0x04e0, "A PFS returned a RetVal of -1 with an errno and errnojr of zero"),  // NOSONAR
    JRPtInvGPRHNumber("JRPtInvGPRHNumber", 0x04e1, "The ptrace call has an incorrect general register number"),  // NOSONAR
    JRFiletooLarge("JRFiletooLarge", 0x04e2, "File size exceeded the value that can be held in object of type off_t"),  // NOSONAR
    JRCantExpClient("JRCantExpClient", 0x04e3, "V_export of a sysplex client file system is not allowed"),  // NOSONAR
    JRIsExported("JRIsExported", 0x04e4, "Moving a file system that is exported is not allowed. Remounting an exported filesystem to a mode where it would then be exported on a served client is not allowed"),  // NOSONAR
    JRFsQuiescedMt("JRFsQuiescedMt", 0x04e5, "A file system operation was rejected because the owning file system is quiesced and the operation cannot be delayed"),  // NOSONAR
    JRMptFsQuiesced("JRMptFsQuiesced", 0x04e6, "A mount request was rejected because a file system in the mount path is quiesced"),  // NOSONAR
    JRMmapSuspended("JRMmapSuspended", 0x04e7, "A memory map request was rejected because the memory map function is suspended"),  // NOSONAR
    JRSfsDiagAct("JRSfsDiagAct", 0x04e8, "A Shared-FS diagnostic operation is already in progress"),  // NOSONAR
    JRInvalidFileTag("JRInvalidFileTag", 0x04e9, "The file tag supplied is invalid"),  // NOSONAR
    JRDOMParms("JRDOMParms", 0x04ea, "The CCADOMToken and CCAMsgIdList are mutually exclusive, both were specified"),  // NOSONAR
    JRTooManyMsgIDs("JRTooManyMsgIDs", 0x04eb, "The maximum number of messages that can be deleted in one operation is 60, more then 60 were specified"),  // NOSONAR
    JRAuthRoutingCode("JRAuthRoutingCode", 0x04ec, "An unauthorized caller (not UID=0) specified a message routing code reserved for authorized caller"),  // NOSONAR
    JRRoutCode("JRRoutCode", 0x04ed, "An invalid message routing code was specified"),  // NOSONAR
    JRDescCode("JRDescCode", 0x04ee, "An invalid message descriptor code was specified"),  // NOSONAR
    JRRoutingList("JRRoutingList", 0x04ef, "All or part of the list of routing codes pointed to by CCARoutCdeList is not addressable by the caller"),  // NOSONAR
    JRDescList("JRDescList", 0x04f0, "All or part of the list of descriptor codes pointed to by CCADescList is not addressable by the caller"),  // NOSONAR
    JRMsgIdList("JRMsgIdList", 0x04f1, "All or part of the list of message ids to be deleted pointed to by CCAMsgIdList is not addressable by the caller"),  // NOSONAR
    JRTooManyRoutCodes("JRTooManyRoutCodes", 0x04f2, "The maximum number of routing codes that can be specified is 128 but was exceeded"),  // NOSONAR
    JRTooManyDescCodes("JRTooManyDescCodes", 0x04f3, "The maximum number of descriptor codes that can be specified is 6 but was exceeded"),  // NOSONAR
    JRMsgId("JRMsgId", 0x04f4, "All or part of the location specified by the caller for the returned message id (CCAMsgIDPtr) is not addressable by the caller"),  // NOSONAR
    JRSysplexEnq("JRSysplexEnq", 0x04f5, "The ENQ for file system sysplex serialization is already held by another local task or by another system in the sysplex"),  // NOSONAR
    JRLfsProtocolLev("JRLfsProtocolLev", 0x04f6, "The function could not be performed because the minimum required LFS protocol level was not met by all systems in the sysplex group"),  // NOSONAR
    JRMemberListBad("JRMemberListBad", 0x04f7, "An inconsistency between the XCF representation of the BPXGRP member group and the local representation exists"),  // NOSONAR
    JRRcvBufTooSmall("JRRcvBufTooSmall", 0x04f8, "A message will not fit in the receive buffer. The size of the receive buffer for the receiving socket, which was set by the setsockopt syscall with the so_rcvbuf option, is too small to contain the data being sent to it. Truncation would occur"),  // NOSONAR
    JRMutualExclInProgress("JRMutualExclInProgress", 0x04f9, "Mutually-exclusive operations are currently in progress in the sysplex. The request cannot proceed"),  // NOSONAR
    JRUnmountAllInProg("JRUnmountAllInProg", 0x04fa, "MODIFY FILESYS=UNMOUNTALL is in progress. MOUNT cannot proceed"),  // NOSONAR
    JRQuiesceInProg("JRQuiesceInProg", 0x04fb, "A pthread quiesce operation is already in progress"),  // NOSONAR
    JRRequestTypeErr("JRRequestTypeErr", 0x04fc, "The request type contains an incorrect value"),  // NOSONAR
    JRNoFreezeExit("JRNoFreezeExit", 0x04fd, "The pthread_quiesce_and_get_np service (BPX1PQG) cannot be performed because the Quick_freeze_exit has not been registered"),  // NOSONAR
    JRNotExitKey("JRNotExitKey", 0x04fe, "The PSW key of the caller is not same as key registered under Quick_freeze_exit"),  // NOSONAR
    JRInvThdq("JRInvThdq", 0x04ff, "Incorrect quick pthread quiesce data structure"),  // NOSONAR
    JRNotFrozen("JRNotFrozen", 0x0500, "A pthread_quiesce_and_get_np (BPX1PQG) unfreeze_all request cannot complete because the process is not frozen"),  // NOSONAR
    JRQFrzExitError("JRQFrzExitError", 0x0501, "A pthread_quiesce_and_get_np (BPX1PQG) service call failed because the language environment quick freeze exit abnormally ended"),  // NOSONAR
    JRExitAlreadyReg("JRExitAlreadyReg", 0x0502, "The quick freeze exit has already been registered"),  // NOSONAR
    JRCannotDeregister("JRCannotDeregister", 0x0503, "The quick freeze exit cannot be deregistered"),  // NOSONAR
    JRRequestorThread("JRRequestorThread", 0x0504, "The requestor thread cannot be frozen"),  // NOSONAR
    JRThdsNotSafe("JRThdsNotSafe", 0x0505, "Specified threads cannot be frozen in a safe state"),  // NOSONAR
    JRDupThreads("JRDupThreads", 0x0506, "Duplicate threads specified in data structure"),  // NOSONAR
    JRSysEntryBad("JRSysEntryBad", 0x0507, "An inconsistency between the XCF representation and the file system representation of a member in the BPXGRP group exists"),  // NOSONAR
    JRFileNotEmpty("JRFileNotEmpty", 0x0508, "The file is not empty"),  // NOSONAR
    JRNoDelRequested("JRNoDelRequested", 0x0509, "The directory does not allow unlinks"),  // NOSONAR
    JRConversionErr("JRConversionErr", 0x050a, "Unicode/390 conversion error"),  // NOSONAR
    JRUnconvertibleChar("JRUnconvertibleChar", 0x050b, "Character is not convertible"),  // NOSONAR
    JRInvalidCcsid("JRInvalidCcsid", 0x050c, "CCSID not supported"),  // NOSONAR
    JRConversionEnv("JRConversionEnv", 0x050d, "Unicode/390 environmental error"),  // NOSONAR
    JRUnknownConversion("JRUnknownConversion", 0x050e, "Unicode/390 environmental error"),  // NOSONAR
    JRNoTaskACEE("JRNoTaskACEE", 0x050f, "Invoker must have task level ACEE"),  // NOSONAR
    JRNotWLMACEE("JRNotWLMACEE", 0x0510, "ACEE must be WLM created"),  // NOSONAR
    JRTLSDONEONIPT("JRTLSDONEONIPT", 0x0511, "IPT already invoked TLS_TASK_ACEE#"),  // NOSONAR
    JRInvEcbPtr("JRInvEcbPtr", 0x0512, "The ECB pointer is not a 31-bit address"),  // NOSONAR
    JRAmode64("JRAmode64", 0x0513, "Invalid addressing mode"),  // NOSONAR
    JRExecExitAboveBar("JRExecExitAboveBar", 0x0514, "Exec user exit is above the bar"),  // NOSONAR
    JRInvalidMedWtAmode("JRInvalidMedWtAmode", 0x0515, "Medium weight process can not change AMODE"),  // NOSONAR
    JRNeedMountLatch("JRNeedMountLatch", 0x0516, "BPXXCDSS service invoker must hold mount latch"),  // NOSONAR
    JRBadLfsVersion("JRBadLfsVersion", 0x0517, "LFS Version incompatibility exists"),  // NOSONAR
    JRAlreadyInShutDown("JRAlreadyInShutDown", 0x0518, "The block or permanent process cannot be registered"),  // NOSONAR
    JRBlockPermAlreadyRegistered("JRBlockPermAlreadyRegistered", 0x0519, "The process or job is already registered"),  // NOSONAR
    JRBlockPermNotRegistered("JRBlockPermNotRegistered", 0x051a, "The process or job is not registered"),  // NOSONAR
    JRJobNameNotValid("JRJobNameNotValid", 0x051b, "The Job Name was not found"),  // NOSONAR
    JRJSTMustBeRegistered("JRJSTMustBeRegistered", 0x051c, "The Job Step Process must be registered"),  // NOSONAR
    JRNotAuthShutdown("JRNotAuthShutdown", 0x051d, "SHUTDOWN_REG call failed permission check for BPX.SHUTDOWN facility class profile"),  // NOSONAR
    JRUnsupportedEnv("JRUnsupportedEnv", 0x051e, "Caller attempted to call a USS Service in an unsupported environment"),  // NOSONAR
    JRPidInvalid("JRPidInvalid", 0x051f, "Pid specified for _BPXK_PIDXFER is invalid"),  // NOSONAR
    JRPidOutOfRange("JRPidOutOfRange", 0x0520, "Pid specified for _BPXK_PIDXFER is out of range"),  // NOSONAR
    JRXferPidNotFound("JRXferPidNotFound", 0x0521, "Pid specified for _BPXK_PIDXFER could not be found"),  // NOSONAR
    JRNoPidXfer("JRNoPidXfer", 0x0522, "The caller is not authorized to debug the target process specified by the _BPXK_PIDXFER environment variable"),  // NOSONAR
    JRPidXferNoExtLink("JRPidXferNoExtLink", 0x0523, "External Links are not allowed with PidXfer"),  // NOSONAR
    JRPidXferSameAS("JRPidXferSameAS", 0x0524, "Pid specified for _BPXK_PIDXFER is available"),  // NOSONAR
    JRPidXferAsyncFail("JRPidXferAsyncFail", 0x0525, "An asynchronous request for this Pid failed"),  // NOSONAR
    JRNoPidXferDefUids("JRNoPidXferDefUids", 0x0526, "Cannot PIDXFER when both PIDs have default userid"),  // NOSONAR
    JRRestartedFd("JRRestartedFd", 0x0527, "The file descriptor is left over after OMVS Restart"),  // NOSONAR
    JRInvBuffSize("JRInvBuffSize", 0x0529, "The length of the buffer is not valid"),  // NOSONAR
    JRInvSendBuffSize("JRInvSendBuffSize", 0x052a, "The send buffer size is not valid"),  // NOSONAR
    JRPXNoMpMu("JRPXNoMpMu", 0x0528, "Cannot PIDXFER to multi-process/multi-user address space"),  // NOSONAR
    JROnePXOnly("JROnePXOnly", 0x052b, "Only one PIDXFER process allowed per address space"),  // NOSONAR
    JRPXExecFileTooBig("JRPXExecFileTooBig", 0x052c, "The size of the specified file exceeds the private region of the PidXfer target address space"),  // NOSONAR
    JRPXNoSpace("JRPXNoSpace", 0x052d, "Not enough virtual storage available in the target PIDXFER address space"),  // NOSONAR
    JRInheMemLimit("JRInheMemLimit", 0x052e, "MemLimit size specified in Inheritance structure is not valid"),  // NOSONAR
    JR64BitNotSupp("JR64BitNotSupp", 0x052f, "Transport does not support 64-bit addresses"),  // NOSONAR
    JRCinetNotAttached("JRCinetNotAttached", 0x0531, "The Cinet is configured and this name does match a stack but that stack is not attached to this socket"),  // NOSONAR
    JRNoCinet("JRNoCinet", 0x0532, "Name does not match, but Common Inet is not configured, or this is not a socket, so this error may not matter to the application"),  // NOSONAR
    JRCinetBadName("JRCinetBadName", 0x0533, "The Cinet is configured and this name does not match any stack"),  // NOSONAR
    JRFilesLocked("JRFilesLocked", 0x0534, "Command not allowed because one or more applications have byte range locks on files in the filesystem"),  // NOSONAR
    JRNoIPv6Stacks("JRNoIPv6Stacks", 0x0535, "There are no TCPIP stacks currently supporting IPv6"),  // NOSONAR
    JRMedProcTerm("JRMedProcTerm", 0x0536, "Call to BPX1MPC for this medium weight process failed because the process is already in termination"),  // NOSONAR
    JRNoListAuthPgmPath("JRNoListAuthPgmPath", 0x0537, "There is no authorized program path list entry"),  // NOSONAR
    JRNoListPgmCntlPath("JRNoListPgmCntlPath", 0x0538, "There is no program control path list entry"),  // NOSONAR
    JRNoAPFPgmName("JRNoAPFPgmName", 0x0539, "There is no APF Program name entry"),  // NOSONAR
    JRAfNotRightForSocket("JRAfNotRightForSocket", 0x053a, "The address family specified in the sockaddr does not match the address family of the socket"),  // NOSONAR
    JRTdOptGone("JRTdOptGone", 0x053b, "The stack chosen by the IPv6 option or by the IPv4 option is not active on this socket"),  // NOSONAR
    JRV6OnlyOnOnly("JRV6OnlyOnOnly", 0x053c, "The IPV6_V6ONLY socket option may only be turned ON"),  // NOSONAR
    JRBadArgValue("JRBadArgValue", 0x053d, "The argument value passed to the function is not within the range allowed"),  // NOSONAR
    JRPreProcInitExitReject("JRPreProcInitExitReject", 0x053e, "A Pre-Process Initiation exit rejected the process initiation"),  // NOSONAR
    JRPreProcInitExitAbend("JRPreProcInitExitAbend", 0x053f, "A Pre-Process Initiation exit Abended"),  // NOSONAR
    JRPosProcInitExitAbend("JRPosProcInitExitAbend", 0x0540, "A Pos-Process Initiation exit Abended. was in error"),  // NOSONAR
    JRPreProcInitExitERROR("JRPreProcInitExitERROR", 0x0541, "A Pre-Process Initiation exit CSVDYNEX call was in error"),  // NOSONAR
    JRPosProcInitExitERROR("JRPosProcInitExitERROR", 0x0542, "A Pos-Process Initiation exit CSVDYNEX call was in error"),  // NOSONAR
    JRInvalidSyslist("JRInvalidSyslist", 0x0543, "The system list is not valid"),  // NOSONAR
    JRMountRedirected("JRMountRedirected", 0x0544, "The mount request has been redirected to another system"),  // NOSONAR
    JRInvIoctlArg("JRInvIoctlArg", 0x0545, "The argument value used in the ioctl is not valid for the command"),  // NOSONAR
    JRInvIoctlArgLen("JRInvIoctlArgLen", 0x0546, "The length of the argument used with the ioctl command is not valid for the command"),  // NOSONAR
    JRDefUidNotAllowed("JRDefUidNotAllowed", 0x0547, "The function is failed because it is not allowed from a user using the default OMVS segment"),  // NOSONAR
    JRZeroScopeTdx("JRZeroScopeTdx", 0x0548, "The Scope_Id has a zero Cinet Transport Driver Index"),  // NOSONAR
    JRZeroIfTdx("JRZeroIfTdx", 0x0549, "An Interface Index has a zero Cinet Transport Index"),  // NOSONAR
    JRTdxMisMatch("JRTdxMisMatch", 0x054a, "Scope_Id and Interface Index do not agree"),  // NOSONAR
    JRShutDownInProgress("JRShutDownInProgress", 0x054b, "Kernel Shutdown is in progress on this system"),  // NOSONAR
    JRCDSFailure("JRCDSFailure", 0x054c, "A failure was encountered while trying to access the CDS"),  // NOSONAR
    JRCanNotBeOwner("JRCanNotBeOwner", 0x054d, "This system can not be a file system owner thru a move, newroot or recovery operation"),  // NOSONAR
    JRInCompleteMove("JRInCompleteMove", 0x054e, "A file system was in the process of changing file owner when the target system (new owner) exited the sysplex during the move operation. The file system is being recovered"),  // NOSONAR
    JROutOfGvskCells("JROutOfGvskCells", 0x054f, "The system is unable to obtain a cell from the GVSK cell pool"),  // NOSONAR
    JRSymbFailed("JRSymbFailed", 0x0567, "A symlink failed symbol resolution"),  // NOSONAR
    JROperUndefined("JROperUndefined", 0x0568, "The operation argument specified with the WRITE_DOWN function for BPX1ENV is undefined"),  // NOSONAR
    JRScopeUndefined("JRScopeUndefined", 0x0569, "The scope argument specified with the WRITE_DOWN function for BPX1ENV is undefined"),  // NOSONAR
    JRNeedTaskAcee("JRNeedTaskAcee", 0x056a, "WD_SCOPE_THD scope was specified with the WRITE_DOWN function for BPX1ENV but the calling task does not have a task level ACEE"),  // NOSONAR
    JRPoeLenErr("JRPoeLenErr", 0x056b, "The length specified for the POE mapping is invalid"),  // NOSONAR
    JRPoeScopeErr("JRPoeScopeErr", 0x056c, "The scope option specified in the POE mapping is incorrect either none or both scope options were specified"),  // NOSONAR
    JRPoeEntryTypeErr("JRPoeEntryTypeErr", 0x056d, "The entry type specified in the POE mapping is invalid"),  // NOSONAR
    JRPoeEntryLenErr("JRPoeEntryLenErr", 0x056e, "The entry type length specified in the POE mapping is invalid"),  // NOSONAR
    JRNotPoeAuthorized("JRNotPoeAuthorized", 0x056f, "The __poe function can not be perfomed because the caller is not permitted to the BPX.POE FACILITY class profile or is not a superuser"),  // NOSONAR
    JRSecurityConflict("JRSecurityConflict", 0x0570, "A mount request was rejected because the NOSECURITY option was specified when (1) the SECLABEL class was active, or (2) when a nonprivileged user requested the mount operation. The operation is not allowed"),  // NOSONAR
    JRNoSecLabel("JRNoSecLabel", 0x0571, "The operation was rejected because the object does not have a security label in an enviroment for which security labels are required"),  // NOSONAR
    JRTooManyHomeIfs("JRTooManyHomeIfs", 0x0572, "A stack did not initialize because the Common Inet Sockets PreRouter could not obtain a buffer large enough to hold all of the home interface addresses"),  // NOSONAR
    JRTooManyRoutes("JRTooManyRoutes", 0x0573, "A stack did not initialize because the Common Inet Sockets PreRouter could not obtain a buffer large enough to hold all of the network routes"),  // NOSONAR
    JRUmountFail("JRUmountFail", 0x0574, "A vfs_umount immediate failed on one or more clients in the sysplex during a remount attempt. The remount cannot proceed"),  // NOSONAR
    JRFIFOInFileSys("JRFIFOInFileSys", 0x0575, "Remount is not allowed when there are FIFOs in the filesystem. FIFOs must be closed prior to remount"),  // NOSONAR
    JRAggregateErr("JRAggregateErr", 0x0576, "Remount is not allowed for a filesystem in an HFS-compatible aggregate if the clone is also mounted"),  // NOSONAR
    JRFileDesJustClosed("JRFileDesJustClosed", 0x0577, "A descriptor passed on this operation has just been closed by another thread"),  // NOSONAR
    JRSeclabelClassInactive("JRSeclabelClassInactive", 0x0578, "A seclabel can only be set on a file when the SECLABEL class is active"),  // NOSONAR
    JRNotSupportedForRemoteFile("JRNotSupportedForRemoteFile", 0x0579, "The requested service is not supported for a remote file such as a NFS mounted file"),  // NOSONAR
    JRIarv64Serv("JRIarv64Serv", 0x05c0, "An invocation of IARV64 service failed"),  // NOSONAR
    JRIarv64FCErr("JRIarv64FCErr", 0x05c1, "An invocation of IARV64FC service failed"),  // NOSONAR
    JRIsMountedRealName("JRIsMountedRealName", 0x05c2, "A mounted file system has a real or alias name that conflicts with this mount request"),  // NOSONAR
    JRSMCFcnCode("JRSMCFcnCode", 0x05c3, "The specified function code is not valid"),  // NOSONAR
    JRSMCFcnFlags("JRSMCFcnFlags", 0x05c4, "The specified function flags are not valid"),  // NOSONAR
    JRSMCWrongMutex("JRSMCWrongMutex", 0x05c5, "The specified mutex is not associated with the specified condition variable"),  // NOSONAR
    JRSMCNotMutex("JRSMCNotMutex", 0x05c6, "The specified shared memory object is not a mutex object (SMMX)"),  // NOSONAR
    JRSMCNotCondvar("JRSMCNotCondvar", 0x05c7, "The specified shared memory object is not a condition variable object (SMCV)"),  // NOSONAR
    JRSMCMutexLocked("JRSMCMutexLocked", 0x05c8, "A destroy of a shared memory mutex cannot be done because the mutex is currently locked by another thread"),  // NOSONAR
    JRSMCWaiters("JRSMCWaiters", 0x05c9, "A destroy of a shared memory mutex or condition variable cannot be done because at least one other thread is waiting for the object"),  // NOSONAR
    JRSMCShrObjAddr("JRSMCShrObjAddr", 0x05ca, "The specified shared object area is not accessible to the caller"),  // NOSONAR
    JRSMCEcbAddr("JRSMCEcbAddr", 0x05cb, "The specified ecb is not accessible to the caller"),  // NOSONAR
    JRSMCOutsideWait("JRSMCOutsideWait", 0x05cc, "A wait function was attempted with the SMC_OutsideWait flag specified which is not an allowed combination"),  // NOSONAR
    JRSmcAlreadySetup("JRSmcAlreadySetup", 0x05cd, "A setup to wait function call was attempted from a thread that is already setup to wait"),  // NOSONAR
    JRSmcShmAcc("JRSmcShmAcc", 0x05ce, "The caller does not have read/write access to the shared memory segment where the specified shared memory object resides"),  // NOSONAR
    JRSmcNotShared("JRSmcNotShared", 0x05cf, "The specified shared memory object is not in memory shareable by multiple address spaces"),  // NOSONAR
    JRSMCNotOwner("JRSMCNotOwner", 0x05d0, "The calling thread does not own the mutex represented by the specified SMMX data area"),  // NOSONAR
    JRSMCMaxCntSys("JRSMCMaxCntSys", 0x05d1, "The maximum number of shared memory mutexes and/or condition variables are in use on the system"),  // NOSONAR
    JRSMCUnusable("JRSMCUnusable", 0x05d2, "The object is no longer usable do to an unexpected failure during an operation against the object"),  // NOSONAR
    JRSMCMutexSetup("JRSMCMutexSetup", 0x05d3, "A setup to wait was done for a mutex with incorrect flag options specified"),  // NOSONAR
    JRSMCNotSetup("JRSMCNotSetup", 0x05d4, "A cancel setup to wait was done but the caller is not currently setup to wait"),  // NOSONAR
    JRSMCMemoryMap("JRSMCMemoryMap", 0x05d5, "The specified Shared memory object is in memory mapped storage which is not supported"),  // NOSONAR
    JRSMCMaxCntSeg("JRSMCMaxCntSeg", 0x05d6, "The maximum number of shared memory mutexes and/or condition variables are in use for a single shared memory segment"),  // NOSONAR
    JRSMCCondWaiters("JRSMCCondWaiters", 0x05d7, "A destroy of a shared memory mutex cannot be done because at least one other thread is waiting for the associated condition variable"),  // NOSONAR
    JRSigkillNotSent("JRSigkillNotSent", 0x05d8, "A SIGKILL signal must first be sent at least 3 seconds prior to the target process before attempting a superkill"),  // NOSONAR
    JRNoGroups("JRNoGroups", 0x05d9, "Can not target a group with a superkill"),  // NOSONAR
    JRUntargetable("JRUntargetable", 0x05da, "Can not target the address space with a superkill"),  // NOSONAR
    JRNoDDorFileSystem("JRNoDDorFileSystem", 0x05db, "Neither DDNAME nor FILESYSTEM was specified on a MOUNT statement in the processed parmlib member"),  // NOSONAR
    JRNoOMVSseg("JRNoOMVSseg", 0x05dc, "User profile has no OMVS segment"),  // NOSONAR
    JRNoUserID("JRNoUserID", 0x05dd, "No userid found"),  // NOSONAR
    JRUserIDUnDeftoRACF("JRUserIDUnDeftoRACF", 0x05de, "UserID is not defined to RACF"),  // NOSONAR
    JRUserIDUnDeftoRACFIA("JRUserIDUnDeftoRACFIA", 0x05df, "User ID is not defined to RACF"),  // NOSONAR
    JRMoveBrlmFailure("JRMoveBrlmFailure", 0x05e0, "Active byte range locks failed to move during a file system move"),  // NOSONAR
    JRAutoMoveable("JRAutoMoveable", 0x05e1, "A filesystem mounted in a mode for which it is capable of being directly mounted to the PFS on all systems is considered automoveable"),  // NOSONAR
    JRInvPFS("JRInvPFS", 0x05e2, "The requested operation is not allowed for this PFS"),  // NOSONAR
    JRTargetPidZombie("JRTargetPidZombie", 0x05e3, "The target pid specified is a zombie, AMODE information is not available for this process"),  // NOSONAR
    JRSMCDisabled("JRSMCDisabled", 0x05e4, "Use of a shared memory mutex has been disabled due to a cleanup problem with the application"),  // NOSONAR
    JRSMCAlreadyInit("JRSMCAlreadyInit", 0x05e5, "Initialization of a shared condition variable or Mutex failed because storage already contains a shared condition variable or mutex"),  // NOSONAR
    JRShrConflict("JRShrConflict", 0x05e6, "This open or remove type operation conflicts with a share reservation that has denied the access intended"),  // NOSONAR
    JRAccessConflict("JRAccessConflict", 0x05e7, "The file is already open in a way that this open is trying to deny"),  // NOSONAR
    JRBlockingDeny("JRBlockingDeny", 0x05e8, "A file may not be opened with Deny flags if O_NONBLOCK is off"),  // NOSONAR
    JROpenTokMax("JROpenTokMax", 0x05e9, "The maximum number of v_open tokens have been allocated for this process"),  // NOSONAR
    JRNoShrsAtOwner("JRNoShrsAtOwner", 0x05ea, "Share reservations were requested on the open of a file but the file is owned by another sysplex member that is at a level which does not support share reservations so they cannot be enforced"),  // NOSONAR
    JRCantMoveShares("JRCantMoveShares", 0x05eb, "A file system may not be moved to a sysplex member that does not support share reservations while there are active reservations on any file within that file system"),  // NOSONAR
    JRShrsLost("JRShrsLost", 0x05ec, "The sysplex member that owned a file terminated and file system ownership has been taken over by a system that does not support the share reservations that had been established on this file so they can no longer be enforced"),  // NOSONAR
    JRFileClosed("JRFileClosed", 0x05ed, "The file has been closed"),  // NOSONAR
    JRInvOpenTok("JRInvOpenTok", 0x05ee, "The Open Token is not valid"),  // NOSONAR
    JRStaleOpenTok("JRStaleOpenTok", 0x05ef, "The Open Token has been closed"),  // NOSONAR
    JRInvAccess("JRInvAccess", 0x05f0, "Access specified on a v_open must be read or write or both"),  // NOSONAR
    JRBadOpenType("JRBadOpenType", 0x05f1, "The VopnOpenType field on a v_open request has a value that is not supported"),  // NOSONAR
    JRCreateParmLen("JRCreateParmLen", 0x05f2, "The length specified on a v_open request for a create parameter is not valid for the type of request"),  // NOSONAR
    JRTokNotReg("JRTokNotReg", 0x05f3, "The VNODE token does not specify a regular file"),  // NOSONAR
    JRUpgradeSet("JRUpgradeSet", 0x05f4, "A v_open request for upgrading share options was specified with an access mode or deny mode which is less restrictive than the current setting"),  // NOSONAR
    JRDowngradeSet("JRDowngradeSet", 0x05f5, "A v_open request for downgrading share options was specified with an access mode or deny mode which is more restrictive than the current setting"),  // NOSONAR
    JRInvDeny("JRInvDeny", 0x05f6, "A share deny specified on a v_open has a value that is not supported"),  // NOSONAR
    JRNotRegFile("JRNotRegFile", 0x05f7, "The operation requested can only be performed on a regular file"),  // NOSONAR
    JRWrtOverride("JRWrtOverride", 0x05f8, "An attempt was made to write to a file with the override of share reservations requested"),  // NOSONAR
    JRBadTruncSize("JRBadTruncSize", 0x05f9, "An attempt was made to open an exising file with v_open and truncate the file to a non-zero offset"),  // NOSONAR
    JRAttrNotSettable("JRAttrNotSettable", 0x05fa, "An attempt was made to open create a file with v_open but the requested attributes for the new file could not be set"),  // NOSONAR
    JRICSFModNotFound("JRICSFModNotFound", 0x05fb, "ICSF random number generate module not found It is needed to open /dev/random and /dev/urandom"),  // NOSONAR
    JRICSFnotActive("JRICSFnotActive", 0x05fc, "ICSF must be running to open/read from /dev/random or /dev/urandom"),  // NOSONAR
    JRICSFRACFfail("JRICSFRACFfail", 0x05fd, "RACF failed your request to use the CSNBRNG service of ICSF Permission is needed to open/read from /dev/random and /dev/urandom"),  // NOSONAR
    JRICSFCardFail("JRICSFCardFail", 0x05fe, "While attempting to open/read from /dev/random or /dev/urandom the PCI Cryptographic Coprocessor or PCI X Cryptographic Coprocessor failed"),  // NOSONAR
    JRICSFCardUnavailable("JRICSFCardUnavailable", 0x05ff, "Could not open/read from /dev/random or /dev/urandom because the specific PCI Cryptographic Coprocessor or PCI X Cryptographic Coprocessor requested for service is temporarily unavailable"),  // NOSONAR
    JRICSFCardnotActive("JRICSFCardnotActive", 0x0600, "Could not open/read from /dev/random or /dev/urandom because the required PCI Cryptographic Coprocessor or PCI X Cryptographic Coprocessor was not active"),  // NOSONAR
    JRICSFUnknownErr("JRICSFUnknownErr", 0x0601, "An unknown ICSF error occurred while trying to open/read from /dev/random or /dev/urandom"),  // NOSONAR
    JRLockPurged("JRLockPurged", 0x0602, "A Byte Range Lock request has been canceled while it was waiting"),  // NOSONAR
    JRBRLMAbend("JRBRLMAbend", 0x0603, "An abend occurred in the Byte Range Lock Manager"),  // NOSONAR
    JRTokMismatch("JRTokMismatch", 0x0604, "The OpenToken passed does not belong to the VnodToken that was passed"),  // NOSONAR
    JRNoVnTok("JRNoVnTok", 0x0605, "An OpenToken was passed but not a VnodToken"),  // NOSONAR
    JRAsyncBadAioToken("JRAsyncBadAioToken", 0x0606, "The async I/O token passed on a cancel request is not valid"),  // NOSONAR
    JROwnerMoved("JROwnerMoved", 0x0607, "The filesystem is now owned by a different system"),  // NOSONAR
    JROwnerNoSup("JROwnerNoSup", 0x0608, "No support for this operation by filesys owner"),  // NOSONAR
    JRClientNoSup("JRClientNoSup", 0x0609, "The client is not at the release level that supports blocking when share reservations are enforced within the sysplex"),  // NOSONAR
    JRBrlmOutOfStorage("JRBrlmOutOfStorage", 0x060a, "The Byte Range Lock Manager component has reported an out of storage condition"),  // NOSONAR
    JRNoMatchingFStype("JRNoMatchingFStype", 0x060b, "This error condition only applies to sysplex configurations This system could not mount a file system that was mounted by another system in the sysplex because there is no active Physical File System that matches the Physical File System TYPE that was specified on the original MOUNT request There are inconsistent FILESYSTYPE statements in the BPXPRMxx parmlib members All systems in the sysplex must specify the same FILESYSTYPE statements"),  // NOSONAR
    JRShutdownFileOwner("JRShutdownFileOwner", 0x060c, "Automounted filesystems are not permitted after shutdown=fileowner has been issued on a system"),  // NOSONAR
    JRShrsInFileSys("JRShrsInFileSys", 0x060d, "Remount is not allowed when there are NFS V4 share reservations on any file in the File System. Move is not allowed when a client system has NFS V4 share reservations on any file"),  // NOSONAR
    JRUnSupportedKey("JRUnSupportedKey", 0x060e, "The caller of the service is running with a PSW Key that is not supported by the service"),  // NOSONAR
    JRKeyMismatch("JRKeyMismatch", 0x060f, "The caller of the service is running with a PSW Key that does not match the key of the TCB that it is running on OR the PSW Key of the caller does not match the storage key of a storage area that it is trying to operate against"),  // NOSONAR
    JRProcessIsReSpawn("JRProcessIsReSpawn", 0x0610, "The request to register as a permanent or blocking process cannot be performed because the current process is respawnable"),  // NOSONAR
    JRShrsNotSupported("JRShrsNotSupported", 0x0612, "The physical file system does not support share reservations"),  // NOSONAR
    JRRecallFailure("JRRecallFailure", 0x0613, "An attempt to retrieve an archived filesystem has failed"),  // NOSONAR
    JRAutomountTerm("JRAutomountTerm", 0x0614, "The automount facility is terminating"),  // NOSONAR
    JRNoOperlogActive("JRNoOperlogActive", 0x0615, "The write to /dev/operlog cannot complete because Operlog is inactive"),  // NOSONAR
    JROperlogRtFailed("JROperlogRtFailed", 0x0616, "The write to /dev/operlog cannot complete because Operlog Routine has failed to queue messages to operlog"),  // NOSONAR
    JRNoDaemon("JRNoDaemon", 0x0617, "The function could not complete. BPX.DAEMON facility class profile not defined"),  // NOSONAR
    JRIpcRemovedAsy("JRIpcRemovedAsy", 0x0618, "Between the start of an asynchronous I/O and the I/O completion the IPC message queue was removed and recreated"),  // NOSONAR
    JRSecEnvrNotSet("JRSecEnvrNotSet", 0x0619, "The SIOCSECENVR IOCTL with the SET argument was not issued by this server"),  // NOSONAR
    JRDuplicateGet("JRDuplicateGet", 0x061a, "The SIOCSECENVR IOCTL with the GET argument was already issued by this server"),  // NOSONAR
    JRSECEnvrDeletedByRd("JRSECEnvrDeletedByRd", 0x061b, "The security object associated with this client has been deleted by a read syscall because the SIOCSECENVR IOCTL with the GET argument was not issued in a timely fashion"),  // NOSONAR
    JRSECENVRerror("JRSECENVRerror", 0x061c, "An error occurred while processing the security environment for this client so the security environment was deleted"),  // NOSONAR
    JRNoSECENVRbuffer("JRNoSECENVRbuffer", 0x061d, "An error occured while trying to obtain a buffer to hold the security environment"),  // NOSONAR
    JRFormatValueInvalid("JRFormatValueInvalid", 0x061e, "First Character of the input message must contain 0x80 for local messages or 0x00 for remote messages"),  // NOSONAR
    JRAutomountLkUp("JRAutomountLkUp", 0x061f, "An error was encountered during pathname lookup because an automount managed file system could not be mounted"),  // NOSONAR
    JRMountPtInProg("JRMountPtInProg", 0x0620, "Another file system is in the process of being mounted on the mount point specified"),  // NOSONAR
    JROptionFlagsErr("JROptionFlagsErr", 0x0621, "Caller specified a value in the option flags parameter of extended loadhfs that is not a supported value"),  // NOSONAR
    JRLodDirectedSubpoolError("JRLodDirectedSubpoolError", 0x0622, "Caller specified a subpool that is not supported on extended loadhfs call with Lod_Directed option flag"),  // NOSONAR
    JRLodDirectedAuthErr("JRLodDirectedAuthErr", 0x0623, "Caller specified the Lod_Directed flag but is not authorized to do so"),  // NOSONAR
    JRLodDirectedNoStorage("JRLodDirectedNoStorage", 0x0624, "Storage obtain request for the directed load failed"),  // NOSONAR
    JRPFSIsRecycling("JRPFSIsRecycling", 0x0625, "The Physical File System is recycling either on this system, or on the system that is the file system owner"),  // NOSONAR
    JRActivityFound("JRActivityFound", 0x0626, "Activity found on the sysplex root file system"),  // NOSONAR
    JRSecLabelMismatch("JRSecLabelMismatch", 0x0627, "During F OMVS,NEWROOT processing, the Seclabel of the current and old roots do not match"),  // NOSONAR
    JRNewRoot("JRNewRoot", 0x0628, "The sysplex root file system has been changed"),  // NOSONAR
    JRAlreadyDubbed("JRAlreadyDubbed", 0x0629, "The task is already dubbed"),  // NOSONAR
    JRAlreadyUnDubbed("JRAlreadyUnDubbed", 0x062a, "The task is already undubbed"),  // NOSONAR
    JRRemntMode("JRRemntMode", 0x062b, "The file system is already in the mode specified by remount"),  // NOSONAR
    JRBadSameMode("JRBadSameMode", 0x062c, "The file system is not in the mode specified by remount samemode"),  // NOSONAR
    JRAsyncANR("JRAsyncANR", 0x062d, "The socket state is not valid to process an accept_and_recv operation"),  // NOSONAR
    JRMountedUnowned("JRMountedUnowned", 0x062e, "The file system is mounted but unowned"),  // NOSONAR
    JRNoRemote("JRNoRemote", 0x062f, "The path specified requires crossing into a remote file system and that is not allowed"),  // NOSONAR
    JRRemoteRFI("JRRemoteRFI", 0x0631, "The file specified with Iocc#RegFileInt is in a file system that can be changed without notice"),  // NOSONAR
    JRFSTypeChanged("JRFSTypeChanged", 0x0632, "The File System Type was changed due to generic type support for HFS and ZFS"),  // NOSONAR
    JRPOEActionErr("JRPOEActionErr", 0x0633, "The __poe() action options were specified incorrectly. POE#ReadPOE, POE#WritePOE and POE#SetGetPOE are mutually exclusive, more than one was specified"),  // NOSONAR
    JRPOESocketScopeErr("JRPOESocketScopeErr", 0x0634, "The __poe() options were specified incorrectly. When POE#ScopeSocket is specifed only POE#ReadPOE is allowed, POE#WritePOE, POE#SetGetPOE or no action option was specified"),  // NOSONAR
    JRPOENotAvailable("JRPOENotAvailable", 0x0635, "The __poe() POE#ReadPOE action option was specfied with scope options POE#ScopeProcess or POE#ScopeThread but the specified POE data was not initialized"),  // NOSONAR
    JRCannotDecrease("JRCannotDecrease", 0x0636, "The value specified is less than the current value"),  // NOSONAR
    JRNoINETNwk("JRNoINETNwk", 0x0637, "A NETWORK statement for AF_INET6 cannot be processed unless there is also an AF_INET NETWORK statement with the same TYPE specified or already active"),  // NOSONAR
    JRInAddrAnyNotAllowed("JRInAddrAnyNotAllowed", 0x0638, "It is not permitted to pass an IP address of all zeros, InAddr_Any or In6Addr_Any, to this service"),  // NOSONAR
    JRAmtNoFsName("JRAmtNoFsName", 0x0639, "The automount file system name was blank because the directory did not match an existing specific entry and the generic entry did not specify a file system name"),  // NOSONAR
    JRDubDuringExec("JRDubDuringExec", 0x063a, "An attempt was made to dub a subtask while an exec or spawn for the new job step was in progress"),  // NOSONAR
    JRNoSetUID("JRNoSetUID", 0x063b, "NOSETUID was not specified on the nonprivileged user mount interface"),  // NOSONAR
    JRNonEmptyMntPtDir("JRNonEmptyMntPtDir", 0x063c, "The mount point directory is not empty"),  // NOSONAR
    JRExceedMaxUsrMntSys("JRExceedMaxUsrMntSys", 0x063d, "The maximum number of nonprivileged user mounts for the system or sysplex has been exceeded"),  // NOSONAR
    JRExceedMaxUsrMntUsr("JRExceedMaxUsrMntUsr", 0x063e, "The maximum number of nonprivileged user mounts for the user has been exceeded"),  // NOSONAR
    JRNotMntPtOwner("JRNotMntPtOwner", 0x063f, "The user does not own the mount point directory when the sticky bit is set"),  // NOSONAR
    JRNotRootOwner("JRNotRootOwner", 0x0640, "The user does not own the file system root when the sticky bit is set"),  // NOSONAR
    JRFileSystemMigrated("JRFileSystemMigrated", 0x0641, "The file system name specified on the nonprivileged user mount is HSM-migrated and automount facility is not running"),  // NOSONAR
    JRSysNameNotAllowed("JRSysNameNotAllowed", 0x0642, "The system name specification is not supported on the nonprivileged user mount operation"),  // NOSONAR
    JRRemountNotAllowed("JRRemountNotAllowed", 0x0643, "Remount operation is not allowed for nonprivileged users"),  // NOSONAR
    JRUserUnMountNotAllowed("JRUserUnMountNotAllowed", 0x0644, "The Nonprivileged user is not allowed to unmount a file system that was mounted by another user"),  // NOSONAR
    JRPFSNotSupported("JRPFSNotSupported", 0x0645, "The specified mount point is not in a supported file system for this mount request"),  // NOSONAR
    JRNoRootAccess("JRNoRootAccess", 0x0646, "The nonprivileged user does not have read, write, and search permission to the specified file system root directory"),  // NOSONAR
    JRNoMntPtAccess("JRNoMntPtAccess", 0x0647, "The nonprivileged user does not have read, write and search permission to the specified mount point directory"),  // NOSONAR
    JRTooManyInProgress("JRTooManyInProgress", 0x0648, "Too many nonprivileged user mounts are in progress at a given time"),  // NOSONAR
    JRMaxPipesUser("JRMaxPipesUser", 0x0649, "The maximum number of pipes has been exceeded for this user"),  // NOSONAR
    JRMaxPipes("JRMaxPipes", 0x064a, "The system limit of 15,360 pipes and FIFOs has been reached"),  // NOSONAR
    JRBadPET("JRBadPET", 0x064b, "PET specified in ThliPET was invalid at the time BPX1STE/BPX4STE was invoked. Value must either be zero or a valid unautherized PET"),  // NOSONAR
    JRReleasedPET("JRReleasedPET", 0x064c, "PET specified in ThliPET has already been released. No timer interrupt has been set for the invoking thread"),  // NOSONAR
    JRUniLseek("JRUniLseek", 0x064d, "This read/write operations fails because an lseek was issued on this file with a prior read or write that caused conversion via Unicode Services with a CCSID that is not a single byte character set"),  // NOSONAR
    JRUniPartialWrt("JRUniPartialWrt", 0x064e, "A file being converted using Unicode Services on a write request, caused the file system to become full or reached the file size limit without writing all converted characters to the file. Writes of partial characters is not supported"),  // NOSONAR
    JRUniOutOfStorage("JRUniOutOfStorage", 0x064f, "Unable to obtain storage for internal buffers used for converting files via Unicode Services"),  // NOSONAR
    JRUniPartialChars("JRUniPartialChars", 0x0650, "A prior write operation on a file being converted using Unicode Services resulted in a partial character being saved but the following operation was a read"),  // NOSONAR
    JRUniCharSpec("JRUniCharSpec", 0x0651, "Character special files are not supported when files are being converted with Unicode Services"),  // NOSONAR
    JRUniOpTooBig("JRUniOpTooBig", 0x0652, "The read or write request trying to convert using Unicode Services specified a length that is too large"),  // NOSONAR
    JRUniPartialCharsShr("JRUniPartialCharsShr", 0x0653, "The read or write request that was converting using Unicode Services, produced a partial character on a shared open at the same time that another thread was doing a read or write"),  // NOSONAR
    JRUniMaxLenTooLong("JRUniMaxLenTooLong", 0x0654, "The maximum character length for one of the CCSIDs being used for conversion is too long"),  // NOSONAR
    JRUniMaxIoBufUser("JRUniMaxIoBufUser", 0x0655, "The Unicode I/O buffer storage allowed for this UID has exceeded the system specified amount"),  // NOSONAR
    JRCanceled("JRCanceled", 0x0656, "The operation has been canceled"),  // NOSONAR
    JRUniCCSIDNotSupported("JRUniCCSIDNotSupported", 0x0657, "The read or write request trying to convert using Unicode Services failed due to an unsupported CCSID"),  // NOSONAR
    JRCPLFcnReqThe("JRCPLFcnReqThe", 0x0658, "The __cpl function code is inconsistent with the request type"),  // NOSONAR
    JRCPLParmVerThe("JRCPLParmVerThe", 0x0659, "The input or output parameter version in the structure buffer passed to __cpl is inconsistent with the request type"),  // NOSONAR
    JRMaxThreadsThe("JRMaxThreadsThe", 0x065f, "The system has reached the maximum number of threads allowed"),  // NOSONAR
    JRSMFTypeSubtypeMismatchThe("JRSMFTypeSubtypeMismatchThe", 0x0660, "The SMF type or subtype parameters do not match the type or subtype specified in the SMF record to be written"),  // NOSONAR
    JRSMFRecordLenMismatchThe("JRSMFRecordLenMismatchThe", 0x0661, "The SMF record length parameters is smaller than the record length specified in the SMF record to be written"),  // NOSONAR
    JROWTTerminatedThe("JROWTTerminatedThe", 0x0662, "The service requested was offloaded to an Offload Worker Task that was already terminated"),  // NOSONAR
    JRFailedBySyscallExit("JRFailedBySyscallExit", 0x0663, "The callable service was disallowed by a pre-syscall exit routine. See the THLI control block for information regarding which exit routine rejected the syscall and the return and reason codes returned by the exit routine"),  // NOSONAR
    JRSyscallExit("JRSyscallExit", 0x0664, "The callable service attempted is not allowed to be called from a dynamic syscall exit routine"),  // NOSONAR
    JJRMigNFSFileThe("JJRMigNFSFileThe", 0x0665, "The file system specified for the migration contains open NFS files and that is not allowed"),  // NOSONAR
    JRMigTargetMountedThe("JRMigTargetMountedThe", 0x0666, "The target file system specified on the migration command is already mounted"),  // NOSONAR
    JRMigTgtNotEmptyThe("JRMigTgtNotEmptyThe", 0x0667, "The target file system is not empty and this is not allowed"),  // NOSONAR
    JRMigNotLocalThe("JRMigNotLocalThe", 0x0668, "The source file system is not mounted locally"),  // NOSONAR
    JRMigNotStartedThe("JRMigNotStartedThe", 0x0669, "The specified source file system has not been designated for migration"),  // NOSONAR
    JRMigIsSwappingThe("JRMigIsSwappingThe", 0x0670, "The specified source file system is in the process of being swapped"),  // NOSONAR
    JRMigAlreadyInProgressMigration("JRMigAlreadyInProgressMigration", 0x0671, "Migration is being canceled for the specified source file system"),  // NOSONAR
    JRMigIsCanceledOnly("JRMigIsCanceledOnly", 0x0672, "Only one migration is allowed to be in progress"),  // NOSONAR
    JRMigSrcNotHFSThe("JRMigSrcNotHFSThe", 0x0673, "The specified source file system must be an HFS or zFS"),  // NOSONAR
    JRInvMigCmdThe("JRInvMigCmdThe", 0x0674, "The specified migration subcommand is not valid"),  // NOSONAR
    JRSwapMissingTgtVnodPtrOne("JRSwapMissingTgtVnodPtrOne", 0x0675, "One or more source vodes do not have pointers to corresponding target vnodes during a migration swap attempt"),  // NOSONAR
    JRSwapDirInUseOne("JRSwapDirInUseOne", 0x0676, "One or more directories are actively being read during a migration swap attempt"),  // NOSONAR
    JRMigFsOptionsLenThe("JRMigFsOptionsLenThe", 0x0677, "The FsOptions length specified for the modify migration is too long"),  // NOSONAR
    JRMigBadRenameThe("JRMigBadRenameThe", 0x0678, "The modify migrate request would cause the source file system name and the target file system name to be the same"),  // NOSONAR
    JRMigRenameTgtErrIntAn("JRMigRenameTgtErrIntAn", 0x0679, "An internal error occurred. File system migration failed to rename the target file system"),  // NOSONAR
    JRMigCancelNotAllowedThe("JRMigCancelNotAllowedThe", 0x067a, "The specified file system migration is not allowed to be canceled at this time"),  // NOSONAR
    JRMigWriteAmt("JRMigWriteAmt", 0x067b, "During a migration, a mirroring write failed to write enough bytes. The migration has been canceled"),  // NOSONAR
    JRMigNotActive("JRMigNotActive", 0x067c, "The specified file system migration is not active"),  // NOSONAR
    JRMigSwapInProgress("JRMigSwapInProgress", 0x067d, "A migration cancel is not allowed when the swap is in progress"),  // NOSONAR
    JRMigNoStorage("JRMigNoStorage", 0x067e, "There is not enough storage in the kernel address space"),  // NOSONAR
    JRMigNotMirrored("JRMigNotMirrored", 0x067f, "The source file system for migration must be mirrored before you can compare the contents"),  // NOSONAR
    JRMigIsExported("JRMigIsExported", 0x0680, "The file system specified for the migration is exported and that is not allowed"),  // NOSONAR
    JRMigInProgressFs("JRMigInProgressFs", 0x0681, "The specified file system has already been designated for migration"),  // NOSONAR
    JRMigDownLevel("JRMigDownLevel", 0x0682, "A system in the sysplex is at a lower release level that does not support file system migration. For HFS file system migration, z/OS release V2R3 and above is supported. For zFS file system migration, z/OS release V2R4 and above is supported"),  // NOSONAR
    JRSwapOWInProg("JRSwapOWInProg", 0x0683, "One or more operations are actively in an osi wait during a migration swap attempt"),  // NOSONAR
    JRRM64AM31("JRRM64AM31", 0x0684, "The invoking task attempted to load an rmode64 program while running amode31"),  // NOSONAR
    JRRMODE64NOTSUPPORTED("JRRMODE64NOTSUPPORTED", 0x0685, "Service does not support rmode 64"),  // NOSONAR
    JRMigRenameTgtErr("JRMigRenameTgtErr", 0x0686, "File system migration failed to rename the target file system"),  // NOSONAR
    JRMigRenameTgtErr2("JRMigRenameTgtErr", 0x0687, "File system migration failed to rename the target file system"),  // NOSONAR
    JRMigNotAllowed("JRMigNotAllowed", 0x0688, "Migration file system is not allowed to start"),  // NOSONAR
    JRNoUserCat("JRNoUserCat", 0x0695, "zFS file system data set is not allowed to be cataloged in the Master Catalog"),  // NOSONAR
    JRIOCTLBackupClient("JRIOCTLBackupClient", 0x0696, "The backup command for the ioctl cannot be issued from a file system client"),  // NOSONAR
    JRMigMoveCancel("JRMigMoveCancel", 0x0697, "The migration was canceled because you are trying to move or remount a file system that is in the progress of being migrated"),  // NOSONAR
    JRMvRemntInProgress("JRMvRemntInProgress", 0x069a, "The file system is either being moved to a different server or is being remounted"),  // NOSONAR
    JRSipcOPENSNAckets("JRSipcOPENSNAckets", 0x7101, "SNAckets IPC Open error occurred"),  // NOSONAR
    JRSipcCONNECTSNAckets("JRSipcCONNECTSNAckets", 0x7102, "SNAckets IPC Connect error occurred"),  // NOSONAR
    JRSipcCLOSESNAckets("JRSipcCLOSESNAckets", 0x7103, "SNAckets IPC Close error occurred"),  // NOSONAR
    JRAckLenFailureThe("JRAckLenFailureThe", 0x7200, "The size of the TPI data is not large enough to support the Prim_type field"),  // NOSONAR
    JRUnExpectedAckThe("JRUnExpectedAckThe", 0x7201, "The Prim_type received from the TPI request does not match the request"),  // NOSONAR
    JRAlreadyBoundThe("JRAlreadyBoundThe", 0x7202, "The Socket has already been bound by a previous request"),  // NOSONAR
    JRNotBoundRead("JRNotBoundRead", 0x7203, "Read not allowed on unbound socket unless it is RAW"),  // NOSONAR
    JRWrongBand("JRWrongBand", 0x7204, "Message received on wrong band"),  // NOSONAR
    JRTLIerror("JRTLIerror", 0x7205, "ERRNO returned by the transport layer interface"),  // NOSONAR
    JRConnFail("JRConnFail", 0x7206, "Previous connection request on this socket failed"),  // NOSONAR
    JRCFGMREQLenErr("JRCFGMREQLenErr", 0x7207, "The size of the returned buffer is not large enough to support a CFGMREQ structure"),  // NOSONAR
    JRBulkModeErr("JRBulkModeErr", 0x7208, "The type of socket request specified is not valid for a Bulkmode socket"),  // NOSONAR
    JRIOCTLAccessAuthorization("JRIOCTLAccessAuthorization", 0x7209, "The user does not have access authority for the requested ioctl"),  // NOSONAR
    JRIOCTLSizeIncorrectThe("JRIOCTLSizeIncorrectThe", 0x720a, "The size of the structure used in the ioctl is not correct"),  // NOSONAR
    JRIOCTLAFNotSupportedThe("JRIOCTLAFNotSupportedThe", 0x720b, "The address family is not supported for the specified ioctl"),  // NOSONAR
    JRIOCTLRTTableSizeThe("JRIOCTLRTTableSizeThe", 0x720c, "The TCP/IP Route table does not fit in the buffer pro vided"),  // NOSONAR
    JRIOCTLTPrimNotSetNo("JRIOCTLTPrimNotSetNo", 0x720d, "No primary interface is defined to TCP/IP"),  // NOSONAR
    JRIOCTLLinkName("JRIOCTLLinkName", 0x720e, "The ifr_name specified was not found to be a valid, defined link name"),  // NOSONAR
    JRDevNumInvalid("JRDevNumInvalid", 0x720f, "The device number specified is not valid for the operation"),  // NOSONAR
    JRIPAddrInvalid("JRIPAddrInvalid", 0x7210, "The IP address specified is not valid for the operation"),  // NOSONAR
    JRLinkNameInvalid("JRLinkNameInvalid", 0x7211, "The link name specified is not valid for the operation"),  // NOSONAR
    JRInterfaceInvalid("JRInterfaceInvalid", 0x7212, "The interface specified is not valid for the operation"),  // NOSONAR
    JRDeviceCTCInvalid("JRDeviceCTCInvalid", 0x7213, "The device conflicts with a CTC device"),  // NOSONAR
    JRDeviceCLAWInvalid("JRDeviceCLAWInvalid", 0x7214, "The device conflicts with a CLAW device"),  // NOSONAR
    JRDeviceLCSInvalid("JRDeviceLCSInvalid", 0x7215, "The device conflicts with a LCS device"),  // NOSONAR
    JRLinksMaxExceeded("JRLinksMaxExceeded", 0x7216, "The number of link names has been exceeded"),  // NOSONAR
    JRInterfaceAlreadyStopped("JRInterfaceAlreadyStopped", 0x7217, "The Interface is already stopped"),  // NOSONAR
    JRInterfaceAlreadyStarted("JRInterfaceAlreadyStarted", 0x7218, "The interface is already started"),  // NOSONAR
    JRDeviceNotDefined("JRDeviceNotDefined", 0x7219, "The device number specified is not defined"),  // NOSONAR
    JRDeviceAlreadyDefined("JRDeviceAlreadyDefined", 0x721a, "The device is already defined"),  // NOSONAR
    JRDeviceUnsupported("JRDeviceUnsupported", 0x721b, "The device type is unsupported"),  // NOSONAR
    JRDeviceActive("JRDeviceActive", 0x721c, "The device type is active"),  // NOSONAR
    JRConfigErr("JRConfigErr", 0x721d, "Config statement is in error"),  // NOSONAR
    JRAFNotSupported("JRAFNotSupported", 0x721e, "Address Family is not supported"),  // NOSONAR
    JRTCPNotUp("JRTCPNotUp", 0x721f, "TCP/IP is not initialized"),  // NOSONAR
    JRUDPNotUp("JRUDPNotUp", 0x7220, "TCP/IP is not initialized"),  // NOSONAR
    JRGetConnErr("JRGetConnErr", 0x7221, "The connection was not in the proper state for retrieving"),  // NOSONAR
    JRPATFNDErr("JRPATFNDErr", 0x7222, "Search for a restricted port failed"),  // NOSONAR
    JRPATFNXErr("JRPATFNXErr", 0x7223, "Search for restricted ports failed"),  // NOSONAR
    JRPATADDErr("JRPATADDErr", 0x7224, "Add new restricted port failed"),  // NOSONAR
    JRPATDELErr("JRPATDELErr", 0x7225, "Delete a restricted port failed"),  // NOSONAR
    JRPATExistErr("JRPATExistErr", 0x7226, "Restricted port entry already existed"),  // NOSONAR
    JRPATNotFound("JRPATNotFound", 0x7227, "Restricted port entry is not found"),  // NOSONAR
    JRInvaliddAddr("JRInvaliddAddr", 0x7228, "Specified address is not valid"),  // NOSONAR
    JRPortRErr("JRPortRErr", 0x7229, "The input specified in the port range statement is not valid"),  // NOSONAR
    JRInterfaceDefinedByHome("JRInterfaceDefinedByHome", 0x722a, "The interface has been used within a Home statement"),  // NOSONAR
    JRDeviceDefinedByLink("JRDeviceDefinedByLink", 0x722b, "The device is defined by a Link statement"),  // NOSONAR
    JRDeviceTypeInvalid("JRDeviceTypeInvalid", 0x722c, "The device type is not valid for the operation requested"),  // NOSONAR
    JRBSDRoutingParmsNeverUsed("JRBSDRoutingParmsNeverUsed", 0x722d, "BSDRoutingParms were never configured for TCP/IP"),  // NOSONAR
    JRBINDOutState("JRBINDOutState", 0x722e, "The current TPL state is not unbound when doing a bind request"),  // NOSONAR
    JRBINDBadSockAddr("JRBINDBadSockAddr", 0x722f, "Bad Socket Address data present in the source IP address"),  // NOSONAR
    JRBINDAddrNActive("JRBINDAddrNActive", 0x7230, "Local address was not active when processing the bind request"),  // NOSONAR
    JRBINDNoPort("JRBINDNoPort", 0x7231, "No port is available. Port entries have reached the maximum number"),  // NOSONAR
    JRBINDAddrInUsed("JRBINDAddrInUsed", 0x7232, "The INADDR_ANY is being used by other users. The bind request fails"),  // NOSONAR
    JRUNBINDNotIDLE("JRUNBINDNotIDLE", 0x7233, "The requested connection was not in the proper state for the request"),  // NOSONAR
    JROPTLERR("JROPTLERR", 0x7234, "The value specified for option_length is not valid"),  // NOSONAR
    JRSendLimit("JRSendLimit", 0x7235, "The send buffer size is not valid. It is too big"),  // NOSONAR
    JRRcvdLimit("JRRcvdLimit", 0x7236, "The received buffer size is not valid. It is too big"),  // NOSONAR
    JRBSTOpt("JRBSTOpt", 0x7237, "The value specified for option_value is not valid"),  // NOSONAR
    JRBRTOpt("JRBRTOpt", 0x7238, "The value specified for option_value is not valid"),  // NOSONAR
    JRIPOpt("JRIPOpt", 0x7239, "The value specified for option_value is not valid"),  // NOSONAR
    JRTOSOpt("JRTOSOpt", 0x723a, "The value specified for type of service is not valid"),  // NOSONAR
    JRTTLOpt("JRTTLOpt", 0x723b, "The value specifed for time to live is not valid"),  // NOSONAR
    JRIPOPTLERR("JRIPOPTLERR", 0x723c, "The value specified for option_length is not valid"),  // NOSONAR
    JRTOSOPTLERR("JRTOSOPTLERR", 0x723d, "The value specified for option_length is not valid"),  // NOSONAR
    JRTTLOPTLERR("JRTTLOPTLERR", 0x723e, "The value specified for option_length is not valid"),  // NOSONAR
    JRConnNotIdle("JRConnNotIdle", 0x723f, "Connect request not in idle state"),  // NOSONAR
    JRBroadcastDest("JRBroadcastDest", 0x7240, "Cannot connect to a broadcast address"),  // NOSONAR
    JRConnectToSelf("JRConnectToSelf", 0x7241, "Connect address is the same as the source address"),  // NOSONAR
    JRConnTCBNotFound("JRConnTCBNotFound", 0x7242, "A TCB was not found for the specified socket"),  // NOSONAR
    JRBadTCBEye("JRBadTCBEye", 0x7243, "The TCB has been freed or overlaid"),  // NOSONAR
    JRNoAcceptTCB("JRNoAcceptTCB", 0x7244, "There is no valid TCB on the accept queue"),  // NOSONAR
    JRDisconTCBNotFound("JRDisconTCBNotFound", 0x7245, "The TCB could not be found for disconnect"),  // NOSONAR
    JRPortAccessAuth("JRPortAccessAuth", 0x7246, "User does not have authority to access this port"),  // NOSONAR
    JRPortBusy("JRPortBusy", 0x7247, "Specified port is in use"),  // NOSONAR
    JRSENDOPTLERR("JRSENDOPTLERR", 0x7248, "The value specified for option_length is not valid"),  // NOSONAR
    JRRECVOPTLERR("JRRECVOPTLERR", 0x7249, "The value specified for option_length is not valid"),  // NOSONAR
    JRBSTOPTLERR("JRBSTOPTLERR", 0x724a, "The value specified for option_length is not valid"),  // NOSONAR
    JRBRTOPTLERR("JRBRTOPTLERR", 0x724b, "The value specified for option_length is not valid"),  // NOSONAR
    JRRtInvalidTOS("JRRtInvalidTOS", 0x724c, "Specified route type of service is not valid"),  // NOSONAR
    JRRtInvalidGateWayAddr("JRRtInvalidGateWayAddr", 0x724d, "Specified route gateway address is not valid"),  // NOSONAR
    JRRtInvalidProtocol("JRRtInvalidProtocol", 0x724e, "Specified route protocol is not valid"),  // NOSONAR
    JRRtInvalidType("JRRtInvalidType", 0x724f, "Specified route type is not valid"),  // NOSONAR
    JRRtInvalidDest("JRRtInvalidDest", 0x7250, "Specified route destination is not valid"),  // NOSONAR
    JRRtInvalidMask("JRRtInvalidMask", 0x7251, "Specified route subnet mask is not valid"),  // NOSONAR
    JRRtInvalidHostAddr("JRRtInvalidHostAddr", 0x7252, "Specified route host ip address is not valid"),  // NOSONAR
    JRHardwareTypeNotSupported("JRHardwareTypeNotSupported", 0x7253, "Specified hardware type is not supported"),  // NOSONAR
    JRLinkTypeInvalid("JRLinkTypeInvalid", 0x7254, "The link type is not valid for the operation requested"),  // NOSONAR
    JRNoHomeStatement("JRNoHomeStatement", 0x7255, "The link name does not have an associated home statement"),  // NOSONAR
    JRIOCTLNotSupported("JRIOCTLNotSupported", 0x7256, "The specified ioctl is not supported in this version of TCP/IP"),  // NOSONAR
    JRLVLSOCNOSUP("JRLVLSOCNOSUP", 0x7257, "The option_name that was specified is not supported"),  // NOSONAR
    JRLVLTCPNOSUP("JRLVLTCPNOSUP", 0x7258, "The level that was specified is not supported"),  // NOSONAR
    JRLVLIPNOSUP("JRLVLIPNOSUP", 0x7259, "The option_name that was specified is not supported"),  // NOSONAR
    JRInvOptLen("JRInvOptLen", 0x725a, "The option length is not valid"),  // NOSONAR
    JRInvOptVal("JRInvOptVal", 0x725b, "The option value is not valid"),  // NOSONAR
    JRConnAlreadyExists("JRConnAlreadyExists", 0x725c, "The address is already in use"),  // NOSONAR
    JRStartNonDeviceInvalid("JRStartNonDeviceInvalid", 0x725d, "An attempt was made to start an interface which is not a device"),  // NOSONAR
    JRBadIOCTLToIF("JRBadIOCTLToIF", 0x725e, "An internal error occurred between the ioctl and Interface layer"),  // NOSONAR
    JRLinkNotDefined("JRLinkNotDefined", 0x725f, "The link name specified is not defined"),  // NOSONAR
    JRMaxStartsExceeded("JRMaxStartsExceeded", 0x7260, "An internal limit of START DEVICEs has been exceeded"),  // NOSONAR
    JRDeviceHasLinks("JRDeviceHasLinks", 0x7261, "An attempt was made to DELETE a device, but the device has at least one LINK defined to it"),  // NOSONAR
    JRDeviceHasNOLinks("JRDeviceHasNOLinks", 0x7262, "An attempt was made to START a device, but the device has no LINKs defined to it"),  // NOSONAR
    JRInterfaceNotDefined("JRInterfaceNotDefined", 0x7263, "The interface specified is not defined"),  // NOSONAR
    JRRtAlreadyExists("JRRtAlreadyExists", 0x7264, "The route already exists"),  // NOSONAR
    JRRtNotDefined("JRRtNotDefined", 0x7265, "The route is not defined"),  // NOSONAR
    JRRtRemoveDirectError("JRRtRemoveDirectError", 0x7266, "The route to be removed is a direct route"),  // NOSONAR
    JRGateWayUnreachable("JRGateWayUnreachable", 0x7267, "The gateway is unreachable by any routes"),  // NOSONAR
    JRRtRemoveIndirectError("JRRtRemoveIndirectError", 0x7268, "The route to be removed is an indirect route"),  // NOSONAR
    JRArpsvMult("JRArpsvMult", 0x7269, "Multiple ATMARP servers are defined for the same link"),  // NOSONAR
    JRDeviceMPCPTPInvalid("JRDeviceMPCPTPInvalid", 0x726a, "A MPCPTP link was defined for an incorrect device type"),  // NOSONAR
    JRDeviceHCHInvalid("JRDeviceHCHInvalid", 0x726b, "An HCH link was defined for an incorrect device type"),  // NOSONAR
    JRDeviceCDLCInvalid("JRDeviceCDLCInvalid", 0x726c, "A CDLC link was defined for an incorrect device type"),  // NOSONAR
    JRDeviceX25Invalid("JRDeviceX25Invalid", 0x726d, "An X.25 link was defined for an incorrect device type"),  // NOSONAR
    JRDeviceATMInvalid("JRDeviceATMInvalid", 0x726e, "An ATM link was defined for an incorrect device type"),  // NOSONAR
    JRLinkAlreadyDefined("JRLinkAlreadyDefined", 0x726f, "The link is already defined"),  // NOSONAR
    JRPvcAlreadyDefined("JRPvcAlreadyDefined", 0x7270, "The PVC is already defined"),  // NOSONAR
    JRPvcNotDefined("JRPvcNotDefined", 0x7271, "The PVC name specified is not defined"),  // NOSONAR
    JRLisNotDefined("JRLisNotDefined", 0x7272, "The LIS name specified is not defined"),  // NOSONAR
    JRArpsvNotDefined("JRArpsvNotDefined", 0x7273, "The ATMARPSV name specified is not defined"),  // NOSONAR
    JRLisInUseForDevice("JRLisInUseForDevice", 0x7274, "Another LINK for this device already specifies this LIS"),  // NOSONAR
    JRPvcLinkNotATM("JRPvcLinkNotATM", 0x7275, "An ATMPVC was defined for an incorrect link type"),  // NOSONAR
    JRLisInconsistent("JRLisInconsistent", 0x7276, "The LIS name is already defined with a different subnet value/mask"),  // NOSONAR
    JRLisOptionsUpdated("JRLisOptionsUpdated", 0x7277, "An ATMLIS redefiniton caused the LIS options to be updated"),  // NOSONAR
    JRPvcInUseAsArpsv("JRPvcInUseAsArpsv", 0x7278, "An attempt was made to delete an ATMPVC which is in use as an ATMARP server"),  // NOSONAR
    JRLinkHasPvcs("JRLinkHasPvcs", 0x7279, "An attempt was made to DELETE a LINK, but the LINK has at least one ATMPVC defined to it"),  // NOSONAR
    JRLisHasLinks("JRLisHasLinks", 0x727a, "An attempt was made to DELETE an ATMLIS, but the ATMLIS has at least one LINK defined to it"),  // NOSONAR
    JRLisHasArpsvs("JRLisHasArpsvs", 0x727b, "An attempt was made to DELETE an ATMLIS, but the ATMLIS has at least one ATMARPSV defined to it"),  // NOSONAR
    JRArpsvInUse("JRArpsvInUse", 0x727c, "An attempt was made to DELETE an ATMARPSV, but at at least one ATM device is using the ATMARPSV"),  // NOSONAR
    JRNotInLis("JRNotInLis", 0x727d, "The IP address of a defined ATMARPSV is not in the specified LIS"),  // NOSONAR
    JRTELSTATERR("JRTELSTATERR", 0x727e, "Attempt to update the telnet attributes when the connection is not yet established"),  // NOSONAR
    JRTELNFNDERR("JRTELNFNDERR", 0x727f, "Telnet session is not found"),  // NOSONAR
    JRArpsvAlreadyDefined("JRArpsvAlreadyDefined", 0x7280, "The ATMARPSV is already defined"),  // NOSONAR
    JRLisMismatch("JRLisMismatch", 0x7281, "The LIS name on the ATMARPSV PVC statement does not match the LIS defined on the link to which the specified PVC is defined"),  // NOSONAR
    JRDeviceSNAInvalid("JRDeviceSNAInvalid", 0x7282, "A SNA link was defined for an incorrect device type"),  // NOSONAR
    JRLinkHasNoPvcOrLis("JRLinkHasNoPvcOrLis", 0x7283, "An ATM link was defined but did not specify a LIS and has no PVCs defined to it"),  // NOSONAR
    JRRtInvalidMaskHost("JRRtInvalidMaskHost", 0x7284, "A host route with a non-zero mask was received"),  // NOSONAR
    JRRtInvalidMaskDefault("JRRtInvalidMaskDefault", 0x7285, "A non-default route was received with a destination of 0"),  // NOSONAR
    JRRtNoBsdRoutingParmsSet("JRRtNoBsdRoutingParmsSet", 0x7286, "A route was added for a link without BSDRoutingParms"),  // NOSONAR
    JRRtInvalidMaskCidr("JRRtInvalidMaskCidr", 0x7287, "The mask does not conform to CIDR requirement"),  // NOSONAR
    JRMsgInvalidFlag("JRMsgInvalidFlag", 0x7288, "The socket does not support the function that was specified with the flags parameter"),  // NOSONAR
    JRVariableSubnettingNotAllowed("JRVariableSubnettingNotAllowed", 0x7289, "IPCONFIG did not specify RIPV2 or length of rtentry is wrong"),  // NOSONAR
    JRsysplexAddrNotFound("JRsysplexAddrNotFound", 0x728a, "The sysplex loopback address 127.0.0.128 could not be resolved to the sysplex domain name by the gethostbyaddr() syscall"),  // NOSONAR
    JRresNotFoundInDNS("JRresNotFoundInDNS", 0x728b, "The specified group or server+group name could not be found within the sysplex domain"),  // NOSONAR
    JRgroupNameRequired("JRgroupNameRequired", 0x728c, "A server name was specified in the sysplexFqDnData structure without a group name"),  // NOSONAR
    JRinvalidBufTokn("JRinvalidBufTokn", 0x728d, "An incorrect CSM buffer token was provided"),  // NOSONAR
    JRMCTTLOpt("JRMCTTLOpt", 0x728e, "The value specified for multicast time to live is not valid"),  // NOSONAR
    JRMCTTLOptLErr("JRMCTTLOptLErr", 0x728f, "The value specified for option length is not valid"),  // NOSONAR
    JRMCLoopOpt("JRMCLoopOpt", 0x7290, "The value specified for multicast loopback is not valid"),  // NOSONAR
    JRMCLoopOptLErr("JRMCLoopOptLErr", 0x7291, "The value specified for option length is not valid"),  // NOSONAR
    JRMCIFOpt("JRMCIFOpt", 0x7292, "The value specified for multicast interface is not valid"),  // NOSONAR
    JRMCIFOptLErr("JRMCIFOptLErr", 0x7293, "The value specified for option length is not valid"),  // NOSONAR
    JRMCAddMemOpt("JRMCAddMemOpt", 0x7294, "The value specified for multicast add membership is not valid"),  // NOSONAR
    JRMCAddMemOptLErr("JRMCAddMemOptLErr", 0x7295, "The value specified for option length is not valid"),  // NOSONAR
    JRMCDropMemOpt("JRMCDropMemOpt", 0x7296, "The value specified for multicast drop membership is not valid"),  // NOSONAR
    JRMCDropMemOptLErr("JRMCDropMemOptLErr", 0x7297, "The value specified for option length is not valid"),  // NOSONAR
    JRMCMaxMem("JRMCMaxMem", 0x7298, "The maximum number of groups per socket has been exceeded"),  // NOSONAR
    JRIESizeMismatchAdd("JRIESizeMismatchAdd", 0x7299, "Request area was not large enough on add/remove"),  // NOSONAR
    JRIESizeMismatchGet("JRIESizeMismatchGet", 0x729a, "Request area was not large enough on get"),  // NOSONAR
    JRIESizeMismatchGetTable("JRIESizeMismatchGetTable", 0x729b, "Request area was not large enough on Get Table"),  // NOSONAR
    JRIESizeMismatchSetTable("JRIESizeMismatchSetTable", 0x729c, "Request area was not large enough on Set Table"),  // NOSONAR
    JRIEOutCntInvalid("JRIEOutCntInvalid", 0x729d, "cOutCnt is zero or less than the cInCnt"),  // NOSONAR
    JRIEInCntInvalid("JRIEInCntInvalid", 0x729e, "cInCnt is zero"),  // NOSONAR
    JRIEGetTableFlags("JRIEGetTableFlags", 0x729f, "Flags set on a GetTable"),  // NOSONAR
    JRIEGetFlags("JRIEGetFlags", 0x72a0, "Flags set on a Get"),  // NOSONAR
    JRIESetTableFlags("JRIESetTableFlags", 0x72a1, "Flags set on a SetTable"),  // NOSONAR
    JRIESetFlags("JRIESetFlags", 0x72a2, "Flags set on a Set"),  // NOSONAR
    JRIEGetTableUnexpected("JRIEGetTableUnexpected", 0x72a3, "Unexpected error on GetTable"),  // NOSONAR
    JRIEGetUnexpected("JRIEGetUnexpected", 0x72a4, "Unexpected error on Get"),  // NOSONAR
    JRIESetTableUnexpected("JRIESetTableUnexpected", 0x72a5, "Unexpected error on SetTable"),  // NOSONAR
    JRIESetUnexpected("JRIESetUnexpected", 0x72a6, "Unexpected error on Set"),  // NOSONAR
    JRIERecoveryError("JRIERecoveryError", 0x72a7, "Unexpected error on Recovery"),  // NOSONAR
    JRIEProfileError("JRIEProfileError", 0x72a8, "Unexpected error during profile processing"),  // NOSONAR
    JRIEAddifcioctltype("JRIEAddifcioctltype", 0x72a9, "Attempt to add an ifcioctl with ifhtype not iflifc"),  // NOSONAR
    JRIEUnknownifcioctlType("JRIEUnknownifcioctlType", 0x72aa, "Unknown ifcioctl Type attempted"),  // NOSONAR
    JRTcpTcpipError("JRTcpTcpipError", 0x72ab, "Refer to TSRB return and reason codes"),  // NOSONAR
    JRTcpInvalidTcpipName("JRTcpInvalidTcpipName", 0x72ac, "Invalid Tsrb_Tcpip_Name"),  // NOSONAR
    JRTcpInvalidRequestCode("JRTcpInvalidRequestCode", 0x72ad, "Invalid Tsrb_Request_Code"),  // NOSONAR
    JRTcpNotInstalledOrUp("JRTcpNotInstalledOrUp", 0x72ae, "TCPIP not installed or active"),  // NOSONAR
    JRTcpInvDelete("JRTcpInvDelete", 0x72af, "The delete requestor did not create the connection"),  // NOSONAR
    JRTcpInvUserData("JRTcpInvUserData", 0x72b0, "Request contained invalid user data"),  // NOSONAR
    JRSKCIBInvalidMessage("JRSKCIBInvalidMessage", 0x72b1, "Invalid IOCTL message type"),  // NOSONAR
    JRSKCIBAlreadySleeping1("JRSKCIBAlreadySleeping1", 0x72b2, "Recursive context sleep"),  // NOSONAR
    JRSKDKINotReadQueue("JRSKDKINotReadQueue", 0x72b3, "Invalid queue for find open/close"),  // NOSONAR
    JRSKDKINotWriteQueue("JRSKDKINotWriteQueue", 0x72b4, "Invalid queue for find poll/iocport"),  // NOSONAR
    JRSKDKIBadFindType("JRSKDKIBadFindType", 0x72b5, "Invalid type for find"),  // NOSONAR
    JRSKDKINotFound("JRSKDKINotFound", 0x72b6, "No such device/module for find"),  // NOSONAR
    JRSKDKIBadQGetType("JRSKDKIBadQGetType", 0x72b7, "Invalid type for queue get info"),  // NOSONAR
    JRSKDKIBadQSetType("JRSKDKIBadQSetType", 0x72b8, "Invalid type for queue set info"),  // NOSONAR
    JRSKHEDBadOpen("JRSKHEDBadOpen", 0x72b9, "Open of stream head driver not allowed"),  // NOSONAR
    JRSKMNTBadMessage("JRSKMNTBadMessage", 0x72ba, "Invalid message type"),  // NOSONAR
    JRSKMNTSnooperAlreadyActive("JRSKMNTSnooperAlreadyActive", 0x72bb, "Snooper module previously pushed"),  // NOSONAR
    JRSKMNTSnooperNotActive("JRSKMNTSnooperNotActive", 0x72bc, "Snooper module not previously pushed"),  // NOSONAR
    JRSKSTOBadIndex("JRSKSTOBadIndex", 0x72bd, "Invalid starting index for STREAMOP"),  // NOSONAR
    JRSKSTOBadStream("JRSKSTOBadStream", 0x72be, "Invalid stream address for STREAMOP"),  // NOSONAR
    JRSKSTOBadMessageType("JRSKSTOBadMessageType", 0x72bf, "Invalid message type for read"),  // NOSONAR
    JRSKSTONoEmptyMessages("JRSKSTONoEmptyMessages", 0x72c0, "Zero message length invalid for write"),  // NOSONAR
    JRSKSTOMessageTooShort("JRSKSTOMessageTooShort", 0x72c1, "Message length too short for write"),  // NOSONAR
    JRSKSTONoControlPart1("JRSKSTONoControlPart1", 0x72c2, "No control part for putpmsg"),  // NOSONAR
    JRSKSTOBadMessageLength1("JRSKSTOBadMessageLength1", 0x72c3, "Invalid message length for putpmsg"),  // NOSONAR
    JRSKSTOBadMessageLength2("JRSKSTOBadMessageLength2", 0x72c4, "Invalid message length for lputpmsg"),  // NOSONAR
    JRSKSTOBadControlInformation("JRSKSTOBadControlInformation", 0x72c5, "Invalid control information for fdinsert"),  // NOSONAR
    JRSKSTONoMessages("JRSKSTONoMessages", 0x72c6, "No queued messages for getband"),  // NOSONAR
    JRSKSTOResourceShortage2("JRSKSTOResourceShortage2", 0x72c7, "Unable to get triple for M_READ message"),  // NOSONAR
    JRSKSTOResourceShortage3("JRSKSTOResourceShortage3", 0x72c8, "Unable to get triple for M_IOCTL message"),  // NOSONAR
    JRSKVRBBadOption1("JRSKVRBBadOption1", 0x72c9, "Invalid option for open"),  // NOSONAR
    JRSKVRBNotUp("JRSKVRBNotUp", 0x72ca, "System not initialized"),  // NOSONAR
    JRSKVRBBadName("JRSKVRBBadName", 0x72cb, "Invalid name for push"),  // NOSONAR
    JRSKVRBNoResources1("JRSKVRBNoResources1", 0x72cc, "Unable to allocate queues for push"),  // NOSONAR
    JRSKVRBNothingPushed1("JRSKVRBNothingPushed1", 0x72cd, "No pushed module exists for pop"),  // NOSONAR
    JRSKVRBNotMultiplexed("JRSKVRBNotMultiplexed", 0x72ce, "Driver not multiplexed for link"),  // NOSONAR
    JRSKVRBNotLinked("JRSKVRBNotLinked", 0x72cf, "No linked driver/module for unlink"),  // NOSONAR
    JRSKVRBBadType("JRSKVRBBadType", 0x72d0, "Invalid type for flush"),  // NOSONAR
    JRSKVRBNoResources2("JRSKVRBNoResources2", 0x72d1, "Unable to get triple for M_FLUSH message"),  // NOSONAR
    JRSKVRBBadMask("JRSKVRBBadMask", 0x72d2, "Invalid signal mask for setsig"),  // NOSONAR
    JRSKVRBBadAction("JRSKVRBBadAction", 0x72d3, "Invalid action for setsig"),  // NOSONAR
    JRSKVRBBadID("JRSKVRBBadID", 0x72d4, "Invalid identifier for spgrp"),  // NOSONAR
    JRSKVRBBadFlag("JRSKVRBBadFlag", 0x72d5, "Invalid flag for spgrp"),  // NOSONAR
    JRSKVRBNoErrno("JRSKVRBNoErrno", 0x72d6, "No error number for spgrp"),  // NOSONAR
    JRSKVRBBadOption2("JRSKVRBBadOption2", 0x72d7, "Invalid behavior option for srdopt"),  // NOSONAR
    JRSKVRBBadOption3("JRSKVRBBadOption3", 0x72d8, "Invalid message option for srdopt"),  // NOSONAR
    JRSKVRBBadOption4("JRSKVRBBadOption4", 0x72d9, "Invalid length option for swropt"),  // NOSONAR
    JRSKVRBBadClass1("JRSKVRBBadClass1", 0x72da, "Invalid IOCTL class for str"),  // NOSONAR
    JRSKVRBBadClass2("JRSKVRBBadClass2", 0x72db, "Invalid IOCTL class for transprnt"),  // NOSONAR
    JRSKVRBNoResources3("JRSKVRBNoResources3", 0x72dc, "Unable to get triple for M_IOCTL message"),  // NOSONAR
    JRSKVRBNothingPushed2("JRSKVRBNothingPushed2", 0x72dd, "No module pushed for look"),  // NOSONAR
    JRSKASMBadType("JRSKASMBadType", 0x72de, "Invalid type for ?SKASM(*RANGE)"),  // NOSONAR
    JRSKSACNoStream("JRSKSACNoStream", 0x72df, "Invalid stream @ for ?SKSAC(*ACCESS)"),  // NOSONAR
    JRSKSACLinkedStream("JRSKSACLinkedStream", 0x72e0, "Invalid stream for ?SKSAC(*ACCESS)"),  // NOSONAR
    JRMultipleRead("JRMultipleRead", 0x72e1, "A request to receive data is already outstanding.)"),  // NOSONAR
    JRDeviceMPCHLCSInvalid("JRDeviceMPCHLCSInvalid", 0x72e2, "A MPC HPDT/LCS link was defined for an incorrect device type"),  // NOSONAR
    JRCmConfigured("JRCmConfigured", 0x72e3, "Cache Manager has been configured for this server socket"),  // NOSONAR
    JRCmNoLog("JRCmNoLog", 0x72e4, "Cache Manager Configuration parameter does not include the size of the log file"),  // NOSONAR
    JRCmNoCache("JRCmNoCache", 0x72e5, "Cache Manager Configuration parameter does not include the size of the cache file"),  // NOSONAR
    JRCmServerNotFound("JRCmServerNotFound", 0x72e6, "Cache Manager has not been configured for this server socket"),  // NOSONAR
    JRCmCantLoad("JRCmCantLoad", 0x72e7, "Invalid Dynamic Exit load module name specified in the Config IOCTL parameters"),  // NOSONAR
    JRCmNoStorage("JRCmNoStorage", 0x72e8, "Cache Manager encountered storage shortage"),  // NOSONAR
    JRCmNoCSm("JRCmNoCSm", 0x72e9, "Cache Manager encountered a CSM storage shortage"),  // NOSONAR
    JRCmNotConfigured("JRCmNotConfigured", 0x72ea, "Cache Manager has not been configured for this server socket"),  // NOSONAR
    JRNotCmAuthorized("JRNotCmAuthorized", 0x72eb, "User is not allowed to use Cache Manager function"),  // NOSONAR
    JRCmParmNotValid("JRCmParmNotValid", 0x72ec, "Invalid parameters used in Cache Manager IOCTL call"),  // NOSONAR
    JRCmLoadModBad("JRCmLoadModBad", 0x72ed, "Invalid Dynamic Exit load module name specified in the Config IOCTL parameters"),  // NOSONAR
    JRUWCONotFound("JRUWCONotFound", 0x72ee, "Cache Manager has not been configured for this server socket"),  // NOSONAR
    JRUWCOStorageFailed("JRUWCOStorageFailed", 0x72ef, "Cache Manager encountered storage shortage"),  // NOSONAR
    JRUWCTStorageFailed("JRUWCTStorageFailed", 0x72f0, "Cache Manager encountered storage shortage"),  // NOSONAR
    JRUWHTStorageFailed("JRUWHTStorageFailed", 0x72f1, "Cache Manager encountered storage shortage"),  // NOSONAR
    JRUWCTFull("JRUWCTFull", 0x72f2, "Cache Manager table has been filled"),  // NOSONAR
    JRCmNoObjects("JRCmNoObjects", 0x72f3, "Maximum number of objects has been reached"),  // NOSONAR
    JRCmDisable("JRCmDisable", 0x72f4, "Cache Manager is not allowed for this TCP/IP stack"),  // NOSONAR
    JRCmLSocKBadState("JRCmLSocKBadState", 0x72f5, "Cache Manager Server is not in the LISTEN state"),  // NOSONAR
    JRCmDestroyFailure("JRCmDestroyFailure", 0x72f6, "Cache Manager Storage cannot be removed"),  // NOSONAR
    JRCmBadConfigData("JRCmBadConfigData", 0x72f7, "Cache Manager cannot call the DGW exits"),  // NOSONAR
    JRCmCacheObjLimit("JRCmCacheObjLimit", 0x72f8, "Cache Manager maximum number of objects has been reached"),  // NOSONAR
    JRCmCacheSpaceLimit("JRCmCacheSpaceLimit", 0x72f9, "Cache Manager Buffer is filled"),  // NOSONAR
    JRCmCacheObjNotFound("JRCmCacheObjNotFound", 0x72fa, "Cache Manager Object is not found"),  // NOSONAR
    JRDynAddCxFailure("JRDynAddCxFailure", 0x72fb, "Cannot add new connection to Dynnamic exits"),  // NOSONAR
    JRDynDelCxFailure("JRDynDelCxFailure", 0x72fc, "Cannot delete a connection from Dynnamic exits"),  // NOSONAR
    JRDynTimerFailure("JRDynTimerFailure", 0x72fd, "Cannot start the timer from Dynnamic exits"),  // NOSONAR
    JRDynRecoveryFailure("JRDynRecoveryFailure", 0x72fe, "Cannot start the recovery from Dynnamic exits"),  // NOSONAR
    JRCmIOVPNotValid("JRCmIOVPNotValid", 0x72ff, "Invalid IOV buffer passed in the load request"),  // NOSONAR
    JRCmLogFull("JRCmLogFull", 0x7300, "LogBuffer is Full"),  // NOSONAR
    JRCmsgDataInvalid("JRCmsgDataInvalid", 0x7301, "cmsghdr data has an incorrect value"),  // NOSONAR
    JRDVIPAAlreadyActive("JRDVIPAAlreadyActive", 0x7302, "The Dynamic VIPA activated via IOCTL was already active"),  // NOSONAR
    JRDVIPANotInVIPARange("JRDVIPANotInVIPARange", 0x7303, "The Dynamic VIPA was not in any defined VIPARange"),  // NOSONAR
    JRDVIPAConflictDVIPA("JRDVIPAConflictDVIPA", 0x7304, "The Dynamic VIPA selected is already defined via VIPADEFine or VIPABackup"),  // NOSONAR
    JRDVIPAConflictIPAddr("JRDVIPAConflictIPAddr", 0x7305, "The Dynamic VIPA selected is already active as a standard IP address"),  // NOSONAR
    JRTooManyDVIPAs("JRTooManyDVIPAs", 0x7306, "The maximum allowed number of DVIPAs is already defined"),  // NOSONAR
    JRDVIPANotDefined("JRDVIPANotDefined", 0x7307, "The Dynamic VIPA selected for deletion was not defined here"),  // NOSONAR
    JRNoDuAvailable("JRNoDuAvailable", 0x7308, "TCP/IP cannot create a dispatchable unit to process the request. Either TCP/IP is not active or there is insufficient common storage available"),  // NOSONAR
    JRPortUnavailable("JRPortUnavailable", 0x7309, "The requested port is marked reserved and is not available to any application"),  // NOSONAR
    JRAFOpNotSupported("JRAFOpNotSupported", 0x730a, "The operation is not supported for a socket in this address family"),  // NOSONAR
    JRCannotMapSockAddr("JRCannotMapSockAddr", 0x730b, "The socket address provided by the application on this call cannot be mapped to an IPv4 socket address"),  // NOSONAR
    JRNetAccessDenied("JRNetAccessDenied", 0x730c, "The user is not permitted to communicate with the specified network"),  // NOSONAR
    JRNotAuthStack("JRNotAuthStack", 0x730d, "Userid is not authorized to access the TCP/IP Stack"),  // NOSONAR
    JRNotAuthPort("JRNotAuthPort", 0x730e, "Userid is not authorized to access the reserved TCP/IP Port"),  // NOSONAR
    JRFRCAReset("JRFRCAReset", 0x730f, "FRCA connection timer expired. Connection is being dropped"),  // NOSONAR
    JRNoCsaStorage("JRNoCsaStorage", 0x7310, "TCP/IP cannot process the request because there is insufficient common storage available"),  // NOSONAR
    JRZeroPortDVIPA("JRZeroPortDVIPA", 0x7311, "A port of zero was specified on a bind when the IP address was a distributed DVIPA and the local stack is a target stack"),  // NOSONAR
    JRTCPIPAlreadyInitialized("JRTCPIPAlreadyInitialized", 0x7312, "TCPIP has already initialized"),  // NOSONAR
    JRIPv6NotEnabled("JRIPv6NotEnabled", 0x7313, "TCP/IP cannot process the IPv6 request because the TCP/IP stack is not currently IPv6 enabled"),  // NOSONAR
    JRInvalidValue("JRInvalidValue", 0x7314, "The value specified is not a valid value"),  // NOSONAR
    JRInvalidOptLength("JRInvalidOptLength", 0x7315, "The option length specified is not valid"),  // NOSONAR
    JRDefaultExceeded("JRDefaultExceeded", 0x7316, "The value specified exceeds the system default"),  // NOSONAR
    JROptNotSupported("JROptNotSupported", 0x7317, "The option specified is not supported on this type of socket"),  // NOSONAR
    JROPTNAllow("JROPTNAllow", 0x7318, "The option name specified is not allowed on getsockopt()"),  // NOSONAR
    JRSockIPv6CommOnly("JRSockIPv6CommOnly", 0x7319, "The user is attempting to either bind or send to an IPv4 mapped IPv6 address on an AF_INET6 socket that only supports IPv6 communications"),  // NOSONAR
    JRSockIPv6IPv4CommError("JRSockIPv6IPv4CommError", 0x731a, "The user is attempting to either send to an IPv4 mapped address when using an IPv6 native source address on an AF_INET6 socket or send to an IPv6 native address when using an IPv4 mapped source address on an AF_INET6 socket"),  // NOSONAR
    JRCmsgNotAllowed("JRCmsgNotAllowed", 0x731b, "The ancillary data object provided on sendmsg() is not allowed on this type of socket"),  // NOSONAR
    JRCmsgHdrInvalid("JRCmsgHdrInvalid", 0x731c, "The information in the ancillary data object header, CMSGHDR, is not valid"),  // NOSONAR
    JRPktInfo("JRPktInfo", 0x731d, "An error was found with the information in the in6_pktinfo structure or in the in_pktinfo structure provided on setsockopt() or sendmsg()"),  // NOSONAR
    JRInvalidAddr("JRInvalidAddr", 0x731e, "The address specified is not a valid IPv6 Multicast address"),  // NOSONAR
    JRConnectBadSockAddr("JRConnectBadSockAddr", 0x731f, "An invalid socket address structure was specified on connect()"),  // NOSONAR
    JRSockIPv6InvalidScopeId("JRSockIPv6InvalidScopeId", 0x7320, "A non-zero scope value was determined to be incorrect"),  // NOSONAR
    JRSockAddrLenInvalid("JRSockAddrLenInvalid", 0x7321, "The length provided for the AF_INET6 socket address structure is not valid"),  // NOSONAR
    JRNOSYSPLEXPORT("JRNOSYSPLEXPORT", 0x7322, "A sysplex wide port is not available for this source DRVIPA"),  // NOSONAR
    JRINUSESYSPLEXPORT("JRINUSESYSPLEXPORT", 0x7323, "The port specified is already in use"),  // NOSONAR
    JRNotAuthFRCA("JRNotAuthFRCA", 0x7324, "Userid is not authorized to access the TCP/IP FRCA service"),  // NOSONAR
    JRSocketNoPeer("JRSocketNoPeer", 0x7325, "Socket is not connected and a datagram has not been successfully received (UDP/RAW)"),  // NOSONAR
    JRV6CKSUMOFFSET("JRV6CKSUMOFFSET", 0x7326, "The user is attempting set the socket option IPPROTO_IPV6 IPV6_CHECKSUM with an odd value for the checksum offset"),  // NOSONAR
    JRIPV6HOPLIMEXCEEDED("JRIPV6HOPLIMEXCEEDED", 0x7327, "TCP/IP has been configured with a maximum IPv6 Hop Limit of zero, which disallows any IPv6 packets from leaving the node"),  // NOSONAR
    JRSOCKADDRPROVIDED("JRSOCKADDRPROVIDED", 0x7328, "The sockaddr provided for a send operation on a connectionless socket is different from the sockaddr provided on the connect"),  // NOSONAR
    JRSPCFError("JRSPCFError", 0x7329, "Unable to associate the sysplexports distributed DVIPA with the EZBEPORT structure"),  // NOSONAR
    JRIPV6ProtocolNotAllowed("JRIPV6ProtocolNotAllowed", 0x732a, "An IPV6 next header ID for an extension header cannot be specified as a socket protocol for an AF_INET6 socket"),  // NOSONAR
    JRProtocolInvalid("JRProtocolInvalid", 0x732b, "The protocol provided for a Raw socket is outside the allowable range"),  // NOSONAR
    JRIOFailure("JRIOFailure", 0x732c, "Error status reported by the device driver as a result of an attempt to perform I/O"),  // NOSONAR
    JRSDQuiesce("JRSDQuiesce", 0x732d, "An attempt was made to bind to a distributed SYSPLEXPORTS DVIPA in Quiescing state"),  // NOSONAR
    JRIPSecNotAvail("JRIPSecNotAvail", 0x732e, "IP Security is not enabled on this stack"),  // NOSONAR
    JRNotAuthIPsec("JRNotAuthIPsec", 0x732f, "Userid is not authorized to issue the ipsec command"),  // NOSONAR
    JRSockOptAccessDenied("JRSockOptAccessDenied", 0x7330, "Userid is not authorized to the socket option"),  // NOSONAR
    JRDVIPAInterfaceAlreadyDefined("JRDVIPAInterfaceAlreadyDefined", 0x7331, "The Dynamic VIPA interface has already been defined via VIPADEFine or VIPABackup"),  // NOSONAR
    JRNotSpDrVipa("JRNotSpDrVipa", 0x7332, "An attempt was made to listen on a socket that is bound to a sysplex distributed DVIPA that is not using SYSPLEXPORTS, and a non-sysplex distributed port that was not user-specified"),  // NOSONAR
    JRRtDestOnLocalHost("JRRtDestOnLocalHost", 0x7333, "Specified route destination is a local address of a non-DVIPA interface"),  // NOSONAR
    JRInvalidSRCIPAddr("JRInvalidSRCIPAddr", 0x7334, "The source IP address that was defined by SRCIP is invalid"),  // NOSONAR
    JRSRCIPIntNotDefined("JRSRCIPIntNotDefined", 0x7335, "The interface name specified on a SRCIP configuration statement does not match any interface name defined on the local system"),  // NOSONAR
    JRSRCIPIntNoRoute("JRSRCIPIntNoRoute", 0x7336, "The interface specified on a SRCIP configuration statement cannot be used as no route to the destination address exists"),  // NOSONAR
    JRConnDeniedPolicy("JRConnDeniedPolicy", 0x7337, "A TCP connection request was denied due to policy"),  // NOSONAR
    JRNextHop("JRNextHop", 0x7338, "The IPV6_NEXTHOP address is not valid"),  // NOSONAR
    JRRoutingHeader("JRRoutingHeader", 0x7339, "An error occurred while processing the routing header on an IPV6_RTHDR socket option"),  // NOSONAR
    JRRtHdrTooLong("JRRtHdrTooLong", 0x733a, "More than eight routing headers were specified"),  // NOSONAR
    JRTtlsDecryptionFailed("JRTtlsDecryptionFailed", 0x733b, "AT-TLS was unable to decrypt data received on a TCP connection"),  // NOSONAR
    JRTtlsEncryptionFailed("JRTtlsEncryptionFailed", 0x733c, "AT-TLS was unable to encrypt data to be sent on a TCP connection"),  // NOSONAR
    JRTtlsHandshakeFailed("JRTtlsHandshakeFailed", 0x733d, "AT-TLS was unable to successfully negotiate a secure TCP connection with the remote end"),  // NOSONAR
    JRTtlsControlDataFailed("JRTtlsControlDataFailed", 0x733e, "AT-TLS was unable to process secure control data received over a TCP connection from the remote partner"),  // NOSONAR
    JRTtlsResetSessionFailed("JRTtlsResetSessionFailed", 0x733f, "AT-TLS received an error resetting the session ID for the secure connection"),  // NOSONAR
    JRTtlsResetCipherFailed("JRTtlsResetCipherFailed", 0x7340, "AT-TLS received an error attempting to generate new session keys for a secure connection"),  // NOSONAR
    JRTtlsAbendRecovery("JRTtlsAbendRecovery", 0x7341, "An abend occurred in AT-TLS causing the secure connection to be reset"),  // NOSONAR
    JRTtlsGroupDeleted("JRTtlsGroupDeleted", 0x7342, "AT-TLS reset a TCP connection because the group the connection was mapped to has been deleted"),  // NOSONAR
    JRTtlsClearTxtReceived("JRTtlsClearTxtReceived", 0x7343, "AT-TLS received clear text data when secure data was expected"),  // NOSONAR
    JRPortNonZero("JRPortNonZero", 0x7344, "The port value is required to be zero"),  // NOSONAR
    JRPortZero("JRPortZero", 0x7345, "The port value is required to be nonzero"),  // NOSONAR
    JRSockIPv6OnlyOption("JRSockIPv6OnlyOption", 0x7346, "The user is attempting to use an IPv4 mapped IPv6 address with an IPv6 only socket option"),  // NOSONAR
    JRSRCIPDistDVIPA("JRSRCIPDistDVIPA", 0x7347, "The source IP address defined by a DESTINATION entry in a SRCIP configuration statement cannot be a distributed DVIPA"),  // NOSONAR
    JRDuplicateSmoAttach("JRDuplicateSmoAttach", 0x7348, "The shared memory object is already attached"),  // NOSONAR
    JRSmoNotAttached("JRSmoNotAttached", 0x7349, "The shared memory object is not attached"),  // NOSONAR
    JRIARV64Error("JRIARV64Error", 0x734a, "IARV64 encountered an error"),  // NOSONAR
    JRNoRouteTable("JRNoRouteTable", 0x734b, "Specified route table does not exist on the TCP/IP stack"),  // NOSONAR
    JRExpBndPortRangeConflict("JRExpBndPortRangeConflict", 0x734c, "An explict bind to a port within the active EXPLICITBINDPORTRANGE is not allowed"),  // NOSONAR
    JRMCPairNotFound("JRMCPairNotFound", 0x734d, "The specified multicast group and interface pair ARE NOT FOUND"),  // NOSONAR
    JRMCMixedOpt("JRMCMixedOpt", 0x734e, "The specified multicast option is mixed of any-source, source-specific or full-state APIs"),  // NOSONAR
    JRMCMaxSrcFlt("JRMCMaxSrcFlt", 0x734f, "The maximum number of source addresses per group and interface pair has been exceeded"),  // NOSONAR
    JRMCDupSrcFlt("JRMCDupSrcFlt", 0x7350, "The same source address is already in the group and interface"),  // NOSONAR
    JRMCEmptySrcIncMode("JRMCEmptySrcIncMode", 0x7351, "The empty source list can not be specified for mode INCLUDE when the multicast group is not yet defined"),  // NOSONAR
    JRMCModeInvalid("JRMCModeInvalid", 0x7352, "The filter mode specified for multicast source fuction when the multicast group is not yet defined"),  // NOSONAR
    JRMCGetSrcInvalid("JRMCGetSrcInvalid", 0x7353, "THE MULTICAST GROUP AND INTERFACE ARE NOT YET DEFINED for get source filter function"),  // NOSONAR
    JRMCSrcAddrInvalid("JRMCSrcAddrInvalid", 0x7354, "The specified source address is not valid"),  // NOSONAR
    JRCmMultipleCaches("JRCmMultipleCaches", 0x7355, "Cache Manager cannot be configured for both a shared and an exclusive cache"),  // NOSONAR
    JRCmChangeCacheType("JRCmChangeCacheType", 0x7356, "Cache Manager cannot be reconfigured for a different type of cache"),  // NOSONAR
    JRUWSXStorageFailed("JRUWSXStorageFailed", 0x7357, "Cache Manager encountered storage shortage"),  // NOSONAR
    JRCmBadCacheType("JRCmBadCacheType", 0x7358, "Cache type specified on request does not match the configured cache type"),  // NOSONAR
    JRDVQuiesce("JRDVQuiesce", 0x7359, "An attempt was made to bind to a distributed DVIPA in Quiescing state"),  // NOSONAR
    JRUnRsvdPortDenied("JRUnRsvdPortDenied", 0x735a, "Application does not have PORT statement UNRSV authority to access a port that is not reserved by a PORT or PORTRANGE profile statement"),  // NOSONAR
    JRNotAuthUnRsvdPort("JRNotAuthUnRsvdPort", 0x735b, "Userid was refused SAF authorization to access a port that is not reserved by a PORT or PORTRANGE profile statement"),  // NOSONAR
    JRUnRsvdTCPPortConflict("JRUnRsvdTCPPortConflict", 0x735c, "WHENBIND and WHENLISTEN cannot be specified concurrently by PORT UNRSV TCP profile statements"),  // NOSONAR
    JRNoCritSocks("JRNoCritSocks", 0x735d, "No critical sockets are available to satisfy the request"),  // NOSONAR
    JRNoPartnerInfo("JRNoPartnerInfo", 0x735e, "No partner information is returned for the request"),  // NOSONAR
    JRInValidTCPIPStack("JRInValidTCPIPStack", 0x735f, "The application tried to use a function which is not supported by this TCPIP stack"),  // NOSONAR
    JRNotSameSecDomain("JRNotSameSecDomain", 0x7360, "Both end points of the connection does not reside in the same security domain"),  // NOSONAR
    JRNoSecDomain("JRNoSecDomain", 0x7361, "The socket call fails because the security domain name for the profile in the SERVAUTH class is not defined"),  // NOSONAR
    JRNoSuspend("JRNoSuspend", 0x7362, "The socket call fails because the socket call is issued in no-suspend mode and there is no information available to be returned"),  // NOSONAR
    JRSockIPv6InvalidScopeIdZero("JRSockIPv6InvalidScopeIdZero", 0x7363, "A zero scope id is not valid for use with the scope of this address"),  // NOSONAR
    JROSMAccessDenied("JROSMAccessDenied", 0x7364, "The user is not permitted to communicate over OSM interfaces"),  // NOSONAR
    JRDisabled("JRDisabled", 0x7365, "The function is currently disabled"),  // NOSONAR
    JRCallerMismatch("JRCallerMismatch", 0x7366, "The authorization of the current caller of the request does not match that of the caller that initialized the function"),  // NOSONAR
    JRTooManyInstances("JRTooManyInstances", 0x7367, "The maximum number of function instances is already open"),  // NOSONAR
    JROutOfSequence("JROutOfSequence", 0x7368, "The current request does not follow the correct request sequence for the function"),  // NOSONAR
    JRTcpGlobalStall("JRTcpGlobalStall", 0x7369, "The TCP connection has been reset due to a global stall"),  // NOSONAR
    JRTcpQueueSize("JRTcpQueueSize", 0x736a, "The TCP connection has been reset due to a queue size problem"),  // NOSONAR
    JRSrcIPInvalidForExternalDest("JRSrcIPInvalidForExternalDest", 0x736b, "The TCP connect source IP address is not valid because the non-z/OS external target for the GRE or ENCAP Distributed VIPA does not have a route back to this address. It will not be able to return the syn ack to the client"),  // NOSONAR
    JRTTLSStopReadDataPending("JRTTLSStopReadDataPending", 0x736c, "The AT-TLS SIOCTTLSCTL TTLSi_Stop_Connection ioctl request can not complete because all of the decrypted data was not read from the socket. All application data received over the secure connection must be read prior to the security stopping on the connection"),  // NOSONAR
    JRTTLSStopWriteDataPending("JRTTLSStopWriteDataPending", 0x736d, "The AT-TLS SIOCTTLSCTL TTLSi_Stop_Connection ioctl request can not complete because application write data is pending to be encrypted on the connection. All application write requests must be completed prior to the security stopping on the connection"),  // NOSONAR
    JRInvalidFilter("JRInvalidFilter", 0x736e, "The filter identifier is wrong or the filter specified is not valid for obtaining FTP daemon configuration. The filter only supports the address space id (ASID) item"),  // NOSONAR
    JRDVIPAServicesNotActive("JRDVIPAServicesNotActive", 0x736f, "Dynamic VIPA services are not available at this time. This situation can occur when TCP/IP has not joined the sysplex, has left the sysplex, or has not completed processing the initial profile"),  // NOSONAR
    JRTtlsResetWriteCipherFailed("JRTtlsResetWriteCipherFailed", 0x7370, "AT-TLS received an error while attempting to generate a new write session key for a secure connection"),  // NOSONAR
    JRTtlsSendSessionTicketFailed("JRTtlsSendSessionTicketFailed", 0x7371, "AT-TLS received an error while attempting to send a session ticket to the client for a secure connection");  // NOSONAR

    private static final Map<Integer, PlatformErrno2> BY_ERRNO = new HashMap<>();

    public static final int ERRNO2_BASE = 0x090c0000;

    static {
        for (PlatformErrno2 e : values()) {
            BY_ERRNO.put(e.errno2, e);
        }
    }

    public final String shortErrorName;
    public final int errno2;
    public final String explanation;

    public static PlatformErrno2 valueOfErrno(int errno2) {
        return BY_ERRNO.getOrDefault(errno2 & 0xffff, null);
    }

    public String format() {
        return String.format("%s %s", this.shortErrorName, this.explanation);
    }

}
