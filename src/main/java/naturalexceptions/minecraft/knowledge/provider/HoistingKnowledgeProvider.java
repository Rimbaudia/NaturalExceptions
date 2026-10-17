package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import naturalexceptions.core.hoisting.HoistMasses;
import naturalexceptions.minecraft.item.ItemHoistRope;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.multiblock.HeadframeDefinition;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Hoists: the Headframe, and the parts every hoist is made of. */
public final class HoistingKnowledgeProvider implements IKnowledgeProvider {

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        ItemStack windlass = new ItemStack(NEBlocks.hoistWindlass != null ? NEBlocks.hoistWindlass : Blocks.planks);
        ItemStack sheave = new ItemStack(NEBlocks.sheave != null ? NEBlocks.sheave : Blocks.planks);
        ItemStack basket = new ItemStack(NEBlocks.hoistBasket != null ? NEBlocks.hoistBasket : Blocks.planks);
        ItemStack rope = new ItemStack(NEItems.hoistRope != null ? NEItems.hoistRope : Items.string);
        registry.registerEntry(
            KnowledgeEntry.builder("headframe", "Headframe", KnowledgeCategory.MULTIBLOCKS)
                .icon(sheave)
                .summary("A hoist over a shaft: a basket on a rope, wound up and let down by a windlass.")
                .description(
                    "Dig a shaft one block across. Over its mouth stand two log posts, one either side, "
                        + HeadframeDefinition.MIN_HEIGHT
                        + " to "
                        + HeadframeDefinition.MAX_HEIGHT
                        + " blocks high on the same ground, with a Sheave between their tops over the shaft; "
                        + "place the Sheave facing along the beam towards the windlass, so its wheel turns that way. "
                        + "Set a Windlass on that ground within "
                        + HeadframeDefinition.REACH
                        + " blocks, its front towards the shaft, and fit it with rope: right-click it with Hoist Rope, "
                        + ItemHoistRope.BLOCKS_PER_COIL
                        + " blocks a coil. Put a Basket in the shaft and the windlass "
                        + "hooks it onto the rope.\n\n"
                        + "Hold right-click on the crank to wind the basket up; sneak and hold to let it down. "
                        + "The brake (the lever at the windlass's other end) holds the rope: set, nothing runs down, "
                        + "though the crank still winds up past its pawl. Released, the heavier side runs down. "
                        + "Riding in the basket, hold use and look up to haul yourself up on the hand-line, or down "
                        + "to let yourself down. Right-click the basket with things to load them as cargo; a crank "
                        + "lifts a rider with a few blocks of stone.")
                .multiblock(HeadframeDefinition.INSTANCE)
                .addProperty(
                    "Posts",
                    "Two log columns, " + HeadframeDefinition.MIN_HEIGHT
                        + "-"
                        + HeadframeDefinition.MAX_HEIGHT
                        + " high, either side of the shaft")
                .addProperty("Reach", ItemHoistRope.BLOCKS_PER_COIL + " blocks per coil of Hoist Rope fitted")
                .addProperty(
                    "Weights",
                    String.format(
                        "Basket %.0f kg, rider %.0f kg, a block of cargo %.0f kg",
                        HoistMasses.BASKET,
                        HoistMasses.RIDER,
                        HoistMasses.BLOCK))
                .addUsage(
                    ProcessStep.builder()
                        .operation("Laying up rope")
                        .machine(new ItemStack(NEBlocks.loom != null ? NEBlocks.loom : Blocks.crafting_table), "Loom")
                        .addInput(new ItemStack(Items.string, 6), "String")
                        .addOutput(rope, "Hoist Rope (" + ItemHoistRope.BLOCKS_PER_COIL + " blocks)")
                        .build())
                .addUsage(
                    ProcessStep.builder()
                        .operation("Riding the hoist")
                        .machine(windlass, "Windlass")
                        .addInput(basket, "Basket")
                        .addInput(sheave, "Sheave")
                        .addNote("Up: wind the crank. Down: ease the brake, or release it.")
                        .build())
                .build());
    }
}
