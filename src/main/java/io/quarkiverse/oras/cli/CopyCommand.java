package io.quarkiverse.oras.cli;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import land.oras.CopyUtils;
import land.oras.Platform;
import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(
    name = "cp",
    aliases = "copy",
    description = "Copy an artifact from one reference to another",
    mixinStandardHelpOptions = true)
class CopyCommand implements Callable<Integer> {
  @CommandLine.ParentCommand Main parent;

  @CommandLine.Parameters(index = "0", paramLabel = "SOURCE")
  String source;

  @CommandLine.Parameters(index = "1", paramLabel = "TARGET")
  String target;

  @CommandLine.Option(
      names = {"-r", "--recursive"},
      description = "Also copy the referrers of the artifact")
  boolean recursive;

  @CommandLine.Option(
      names = "--platform",
      split = ",",
      paramLabel = "OS/ARCH[/VARIANT][:OS_VERSION]",
      converter = PlatformConverter.class,
      description = "Only copy the manifests of an index matching these platforms")
  List<Platform> platforms;

  @Override
  public Integer call() {
    Registry registry = parent.registry();
    ContainerRef from = ContainerRef.parse(source);
    ContainerRef to = ContainerRef.parse(target);
    CopyUtils.CopyOptions options =
        recursive ? CopyUtils.CopyOptions.deep() : CopyUtils.CopyOptions.shallow();
    if (platforms != null && !platforms.isEmpty()) {
      options = options.withPlatformFilter(Set.copyOf(platforms));
    }
    CopyUtils.copy(registry, from, registry, to, options);
    System.out.println("Copied " + source + " => " + target);
    System.out.println("Digest: " + registry.probeDescriptor(to).getDigest());
    return 0;
  }
}
