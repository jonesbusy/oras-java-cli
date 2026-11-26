package io.quarkiverse.oras.cli;

import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import picocli.CommandLine;

@ApplicationScoped
class CommandLineConfiguration {
  // Report registry and I/O failures as a one-line error instead of a stack trace.
  @Produces
  CommandLine commandLine(PicocliCommandLineFactory factory) {
    return factory
        .create()
        .setExecutionExceptionHandler(
            (exception, commandLine, parseResult) -> {
              String message = exception.getMessage();
              commandLine
                  .getErr()
                  .println(
                      "Error: " + (message == null ? exception.getClass().getName() : message));
              return 1;
            });
  }
}
