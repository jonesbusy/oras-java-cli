# ORAS Java CLI (`orasj`)

`orasj` is an experimental Java command-line client for OCI registries. It uses [Quarkus](https://quarkus.io/), [Quarkus ORAS](https://github.com/quarkiverse/quarkus-oras), and the ORAS Java SDK. The project is a work in progress; it currently implements a focused subset of the ORAS CLI.

## Requirements

- JDK 25
- Maven 3.9+
- The Quarkus ORAS extension version configured in the root `pom.xml` (`0.8.3-SNAPSHOT` at present)

The project defaults to `0.0.0-SNAPSHOT`; release builds derive their version from the semantic-version tag, so no POM edit is needed. The extension is currently a snapshot dependency. Install a matching checkout of [quarkus-oras](https://github.com/quarkiverse/quarkus-oras) in your local Maven repository before building:

```sh
cd /path/to/quarkus-oras
mvn install
cd /path/to/oras-java-cli
mvn verify
```

The tests run the CLI against a [zot](https://zotregistry.dev/) registry started by the Quarkus ORAS dev service on `localhost:15000`, so Docker or Podman is required. Surefire sets `HOME` to `src/test/resources/home`, whose `registries.conf` drives the rewrite tests; your own registry configuration is not used.

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
./target/orasj resolve localhost:5000/example:v1
./target/orasj resolve --full-reference --platform linux/arm64 localhost:5000/multi:v1
./target/orasj discover --artifact-type application/vnd.example.sbom localhost:5000/example:v1
./target/orasj cp --recursive localhost:5000/example:v1 localhost:5000/copy:v1
./target/orasj repo ls localhost:5000
./target/orasj repo tags localhost:5000/example
./target/orasj blob fetch --output ./layer.tar localhost:5000/example@sha256:...
./target/orasj blob delete localhost:5000/example@sha256:...
```

| Command | Description |
| --- | --- |
| `push`, `pull` | Push files as an artifact, or pull an artifact's files into a directory. |
| `manifest push\|fetch\|get\|delete` | Manage a single manifest. |
| `resolve` (alias `digest`) | Print the digest of a reference. `-l`/`--full-reference` prints the final reference after `registries.conf` rewrites; `--platform os/arch[/variant][:os_version]` selects a manifest from a multi-platform index. |
| `discover` | Print the referrers of a manifest as a tree, optionally filtered with `--artifact-type`. |
| `cp` (alias `copy`) | Copy an artifact. `-r`/`--recursive` also copies referrers; `--platform` (comma-separated) keeps only matching manifests of an index. |
| `repo ls`, `repo tags` | List the repositories of a registry, or the tags of a repository. |
| `blob fetch`, `blob delete` | Fetch a blob to a file (`--output -` for stdout), or delete it. The reference must include a digest. |

### Reference rewrites

References are resolved with the ORAS Java SDK's `registries.conf` support (`/etc/containers/registries.conf`, `$HOME/.config/containers/registries.conf` and their `.d` drop-in directories): aliases, `unqualified-search-registries`, `[[registry]]` prefix-to-location rewrites and `insecure` locations. `resolve --full-reference` shows where a reference ends up. For example, with:

```toml
unqualified-search-registries = ["localhost:5000"]

[[registry]]
prefix = "registry.example.com/demo"
location = "localhost:5000/rewritten"

[aliases]
"short-app" = "localhost:5000/rewritten/app"
```

```sh
./target/orasj resolve -l registry.example.com/demo/app:v1   # localhost:5000/rewritten/app@sha256:...
./target/orasj resolve -l short-app:v1                       # localhost:5000/rewritten/app@sha256:...
./target/orasj resolve -l team/app:v1                        # localhost:5000/team/app@sha256:...
```

Use `--help` on `./target/orasj` or any subcommand for syntax. Registry configuration and authentication use the Quarkus ORAS extension; see its [documentation](https://github.com/quarkiverse/quarkus-oras) for supported settings. This initial CLI intentionally does not implement all options and features of the Go ORAS CLI.

## Install

Native executables for Linux (x86-64 and ARM64) are published through the [`jonesbusy/homebrew-tap`](https://github.com/jonesbusy/homebrew-tap) Homebrew tap for Homebrew on Linux. macOS binaries are not published yet; on macOS, use the JAR.

Install with Homebrew:

```sh
brew install jonesbusy/tap/orasj
orasj --version
```

The native archives and the cross-platform JAR can also be downloaded from [GitHub Releases](https://github.com/jonesbusy/oras-java-cli/releases).

## Releases

Push a semantic-version tag such as `1.2.3` or `v1.2.3` to trigger the release workflow. It builds and tests the JAR, builds native executables on Linux x86-64 and Linux ARM64, and then runs [JReleaser](https://jreleaser.org/) (configured in `jreleaser.yml`) to:

- create the GitHub Release with GitHub-generated notes, the native `.zip` archives, the JAR and `checksums_sha256.txt`;
- update the `orasj` formula in `jonesbusy/homebrew-tap`.

Publishing the formula requires a `HOMEBREW_TAP_TOKEN` repository secret: a fine-grained personal access token with `Contents: Read and write` on `jonesbusy/homebrew-tap` only. The release does not publish to Maven Central.

To preview the generated formula locally, place artifacts named as in `jreleaser.yml` under `artifacts/` (the archives must be real `.zip` files) and run:

```sh
RELEASE_TAG=1.2.3 JRELEASER_PROJECT_VERSION=1.2.3 JRELEASER_GITHUB_TOKEN=dummy \
  jbang org.jreleaser:jreleaser:1.27.0 prepare
cat out/jreleaser/prepare/orasj/brew/Formula/orasj.rb
```

## License

Apache License 2.0. See [LICENSE](LICENSE).
