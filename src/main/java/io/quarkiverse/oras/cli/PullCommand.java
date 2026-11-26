package io.quarkiverse.oras.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import picocli.CommandLine;

@CommandLine.Command(
    name = "pull",
    description = "Pull an artifact",
    mixinStandardHelpOptions = true)
class PullCommand implements Callable<Integer> {
  @CommandLine.ParentCommand Main parent;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "REFERENCE",
      description = "Reference to pull, for example localhost:5000/repo:tag")
  String reference;

  @CommandLine.Option(
      names = "--output",
      defaultValue = ".",
      paramLabel = "DIR",
      description = "Directory to write files to (default: ${DEFAULT-VALUE})")
  Path output;

  @Override
  public Integer call() throws Exception {
    // The SDK's extraction guard compares entries against the absolute target, so a relative
    // directory (including the default ".") would reject every entry of a pushed folder.
    Path target = output.toAbsolutePath().normalize();
    Files.createDirectories(target);
    parent.registry().pullArtifact(ContainerRef.parse(reference), target, true);
    System.out.println("Pulled " + reference + " to " + target);
    return 0;
  }
}
