#!/bin/bash

set -x -o xtrace

export EDC_AWS_ENDPOINT_OVERRIDE=$(minikube service minio -n cx --url)

export PLATO_DATA_MANAGEMENT_URL=$(minikube service plato-controlplane -n cx --url | sed -n 3p)
export PLATO_DATA_MANAGEMENT_URL="${PLATO_DATA_MANAGEMENT_URL}/data"
export PLATO_IDS_URL="http://plato-controlplane:8084/api/v1/ids"
export PLATO_DATA_PLANE_URL=foo
export PLATO_DATA_MANAGEMENT_API_AUTH_KEY=password
#export PLATO_BACKEND_SERVICE_BACKEND_API_URL=$(minikube service plato-backend-application -n cx --url | sed -n 2p)
export PLATO_BACKEND_SERVICE_BACKEND_API_URL=http://localhost
export PLATO_AWS_SECRET_ACCESS_KEY=platoqwerty123
export PLATO_AWS_ACCESS_KEY_ID=platoqwerty123

export SOKRATES_DATA_MANAGEMENT_URL=$(minikube service sokrates-controlplane -n cx --url | sed -n 3p)
export SOKRATES_DATA_MANAGEMENT_URL="${SOKRATES_DATA_MANAGEMENT_URL}/data"
export SOKRATES_IDS_URL="http://sokrates-controlplane:8084/api/v1/ids"
export SOKRATES_DATA_PLANE_URL=foo
export SOKRATES_DATA_MANAGEMENT_API_AUTH_KEY=password
#export SOKRATES_BACKEND_SERVICE_BACKEND_API_URL=$(minikube service sokrates-backend-application -n cx --url | sed -n 2p)
export SOKRATES_BACKEND_SERVICE_BACKEND_API_URL=http://localhost
export SOKRATES_AWS_SECRET_ACCESS_KEY=sokratesqwerty123
export SOKRATES_AWS_ACCESS_KEY_ID=sokratesqwerty123

kubectl port-forward -n cx plato-postgresql-0 5555:5432 &
kubectl port-forward -n cx sokrates-postgresql-0 6666:5432 &

export PLATO_DATABASE_URL=jdbc:postgresql://localhost:5555/edc
export PLATO_DATABASE_USER=user
export PLATO_DATABASE_PASSWORD=password

export SOKRATES_DATABASE_URL=jdbc:postgresql://localhost:6666/edc
export SOKRATES_DATABASE_USER=user
export SOKRATES_DATABASE_PASSWORD=password

./mvnw spotless:apply
./mvnw -s settings.xml -B -Pbusiness-tests -pl edc-tests test -Dtest=RunCucumberTest
