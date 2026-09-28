package naturalexceptions.minecraft.metallurgy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Minecraft-facing host bindings for the host-independent ore feedstock registry.
 *
 * <p>
 * Mods and content packs can register their own exact items, blocks, or OreDictionary names without adding
 * another branch to the smelter. A binding distinguishes raw ore input from a metallic product made by that ore.
 */
public final class FeedstockItemRegistry {

    public enum InputKind {
        RAW_ORE,
        METALLIC_PRODUCT
    }

    public static final class ResolvedInput {

        private final OreFeedstockDefinition definition;
        private final MaterialRef materialRef;
        private final CanonicalComposition composition;
        private final MaterialProperties properties;
        private final InputKind kind;
        private final MaterialQuantity inputMass;

        private ResolvedInput(OreFeedstockDefinition definition, InputKind kind) {
            this(definition, kind, null);
        }

        private ResolvedInput(OreFeedstockDefinition definition, InputKind kind, MaterialQuantity inputMass) {
            this.definition = definition;
            this.materialRef = definition.metallicProductRef();
            this.composition = definition.metallicProductComposition();
            this.properties = definition.metallicProperties();
            this.kind = kind;
            this.inputMass = inputMass != null ? inputMass
                : (kind == InputKind.RAW_ORE ? definition.standardChunkMass() : MaterialQuantity.ofKilograms(1.0));
        }

        private ResolvedInput(MaterialRef materialRef, CanonicalComposition composition,
            MaterialProperties properties) {
            this(materialRef, composition, properties, null);
        }

        private ResolvedInput(MaterialRef materialRef, CanonicalComposition composition, MaterialProperties properties,
            MaterialQuantity inputMass) {
            this.definition = null;
            this.materialRef = materialRef;
            this.composition = composition;
            this.properties = properties;
            this.kind = InputKind.METALLIC_PRODUCT;
            this.inputMass = inputMass != null ? inputMass : MaterialQuantity.ofKilograms(1.0);
        }

        public OreFeedstockDefinition definition() {
            return definition;
        }

        public MaterialRef materialRef() {
            return materialRef;
        }

        public CanonicalComposition composition() {
            return composition;
        }

        public MaterialProperties properties() {
            return properties;
        }

        public InputKind kind() {
            return kind;
        }

        public MaterialQuantity inputMass() {
            return inputMass;
        }
    }

    private static final class ItemKey {

        private final Item item;
        private final int metadata;

        private ItemKey(Item item, int metadata) {
            this.item = item;
            this.metadata = metadata;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ItemKey)) return false;
            ItemKey that = (ItemKey) other;
            return item == that.item && metadata == that.metadata;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + metadata;
        }
    }

    private static final Map<ItemKey, ResolvedInput> ITEMS = new HashMap<>();
    private static final Map<Block, ResolvedInput> BLOCKS = new HashMap<>();
    private static final Map<String, ResolvedInput> ORE_DICTIONARY = new HashMap<>();
    private static boolean builtinsRegistered;

    private FeedstockItemRegistry() {}

    public static void registerFeedstock(OreFeedstockDefinition definition, String rawOreDictionaryName,
        String metallicProductOreDictionaryName) {
        OreFeedstockDefinition.register(Objects.requireNonNull(definition, "definition"));
        if (rawOreDictionaryName != null && !rawOreDictionaryName.isEmpty()) {
            registerOreDictionaryName(rawOreDictionaryName, definition.feedstockId(), InputKind.RAW_ORE);
        }
        if (metallicProductOreDictionaryName != null && !metallicProductOreDictionaryName.isEmpty()) {
            registerOreDictionaryName(
                metallicProductOreDictionaryName,
                definition.feedstockId(),
                InputKind.METALLIC_PRODUCT);
        }
    }

    public static void registerMaterialProduct(String oreDictionaryName, MaterialRef materialRef,
        CanonicalComposition composition, MaterialProperties properties) {
        registerMaterialProduct(oreDictionaryName, materialRef, composition, properties, null);
    }

    public static void registerMaterialProduct(String oreDictionaryName, MaterialRef materialRef,
        CanonicalComposition composition, MaterialProperties properties, MaterialQuantity inputMass) {
        if (oreDictionaryName == null || oreDictionaryName.isEmpty()) {
            throw new IllegalArgumentException("OreDictionary name cannot be empty");
        }
        ORE_DICTIONARY.put(
            oreDictionaryName,
            new ResolvedInput(
                Objects.requireNonNull(materialRef, "materialRef"),
                Objects.requireNonNull(composition, "composition"),
                Objects.requireNonNull(properties, "properties"),
                inputMass));
    }

    public static void registerItem(Item item, int metadata, ResourceId feedstockId, InputKind kind) {
        registerItem(item, metadata, feedstockId, kind, null);
    }

    public static void registerItem(Item item, int metadata, ResourceId feedstockId, InputKind kind,
        MaterialQuantity inputMass) {
        ITEMS.put(new ItemKey(Objects.requireNonNull(item, "item"), metadata), resolve(feedstockId, kind, inputMass));
    }

    public static void registerBlock(Block block, ResourceId feedstockId, InputKind kind) {
        registerBlock(block, feedstockId, kind, null);
    }

    public static void registerBlock(Block block, ResourceId feedstockId, InputKind kind, MaterialQuantity inputMass) {
        BLOCKS.put(Objects.requireNonNull(block, "block"), resolve(feedstockId, kind, inputMass));
    }

    public static void registerOreDictionaryName(String name, ResourceId feedstockId, InputKind kind) {
        registerOreDictionaryName(name, feedstockId, kind, null);
    }

    public static void registerOreDictionaryName(String name, ResourceId feedstockId, InputKind kind,
        MaterialQuantity inputMass) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("OreDictionary name cannot be empty");
        }
        ORE_DICTIONARY.put(name, resolve(feedstockId, kind, inputMass));
    }

    public static ResolvedInput resolve(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        ResolvedInput exact = ITEMS.get(new ItemKey(stack.getItem(), stack.getItemDamage()));
        if (exact == null) {
            exact = ITEMS.get(new ItemKey(stack.getItem(), -1));
        }
        if (exact != null) {
            return exact;
        }

        Block block = Block.getBlockFromItem(stack.getItem());
        ResolvedInput blockInput = block == null ? null : BLOCKS.get(block);
        if (blockInput != null) {
            return blockInput;
        }

        try {
            for (int oreId : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
                ResolvedInput oreInput = ORE_DICTIONARY.get(net.minecraftforge.oredict.OreDictionary.getOreName(oreId));
                if (oreInput != null) {
                    return oreInput;
                }
            }
        } catch (Throwable ignored) {
            // Some unit-test item stacks are not fully registered with the legacy OreDictionary.
        }
        return null;
    }

    public static boolean isRegistered(ItemStack stack) {
        return resolve(stack) != null;
    }

    /** Registers the built-in host representations; safe to call after either item or block registration. */
    public static void registerBuiltins() {
        OreFeedstockDefinition copper = OreFeedstockDefinition.copperOre();
        OreFeedstockDefinition tin = OreFeedstockDefinition.tinOreChunk();
        OreFeedstockDefinition lead = OreFeedstockDefinition.leadOreChunk();
        OreFeedstockDefinition iron = OreFeedstockDefinition.ironOreChunk();
        OreFeedstockDefinition gold = OreFeedstockDefinition.goldOreChunk();

        if (NEItems.copperIngot != null) {
            registerItem(NEItems.copperIngot, -1, copper.feedstockId(), InputKind.METALLIC_PRODUCT);
        }
        if (NEBlocks.copperOre != null) {
            registerBlock(NEBlocks.copperOre, copper.feedstockId(), InputKind.RAW_ORE);
        }
        if (NEBlocks.copperBlock != null) {
            registerBlock(
                NEBlocks.copperBlock,
                copper.feedstockId(),
                InputKind.METALLIC_PRODUCT,
                MaterialQuantity.ofKilograms(9.0));
        }

        // Vanilla Iron
        try {
            if (net.minecraft.init.Items.iron_ingot != null) {
                registerItem(
                    net.minecraft.init.Items.iron_ingot,
                    -1,
                    iron.feedstockId(),
                    InputKind.METALLIC_PRODUCT,
                    MaterialQuantity.ofKilograms(1.0));
            }
            if (net.minecraft.init.Blocks.iron_ore != null) {
                registerBlock(
                    net.minecraft.init.Blocks.iron_ore,
                    iron.feedstockId(),
                    InputKind.RAW_ORE,
                    MaterialQuantity.ofKilograms(1.0));
            }
            if (net.minecraft.init.Blocks.iron_block != null) {
                registerBlock(
                    net.minecraft.init.Blocks.iron_block,
                    iron.feedstockId(),
                    InputKind.METALLIC_PRODUCT,
                    MaterialQuantity.ofKilograms(9.0));
            }
        } catch (Throwable ignored) {}

        // Vanilla Gold
        try {
            if (net.minecraft.init.Items.gold_ingot != null) {
                registerItem(
                    net.minecraft.init.Items.gold_ingot,
                    -1,
                    gold.feedstockId(),
                    InputKind.METALLIC_PRODUCT,
                    MaterialQuantity.ofKilograms(1.0));
            }
            if (net.minecraft.init.Blocks.gold_ore != null) {
                registerBlock(
                    net.minecraft.init.Blocks.gold_ore,
                    gold.feedstockId(),
                    InputKind.RAW_ORE,
                    MaterialQuantity.ofKilograms(1.0));
            }
            if (net.minecraft.init.Blocks.gold_block != null) {
                registerBlock(
                    net.minecraft.init.Blocks.gold_block,
                    gold.feedstockId(),
                    InputKind.METALLIC_PRODUCT,
                    MaterialQuantity.ofKilograms(9.0));
            }
            if (net.minecraft.init.Items.gold_nugget != null) {
                registerItem(
                    net.minecraft.init.Items.gold_nugget,
                    -1,
                    gold.feedstockId(),
                    InputKind.METALLIC_PRODUCT,
                    MaterialQuantity.ofMilligrams(111111));
            }
        } catch (Throwable ignored) {}

        registerOreDictionaryName("oreCopper", copper.feedstockId(), InputKind.RAW_ORE);
        registerOreDictionaryName("oreTin", tin.feedstockId(), InputKind.RAW_ORE);
        registerOreDictionaryName("oreLead", lead.feedstockId(), InputKind.RAW_ORE);
        registerOreDictionaryName("ingotCopper", copper.feedstockId(), InputKind.METALLIC_PRODUCT);
        registerOreDictionaryName("ingotTin", tin.feedstockId(), InputKind.METALLIC_PRODUCT);
        registerOreDictionaryName("ingotLead", lead.feedstockId(), InputKind.METALLIC_PRODUCT);
        registerOreDictionaryName("dustCopper", copper.feedstockId(), InputKind.METALLIC_PRODUCT);
        registerOreDictionaryName("dustTin", tin.feedstockId(), InputKind.METALLIC_PRODUCT);
        registerOreDictionaryName("dustLead", lead.feedstockId(), InputKind.METALLIC_PRODUCT);
        registerOreDictionaryName(
            "blockCopper",
            copper.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(9.0));

        registerOreDictionaryName("oreIron", iron.feedstockId(), InputKind.RAW_ORE, MaterialQuantity.ofKilograms(1.0));
        registerOreDictionaryName(
            "ingotIron",
            iron.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(1.0));
        registerOreDictionaryName(
            "blockIron",
            iron.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(9.0));
        registerOreDictionaryName(
            "dustIron",
            iron.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(1.0));
        registerOreDictionaryName(
            "nuggetIron",
            iron.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofMilligrams(111111));

        registerOreDictionaryName("oreGold", gold.feedstockId(), InputKind.RAW_ORE, MaterialQuantity.ofKilograms(1.0));
        registerOreDictionaryName(
            "ingotGold",
            gold.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(1.0));
        registerOreDictionaryName(
            "blockGold",
            gold.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(9.0));
        registerOreDictionaryName(
            "dustGold",
            gold.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofKilograms(1.0));
        registerOreDictionaryName(
            "nuggetGold",
            gold.feedstockId(),
            InputKind.METALLIC_PRODUCT,
            MaterialQuantity.ofMilligrams(111111));

        registerMaterialProduct(
            "ingotBronze",
            BuiltinMaterials.BRONZE.ref(),
            BuiltinMaterials.BRONZE_COMPOSITION,
            BuiltinMaterials.BRONZE_PROPERTIES);
        registerMaterialProduct(
            "dustBronze",
            BuiltinMaterials.BRONZE.ref(),
            BuiltinMaterials.BRONZE_COMPOSITION,
            BuiltinMaterials.BRONZE_PROPERTIES);
        builtinsRegistered = true;
    }

    public static boolean builtinsRegistered() {
        return builtinsRegistered;
    }

    private static ResolvedInput resolve(ResourceId feedstockId, InputKind kind, MaterialQuantity inputMass) {
        OreFeedstockDefinition definition = OreFeedstockDefinition.forFeedstockId(feedstockId);
        if (definition == null) {
            throw new IllegalArgumentException("Feedstock is not registered: " + feedstockId);
        }
        return new ResolvedInput(definition, Objects.requireNonNull(kind, "kind"), inputMass);
    }
}
