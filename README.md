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

Run from the packaged Quarkus application:

```sh
java -jar target/quarkus-app/quarkus-run.jar --help
java -jar target/quarkus-app/quarkus-run.jar push --file ./artifact.tar localhost:5000/example:v1
```

Build a native executable with a compatible GraalVM or Mandrel installation:

```sh
mvn package -Dnative
```

The native executable is produced as `target/orasj-runner`; invoke it with `./target/orasj-runner --help` to see the available commands.

## Commands

The Picocli root command is `orasj` (not `oras`, to distinguish this project from the Go ORAS CLI). Quarkus names the native binary `orasj-runner` by default. Examples below use the native executable; with the packaged JAR, prepend `java -jar target/quarkus-app/quarkus-run.jar` to the command.

```sh
./target/orasj-runner push --file ./artifact.tar localhost:5000/example:v1
./target/orasj-runner pull --output ./download localhost:5000/example:v1
./target/orasj-runner manifest push --file ./manifest.json localhost:5000/example:v1
./target/orasj-runner manifest fetch --output ./manifest.json localhost:5000/example:v1
./target/orasj-runner manifest get localhost:5000/example:v1
./target/orasj-runner manifest delete localhost:5000/example:v1
```

Use `--help` on `./target/orasj-runner` or any subcommand for syntax. Registry configuration and authentication use the Quarkus ORAS extension; see its [documentation](https://github.com/quarkiverse/quarkus-oras) for supported settings. This initial CLI intentionally does not implement all options and features of the Go ORAS CLI.
