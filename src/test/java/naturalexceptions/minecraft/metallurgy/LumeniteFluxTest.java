package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Lumenite as a smelting flux: fluxed ore gives more metal and less slag, and the total mass is unchanged. */
final class LumeniteFluxTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static long[] smeltOneOre(boolean fluxed) {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        if (fluxed) assertTrue(smelter.insertFeed(new ItemStack(NEItems.lumeniteShard)), "shard accepted as flux");
        smelter.vesselState()
            .addRawOre(OreFeedstockDefinition.copperOre());
        smelter.vesselState()
            .addFuel(1000);
        for (int i = 0; i < 400; i++) smelter.vesselState()
            .step(1, 293.15);
        long metal = 0, slag = 0;
        for (AggregateLayer layer : smelter.vesselState()
            .layers()) {
            if (layer.phaseType() == AggregatePhaseType.MOLTEN_METAL) metal += layer.mass()
                .milligrams();
            if (layer.phaseType() == AggregatePhaseType.SLAG) slag += layer.mass()
                .milligrams();
        }
        return new long[] { metal, slag, smelter.vesselState()
            .fluxCharges() };
    }

    @Test
    void fluxedOreGivesMoreMetalAndLessSlagWithMassConserved() {
        long[] plain = smeltOneOre(false);
        long[] fluxed = smeltOneOre(true);
        assertTrue(plain[0] > 0 && plain[1] > 0, "the ore melted into metal and slag");
        assertTrue(fluxed[0] > plain[0], "more metal: " + fluxed[0] + " vs " + plain[0]);
        assertTrue(fluxed[1] < plain[1], "less slag");
        assertEquals(plain[0] + plain[1], fluxed[0] + fluxed[1], "metal + slag unchanged");
        assertEquals(
            Math.round(plain[1] * VesselState.FLUX_RECOVERY),
            fluxed[0] - plain[0],
            1.0,
            "a quarter of the slag");
        assertEquals(VesselState.FLUX_PER_SHARD - 1, fluxed[2], "one flux charge used");
    }

    @Test
    void fluxSurvivesSaving() {
        VesselState state = new VesselState();
        state.addFlux(7);
        NBTTagCompound nbt = new NBTTagCompound();
        VesselStateNbtCodec.writeToNbt(state, nbt);
        VesselState loaded = VesselStateNbtCodec
            .readFromNbt(nbt, new MaterialPropertyResolver(), VesselState.DEFAULT_CAPACITY);
        assertEquals(7, loaded.fluxCharges());
    }
}
