#!/bin/bash

cd plugins

mvn -nsu -DskipTests clean install

cd ..

docker build  -t nuxeo-test/tenants -f Dockerfile$1 .

IMAGEID=$(docker images -q nuxeo-test/tenants)

docker tag $IMAGEID gcr.io/jx-preprod/nuxeo-mt$1
docker push gcr.io/jx-preprod/nuxeo-mt$1

