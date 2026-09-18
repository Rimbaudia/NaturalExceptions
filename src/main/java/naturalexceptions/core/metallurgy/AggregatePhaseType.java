package naturalexceptions.core.metallurgy;

/**
 * Macroscopic aggregate phase type in metallurgical vessels.
 */
public enum AggregatePhaseType {

    /**
     * Unmelted solid feedstock, ore chunks, or solid flux.
     */
    SOLID_CHARGE,

    /**
     * Molten metallic layer (dense, accumulates at hearth bottom).
     */
    MOLTEN_METAL,

    /**
     * Molten silicate / gangue waste layer (lower density, floats atop molten metal).
     */
    SLAG,

    /**
     * Gaseous combustion or reduction product before exhaust sink absorption.
     */
    GAS_EXHAUST
}
