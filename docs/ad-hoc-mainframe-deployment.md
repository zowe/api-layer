## Ad-Hoc deployment of API Mediation Layer to Mainframe system

## Technology

Using [zowe-api-dev](https://github.com/zowe/sample-spring-boot-api-service/blob/master/zowe-rest-api-sample-spring/docs/devtool.md
) tool from Zowe SDK.

Underlying technology is Zowe CLI.

**WARNING: TECHNOLOGY AND IMPLEMENTATION IS STILL IN EXPERIMENTAL STAGE**

### Prerequisities
 - Install Zowe CLI - https://docs.zowe.org/stable/user-guide/cli-installcli.html#installing-zowe-cli
 - Install zowe-api-dev
   ```
   npm -g install @zowedev/zowe-api-dev
   ```
 - Create Zowe CLI profiles for Z/OSMF and USS
   ```
   zowe profiles create zosmf-profile <profile_id> --host <hostname> --port <port> --user <userid> --pass "<password>" --reject-unauthorized false
   zowe profiles create ssh-profile <profile_id> --host <hostname> --user <userid> --password "<password>"
   ```
### Procedure

Procedure is separated into 3 distinct phases

*All commands are run from repository root folder*

#### Phase 1 - Initial run

objective of this phase is to setup zowe-api-dev and perform first deployment

 - Create `user-zowe-api.json` file in repository root folder. Fill in specific information. 
  
    *Keep ports and job identifiers unique so you do not overlap other developers deployments*

    *Sample user-zowe-api.json*
    ```json
    {
    "javaHome": "<java home path>",
    "javaLoadlib": "",
    "jobcard": [
        "//<job id> JOB <your mainframe account number>,'APIML',MSGCLASS=A,CLASS=A,",
        "//  MSGLEVEL=(1,1),REGION=0M",
        "/*JOBPARM SYSAFF=*"
    ],
    "zosHlq": "<dataset hlq for zfs>",
    "zosTargetDir": "<deployment directory, use home directory>",
    "zoweProfileName": "<name of zowe profile for zosmf  and uss>",
    "basePort": <base port for deployment>,
    "systemHostname": "<hostname of deployment system>"
    }
    ```
 - Create a directory next to your API Mediation layer repository called `api-layer-deploy`. Place the following files inside:

   ```
   |
   +- api-layer
   | +- / api layer repository /
   | ...
   +- api-layer-deploy
     +- apiml.keystore.p12
     +- apiml.truststore.p12
     +- zosmf.yml
   ```

    - apiml.keystore.p12 - Keystore containing certificate with alias `apiml` to be used by deployed services
    - apiml.truststore.p12 - Kestore with CA certs and trust chain to be used to verify certificates
    - zosmf.yml - zosmf static definition

 - Run `zowe-api-dev zfs`. ZFS filesystem is be created and mounted to the `zosTargetDir`.
 
 - Build deployment artifacts.
   - Run Gradle build task
   - Run Gradle packageLibs task
 
 - Run `zowe-api-dev deploy`. Binaries are be deployed. 
 
 - Run `zowe-api-dev config --name zos`. Configuration is deployed.
 
 - Start deployed instance by running 'zowe-api-dev start --job'. JES Job is started. You will get Job ID of started job.

 *At this point you can go and use the deployment. You will not have to repeat these steps (Except the zfs mount if you dispose of it)*

#### Phase 2 - Operate the solution

`zowe-api-dev status` - To understand the state of your job.

`zowe-api-dev start --job` - Starts the deployment job. Make sure the job is not running before.

`zowe-api-dev deploy` - After code change, you can rerun this command to redeploy the binaries again. Only patch is deployed for efficiency. You can use `--force` to redeploy everything.

`zowe-api-dev config --name zos` - After configuration change, you can run this command to redeploy changed files. There is no force and the redeployment sometimes finds no changes, even when changes have been made. In that case, inspect `.zowe-api-dev` folder and remove the mirrored deployment files and rerun the command.

`zowe zos-jobs cancel job <job id>` - Stop your running job. There is no better way at the moment. We have issue pending with zowe-api-dev.

#### Phase 3 - Dispose of the deployment

*Make sure the deployment is not started, stop the job if it is*

`zowe-api-dev zfs --unmount --delete` - Unmount and delete zfs. *Not tested*

### Flow of configuration

User variables are stored in `user-zowe-api.json`. You should not need to change `zowe-api.json`. `config/zowe-api-dev/template-bpxbatch.jcl` is used to run main run script on mainframe. This sets the deployment configuration into the stdenv on mainframe.

Wrapper script `config/zowe-api-dev/run-wrapper.sh` is launched on mainframe. This is where you can change things and configure the deployment. This wrapper sets environment, unpacks dependencies and runs Zowe's Apiml start script `zowe-install/src/main/resources/component-scripts/start.sh`, which in turn starts Apiml.

*You can use the config deploy to upload the config changes only.*

### Notes and Known issues

 - The delta algorithm that zowe-api-dev is using is not perfect. Sometimes manual delete of `.zowe-api-dev/uploadedFiles` cache is needed to force deployment.
 - Zowe CLI sometimes fails to upload large files with error: `FATAL ERROR: Ineffective mark-compacts near heap limit Allocation failed - JavaScript heap out of memory
`. When retried, upload passes. Further optimization can be done to reduce size of our deployment.

### Deployment size optimization

*Following changes are additional to standard packaging*

Code packaging into jars is enhanced. 

#### Thin jars package
Next to normal BootJar Gradle task, Jar task for core services is enabled, which package thin jar without dependencies packed in. Additional jar file is created with `-thin` suffix and classpath entries in it's manifest. All relevant libraries are expected to be present in `./lib` folder. This task is executed on project build or individual module build.

#### Libraries package
Gradle task on root project level: `packageLibs` is added. It grabs all dependencies from the modules and packs into libraries.zip file. It is not included in build for performance reasons and needs to be executed manually prior deployment. It's not needed for subsequent deployments without dependencies changes.
