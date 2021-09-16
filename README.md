# ess-lms-canvas-3rdPartyProvisioning
App for 3rd Party Provisioning file uploads

## File Upload Details

POST with the following params (multipart/form-data):

| Parameter | Details | Notes | 
| --- | --- | --- |
| `deptFileUpload` (required) | 1 or more files | |
| `customUsersNotification` (optional) | true/false | If set to `true`, there must be a users csv with new users, as well as a `message.properties` file with a sender/subject/body  |

### Upload a zip file
Endpoint is `/rest/upload/<DEPT_CODE>/zip`

### Upload all files at once
Endpoint is `/rest/upload/<DEPT_CODE>`

## Developer Details

To Debug w/ Intellij, forward 5005 (in kube-forwarder, or k9s) to any desired port and then hook intellij up to that

```
helm upgrade lmslti3rdpartyprovisioning harbor-prd/k8s-boot -f helm-common.yaml -f helm-dev.yaml -f ../helm-vault-local.yaml --install
```

```
helm upgrade lmslti3rdpartyprovisioning harbor-prd/k8s-boot -f helm-common.yaml -f helm-snd.yaml --install
```

Install the batch job
```
helm upgrade deptprov-resultsemail ../k8s --values helm-common.yaml,helm-dev.yaml,helm-batch-resultsemail.yaml,../helm-vault-local.yaml --install
helm upgrade deptprov-resultsemail-reg ../k8s --values helm-common.yaml,helm-reg.yaml,helm-batch-resultsemail.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
helm upgrade deptprov-resultsemail-stg ../k8s --values helm-common.yaml,helm-stg.yaml,helm-batch-resultsemail.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
helm upgrade deptprov-resultsemail-prd ../k8s --values helm-common.yaml,helm-prd.yaml,helm-batch-resultsemail.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
```