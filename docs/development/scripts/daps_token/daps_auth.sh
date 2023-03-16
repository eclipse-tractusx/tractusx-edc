#!/bin/bash

####################################################
# Update these variables before running the script #
####################################################
token_url="https://mydaps/token"
client_id="99:83:A7:17:86:FF:98:93:CE:A0:DD:A1:F1:36:FF:F6:0F:75:0A:23:keyid:99:83:A7:17:86:FF:98:93:CE:A0:DD:A1:F1:36:FA:F6:0F:75:0A:24"
# resource is later used as token audience, so it must be the IDS url of the token receiving connector
resource="https://receiving-connector/api/v1/ids/data"


base64_encode()
{
	declare input=${1:-$(</dev/stdin)}
	printf '%s' "${input}" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n'
}

hmacsha256_sign()
{
  secret="foo"
	declare input=${1:-$(</dev/stdin)}
	printf '%s' "${input}" | openssl dgst -binary -sha256 -hmac "${secret}"
}

audience="idsc:IDS_CONNECTORS_ALL"
header="{\"alg\": \"RS256\", \"typ\": \"JWT\"}"
payload="{\"iss\": \"${client_id}\",\"sub\":\"${client_id}\", \"exp\": 9999999999, \"iat\": 0, \"jti\": \"$(uuidgen)\", \"aud\": \"${audience}\"}"

header_base64=$(echo "${header}" | base64_encode)
payload_base64=$(echo "${payload}" | base64_encode)
header_payload=$(echo "${header_base64}.${payload_base64}")

pem=$( cat key.pem )
signature=$( openssl dgst -sha256 -sign <(echo -n "${pem}") <(echo -n "${header_payload}") | openssl base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n' )

jwt="${header_payload}"."${signature}"

grant_type="client_credentials"
client_assertion_type="urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
scope="idsc:IDS_CONNECTOR_ATTRIBUTES_ALL"

curl -X POST ${token_url} \
   -H "Content-Type: application/x-www-form-urlencoded" \
   -H "Accept: application/json" \
   -d "grant_type=${grant_type}&client_assertion_type=${client_assertion_type}&resource=${resource}&scope=${scope}&client_assertion=${jwt}"
