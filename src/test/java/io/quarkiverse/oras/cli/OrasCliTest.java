package io.quarkiverse.oras.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
class OrasCliTest {
  @Inject @TopCommand Main command;

  @Test
  void injectsConfiguredRegistryIntoRootCommand() {
    assertNotNull(command.registry);
  }

  @Test
  void exposesPushPullAndManifestCommands() {
    CommandLine commandLine = new CommandLine(new Main());

    assertTrue(commandLine.getSubcommands().containsKey("push"));
    assertTrue(commandLine.getSubcommands().containsKey("pull"));
    assertTrue(commandLine.getSubcommands().containsKey("manifest"));
    assertTrue(commandLine.getSubcommands().get("manifest").getSubcommands().containsKey("get"));
  }
}
