package io.quarkiverse.oras.cli;

import io.quarkiverse.oras.runtime.OrasRegistry;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import land.oras.Registry;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "orasj",
    description = "Manage OCI artifacts",
    mixinStandardHelpOptions = true,
    versionProvider = Main.VersionProvider.class,
    subcommands = {
      PushCommand.class,
      PullCommand.class,
      ManifestCommand.class,
      ResolveCommand.class,
      DiscoverCommand.class,
      CopyCommand.class,
      RepoCommand.class,
      BlobCommand.class
    })
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
}
