package naturalexceptions.minecraft.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.fluid.FluidWorldState;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemFluidVessel;
import naturalexceptions.minecraft.item.ItemMaterialSample;

final class FallenStarHostRepresentationTest {

    @BeforeAll
    static void registerMaterials() {
        BuiltinContent.register();
    }

    @Test
    void liquidAndSolidUsePhaseAppropriateCarriersWithoutChangingCanonicalMatter() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(735.0);
        DeterministicId impactId = DeterministicId.derive("impact", "host-form");
        CanonicalComposition composition = BuiltinMaterials.STELLAR_MATTER_COMPOSITION;
        CanonicalMaterialKey liquidKey = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        ActiveThermalState liquidThermal = ActiveThermalState
            .atTemperature(1780.0, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        FluidParcel liquid = new FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            liquidKey,
            composition,
            mass,
            liquidThermal,
            impactId);

        ItemStack liquidCarrier = new BlockFallenStar().recoveryStack(liquid);

        assertInstanceOf(ItemFluidVessel.class, liquidCarrier.getItem());
        FluidParcel heldLiquid = ItemFluidVessel.contents(liquidCarrier);
        assertEquals(BuiltinMaterials.STELLAR_MATTER.ref(), heldLiquid.materialRef());
        assertEquals(mass, heldLiquid.mass());
        assertEquals(
            1780.0,
            heldLiquid.thermalState()
                .temperatureKelvin(),
            1.0e-8);
        assertEquals(impactId, heldLiquid.provenanceId());

        CanonicalMaterialKey solidKey = liquidKey.withPhase(MaterialPhase.SOLID);
        ActiveThermalState solidThermal = ActiveThermalState
            .atTemperature(1100.0, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        FluidParcel solid = new FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            solidKey,
            composition,
            mass,
            solidThermal,
            impactId);
        ItemStack solidCarrier = new BlockFallenStar().recoveryStack(solid);

        assertInstanceOf(ItemMaterialSample.class, solidCarrier.getItem());
        assertEquals(BuiltinMaterials.STELLAR_MATTER.ref(), ItemMaterialSample.getMaterialRef(solidCarrier));
        assertEquals(mass, ItemMaterialSample.getMass(solidCarrier));
        assertEquals(
            solidThermal.energyJoules(),
            ItemMaterialSample.getThermalState(solidCarrier, BuiltinMaterials.STELLAR_MATTER_PROPERTIES)
                .energyJoules(),
            1.0e-8);
        assertEquals(impactId, ItemMaterialSample.getProvenance(solidCarrier));
    }

    @Test
    void blockHostMetadataSeparatesLiquidFromSolidAndVesselHasSurvivalRecipe() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(735.0);
        CanonicalComposition composition = BuiltinMaterials.STELLAR_MATTER_COMPOSITION;
        CanonicalMaterialKey liquidKey = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        FluidParcel liquid = new FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            liquidKey,
            composition,
            mass,
            ActiveThermalState
                .atTemperature(1780.0, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES));
        FluidParcel solid = new FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            liquidKey.withPhase(MaterialPhase.SOLID),
            composition,
            mass,
            ActiveThermalState
                .atTemperature(1100.0, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES));
        int liquidMeta = BlockFluidCell.metadataFor(liquid);
        int solidMeta = BlockFluidCell.metadataFor(solid);
        // Minecraft 1.7.10 stores only four metadata bits; the encoding must survive that.
        assertTrue(liquidMeta >= 0 && liquidMeta <= 15 && solidMeta >= 0 && solidMeta <= 15);
        assertEquals(false, BlockFluidCell.isSolidMetadata(liquidMeta));
        assertEquals(true, BlockFluidCell.isSolidMetadata(solidMeta));
        assertEquals(BlockFluidCell.KIND_GLOWING_SOLID, BlockFluidCell.kindOf(solidMeta));
        FluidParcel cold = new FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            liquidKey.withPhase(MaterialPhase.SOLID),
            composition,
            mass,
            ActiveThermalState
                .atTemperature(300.0, 0.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES));
        assertEquals(BlockFluidCell.KIND_SOLID, BlockFluidCell.kindOf(BlockFluidCell.metadataFor(cold)));

        ItemFluidVessel vessel = new ItemFluidVessel();
        net.minecraft.item.crafting.IRecipe recipe = ItemFluidVessel.survivalRecipe(vessel);
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        grid.setInventorySlotContents(0, new ItemStack(net.minecraft.init.Blocks.glass));
        grid.setInventorySlotContents(2, new ItemStack(net.minecraft.init.Blocks.glass));
        grid.setInventorySlotContents(3, new ItemStack(net.minecraft.init.Blocks.glass));
        grid.setInventorySlotContents(4, new ItemStack(net.minecraft.init.Blocks.glass));
        grid.setInventorySlotContents(5, new ItemStack(net.minecraft.init.Blocks.glass));
        assertEquals(true, recipe.matches(grid, (World) null));
        assertEquals(
            vessel,
            recipe.getCraftingResult(grid)
                .getItem());
    }

    @Test
    void replacingSolidHostTransfersParcelIntoCarrierInsteadOfLeavingHiddenFluidState() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(735.0);
        CanonicalComposition composition = BuiltinMaterials.STELLAR_MATTER_COMPOSITION;
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.SOLID, null, null);
        FluidParcel parcel = new FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            key,
            composition,
            mass,
            ActiveThermalState
                .atTemperature(1100.0, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES),
            DeterministicId.derive("impact", "replacement"));
        FluidWorldState state = new FluidWorldState();
        FluidCellKey cell = new FluidCellKey(0, 24, 70, -12);
        state.put(cell, parcel);

        ItemStack carrier = BlockFluidCell.takeParcelForRecovery(state, cell, new BlockFallenStar());

        assertNotNull(carrier);
        assertNull(state.parcelAt(cell));
        assertEquals(mass, ItemMaterialSample.getMass(carrier));
        assertEquals(parcel.provenanceId(), ItemMaterialSample.getProvenance(carrier));
    }

    @Test
    void customHostTexturesExistAsAuthoredSixteenPixelResources() {
        for (String texture : Arrays.asList(
            "blocks/fallen_star_solid.png",
            "blocks/fallen_star_molten.png",
            "blocks/fluid_liquid.png",
            "blocks/fluid_solid.png",
            "items/fluid_vessel_empty.png",
            "items/fluid_vessel_frame.png",
            "items/fluid_vessel_contents.png",
            "items/filter_mask.png",
            "items/field_dressing.png",
            "items/field_assay.png",
            "items/refractory_brick.png",
            "items/refractory_mix.png",
            "items/almanac.png")) {
            File file = new File("src/main/resources/assets/naturalexceptions/textures", texture);
            org.junit.jupiter.api.Assertions.assertTrue(file.isFile(), texture);
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
                assertEquals(16, image.getWidth(), texture);
                // Animated textures are vertical strips of 16x16 frames with an animation descriptor.
                assertEquals(0, image.getHeight() % 16, texture);
                if (image.getHeight() > 16) {
                    org.junit.jupiter.api.Assertions.assertTrue(new File(file.getPath() + ".mcmeta").isFile(), texture);
                }
            } catch (java.io.IOException error) {
                throw new AssertionError(error);
            }
        }
        try {
            String dir = "src/main/resources/assets/naturalexceptions/textures/";
            java.awt.image.BufferedImage molten = javax.imageio.ImageIO
                .read(new File(dir + "blocks/fallen_star_molten.png"));
            java.awt.image.BufferedImage solid = javax.imageio.ImageIO
                .read(new File(dir + "blocks/fallen_star_solid.png"));
            org.junit.jupiter.api.Assertions.assertTrue(
                meanLuminance(molten) > meanLuminance(solid) + 40.0,
                "molten stellar matter reads brighter than the set glass");
            java.awt.image.BufferedImage frame = javax.imageio.ImageIO
                .read(new File(dir + "items/fluid_vessel_frame.png"));
            java.awt.image.BufferedImage contents = javax.imageio.ImageIO
                .read(new File(dir + "items/fluid_vessel_contents.png"));
            assertEquals(0, frame.getRGB(7, 11) >>> 24, "the jar interior lets the tinted contents show through");
            assertEquals(255, contents.getRGB(7, 11) >>> 24, "the contents layer fills the jar body");
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    private static double meanLuminance(java.awt.image.BufferedImage image) {
        double total = 0.0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int rgb = image.getRGB(x, y);
            total += 0.3 * ((rgb >> 16) & 255) + 0.59 * ((rgb >> 8) & 255) + 0.11 * (rgb & 255);
        }
        return total / 256.0;
    }
}
