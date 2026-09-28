package naturalexceptions.core.biology;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.id.ResourceId;

/** Physical armor plate backed by an existing P5 component and its damage state.
 * Resolver capacity uses hardness(Mohs) × thickness(m) × 3000 as a bounded joule proxy.
 */
public final class ArmorLayer {
    private final ComponentInstance component;
    private final MaterialProperties material;
    private final double thicknessMetres;
    private final Set<ResourceId> coveredRegions;

    public ArmorLayer(ComponentInstance component, MaterialProperties material, double thicknessMetres,
        Set<ResourceId> coveredRegions) {
        this.component = Objects.requireNonNull(component, "component");
        this.material = Objects.requireNonNull(material, "material");
        if (!Double.isFinite(material.hardness()) || material.hardness() <= 0.0)
            throw new IllegalArgumentException("armor material hardness must be positive and finite");
        if (!Double.isFinite(thicknessMetres) || thicknessMetres <= 0.0)
            throw new IllegalArgumentException("thickness must be positive and finite");
        if (coveredRegions == null || coveredRegions.isEmpty()) throw new IllegalArgumentException("armor must cover a region");
        this.thicknessMetres = thicknessMetres;
        this.coveredRegions = Collections.unmodifiableSet(new LinkedHashSet<>(coveredRegions));
    }
    public ComponentInstance component() { return component; }
    public MaterialProperties material() { return material; }
    public double thicknessMetres() { return thicknessMetres; }
    public Set<ResourceId> coveredRegions() { return coveredRegions; }
    boolean covers(ResourceId region) { return coveredRegions.contains(region); }
}
