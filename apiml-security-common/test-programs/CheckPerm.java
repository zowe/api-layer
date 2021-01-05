import com.ibm.os390.security.PlatformAccessControl;
import com.ibm.os390.security.PlatformReturned;

public class CheckPerm {
    static int failed = 0;

    static PlatformReturned checkPermission(String userid, String resourceClass, String resourceName, int accessLevel,
            int expectedErrno, int expectedErrno2, int secondExpectedErrno, int secondExpectedErrno2) {
        System.out.print(String.format("PlatformAccessControl.checkPermission(%s, %s, %s, %d) = ", userid,
                resourceClass, resourceName, accessLevel));
        PlatformReturned r = PlatformAccessControl.checkPermission(userid,
                resourceClass, resourceName, accessLevel);
        System.out.println(returnedToString(r));
        if (expectedErrno == 0) {
            if (r == null) {
                System.out.println("OK - null expected");
            } else {
                System.out.println("ERROR - null expected but returned: " + returnedToString(r));
                failed++;
            }
        } else {
            if (r == null) {
                System.out.println("ERROR - unexpected errno and errno2 - expected null");
                failed++;
            } else if (((r.errno == expectedErrno) && (r.errno2 == expectedErrno2)) || ((r.errno == secondExpectedErrno) && (r.errno2 == secondExpectedErrno2))) {
                System.out.println("OK - expected errno and errno2 returned");
            } else {
                System.out.println(String.format("ERROR - unexpected errno and errno2 returned. Expected %d and 0x%04x",
                        expectedErrno, expectedErrno2));
                failed++;
            }
        }
        return r;
    }

    static String returnedToString(PlatformReturned r) {
        if (r == null) {
            return null;
        }
        return String.format("PlatformReturned(rc=%d, errno=%d, errno2=0x%04x, errnoMsg=%s, stringRet=%s, success=%s)",
                r.rc, r.errno, r.errno2, r.errnoMsg, r.stringRet, r.success);
    }

    public static void main(String args[]) {
        String userid = args[0];
        String badUserid = args[1];
        String okClass = args[2];
        String badClass = args[3];
        String okResource = args[4];
        String badResource = args[5];

        int okAccessLevel = Integer.parseInt(args[6]);
        int badAccessLevel = Integer.parseInt(args[7]);

        checkPermission(userid, okClass, okResource, okAccessLevel, 0, 0, 0, 0);
        checkPermission(userid, badClass, okResource, okAccessLevel, 143, 0x93800cf, -1, -1);  // Last two bytes are equal to PlatformErrno2.JRSAFResourceUndefined
        checkPermission(userid, okClass, badResource, okAccessLevel, 143, 0x93800cf, -1, -1);  // Last two bytes are equal to PlatformErrno2.JRSAFResourceUndefined
        checkPermission(userid, okClass, okResource, badAccessLevel, 139, 0x93800d9, -1, -1);  // Last two bytes are equal to PlatformErrno2.JRNoResourceAccess
        checkPermission(badUserid, okClass, okResource, okAccessLevel, 139, 0x93800d9, 143, 0x93800f9);  // Last two bytes are equal to PlatformErrno2.JRNoResourceAccess (second pair of expected values is for RACF that returns JRSAFNoUser)
        checkPermission(badUserid, okClass, badResource, okAccessLevel, 143, 0x93800cf, 143, 0x93800f9);  // Last two bytes are equal to PlatformErrno2.JRSAFResourceUndefined (second pair of expected values is for RACF that returns JRSAFNoUser)
        checkPermission(badUserid, badClass, badResource, okAccessLevel, 143, 0x93800cf, 143, 0x93800f9);  // Last two bytes are equal to PlatformErrno2.JRSAFResourceUndefined (second pair of expected values is for RACF that returns JRSAFNoUser)
        System.out.println("Total failures: " + failed);
        System.exit(failed);
    }
}
