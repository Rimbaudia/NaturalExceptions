package naturalexceptions.core.material;

/**
 * Mutable or instance-specific state of an individual physical material sample or carrier.
 *
 * <p>
 * Demonstrates the strict architectural distinction between:
 * <ul>
 * <li><b>Canonical physical material identity</b> ({@link CanonicalMaterialKey} and {@link MaterialRef}),
 * which defines what a substance physically is; and</li>
 * <li><b>Mutable sample state</b> ({@link MaterialSampleState}), which tracks transient properties
 * of an individual physical instance (current temperature, container, stack count, player alias,
 * discovery status).</li>
 * </ul>
 *
 * <p>
 * Under no circumstances does changing mutable sample state alter {@link MaterialRef} or {@link CanonicalMaterialKey}.
 */
public final class MaterialSampleState {

    private double temperatureKelvin;
    private String playerAlias;
    private boolean discovered;
    private String containerId;
    private int stackCount;

    public MaterialSampleState(double temperatureKelvin, String playerAlias, boolean discovered, String containerId,
        int stackCount) {
        this.temperatureKelvin = temperatureKelvin;
        this.playerAlias = playerAlias;
        this.discovered = discovered;
        this.containerId = containerId;
        this.stackCount = stackCount;
    }

    public static MaterialSampleState ambient() {
        return new MaterialSampleState(293.15, null, false, null, 1);
    }

    public double temperatureKelvin() {
        return temperatureKelvin;
    }

    public void setTemperatureKelvin(double temperatureKelvin) {
        this.temperatureKelvin = temperatureKelvin;
    }

    public String playerAlias() {
        return playerAlias;
    }

    public void setPlayerAlias(String playerAlias) {
        this.playerAlias = playerAlias;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public String containerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public int stackCount() {
        return stackCount;
    }

    public void setStackCount(int stackCount) {
        this.stackCount = stackCount;
    }
}
