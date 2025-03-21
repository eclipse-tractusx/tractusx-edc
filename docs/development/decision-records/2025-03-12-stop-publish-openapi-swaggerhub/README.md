# Stop publish OpenAPI to SwaggerHub

## Decision

We will discontinue publishing OpenAPI documentation to SwaggerHub.

## Rationale

Since the project decided to publish OpenAPI documentation on [GitHub Pages](https://github.com/eclipse-tractusx/tractusx-edc/tree/gh-pages/openapi) 
maintaining it on SwaggerHub is obsolete. The reasoning behind choosing GitHub Pages as the preferred approach is 
detailed [here](https://github.com/eclipse-tractusx/tractusx-edc/issues/1409).

## Approach

Remove the `publish-swaggerhub.yaml` file and its usage in `publish-new-snapshot.yaml` and `release.yml`.
Replace all references to SwaggerHub OpenAPI URLs in the documentation with the new GitHub Pages links.
