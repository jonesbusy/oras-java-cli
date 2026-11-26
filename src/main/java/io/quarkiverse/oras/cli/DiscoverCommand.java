package io.quarkiverse.oras.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import land.oras.ArtifactType;
import land.oras.ContainerRef;
import land.oras.ManifestDescriptor;
import land.oras.Registry;
import picocli.CommandLine;

@CommandLine.Command(
    name = "discover",
    description = "Show the referrers of a manifest as a tree",
    mixinStandardHelpOptions = true)
class DiscoverCommand implements Callable<Integer> {
  @CommandLine.ParentCommand Main parent;

  @CommandLine.Parameters(index = "0", paramLabel = "REFERENCE")
  String reference;

  @CommandLine.Option(
      names = "--artifact-type",
      paramLabel = "TYPE",
      description = "Only show referrers with this artifact type")
  String artifactType;

  @Override
  public Integer call() {
    Registry registry = parent.registry();
    ContainerRef ref = ContainerRef.parse(reference);
    String digest =
        ref.getDigest() != null ? ref.getDigest() : registry.probeDescriptor(ref).getDigest();
    ContainerRef subject = ref.withDigest(digest);
    System.out.println(subject.forRegistry(registry));
    printReferrers(registry, subject, "");
    return 0;
  }

  private void printReferrers(Registry registry, ContainerRef subject, String indent) {
    List<ManifestDescriptor> referrers =
        registry
            .getReferrers(subject, artifactType == null ? null : ArtifactType.from(artifactType))
            .getManifests();
    Map<String, List<ManifestDescriptor>> byType = new LinkedHashMap<>();
    for (ManifestDescriptor referrer : referrers) {
      String type = referrer.getArtifactType() == null ? "<unknown>" : referrer.getArtifactType();
      byType.computeIfAbsent(type, k -> new ArrayList<>()).add(referrer);
    }
    int typeIndex = 0;
    for (var entry : byType.entrySet()) {
      boolean lastType = ++typeIndex == byType.size();
      System.out.println(indent + (lastType ? "└── " : "├── ") + entry.getKey());
      String typeIndent = indent + (lastType ? "    " : "│   ");
      List<ManifestDescriptor> manifests = entry.getValue();
      for (int i = 0; i < manifests.size(); i++) {
        boolean last = i == manifests.size() - 1;
        System.out.println(typeIndent + (last ? "└── " : "├── ") + manifests.get(i).getDigest());
        printReferrers(
            registry,
            subject.withDigest(manifests.get(i).getDigest()),
            typeIndent + (last ? "    " : "│   "));
      }
    }
  }
}
