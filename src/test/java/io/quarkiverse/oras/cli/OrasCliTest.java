package io.quarkiverse.oras.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
class OrasCliTest {
  @Inject @TopCommand Main command;

  @Test
  void injectsConfiguredRegistryIntoRootCommand() {
    assertNotNull(command.registry());
  }

  @Test
  void exposesCommands() {
    CommandLine commandLine = new CommandLine(new Main());
    var subcommands = commandLine.getSubcommands();

    assertEquals(
        Set.of(
            "push",
            "pull",
            "manifest",
            "resolve",
            "digest",
            "discover",
            "cp",
            "copy",
            "repo",
            "repository",
            "blob"),
        subcommands.keySet());
    assertEquals(
        Set.of("push", "fetch", "get", "delete"),
        subcommands.get("manifest").getSubcommands().keySet());
    assertEquals(Set.of("ls", "tags"), subcommands.get("repo").getSubcommands().keySet());
    assertEquals(Set.of("fetch", "delete"), subcommands.get("blob").getSubcommands().keySet());
  }
}
