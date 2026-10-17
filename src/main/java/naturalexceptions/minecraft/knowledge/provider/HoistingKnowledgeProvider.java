package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import naturalexceptions.core.hoisting.HoistMasses;
import naturalexceptions.minecraft.furniture.TileEntityDerrickPivot;
import naturalexceptions.minecraft.furniture.TileEntityHoist;
import naturalexceptions.minecraft.item.ItemHoistRope;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.multiblock.DerrickDefinition;
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
                .addRelated("derrick")
                .addRelated("counterweight_box")
                .build());
        ItemStack pivot = new ItemStack(NEBlocks.derrickPivot != null ? NEBlocks.derrickPivot : Blocks.cobblestone);
        ItemStack box = new ItemStack(NEBlocks.counterweightBox != null ? NEBlocks.counterweightBox : Blocks.planks);
        registry.registerEntry(
            KnowledgeEntry.builder("derrick", "Derrick", KnowledgeCategory.MULTIBLOCKS)
                .icon(pivot)
                .summary("A hoist on a boom that swings: lift out of a pit, swing round, set down on the lip.")
                .description(
                    "Set a Derrick Pivot on the ground and stand a mast of logs on it, " + DerrickDefinition.MIN_MAST
                        + " to "
                        + DerrickDefinition.MAX_MAST
                        + " high. From the mast's "
                        + "top run a boom of logs out level, "
                        + DerrickDefinition.MIN_BOOM
                        + " to "
                        + DerrickDefinition.MAX_BOOM
                        + " long, with a Sheave at its tip (placed facing along the "
                        + "boom). A Windlass on the same ground within "
                        + DerrickDefinition.REACH
                        + " blocks of the "
                        + "pivot works it: its rope runs up the mast, out along the boom, over the sheave and down "
                        + "to a Basket set under the tip.\n\n"
                        + "Wind and brake it as a headframe's. Right-click the pivot to swing the boom a "
                        + "quarter-turn clockwise, sneak to swing it the other way: the basket goes round with the "
                        + "tip. The boom swings only when the way round is clear, and the basket only when it is "
                        + "hoisted up or its own way round is clear too: hoist, swing, lower.")
                .multiblock(DerrickDefinition.INSTANCE)
                .addProperty(
                    "Mast",
                    DerrickDefinition.MIN_MAST + "-" + DerrickDefinition.MAX_MAST + " logs on the pivot")
                .addProperty(
                    "Boom",
                    DerrickDefinition.MIN_BOOM + "-" + DerrickDefinition.MAX_BOOM + " out, Sheave at the tip")
                .addProperty("Swing", "A quarter-turn a time, " + TileEntityDerrickPivot.TICKS / 20 + " s")
                .addRelated("headframe")
                .addRelated("counterweight_box")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("counterweight_box", "Counterweight Box", KnowledgeCategory.MULTIBLOCKS)
                .icon(box)
                .summary("A box of stone on the windlass's drum, wound the other way: it balances the basket.")
                .description(
                    "Dig a well straight down behind a windlass (on the side away from the rope), as deep as "
                        + "the basket is to travel, and set a Counterweight Box in it: the windlass hangs it from "
                        + "its drum, the other way round from the basket. As the basket goes down the box comes up; "
                        + "as the basket comes up the box goes down, and its well's floor and mouth are the "
                        + "basket's limits.\n\n"
                        + "Right-click the hanging box with a block to load it, with an empty hand to take a block "
                        + "back. Weighted heavier than the basket, the basket rises with the brake off; lighter, "
                        + "it sinks, slower than it would alone. Two blocks of stone about balance a rider.")
                .addProperty("Capacity", TileEntityHoist.COUNTER_CAPACITY + " blocks")
                .addProperty(
                    "Weight",
                    String.format("Box %.0f kg, %.0f kg a block", HoistMasses.COUNTERWEIGHT_BOX, HoistMasses.BLOCK))
                .addRelated("headframe")
                .addRelated("derrick")
                .build());
    }
}
