package io.quarkiverse.oras.cli;

import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.List;
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
              commandLine.getErr().println("Error: " + describe(exception));
              return 1;
            });
  }

  /**
   * Joins the messages of the exception and its causes, since the SDK often wraps the actionable
   * reason (for example a path or HTTP error) inside a generic message.
   */
  static String describe(Throwable exception) {
    List<String> messages = new ArrayList<>();
    for (Throwable t = exception; t != null && messages.size() < 10; t = t.getCause()) {
      String message = t.getMessage();
      Throwable cause = t.getCause();
      // Skip wrappers whose message is just "ClassName: cause message".
      if (message == null || (cause != null && message.equals(cause.toString()))) {
        continue;
      }
      if (messages.isEmpty() || !messages.getLast().contains(message)) {
        messages.add(message);
      }
      if (cause == t) {
        break;
      }
    }
    return messages.isEmpty() ? exception.getClass().getName() : String.join(": ", messages);
  }
}
