# Divorce Case Maintenance Service [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This is a case maintenance service. This service facilitates all the communication between Core Case Data and the
Divorce system.

## Setup

**Prerequisites**

- [JDK 11](https://openjdk.java.net/)
- [Docker](https://www.docker.com)
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)


**Building**

The project uses [Gradle](https://gradle.org) as a build tool but you don't have to install it locally since there is a
`./gradlew` wrapper script.

To build project please execute the following command:

```bash
    ./gradlew build
```

If the build failed because of tests try running the following

```bash
    ./gradlew build -x test
```
This command will run the build task but it will exclude the test task

**Running**

Running the service locally requires several APIs in the AAT environment. You will need an active VPN, to have permission to read the nfdiv-aat vault and to be logged into azure CLI.

```
./gradlew bootRun
```

##Testing

To run all unit tests please execute following command:

```bash
    ./gradlew test
```

**Coding style tests**

To run all checks (including unit tests) please execute following command:

```bash
    ./gradlew check
```
**Mutation tests**

To run all mutation tests execute the following command:

```
/gradlew pitest

```

**Integration tests**

To run all integration tests locally:

Start the app:

```
./gradlew bootRun
```

Run the tests:

`./gradlew functional`

##Developing
**API documentation**

API documentation is provided with Swagger:
 - `http://localhost:4010/swagger-ui.html` - UI to interact with the API resources

The `documentation.swagger.enabled` property should be 'true' to enable Swagger.

**Versioning**

We use [SemVer](http://semver.org/) for versioning.
For the versions available, see the tags on this repository.

**Standard API**

We follow [RESTful API standards](https://hmcts.github.io/restful-api-standards/).

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

