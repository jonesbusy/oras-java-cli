package io.quarkiverse.oras.cli;

import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import land.oras.Platform;
import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(
    name = "resolve",
    aliases = "digest",
    description = {
      "Resolve the digest of an artifact.",
      "Aliases, unqualified-search registries and location rewrites from registries.conf"
          + " are applied before resolving."
    },
    mixinStandardHelpOptions = true)
class ResolveCommand implements Callable<Integer> {
  @CommandLine.ParentCommand Main parent;

  @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
  String reference;

  @CommandLine.Option(
      names = {"-l", "--full-reference"},
      description = "Print the final reference (after rewrites) with its digest")
  boolean fullReference;

  @CommandLine.Option(
      names = "--platform",
      paramLabel = "OS/ARCH[/VARIANT][:OS_VERSION]",
      converter = PlatformConverter.class,
      description = "Resolve the manifest matching this platform in a multi-platform index")
  Platform platform;

  @Override
  public Integer call() {
    Registry registry = parent.registry();
    ContainerRef ref = ContainerRef.parse(reference);
    String digest =
        platform == null
            ? registry.probeDescriptor(ref).getDigest()
            : resolvePlatform(registry, ref);
    System.out.println(
        fullReference ? ref.forRegistry(registry).withDigest(digest).toString() : digest);
    return 0;
  }

  private String resolvePlatform(Registry registry, ContainerRef ref) {
    boolean includeVersion = platform.osVersion() != null;
    return registry.getIndex(ref).getManifests().stream()
        .filter(m -> m.getPlatformOrNull() != null)
        .filter(m -> Platform.matches(m.getPlatform(), platform, includeVersion))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No manifest in " + reference + " matches platform " + platform))
        .getDigest();
  }
}
