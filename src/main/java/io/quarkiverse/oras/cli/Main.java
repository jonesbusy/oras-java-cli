package io.quarkiverse.oras.cli;

import io.quarkiverse.oras.runtime.OrasRegistry;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import land.oras.LocalPath;
import land.oras.Manifest;
import land.oras.Registry;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "orasj",
    description = "Manage OCI artifacts",
    mixinStandardHelpOptions = true,
    versionProvider = Main.VersionProvider.class,
    subcommands = {Main.Push.class, Main.Pull.class, Main.ManifestCommand.class})
public class Main implements Runnable {
  // Resolved on first use so --help and --version work without reading registry credentials.
  @Inject
  @OrasRegistry("foobar")
  Instance<Registry> registry;

  Registry registry() {
    return registry.get();
  }

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  static class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
      return new String[] {
        "orasj "
            + ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.version", String.class)
                .orElse("unknown")
      };
    }
  }

  @CommandLine.Command(
      name = "push",
      description = "Push an artifact",
      mixinStandardHelpOptions = true)
  static class Push implements Callable<Integer> {
    @CommandLine.ParentCommand Main parent;

    @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
    String reference;

    @CommandLine.Option(names = "--file", required = true, paramLabel = "PATH")
    Path file;

    public Integer call() {
      var manifest =
          parent.registry().pushArtifact(ContainerRef.parse(reference), LocalPath.of(file));
      System.out.println("Pushed " + manifest.getDigest());
      return 0;
    }
  }

  @CommandLine.Command(
      name = "pull",
      description = "Pull an artifact",
      mixinStandardHelpOptions = true)
  static class Pull implements Callable<Integer> {
    @CommandLine.ParentCommand Main parent;

    @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
    String reference;

    @CommandLine.Option(names = "--output", defaultValue = ".", paramLabel = "DIR")
    Path output;

    public Integer call() throws Exception {
      Files.createDirectories(output);
      parent.registry().pullArtifact(ContainerRef.parse(reference), output, true);
      return 0;
    }
  }

  @CommandLine.Command(
      name = "manifest",
      description = "Manage manifests",
      mixinStandardHelpOptions = true,
      subcommands = {
        ManifestPush.class,
        ManifestFetch.class,
        ManifestGet.class,
        ManifestDelete.class
      })
  static class ManifestCommand implements Runnable {
    @CommandLine.ParentCommand Main parent;

    public void run() {
      CommandLine.usage(this, System.out);
    }
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
  static class ManifestPush extends ManifestReference {
    @CommandLine.Option(names = "--file", required = true, paramLabel = "PATH")
    Path file;

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
  static class ManifestFetch extends ManifestReference {
    @CommandLine.Option(names = "--output", required = true, paramLabel = "PATH")
    Path output;

    public Integer call() throws Exception {
      Files.writeString(output, registry().getDescriptor(ref()).getJson());
      return 0;
    }
  }

  @CommandLine.Command(
      name = "get",
      description = "Print a manifest",
      mixinStandardHelpOptions = true)
  static class ManifestGet extends ManifestReference {
    public Integer call() {
      System.out.println(registry().getDescriptor(ref()).getJson());
      return 0;
    }
  }

  @CommandLine.Command(
      name = "delete",
      description = "Delete a manifest",
      mixinStandardHelpOptions = true)
  static class ManifestDelete extends ManifestReference {
    public Integer call() {
      registry().deleteManifest(ref());
      return 0;
    }
  }
}
