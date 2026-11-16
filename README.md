# ORAS Java CLI (`orasj`)

`orasj` is an experimental Java command-line client for OCI registries. It uses [Quarkus](https://quarkus.io/), [Quarkus ORAS](https://github.com/quarkiverse/quarkus-oras), and the ORAS Java SDK. The project is a work in progress; it currently implements a focused subset of the ORAS CLI.

## Requirements

- JDK 25
- Maven 3.9+
- The Quarkus ORAS extension version configured in the root `pom.xml` (`0.8.3-SNAPSHOT` at present)

The extension is currently a snapshot dependency. Install a matching checkout of [quarkus-oras](https://github.com/quarkiverse/quarkus-oras) in your local Maven repository before building:

```sh
cd /path/to/quarkus-oras
mvn install
cd /path/to/oras-java-cli
mvn verify
```

## Run

Build a self-contained executable JAR and run it with Java or JBang:

```sh
mvn package
java -jar target/orasj.jar --help
jbang target/orasj.jar --help
jbang target/orasj.jar push --file ./artifact.tar localhost:5000/example:v1
```

The JAR is packaged as a Quarkus uber-JAR, so it does not depend on a separate `quarkus-app` directory. The Quarkus banner is disabled and the default log level is `ERROR`; stdout is reserved for command output.

Build a Linux native executable using Quarkus's Mandrel builder container (Docker or Podman required):

```sh
mvn package -Dnative -Dquarkus.native.container-build=true
```

The native executable is produced as `target/orasj`; invoke it with `./target/orasj --help` to see the available commands. The native profile disables the JAR package.

## Commands

The Picocli root command is `orasj` (not `oras`, to distinguish this project from the Go ORAS CLI). The native binary is named `orasj`. Examples below use the native executable; with the JAR, prepend `java -jar target/orasj.jar` to the command (or use `jbang target/orasj.jar`).

```sh
./target/orasj push --file ./artifact.tar localhost:5000/example:v1
./target/orasj pull --output ./download localhost:5000/example:v1
./target/orasj manifest push --file ./manifest.json localhost:5000/example:v1
./target/orasj manifest fetch --output ./manifest.json localhost:5000/example:v1
./target/orasj manifest get localhost:5000/example:v1
./target/orasj manifest delete localhost:5000/example:v1
```

Use `--help` on `./target/orasj` or any subcommand for syntax. Registry configuration and authentication use the Quarkus ORAS extension; see its [documentation](https://github.com/quarkiverse/quarkus-oras) for supported settings. This initial CLI intentionally does not implement all options and features of the Go ORAS CLI.

## Releases

Push a semantic-version tag such as `1.2.3` or `v1.2.3` to trigger a GitHub Release. Each release includes a Linux x86-64 native executable, the cross-platform JAR, and SHA-256 checksums. The workflow does not publish to Maven Central.
