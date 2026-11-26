package io.quarkiverse.oras.cli;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import land.oras.ContainerRef;
import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(
    name = "blob",
    description = "Blob operations",
    mixinStandardHelpOptions = true,
    subcommands = {BlobCommand.Fetch.class, BlobCommand.Delete.class})
class BlobCommand implements Runnable {
  @CommandLine.ParentCommand Main parent;

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  abstract static class BlobReference implements Callable<Integer> {
    @CommandLine.ParentCommand BlobCommand parent;

    @CommandLine.Spec CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters(index = "0", paramLabel = "NAME@DIGEST")
    String reference;

    ContainerRef ref() {
      ContainerRef ref = ContainerRef.parse(reference);
      if (ref.getDigest() == null) {
        throw new CommandLine.ParameterException(
            spec.commandLine(), "A blob reference must include a digest: " + reference);
      }
      return ref;
    }

    Registry registry() {
      return parent.parent.registry();
    }
  }

  @CommandLine.Command(
      name = "fetch",
      description = "Fetch a blob",
      mixinStandardHelpOptions = true)
  static class Fetch extends BlobReference {
    @CommandLine.Option(
        names = "--output",
        required = true,
        paramLabel = "PATH",
        description = "File to write the blob to, or - for stdout")
    String output;

    @Override
    public Integer call() throws Exception {
      ContainerRef ref = ref();
      if ("-".equals(output)) {
        try (InputStream in = registry().fetchBlob(ref)) {
          in.transferTo(System.out);
        }
        System.out.flush();
      } else {
        registry().fetchBlob(ref, Path.of(output));
      }
      return 0;
    }
  }

  @CommandLine.Command(
      name = "delete",
      description = "Delete a blob",
      mixinStandardHelpOptions = true)
  static class Delete extends BlobReference {
    @Override
    public Integer call() {
      registry().deleteBlob(ref());
      System.out.println("Deleted " + reference);
      return 0;
    }
  }
}
