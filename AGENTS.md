# Repository guidance

## Project

This repository is an experimental, single-module Java ORAS CLI built with Quarkus Picocli and the Quarkus ORAS extension. The root `pom.xml` is the application build. The command root is `orasj`; do not rename it to `oras`, which is the Go CLI's command name. The supported commands are `push`, `pull`, `manifest`, `resolve`, `discover`, `cp`, `repo`, and `blob`; do not add others unless asked.

## Build and verification

- Use JDK 25 and the Quarkus version and extension versions in the root `pom.xml`.
- `quarkus-oras` is currently a snapshot dependency. If Maven cannot resolve it, build/install the matching sibling extension checkout before building this repository.
- Run `mvn spotless:apply` after Java or POM edits, then `mvn verify` (Docker or Podman is required: tests use a zot dev service on `localhost:15000`). For CLI changes, also verify root and nested `--help` output from the packaged application.
- Native image builds require a compatible GraalVM or Mandrel toolchain; use `mvn package -Dnative` when that toolchain is available.
- Update `README.md` when command names, options, build steps, or supported scope changes.

## Implementation

- Keep Quarkus command integration in Picocli (`@TopCommand` and `@CommandLine.Command`) and use the extension's existing registry APIs/configuration instead of creating parallel registry clients.
- Keep CLI code under `io.quarkiverse.oras.cli`; do not place application classes in the SDK's `land.oras` package.
- Keep credentials out of source and documentation examples; use the extension's supported authentication configuration.
- This project is intentionally not a complete port of the Go ORAS CLI. Avoid adding unsupported flags or commands speculatively.
