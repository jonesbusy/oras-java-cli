package io.quarkiverse.oras.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;

class CommandLineConfigurationTest {
  @Test
  void describeIncludesCauseMessages() {
    var exception =
        new OrasException(
            "Failed to extract tar.gz file", new IOException("Entry is outside of the target dir"));

    assertEquals(
        "Failed to extract tar.gz file: Entry is outside of the target dir",
        CommandLineConfiguration.describe(exception));
  }

  @Test
  void describeSkipsWrapperMessages() {
    var cause = new OrasException("Failed to extract tar.gz file");

    assertEquals(
        "Failed to extract tar.gz file",
        CommandLineConfiguration.describe(new RuntimeException(cause)));
  }

  @Test
  void describeFallsBackToClassName() {
    assertEquals(
        "java.lang.IllegalStateException",
        CommandLineConfiguration.describe(new IllegalStateException()));
  }
}
