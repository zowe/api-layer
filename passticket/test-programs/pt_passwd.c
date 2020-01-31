#define _OPEN_SYS 1
#include <pwd.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <pthread.h>
#include <stdlib.h>

#define PSATOLD 0x21C
#define TCBSTCB 0x138
#define STCBOTCB 0x0D8
#define OTCBTHLI 0x0BC

#define THLIAPPLIDLEN 0x052
#define THLIAPPLID 0x070

int set_applid(const char* applid) {
    void *__ptr32 psa = 0;
    void *__ptr32 tcb = *(void *__ptr32 *)(psa + PSATOLD);
    printf("tcb=%p\n", tcb);
    void *__ptr32 stcb = *(void *__ptr32 *)(tcb + TCBSTCB);
    printf("stcb=%p\n", stcb);
    void *__ptr32 otcb = *(void *__ptr32 *)(stcb + STCBOTCB);
    printf("otcb=%p\n", otcb);
    void *__ptr32 thli = *(void *__ptr32 *)(otcb + OTCBTHLI);
    printf("thli=%p\n", thli);

    if (memcmp("THLI", thli, 4) != 0)
    {
        int rc = -2;
        printf("Could not set APPLID: BPXYTHLI control block not found\b");
        return -1;
    }

    char *origApplid = (char *)malloc(9);

    const int applidLength = strlen(applid);

    printf("APPLID length: %d\n", applidLength);
    printf("APPLID value: %s\n", applid);

    char *__ptr32 thliApplidLen = (char *__ptr32)(thli + THLIAPPLIDLEN);
    *thliApplidLen = applidLength;

    void *__ptr32 thliApplid = (void *__ptr32)(thli + THLIAPPLID);
    memset(thliApplid, ' ', 8);
    origApplid[8] = 0;
    memcpy(origApplid, thliApplid, 8);
    memcpy(thliApplid, applid, applidLength);

    printf("Orig APPLID value: %s\n", origApplid);

    /* A call to pthread_security_np causes that the value set above is correctly propagated */
    pthread_security_np(0, 0, 0, NULL, NULL, 0);
    errno = 0;
    return 0;
}

int main(int argc, char *argv[]) {
    for (int i = 0; i < argc; i++)
        printf("argv[%d] == \"%s\"\n", i, argv[i]);

    const char *userid = argv[1];
    const char *applid = argv[2];
    const char *passticket = argv[3];

    int rc;

    set_applid(applid);
    rc = __passwd(userid, passticket, 0);
    printf("__passwd rc=%d, errno=%d strerror=%s\n", rc, errno, strerror(errno));

    rc = __passwd_applid(userid, passticket, 0, applid);
    printf("__passwd_applid rc=%d, errno=%d strerror=%s\n", rc, errno, strerror(errno));

    return 0;
}
