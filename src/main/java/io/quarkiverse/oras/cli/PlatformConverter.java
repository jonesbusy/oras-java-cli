package io.quarkiverse.oras.cli;

import land.oras.Platform;
import picocli.CommandLine;

/** Converts {@code os/arch[/variant][:os_version]} into a {@link Platform}. */
class PlatformConverter implements CommandLine.ITypeConverter<Platform> {
  @Override
  public Platform convert(String value) {
    String spec = value;
    String osVersion = null;
    int colon = value.indexOf(':');
    if (colon >= 0) {
      spec = value.substring(0, colon);
      osVersion = value.substring(colon + 1);
    }
    String[] parts = spec.split("/", -1);
    boolean blank = osVersion != null && osVersion.isBlank();
    for (String part : parts) {
      blank |= part.isBlank();
    }
    if (parts.length < 2 || parts.length > 3 || blank) {
      throw new CommandLine.TypeConversionException(
          "expected os/arch[/variant][:os_version] but got '" + value + "'");
    }
    Platform platform = Platform.of(parts[0], parts[1], parts.length == 3 ? parts[2] : null);
    return osVersion == null ? platform : platform.withOsVersion(osVersion);
  }
}
