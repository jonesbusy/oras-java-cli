# ORAS Java CLI

A Quarkus command-line client for OCI registries, powered by [Quarkus ORAS](https://github.com/quarkiverse/quarkus-oras) and the ORAS Java SDK.

## Build

Install the sibling Quarkus ORAS extension (currently `0.8.3-SNAPSHOT`) into your local Maven repository, then build this project:

```shell
cd /home/valentin/git/github/quarkus-oras && mvn install
cd -
mvn package
```

The executable JAR is `java-oras-cli/target/quarkus-app/`. Run it with:

```shell
java -jar java-oras-cli/target/quarkus-app/quarkus-run.jar --help
```

Native build (requires a GraalVM or Mandrel native-image toolchain):

```shell
mvn package -Dnative
```

## Commands

The default registry is configured for resolving references to any registry. Supply credentials and registry settings through the extension's `quarkus.oras.registries.default.*` configuration or the supported ORAS auth configuration.

```shell
oras push --file ./artifact.tar localhost:5000/example:v1
oras pull --output ./download localhost:5000/example:v1
oras manifest push --file ./manifest.json localhost:5000/example:v1
oras manifest fetch --output ./manifest.json localhost:5000/example:v1
oras manifest get localhost:5000/example:v1
oras manifest delete localhost:5000/example:v1
```

For the packaged application use the same commands after the JAR path. `--help` is available on the root and each subcommand.
