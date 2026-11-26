package io.quarkiverse.oras.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.LocalPath;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.Platform;
import land.oras.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the CLI against the zot registry started by the Quarkus ORAS dev service on localhost:15000.
 *
 * <p>Surefire points HOME at {@code src/test/resources/home}, whose registries.conf marks the
 * registry insecure, rewrites {@code registry.example.com/demo} to {@code
 * localhost:15000/rewritten} and aliases {@code short-app} to {@code
 * localhost:15000/rewritten/app}.
 */
@QuarkusMainTest
class OrasCliZotTest {
  private static final String ZOT = "localhost:15000";
  private static final String SBOM_TYPE = "application/vnd.example.sbom";

  // Builds fixtures the CLI cannot create (indexes, referrers) and reads results back.
  private final Registry sdk = Registry.builder().insecure().build();

  @TempDir Path tempDir;

  @Test
  void resolveAppliesPrefixRewrite(QuarkusMainLauncher launcher) throws Exception {
    String repo = unique("resolve");
    String digest = push(launcher, ZOT + "/rewritten/" + repo + ":v1", "hello");

    assertEquals(digest, run(launcher, "resolve", "registry.example.com/demo/" + repo + ":v1"));
    assertEquals(digest, run(launcher, "digest", "registry.example.com/demo/" + repo + ":v1"));
    assertEquals(
        ZOT + "/rewritten/" + repo + "@" + digest,
        run(launcher, "resolve", "-l", "registry.example.com/demo/" + repo + ":v1"));
  }

  @Test
  void resolveAppliesAlias(QuarkusMainLauncher launcher) throws Exception {
    String tag = unique("alias");
    String digest = push(launcher, ZOT + "/rewritten/app:" + tag, "alias");

    assertEquals(
        ZOT + "/rewritten/app@" + digest,
        run(launcher, "resolve", "--full-reference", "short-app:" + tag));
  }

  @Test
  void resolveUsesUnqualifiedSearchRegistry(QuarkusMainLauncher launcher) throws Exception {
    String repo = unique("unqualified");
    String digest = push(launcher, ZOT + "/team/" + repo + ":v1", "unqualified");

    assertEquals(
        ZOT + "/team/" + repo + "@" + digest,
        run(launcher, "resolve", "-l", "team/" + repo + ":v1"));
  }

  @Test
  void resolveSelectsPlatformFromIndex(QuarkusMainLauncher launcher) throws Exception {
    String repo = ZOT + "/" + unique("multi");
    MultiPlatform index = pushIndex(repo);

    assertEquals(index.indexDigest(), run(launcher, "resolve", repo + ":multi"));
    assertEquals(
        index.amd64Digest(),
        run(launcher, "resolve", "--platform", "linux/amd64", repo + ":multi"));
    assertEquals(
        index.arm64Digest(),
        run(launcher, "resolve", "--platform", "linux/arm64/v8", repo + ":multi"));

    LaunchResult missing =
        launcher.launch("resolve", "--platform", "windows/amd64", repo + ":multi");
    assertEquals(1, missing.exitCode());
    assertTrue(missing.getErrorOutput().contains("matches platform"), missing.getErrorOutput());
  }

  @Test
  void resolveReportsMissingReference(QuarkusMainLauncher launcher) {
    LaunchResult result = launcher.launch("resolve", ZOT + "/" + unique("missing") + ":v1");

    assertEquals(1, result.exitCode());
    assertTrue(result.getErrorOutput().startsWith("Error: "), result.getErrorOutput());
  }

  @Test
  void repoListsRepositoriesAndTags(QuarkusMainLauncher launcher) throws Exception {
    String repo = unique("tags");
    push(launcher, ZOT + "/" + repo + ":b", "b");
    push(launcher, ZOT + "/" + repo + ":a", "a");

    assertEquals("a\nb", run(launcher, "repo", "tags", ZOT + "/" + repo));
    assertTrue(run(launcher, "repo", "ls", ZOT).lines().anyMatch(repo::equals));
  }

  @Test
  void discoverPrintsReferrerTree(QuarkusMainLauncher launcher) throws Exception {
    String ref = ZOT + "/" + unique("discover") + ":v1";
    String digest = push(launcher, ref, "subject");
    String sbom = attachSbom(launcher, ref);
    String subject = ContainerRef.parse(ref).withDigest(digest).toString();

    assertEquals(
        String.join("\n", subject, "└── " + SBOM_TYPE, "    └── " + sbom),
        run(launcher, "discover", ref));
    assertEquals(subject, run(launcher, "discover", "--artifact-type", "other/type", ref));
  }

  @Test
  void copyCopiesArtifactAndReferrers(QuarkusMainLauncher launcher) throws Exception {
    String source = ZOT + "/" + unique("source") + ":v1";
    String digest = push(launcher, source, "copy me");
    String sbom = attachSbom(launcher, source);
    String shallow = ZOT + "/" + unique("shallow") + ":v1";
    String deep = ZOT + "/" + unique("deep") + ":v1";

    assertEquals(
        "Copied " + source + " => " + shallow + "\nDigest: " + digest,
        run(launcher, "cp", source, shallow));
    assertTrue(
        sdk.getReferrers(ContainerRef.parse(shallow).withDigest(digest), null)
            .getManifests()
            .isEmpty());

    run(launcher, "copy", "-r", source, deep);
    assertEquals(
        List.of(sbom),
        sdk.getReferrers(ContainerRef.parse(deep).withDigest(digest), null).getManifests().stream()
            .map(ManifestDescriptor::getDigest)
            .toList());
  }

  @Test
  void copyFiltersIndexByPlatform(QuarkusMainLauncher launcher) throws Exception {
    String repo = ZOT + "/" + unique("multi-copy");
    MultiPlatform index = pushIndex(repo);
    String target = ZOT + "/" + unique("amd64-only") + ":v1";

    run(launcher, "cp", "--platform", "linux/amd64", repo + ":multi", target);

    assertEquals(
        index.amd64Digest(), run(launcher, "resolve", "--platform", "linux/amd64", target));
    assertNotEquals(
        0, launcher.launch("resolve", "--platform", "linux/arm64/v8", target).exitCode());
  }

  @Test
  void blobFetch(QuarkusMainLauncher launcher) throws Exception {
    String repo = ZOT + "/" + unique("blob");
    push(launcher, repo + ":v1", "blob content");
    String layer = sdk.getManifest(ContainerRef.parse(repo + ":v1")).getLayers().get(0).getDigest();
    String blob = repo + "@" + layer;
    Path output = tempDir.resolve("fetched");

    run(launcher, "blob", "fetch", "--output", output.toString(), blob);
    assertEquals("blob content", Files.readString(output));
    assertEquals("blob content", run(launcher, "blob", "fetch", "--output", "-", blob));
  }

  @Test
  void blobDelete(QuarkusMainLauncher launcher) {
    // zot refuses to delete blobs still referenced by a manifest, so delete an unreferenced one.
    String repo = ZOT + "/" + unique("blob-delete");
    String digest =
        sdk.pushBlob(ContainerRef.parse(repo), "orphan".getBytes(StandardCharsets.UTF_8))
            .getDigest();
    String blob = repo + "@" + digest;

    assertEquals("Deleted " + blob, run(launcher, "blob", "delete", blob));
    assertEquals(1, launcher.launch("blob", "fetch", "--output", "-", blob).exitCode());
  }

  @Test
  void blobRequiresDigest(QuarkusMainLauncher launcher) {
    LaunchResult result = launcher.launch("blob", "delete", ZOT + "/blob:v1");

    assertNotEquals(0, result.exitCode());
    assertTrue(result.getErrorOutput().contains("must include a digest"), result.getErrorOutput());
  }

  private static String unique(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private static String run(QuarkusMainLauncher launcher, String... args) {
    LaunchResult result = launcher.launch(args);
    assertEquals(
        0, result.exitCode(), () -> String.join(" ", args) + ": " + result.getErrorOutput());
    return result.getOutput();
  }

  private String push(QuarkusMainLauncher launcher, String ref, String content) throws Exception {
    Path file = tempDir.resolve(UUID.randomUUID() + ".txt");
    Files.writeString(file, content);
    String output = run(launcher, "push", "--file", file.toString(), ref);
    assertTrue(output.startsWith("Pushed sha256:"), output);
    return output.substring("Pushed ".length());
  }

  /** Pushes an SBOM referrer of {@code ref} with the CLI and returns its digest. */
  private String attachSbom(QuarkusMainLauncher launcher, String ref) throws Exception {
    Manifest subject = sdk.getManifest(ContainerRef.parse(ref));
    Manifest sbom =
        Manifest.empty()
            .withArtifactType(ArtifactType.from(SBOM_TYPE))
            .withSubject(subject.getDescriptor().toSubject());
    Path file = tempDir.resolve(UUID.randomUUID() + ".json");
    Files.writeString(file, sbom.toJson());
    String sbomRef = ContainerRef.parse(ref).withTag(unique("sbom")).toString();
    return run(launcher, "manifest", "push", "--file", file.toString(), sbomRef)
        .substring("Pushed ".length());
  }

  private MultiPlatform pushIndex(String repo) throws Exception {
    Path amd64File = Files.writeString(tempDir.resolve("amd64.txt"), "amd64");
    Path arm64File = Files.writeString(tempDir.resolve("arm64.txt"), "arm64");
    Manifest amd64 = sdk.pushArtifact(ContainerRef.parse(repo + ":amd64"), LocalPath.of(amd64File));
    Manifest arm64 = sdk.pushArtifact(ContainerRef.parse(repo + ":arm64"), LocalPath.of(arm64File));
    Index index =
        sdk.pushIndex(
            ContainerRef.parse(repo + ":multi"),
            Index.fromManifests(
                List.of(
                    ManifestDescriptor.of(amd64).withPlatform(Platform.linuxAmd64()),
                    ManifestDescriptor.of(arm64).withPlatform(Platform.linuxArm64V8()))));
    return new MultiPlatform(
        index.getDescriptor().getDigest(), amd64.getDigest(), arm64.getDigest());
  }

  private record MultiPlatform(String indexDigest, String amd64Digest, String arm64Digest) {}
}
