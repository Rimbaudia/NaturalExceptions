package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.registry.NEItems;

public final class MaterialKnowledgeProvider implements IKnowledgeProvider {

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        ItemStack copperStack = new ItemStack(NEItems.copperIngot != null ? NEItems.copperIngot : Items.iron_ingot);
        ItemStack ironStack = new ItemStack(Items.iron_ingot);
        ItemStack goldStack = new ItemStack(Items.gold_ingot);

        // 1. Copper
        registry.registerEntry(
            KnowledgeEntry.builder("material_copper", "Copper (Cu)", KnowledgeCategory.MATERIALS)
                .icon(copperStack)
                .summary("Ductile, malleable red-orange metal; the cornerstone of early metallurgy and tools.")
                .description(
                    "Copper is the first accessible working metal in Natural Exceptions.\n\n"
                        + "\u00a7lThermal Properties:\u00a7r\n"
                        + "Copper melts at 1085 \u00b0C (1358 K). This temperature is comfortably achievable in the "
                        + "Starter Smelter using passive air draft with common fuels (charcoal or coal).\n\n"
                        + "\u00a7lMechanical Applications:\u00a7r\n"
                        + "Cast copper pickaxes possess Harvest Level 1, capable of mining stone, coal, and copper ore. "
                        + "Copper is also used to craft a Tuyere. The Tuyere and Hand Bellows accelerate heating, "
                        + "but ordinary strong fuels can melt iron under natural draft over time.")
                .addProperty("Atomic Symbol", "Cu (29)")
                .addProperty("Melting Point", "1085 \u00b0C (1358 K)")
                .addProperty("Density", "8.96 g/cm\u00b3")
                .addProperty("Specific Heat (Solid)", "385 J/(kg\u00b7K)")
                .addProperty("Smelter Heat Tier", "Tier 1 (Passive Draft)")
                .addProperty("Tool Harvest Level", "Level 1 (Stone, Coal, Copper)")
                .addRelated("copper_ore")
                .addRelated("starter_smelter")
                .addRelated("smelter_tuyere")
                .build());

        // 2. Iron
        registry.registerEntry(
            KnowledgeEntry.builder("material_iron", "Iron (Fe)", KnowledgeCategory.MATERIALS)
                .icon(ironStack)
                .summary("High-strength structural transition metal; enables mining diamond and redstone.")
                .description(
                    "Iron is a refractory, durable metal essential for advanced technology and mining.\n\n"
                        + "\u00a7lThermal Properties:\u00a7r\n"
                        + "Iron has a high melting point of 1538 \u00b0C (1811 K). Strong fuel such as charcoal or coal "
                        + "can reach this temperature with natural draft, though heating takes time. A mounted Tuyere "
                        + "and Hand Bellows increase draft and speed the process.\n\n"
                        + "\u00a7lMechanical Applications:\u00a7r\n"
                        + "Cast iron tools have Harvest Level 2, allowing extraction of redstone, lapis, gold, and diamonds. "
                        + "Iron possesses substantially higher yield strength and durability than copper.")
                .addProperty("Atomic Symbol", "Fe (26)")
                .addProperty("Melting Point", "1538 \u00b0C (1811 K)")
                .addProperty("Density", "7.87 g/cm\u00b3")
                .addProperty("Specific Heat (Solid)", "449 J/(kg\u00b7K)")
                .addProperty("Smelter Heat Tier", "Strong fuel; forced draft speeds heating")
                .addProperty("Tool Harvest Level", "Level 2 (Iron, Redstone, Diamond)")
                .addRelated("iron_ore")
                .addRelated("smelter_tuyere")
                .addRelated("bellows")
                .build());

        // 3. Gold
        registry.registerEntry(
            KnowledgeEntry.builder("material_gold", "Gold (Au)", KnowledgeCategory.MATERIALS)
                .icon(goldStack)
                .summary("Extremely dense, corrosion-proof noble metal with high malleability.")
                .description(
                    "Gold is a heavy precious metal that melts at 1064 \u00b0C (1337 K). It is easily melted under "
                        + "passive draft in the Starter Smelter. Its high density (19.32 g/cm\u00b3) makes gold castings "
                        + "exceptionally heavy.")
                .addProperty("Atomic Symbol", "Au (79)")
                .addProperty("Melting Point", "1064 \u00b0C (1337 K)")
                .addProperty("Density", "19.32 g/cm\u00b3")
                .addProperty("Smelter Heat Tier", "Tier 1 (Passive Draft)")
                .build());

    }
}
