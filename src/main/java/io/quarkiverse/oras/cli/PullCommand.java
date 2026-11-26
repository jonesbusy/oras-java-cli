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

  @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
  String reference;

  @CommandLine.Option(names = "--output", defaultValue = ".", paramLabel = "DIR")
  Path output;

  @Override
  public Integer call() throws Exception {
    Files.createDirectories(output);
    parent.registry().pullArtifact(ContainerRef.parse(reference), output, true);
    return 0;
  }
}
