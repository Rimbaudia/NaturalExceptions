package naturalexceptions.core.biology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;

/** Immutable, bounded description of an organism's regions, structure, and physiology. */
public final class BodyPlan {
    public static final int MAX_REGIONS = 8;
    private final ResourceId id;
    private final Map<ResourceId, BodyRegionDefinition> regions;
    private final PhysiologyProfile physiology;
    private final MaterialProperties structuralMaterial;

    public BodyPlan(ResourceId id, List<BodyRegionDefinition> regions, PhysiologyProfile physiology,
        MaterialProperties structuralMaterial) {
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(regions, "regions");
        if (regions.isEmpty() || regions.size() > MAX_REGIONS) throw new IllegalArgumentException("Body plan must contain 1 to 8 regions");
        this.physiology = Objects.requireNonNull(physiology, "physiology");
        this.structuralMaterial = Objects.requireNonNull(structuralMaterial, "structuralMaterial");
        LinkedHashMap<ResourceId, BodyRegionDefinition> indexed = new LinkedHashMap<>();
        for (BodyRegionDefinition region : regions) {
            if (region == null || indexed.put(region.id(), region) != null)
                throw new IllegalArgumentException("Body plan has a null or duplicate region");
        }
        this.regions = Collections.unmodifiableMap(indexed);
    }

    public ResourceId id() { return id; }
    public Map<ResourceId, BodyRegionDefinition> regions() { return regions; }
    public List<BodyRegionDefinition> orderedRegions() { return Collections.unmodifiableList(new ArrayList<>(regions.values())); }
    public BodyRegionDefinition region(ResourceId id) { return regions.get(id); }
    public PhysiologyProfile physiology() { return physiology; }
    public MaterialProperties structuralMaterial() { return structuralMaterial; }

    @Override public boolean equals(Object other) {
        return other instanceof BodyPlan && id.equals(((BodyPlan) other).id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
