package io.quarkiverse.oras.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import land.oras.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

class PlatformConverterTest {
  private final PlatformConverter converter = new PlatformConverter();

  @Test
  void parsesOsAndArchitecture() {
    assertEquals(Platform.of("linux", "amd64"), converter.convert("linux/amd64"));
  }

  @Test
  void parsesVariant() {
    assertEquals(Platform.of("linux", "arm", "v7"), converter.convert("linux/arm/v7"));
  }

  @Test
  void parsesOsVersion() {
    assertEquals(
        Platform.of("windows", "amd64").withOsVersion("10.0.20348"),
        converter.convert("windows/amd64:10.0.20348"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"linux", "linux/", "/amd64", "linux/arm/v7/extra", "linux/amd64:"})
  void rejectsInvalidPlatforms(String value) {
    assertThrows(CommandLine.TypeConversionException.class, () -> converter.convert(value));
  }
}
