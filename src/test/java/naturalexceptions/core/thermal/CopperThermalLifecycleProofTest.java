package naturalexceptions.core.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.property.ResolvedProperties;
import naturalexceptions.minecraft.persistence.MaterialCarrierNbtCodec;

/**
 * Section 26 Vertical Acceptance Proof: Full thermal lifecycle of canonical copper.
 *
 * <p>
 * Proves end-to-end that the SAME durable material {@code naturalexceptions:copper} can:
 * <ol>
 * <li>Resolve canonical properties via the production layered resolver;</li>
 * <li>Start at ambient equilibrium (implicit state);</li>
 * <li>Absorb thermal energy, heating from ambient to melting point (1357.77 K);</li>
 * <li>Traverse the latent heat plateau (absorbing 205,000 J/kg) at constant temperature;</li>
 * <li>Transition to liquid phase and superheat to 1500 K;</li>
 * <li>Persist to NBT as liquid and reload without losing truth or identity;</li>
 * <li>Release energy, cool back to 1357.77 K, freeze back to solid, and cool to ambient;</li>
 * <li>Relax back into implicit ambient equilibrium without any smelter or furnace logic.</li>
 * </ol>
 */
final class CopperThermalLifecycleProofTest {

    private static final ResourceId COPPER_ID = ResourceId.of("naturalexceptions", "copper");

    @BeforeAll
    static void init() {
        BuiltinContent.register();
    }

    @Test
    void completeCopperThermalLifecycleAndPersistenceProof() {
        // 1. Canonical copper MaterialRef & Durable ID
        MaterialRef copperRef = MaterialRef.authored(COPPER_ID);
        assertEquals(
            "naturalexceptions:copper",
            copperRef.authoredId()
                .toString());
        assertEquals("mat:authored:naturalexceptions:copper", copperRef.asString());

        // 2. Resolve properties via production property resolver
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        ResolvedProperties resolved = resolver.resolve(BuiltinMaterials.COPPER);
        assertNotNull(resolved);
        MaterialProperties props = resolved.properties();

        assertEquals(8.96, props.density(), 0.01);
        assertEquals(385.0, props.specificHeatSolid(), 0.01);
        assertEquals(410.0, props.specificHeatLiquid(), 0.01);
        assertEquals(1357.77, props.meltingPointKelvin(), 0.01);
        assertEquals(205000.0, props.latentHeatOfFusion(), 0.01);
        assertEquals(401.0, props.thermalConductivity(), 0.01);

        // 3. Physical sample: 1.0 kg copper at ambient (293.15 K)
        double massKg = 1.0;
        ActiveThermalState copperSample = ActiveThermalState.ambient(massKg, props);
        assertEquals(293.15, copperSample.temperatureKelvin(), 0.001);
        assertEquals(MaterialPhase.SOLID, copperSample.phase());
        assertEquals(0.0, copperSample.meltFraction(), 0.001);
        assertTrue(copperSample.isEquilibratedWith(293.15, 0.5));

        // 4. Energy added: heating to solidus (1357.77 K)
        // Energy required to reach solidus from ambient: m * c_p,solid * (1357.77 - 293.15)
        double deltaEnergyToMelt = massKg * props.specificHeatSolid() * (props.meltingPointKelvin() - 293.15);
        FixedTemperatureReservoir heatSource = new FixedTemperatureReservoir(1800.0);
        ThermalTransfer.transferConservative(heatSource, new FiniteThermalBody(copperSample), deltaEnergyToMelt);

        assertEquals(props.meltingPointKelvin(), copperSample.temperatureKelvin(), 0.001);
        assertEquals(0.0, copperSample.meltFraction(), 0.001);
        assertEquals(MaterialPhase.SOLID, copperSample.phase());

        // 5. Phase boundary reached: Latent heat plateau absorption
        double latentHeat = massKg * props.latentHeatOfFusion(); // 205,000 J

        // Halfway through melting
        ThermalTransfer.transferConservative(heatSource, new FiniteThermalBody(copperSample), latentHeat * 0.5);
        assertEquals(props.meltingPointKelvin(), copperSample.temperatureKelvin(), 0.001); // Temperature must remain
                                                                                           // constant!
        assertEquals(0.50, copperSample.meltFraction(), 0.001);
        assertEquals(MaterialPhase.SOLID, copperSample.phase()); // Mushy solid

        // Complete melting
        ThermalTransfer.transferConservative(heatSource, new FiniteThermalBody(copperSample), latentHeat * 0.5);
        assertEquals(props.meltingPointKelvin(), copperSample.temperatureKelvin(), 0.001);
        assertEquals(1.0, copperSample.meltFraction(), 0.001);
        assertEquals(MaterialPhase.LIQUID, copperSample.phase()); // Fully liquid!

        // 6. Superheating: Heat liquid copper to 1500 K
        double superheatEnergy = massKg * props.specificHeatLiquid() * (1500.0 - props.meltingPointKelvin());
        ThermalTransfer.transferConservative(heatSource, new FiniteThermalBody(copperSample), superheatEnergy);

        assertEquals(1500.0, copperSample.temperatureKelvin(), 0.01);
        assertEquals(1.0, copperSample.meltFraction(), 0.001);
        assertEquals(MaterialPhase.LIQUID, copperSample.phase());

        // 7. Save & reload in superheated liquid state
        NBTTagCompound liquidNbt = new NBTTagCompound();
        MaterialCarrierNbtCodec
            .writeToNbt(copperRef, (naturalexceptions.core.material.CanonicalMaterialKey) null, liquidNbt);
        MaterialCarrierNbtCodec.writeThermalStateToNbt(copperSample, liquidNbt);
        assertTrue(MaterialCarrierNbtCodec.hasActiveThermalState(liquidNbt));

        // Reload into completely fresh instance
        MaterialRef reloadedRef = MaterialCarrierNbtCodec.readRefFromNbt(liquidNbt);
        assertEquals(copperRef, reloadedRef);
        ActiveThermalState reloadedSample = MaterialCarrierNbtCodec.readThermalStateFromNbt(liquidNbt, props);
        assertNotNull(reloadedSample);
        assertEquals(copperSample.energyJoules(), reloadedSample.energyJoules(), 0.001);
        assertEquals(1500.0, reloadedSample.temperatureKelvin(), 0.01);
        assertEquals(MaterialPhase.LIQUID, reloadedSample.phase());

        // 8. Energy removed: Cooling back to liquidus
        AmbientReservoir coldSink = new AmbientReservoir(293.15);
        ThermalTransfer.transferConservative(new FiniteThermalBody(reloadedSample), coldSink, superheatEnergy);
        assertEquals(props.meltingPointKelvin(), reloadedSample.temperatureKelvin(), 0.01);
        assertEquals(1.0, reloadedSample.meltFraction(), 0.001);

        // 9. Freezing transition: Latent heat release
        ThermalTransfer.transferConservative(new FiniteThermalBody(reloadedSample), coldSink, latentHeat);
        assertEquals(props.meltingPointKelvin(), reloadedSample.temperatureKelvin(), 0.01);
        assertEquals(0.0, reloadedSample.meltFraction(), 0.001);
        assertEquals(MaterialPhase.SOLID, reloadedSample.phase()); // Frozen solid!

        // 10. Ambient relaxation: Cool back to ambient equilibrium (293.15 K)
        ThermalTransfer.transferConservative(new FiniteThermalBody(reloadedSample), coldSink, deltaEnergyToMelt);
        assertEquals(293.15, reloadedSample.temperatureKelvin(), 0.01);
        assertTrue(reloadedSample.isEquilibratedWith(293.15, 0.5));

        // 11. Final Save & Reload: Collapses to implicit ambient state
        NBTTagCompound ambientNbt = new NBTTagCompound();
        MaterialCarrierNbtCodec
            .writeToNbt(copperRef, (naturalexceptions.core.material.CanonicalMaterialKey) null, ambientNbt);
        MaterialCarrierNbtCodec.writeThermalStateToNbt(reloadedSample, ambientNbt);

        // Sparse simulation verification: Equilibrated ambient sample omits the Thermal compound tag
        assertFalse(MaterialCarrierNbtCodec.hasActiveThermalState(ambientNbt));
        MaterialRef finalRef = MaterialCarrierNbtCodec.readRefFromNbt(ambientNbt);
        assertEquals(copperRef, finalRef);
    }
}
