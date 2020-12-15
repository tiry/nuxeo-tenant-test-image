#!/bin/bash

cd plugins

mvn -o -DskipTests clean install

cd ..

docker build  -t nuxeo-test/tenants -f Dockerfile .

IMAGEID=$(docker images -q nuxeo-test/tenants)

docker tag $IMAGEID gcr.io/jx-preprod/nuxeo-mt
docker push gcr.io/jx-preprod/nuxeo-mt

