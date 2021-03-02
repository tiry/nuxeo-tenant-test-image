### About

This repository contains what is needed to build an image used to test Multi-tenant deployment of Nuxeo in a k8s cluster.

### Modules

#### plugins/core

Simple Nuxeo plugin that will change the LoginScreen according to the `nuxeo.tenantId` property

#### plugin/k8s-hpa-metrics

Custom metrics reporter used to leverage K8S HPA with Nuxeo.

#### plugin/nuxeo-extended-session

Allows to redeploy Nuxeo pods without having to re-authenticate

#### plugin/packages

Marketplace package used to deploy everything in image

    - google-storage-2021.1.19.zip
    - nuxeo-arender-2021.1.0.zip
    - nuxeo-statistics-package-1.0-SNAPSHOT.zip
    - nuxeo-web-ui-3.0.2.zip

#### Docker file

Build a custom Docker image integrating the plugins and some additiona packages like GCP Store and WebUI.


#### Usage

Build standard Nuxeo image and upload to GCR

    ./build.sh


Build Nuxeo image with ARender + upload to GCR

    ./build.sh -arender
