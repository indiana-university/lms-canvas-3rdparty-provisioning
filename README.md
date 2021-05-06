# ess-lms-canvas-3rdPartyProvisioning
App for 3rd Party Provisioning file uploads

To Debug w/ Intellij, forward 5005 (in kube-forwarder, or k9s) to any desired port and then hook intellij up to that

```
helm upgrade lmslti3rdpartyprovisioning harbor-prd/k8s-boot -f helm-common.yaml -f helm-dev.yaml --install
```

```
helm upgrade lmslti3rdpartyprovisioning harbor-prd/k8s-boot -f helm-common.yaml -f helm-snd.yaml --install
```

Install the batch job
```
helm upgrade resultsemail ../k8s --values helm-common.yaml,helm-dev.yaml,helm-batch-resultsemail.yaml --install
helm upgrade deptprov-resultsemail-reg ../k8s --values helm-common.yaml,helm-reg.yaml,helm-batch-resultsemail.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
helm upgrade deptprov-resultsemail-stg ../k8s --values helm-common.yaml,helm-stg.yaml,helm-batch-resultsemail.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
helm upgrade deptprov-resultsemail-prd ../k8s --values helm-common.yaml,helm-prd.yaml,helm-batch-resultsemail.yaml --install -n ua-vpit--enterprise-systems--lms--helm-release
```