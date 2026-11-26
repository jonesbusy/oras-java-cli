package io.quarkiverse.oras.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(
    name = "manifest",
    description = "Manage manifests",
    mixinStandardHelpOptions = true,
    subcommands = {
      ManifestCommand.Push.class,
      ManifestCommand.Fetch.class,
      ManifestCommand.Get.class,
      ManifestCommand.Delete.class
    })
class ManifestCommand implements Runnable {
  @CommandLine.ParentCommand Main parent;

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  abstract static class ManifestReference implements Callable<Integer> {
    @CommandLine.ParentCommand ManifestCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
    String reference;

    ContainerRef ref() {
      return ContainerRef.parse(reference);
    }

    Registry registry() {
      return parent.parent.registry();
    }
  }

  @CommandLine.Command(
      name = "push",
      description = "Push a manifest",
      mixinStandardHelpOptions = true)
  static class Push extends ManifestReference {
    @CommandLine.Option(names = "--file", required = true, paramLabel = "PATH")
    Path file;

    @Override
    public Integer call() throws Exception {
      var manifest = registry().pushManifest(ref(), Manifest.fromPath(file));
      System.out.println("Pushed " + manifest.getDigest());
      return 0;
    }
  }

  @CommandLine.Command(
      name = "fetch",
      description = "Fetch a manifest to a file",
      mixinStandardHelpOptions = true)
  static class Fetch extends ManifestReference {
    @CommandLine.Option(names = "--output", required = true, paramLabel = "PATH")
    Path output;

    @Override
    public Integer call() throws Exception {
      Files.writeString(output, registry().getDescriptor(ref()).getJson());
      return 0;
    }
  }

  @CommandLine.Command(
      name = "get",
      description = "Print a manifest",
      mixinStandardHelpOptions = true)
  static class Get extends ManifestReference {
    @Override
    public Integer call() {
      System.out.println(registry().getDescriptor(ref()).getJson());
      return 0;
    }
  }

  @CommandLine.Command(
      name = "delete",
      description = "Delete a manifest",
      mixinStandardHelpOptions = true)
  static class Delete extends ManifestReference {
    @Override
    public Integer call() {
      registry().deleteManifest(ref());
      return 0;
    }
  }
}
