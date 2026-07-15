# `laaws-configuration-service` Release Notes

## 2.10.0 (LOCKSS 2.0.91-beta2)

### API Changes

* Relabel REST endpoint paths: `normalizeUrl` to `normalizeurl` and `mimeType` to `mediatypes`; update associated code references, tests, and Swagger specifications
* Fix `GET /austatuses/{auid}` to return 404 for non-existent AUID
* Add pagination support for AU configurations retrieval (issue #569)

### OpenAPI / Code Generation

* Define additional schemas (`AuStateBean`, ancillary enums, `PlatformConfigurationWsResult`) without requiring string-serialized endpoints, to facilitate Python et al. code generation
* Bring definition of `PlatformConfigurationWsResult` in line with `lockss-util-entities`
* Fix nested schema definition
* Patch OpenAPI spec for swagger-codegen `spring` and `python` targets

### Testing

* Add missing `@Test` annotations in multiple test files
* Add `MockServer` integration for testing previously external HTTP requests to example.org

### Configuration

* Adopt 2.0-beta2 port conventions
* Define `build.java.projectAbbr`
* Add `NETWORK_POLICY_MANAGER_DESC`

### Fixes

* Prevent bug when a class name is a prefix of a longer class


## Changes Since 2.0.5.0
* Remove  Travis CI
* Move to OpenAPI 3
* Move to Java 17
* Move to springdoc
* Move TypedUserAccount into lockss-core

### Api Changes
  * Add migration config section
  * Moved REST endpoints needed for SOAP to under /ws
  * Add support for migration forwarding
  * Accept X-Lockss-Request-Cookie header to pass with client update requests
  * Changed PUT on /users/{username} to PATCH
  * Add support for user account configuration
  * Add new endpoint for usernames; GET and POST on /users is now symmetric
  * Remove support for bulk retrieval of user accounts across REST, StateManager, and StateStore APIs and their implementations

### Serialization
 * Use ObjectWriter from UserAccount for JSON serialization
  * Configure JSON serializer to serialize only fields of UserAccount in getUserAccount
  * Remove TypedUserAccount wrapper; use @JsonTypeInfo and @JsonSubTypes support in Jackson
  * Configure serialization parameters for ObjectMapper used to send UserAccounts

### Error Handling
  * Include message in error responses
  * Enforce AUID in path matches AUID in the submitted AU configuration
  * Return a 404 if attempting to update a non-existent UserAccount
  * Respond with 400 Bad Request if the POST request is missing a JSON body
  * Handle IOException thrown by RestConfigClient calls
  * HttpStatusCode refactor


## Changes Since 2.0.4.1

*   Switched to a 3-part version numbering scheme.

## 2.0.4.1

### Security

*   Out of an abundance of caution, re-released 2.0.4.0 with Jackson-Databind 2.9.10.8 (CVE-2021-20190).

## 2.0.4.0

### Features

*   ...

### Fixes

*   ...

## 2.0.3.0

### Features

*   REST services authenticate, clients provide credentials.
*   Improved startup coordination and ready waiting of all services and databases.
*   Improved coordination of initial plugin registry crawls.

### Fixes

*   Better handling of missing optional files.
