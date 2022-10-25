#!/bin/bash

set -x -o xtrace

if [ "$1" = "1" ]; then

    # TODO manually update image name in values.yaml
    ./mvnw spotless:apply package -Pwith-docker-image -Dmaven.test.skip=true

    minikube image load edc-controlplane-postgresql-hashicorp-vault:latest
    minikube image load edc-dataplane-hashicorp-vault:latest

    cd edc-tests/src/main/resources/deployment/helm/all-in-one
    helm uninstall edc-all-in-one --namespace edc-all-in-one
    helm install edc-all-in-one --namespace edc-all-in-one --create-namespace .
    cd ../../../../../../..

    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=sokratesbackendapplication --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=sokratesbackendapplication && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=platobackendapplication --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=platobackendapplication && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=sokratesedcdataplane --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=sokratesedcdataplane && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=platoedcdataplane --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=platoedcdataplane && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=sokratesvault --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=sokratesvault && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=platovault --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=platovault && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=sokratespostgresql --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=sokratespostgresql && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=platopostgresql --timeout=120s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=platopostgresql && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=sokratesedccontrolplane --timeout=600s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=sokratesedccontrolplane && exit 1)
    kubectl wait --for=condition=ready pod -n edc-all-in-one -l app.kubernetes.io/name=platoedccontrolplane --timeout=600s || (kubectl logs -n edc-all-in-one -l app.kubernetes.io/name=platoedccontrolplane && exit 1)

fi

export PLATO_DATA_MANAGEMENT_URL=$(minikube service plato-edc-controlplane -n edc-all-in-one --url | sed -n 3p)
export PLATO_DATA_MANAGEMENT_URL="${PLATO_DATA_MANAGEMENT_URL}/data"
export PLATO_IDS_URL="http://plato-edc-controlplane:8282/api/v1/ids"
export PLATO_DATA_PLANE_URL=foo
export PLATO_DATA_MANAGEMENT_API_AUTH_KEY=password
export PLATO_BACKEND_SERVICE_BACKEND_API_URL=$(minikube service plato-backend-application -n edc-all-in-one --url | sed -n 2p)

export SOKRATES_DATA_MANAGEMENT_URL=$(minikube service sokrates-edc-controlplane -n edc-all-in-one --url | sed -n 3p)
export SOKRATES_DATA_MANAGEMENT_URL="${SOKRATES_DATA_MANAGEMENT_URL}/data"
export SOKRATES_IDS_URL="http://sokrates-edc-controlplane:8282/api/v1/ids"
export SOKRATES_BACKEND_URL=$(minikube service sokrates-backend-application -n edc-all-in-one --url | sed -n 2p)
export SOKRATES_DATA_PLANE_URL=foo
export SOKRATES_DATA_MANAGEMENT_API_AUTH_KEY=password
export SOKRATES_BACKEND_SERVICE_BACKEND_API_URL=$(minikube service sokrates-backend-application -n edc-all-in-one --url | sed -n 2p)

kubectl port-forward -n edc-all-in-one plato-postgresql-0 5555:5432 &
kubectl port-forward -n edc-all-in-one sokrates-postgresql-0 6666:5432 &

export PLATO_DATABASE_URL=jdbc:postgresql://localhost:5555/edc
export PLATO_DATABASE_PASSWORD=psql_password
export PLATO_DATABASE_USER=postgresql_sandbox_user

export SOKRATES_DATABASE_URL=jdbc:postgresql://localhost:6666/edc
export SOKRATES_DATABASE_PASSWORD=psql_password
export SOKRATES_DATABASE_USER=postgresql_sandbox_user

./mvnw spotless:apply
./mvnw -s settings.xml -B -Pbusiness-tests -pl edc-tests test -Dtest=RunCucumberTest
