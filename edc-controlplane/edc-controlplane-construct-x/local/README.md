# Construct-X Local Docker Testbed


This `docker-compose.yaml` provides you a minimal environment for testing a pair of construct-x-edc's against each other.

It will start the following containers on your local machine: 

- one instance of an issuer-service 
- two instances of identity-hubs (for consumer and provider each)
- two instances of our current Construct-X controlplanes (as above)
- two instances of our current Construct-X dataplanes (as above)
- one Postgres DB (which is, for the sake of saving you resources on your local machine, shared by all aforementioned containers)
- one HashiCorp Vault (also shared)
- one (very short-lived) init-container, whose only job is to create edr-signing keys for the dataplanes, store them on the vault and then to terminate



Before anything else, please make sure you have the docker images for con-x-controlplane-postgresql-hashicorp-vault in your local docker repository, see [here](../con-x-controlplane-postgresql-hashicorp-vault/README.md) and [here](../../../edc-dataplane/edc-dataplane-construct-x/con-x-dataplane-postgresql-hashicorp-vault/README.md). 

Beyond that, you need to obtain the docker images needed to run the identity hub and the issuer services. In order to do so, please check out this [repository](https://github.com/FraunhoferISST/dev-identity-services) and clone it onto your local machine. The upper section of this [README](https://github.com/FraunhoferISST/dev-identity-services/blob/main/runtimes/dev/README.md) informs 
you about the steps necessary to create the docker images. 


### Start the environment
Now we are ready to start the environment. For that, please run (from the `local` folder of this README file!)

Optionally (from the project root)
``` 
cd edc-controlplane/edc-controlplane-construct-x/local
```
Then:

```
docker compose up
```

Then please use the attached Bruno collection. ([Bruno](https://www.usebruno.com/) is a convenient http client, which
you should install, if you haven't already). In order to this, choose the "open" (not "import") option item in the Bruno UI and select the respective [folder](bruno/con-x-local-test). If prompted by a popup if you want to choose "Safe Mode" or "Developer Mode", then just select "Safe Mode". 

After that, please use the icon in the upper right corner
of the Bruno GUI to select the `local-con-x-env` environment (which stores the variables that the requests will need).

In that collection you should first run the requests of the `identities` folder. These request can also be safely executed from top to bottom in batch mode, especially if you are more interested in doing the "normal" `transactions` between the two edc's. Though can find more details one the `identities` folder below, if you're interested. 
After you have completed all required steps, the provider and the consumer identity are onboarded in your own dataspace
and ready to interact with each other. 

Now you are ready to perform a simple contract negotiation and data transfer between these two actors. 

Be sure to also read the documentation that is attached to the folders in the Bruno collection. You may also want to
check the pre- and postrequest scripts of many requests, because they may give you further insights. You can find the docs content 
in the Bruno GUI on the far right side of the menu bar, where the "Headers", "Auth", "Vars", etc. items are located. You may have to unwrap the 
menu using the ">>" icon. 

In a nutshell, we are presenting the following workflows here:

### Create an issuer-participant (`Prepare Issuer` folder)

The issuer-participant will act as the dataspaces' trusted issuer. This issuer is mandated to sign and hand
out verifiable credentials, which the members of the dataspace can use to prove their membership (or potentially other
relevant properties of themselves) to other partners in the same dataspace. After the registration of the issuer we are
also providing the basic definition of the credential that shall be issued. And we also need register the expected (
user-) members of the dataspace at the issuer service as holders at the trusted issuer's participant context. 

Assuming that the majority of users does not (at least in the beginning) want to get into the details of designing credentials, you can most probably skip the `Optionalconfig` folder (though it does no harm, if you run these requests, as long as you don't edit these requests in any way). If you're interested in the (rather advanced) topic of using customized credential subject contents in your credentials, you can take a further look at this [README](https://github.com/FraunhoferISST/dev-identity-services/blob/main/runtimes/dev/README.md). 

Pretty much the same goes for the `createAttestation` and the `createCredentialDef` requests. If you're an average user, you just need to know that 
they are a technical necessity at this point and you just to need to run them to ensure that rest of the requests in this collection can be executed properly. 

### Create a consumer and a provider identity

Somewhat similar to the creation of the issuer, we will now create a consumer and a provider identity on their
respective wallets (i.e. identity hubs). After the creation, we receive an api token and a sts secret from the identity
hub. The sts secret is essential for operating the edc. So we need to store that in the hashicorp vault under a given
secret alias, so that the edc can use it later. Also, we can take a look at the DID document, that was generated on the
identity hubs. And we need to tell our identity hub, that we want to request a membership token from the trusted issuer.

When this is done, we can have a look at the credentials, that the issuer hopefully delivered to consumer and provider
respectively. And we can also do some kind of a simulated DCP flow with the just created credentials. Please see the
documentation in the Bruno collection if you are interested in learning some more details (though that is directed at
the more advanced members of the audience here, beginners can definitely skip that part).

#### Known issue / validating the identity setup
In rare cases (chances seem to be below 0.5 %) there is currently a possibility, that one of the `CreateParticipant` calls may (silently) fail. We assume that this is something that needs to be fixed on the upstream EDC identity hub project. See this [issue](https://github.com/eclipse-edc/IdentityHub/issues/913) for details. If you are unfortunate enough encounter this bug, you should notice that one of the calls in the `InspectOutcome` folder shows an empty response and that (at least) the last call of `Simulated DCP Flow` shows a negative test result. 

If one encounter one these symptoms, we would suggest that you cleanly restart the entire docker compose (see below). Chances 
are near 99 % that on your next attempt, you won't encounter this problem again. 

Also, if you're interested in some more details 


### Do a transaction between provider and consumer

Finally, we are ready now to do a more or less 'normal' DSP/DCP protocol backed transaction between the consumer and the
provider. I.e. firstly we need the provider to prepare a data asset, which the consumer can negotiate with him for. Then
the consumer
can discover this asset via a catalog request towards the provider edc, initiate a negotiation and a transfer. If you
are interested
in a more detailed explanation of these interactions, please see
the [EDC Samples](https://github.com/eclipse-edc/Samples/tree/main/transfer).

NOTE: The requests in the `transactions/consumer` folder are definitely not suited for being run in batch mode. Instead, 
especially before you run the `InitiateNegotiation` or the `Get EDR` requests, it is strongly recommended that you make sure 
that a healthy timespan (like 2 seconds) lies between them and the respective previous request. This is because the 
EDC controlplane needs some time to complete the negotiation process respectively retrieve the EDR token from the provider side. 


### Clean restart
When you're done testing and want to end your session (using 'CTRL-C' on the terminal, where you started docker
compose),
you may want to run

```
docker compose down -v
```

This will delete the data from your previous session and ensure, that the next time, you are starting this, you will
have no data remnants in your containers, which may cause confusion or conflicts, when you start the docker-compose.yaml
and the requests of the Bruno collection later again. 
