package naturalexceptions.core.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Diagnostic trace of pipeline layers applied during material property resolution.
 */
public final class ResolverTrace {

    private final List<String> appliedLayers;

    public ResolverTrace(List<String> appliedLayers) {
        this.appliedLayers = Collections.unmodifiableList(new ArrayList<>(appliedLayers));
    }

    public List<String> appliedLayers() {
        return appliedLayers;
    }

    public boolean layerApplied(String layerName) {
        return appliedLayers.contains(layerName);
    }

    @Override
    public String toString() {
        return "ResolverTrace" + appliedLayers;
    }
}
