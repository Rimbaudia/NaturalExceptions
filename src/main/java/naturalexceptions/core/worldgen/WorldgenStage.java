package naturalexceptions.core.worldgen;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Stable authored ordering for the chunk materialization pipeline. */
public enum WorldgenStage {

    BASE_TERRAIN("base_terrain"),
    GEOLOGY("geology"),
    HYDROLOGY("hydrology"),
    SOIL_SURFACE("soil_surface"),
    ECOLOGY("ecology"),
    RESOURCES("resources"),
    STRUCTURES("structures"),
    INITIAL_ANOMALIES("initial_anomalies");

    public static final List<WorldgenStage> PIPELINE = Collections.unmodifiableList(Arrays.asList(values()));

    private final String key;

    WorldgenStage(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static WorldgenStage fromKey(String key) {
        for (WorldgenStage stage : values()) {
            if (stage.key.equals(key)) return stage;
        }
        throw new IllegalArgumentException("Unknown worldgen stage key: " + key);
    }
}
