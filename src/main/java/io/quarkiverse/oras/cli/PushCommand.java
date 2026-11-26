package io.quarkiverse.oras.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import land.oras.LocalPath;
import picocli.CommandLine;

@CommandLine.Command(
    name = "push",
    description = "Push an artifact",
    mixinStandardHelpOptions = true)
class PushCommand implements Callable<Integer> {
  @CommandLine.ParentCommand Main parent;

  @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
  String reference;

  @CommandLine.Option(names = "--file", required = true, paramLabel = "PATH")
  Path file;

  @Override
  public Integer call() {
    var manifest =
        parent.registry().pushArtifact(ContainerRef.parse(reference), LocalPath.of(file));
    System.out.println("Pushed " + manifest.getDigest());
    return 0;
  }
}
