package io.quarkiverse.oras.cli;

import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import picocli.CommandLine;

@CommandLine.Command(
    name = "repo",
    aliases = "repository",
    description = "Repository operations",
    mixinStandardHelpOptions = true,
    subcommands = {RepoCommand.Ls.class, RepoCommand.Tags.class})
class RepoCommand implements Runnable {
  @CommandLine.ParentCommand Main parent;

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  @CommandLine.Command(
      name = "ls",
      description = "List the repositories of a registry",
      mixinStandardHelpOptions = true)
  static class Ls implements Callable<Integer> {
    @CommandLine.ParentCommand RepoCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "REGISTRY", description = "host[:port]")
    String registry;

    @Override
    public Integer call() {
      parent.parent.registry().copy(registry).getRepositories().repositories().stream()
          .sorted()
          .forEach(System.out::println);
      return 0;
    }
  }

  @CommandLine.Command(
      name = "tags",
      description = "List the tags of a repository",
      mixinStandardHelpOptions = true)
  static class Tags implements Callable<Integer> {
    @CommandLine.ParentCommand RepoCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "REPOSITORY")
    String repository;

    @Override
    public Integer call() {
      parent.parent.registry().getTags(ContainerRef.parse(repository)).tags().stream()
          .sorted()
          .forEach(System.out::println);
      return 0;
    }
  }
}
