## Ad-Hoc deployment of API Mediation Layer to Mainframe system

## Technology

Using zowe-api-dev tool https://github.com/zowe/sample-spring-boot-api-service/blob/master/zowe-rest-api-sample-spring/docs/devtool.md

### Prerequisities
 - Have Zowe CLI installed https://docs.zowe.org/stable/user-guide/cli-installcli.html#installing-zowe-cli
 - Have zowe-api-dev installed
   ```
   npm -g install @zowedev/zowe-api-dev

   ```
 - Have Zowe CLI profiles for zosmf and uss
  ```
    zowe profiles create zosmf-profile <profile_id> --host <hostname> --port <port> --user <userid> --pass "<password>" --reject-unauthorized false
    zowe profiles create ssh-profile <profile_id> --host <hostname> --user <userid> --password "<password>"
  ```
### Procedure

all commands are run from root repository folder
 - Run 'zowe-api-dev init --account=<mf-account-id>'. 'user-zowe-api.json' will be generated in repository root folder.
 - Edit 'user-zowe-api.json':
   - Add "basePort": 10010 node
   - Change the jobcard with unique job name.
 - Edit 'zowe-api.json':
    - Provide path to valid keystore and truststore for your environment **//TODO we need better way**
 - Run 'zowe-api-dev zfs'. ZFS filesystem will be created and mounted
 - **//TODO not implemented** Run 'zowe-api-dev deploy'. Binaries will be deployed. After code change, you can rerun this command to redeploy the binaries again. Only patch is deployed for efficiency.
 - Run 'zowe-api-dev config --name zos'. Configuration is deployed. After configuration change, you can run this command to redeploy changed files.
 - Start your deployed instance as a USS process by running 'zowe-api-dev start'. You will see logs returned back to you. Stop the instance by pressing ^C.
 - Start your deployed instance as Job by running 'zowe-api-dev start --job'. You will get Job ID. Stop the Job by running 'zowe-api-dev stop' **//TODO Broken for bpxbatch, issue being raised** or view status of your last submitted job by running 'zowe-api-dev status'.

### Configuration

**//TODO**
