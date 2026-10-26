package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.power.Wind;
import naturalexceptions.minecraft.fabrication.CastingFold;
import naturalexceptions.minecraft.gear.GearKind;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ToolEdge;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.worldgen.WindGates;

/**
 * Guidance past the first copper: steel and the tool tiers, the sky and what waits in it, gear, and keeping alive.
 * The Field Manual stops at a copper casting; these go on from there.
 */
public final class WorldKnowledgeProvider implements IKnowledgeProvider {

    private static ItemStack icon(Block block) {
        return new ItemStack(block != null ? block : Blocks.stone);
    }

    private static ItemStack icon(Item item) {
        return new ItemStack(item != null ? item : net.minecraft.init.Items.paper);
    }

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        smithing(registry);
        sky(registry);
        gear(registry);
        survival(registry);
    }

    // --- Smithing -------------------------------------------------------------------------------------------

    private static void smithing(KnowledgeRegistry registry) {
        registry.registerEntry(
            KnowledgeEntry.builder("tool_tiers", "Tool Tiers", KnowledgeCategory.ASSEMBLY)
                .icon(new ItemStack(net.minecraft.init.Items.iron_pickaxe))
                .summary("A cast tool mines by what its head is made of.")
                .description(
                    "A head's metal sets what it can mine; its shape and its wear set only how fast. Copper mines like "
                        + "stone (iron ore, but not diamond); bronze and iron like iron; steel like diamond. Soft gold "
                        + "and tin mine like wood.\n\n"
                        + "A steel head is hard enough to take an inlaid edge (see Tool Edges).")
                .addProperty("Copper", tier(BuiltinMaterials.COPPER_PROPERTIES.hardness()))
                .addProperty("Iron", tier(BuiltinMaterials.IRON_PROPERTIES.hardness()))
                .addProperty("Steel", tier(BuiltinMaterials.STEEL_PROPERTIES.hardness()))
                .addRelated("steel")
                .addRelated("tool_edges")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("steel", "Steel", KnowledgeCategory.METALLURGY)
                .icon(new ItemStack(net.minecraft.init.Items.coal, 1, 1))
                .summary("Iron that has taken up a little carbon from charcoal while molten.")
                .description(
                    "Put charcoal (not coal) in the smelter's crucible, not its firebox: it lies in the charge as "
                        + "carbon. When the iron there melts, it takes the carbon up. A little makes Steel, harder than "
                        + "iron and tough; much more makes brittle Cast Iron, which melts far lower.\n\n"
                        + "One piece of charcoal is right for one to ten ingots' worth of iron. Iron melts at some "
                        + (int) BuiltinMaterials.IRON_PROPERTIES.meltingPointKelvin()
                        + " K: past what a plain fire reaches. It takes forced draught and a Colossus Crucible in the "
                        + "shell, or two Electric Heaters on a well-charged Battery Rack.")
                .addProperty("Carbon", "0.2 to 2 parts in a hundred: steel; more: cast iron")
                .addProperty("Hardness", String.format("%.1f (iron %.1f)", BuiltinMaterials.STEEL_PROPERTIES.hardness(), BuiltinMaterials.IRON_PROPERTIES.hardness()))
                .addRelated("tool_tiers")
                .addRelated("power_machines")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("casting_fold", "Folding Castings", KnowledgeCategory.CASTING)
                .icon(icon(NEBlocks.forgingAnvil))
                .summary("Castings piled on the anvil fold into a full ingot.")
                .description(
                    "An ingot mould's casting is a fifth of an ingot. Lay castings of the same metal on one another on "
                        + "the Forging Anvil (by hand, or warm in tongs) and they pile up. Once the pile holds an "
                        + "ingot's worth, strike it with the hammer: one blow folds an ingot off it while it is hot "
                        + "enough to work, " + CastingFold.COLD_BLOWS
                        + " cold. What is over stays on the anvil.\n\n"
                        + "Copper castings make copper ingots (nine make a Copper Block); iron and gold castings, iron "
                        + "and gold ingots.")
                .addProperty("An ingot", "about five castings")
                .addRelated("tool_tiers")
                .build());
        StringBuilder edges = new StringBuilder();
        for (ToolEdge edge : ToolEdge.values()) edges.append(edge.prefix)
            .append(": ")
            .append(edge.effect)
            .append("\n\n");
        registry.registerEntry(
            KnowledgeEntry.builder("tool_edges", "Tool Edges", KnowledgeCategory.ASSEMBLY)
                .icon(icon(NEItems.lumeniteShard))
                .summary("Three Lumenite shards, or three pieces of a fallen star's crust, inlaid along a steel head.")
                .description(
                    "Craft a steel tool with three Lumenite shards, or three samples of Fused Impact Crust from a Star "
                        + "Rain crater. Only a head that mines like diamond holds an edge, and it holds one only. An "
                        + "edged tool cuts faster, and each edge has a gift of its own:\n\n"
                        + edges.toString()
                            .trim())
                .addRelated("tool_tiers")
                .addRelated("star_rain")
                .build());
    }

    private static String tier(double hardness) {
        return new String[] { "like wood", "like stone", "like iron", "like diamond" }[DerivedCapability
            .harvestLevel(hardness)];
    }

    // --- The sky --------------------------------------------------------------------------------------------

    private static void sky(KnowledgeRegistry registry) {
        registry.registerEntry(
            KnowledgeEntry.builder("sky_islands", "The Sky Islands", KnowledgeCategory.SKY)
                .icon(icon(NEBlocks.skystoneRock))
                .summary("Land high over the world, held up on the wind, and what it is made of.")
                .description(
                    "Islands float over the land, from some y 130 up. Their rock is Skystone, which a Mason's Bench "
                        + "dresses into building stone; Stormglass grows in veins in it, the sky's own crystal, the heart "
                        + "of most of its machines. Cloud gathers on their rims and under them: it is no floor. Step onto "
                        + "it and you sink slowly through, unhurt, and below it is the long drop; hold jump to climb "
                        + "back up out of it. Only a pair of Cloud Treads stands on cloud. Windbloom grows "
                        + "on the islands (steeped, it keeps falls from hurting for a while).\n\n"
                        + "Up there the wind blows full (it is still on the ground and rises from y " + Wind.CALM_Y
                        + "), so windmills give their best. Nothing lives on the islands of itself: what you meet "
                        + "there keeps the dungeons.")
                .addRelated("sky_dungeons")
                .addRelated("wind_gates")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("sky_dungeons", "Sky Dungeons", KnowledgeCategory.SKY)
                .icon(icon(NEItems.skyKey))
                .summary("Halls under the great islands: a key hidden in them, a guardian, and its vault.")
                .description(
                    "The great islands hold dungeons, built by a civilisation that is dead now. Each hides a Sky Key "
                        + "in one of its rooms; held, the key pulls toward the dungeon's sealed vault, and used it says "
                        + "how far and how deep. A Surveyor's Chart is left in the entrance hall: opened, it maps the "
                        + "level you are on as you walk it.\n\n"
                        + "Deep down, behind the key, the dungeon's guardian waits; the vault opens only when it falls. "
                        + "A Citadel is a whole dead city, its Automatons still keeping the quarantine its Castellan "
                        + "set; a Sanctum is where the dead were carried, and they are gathered there still.")
                .addRelated("sky_islands")
                .addRelated("gear_screen")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("reaching_the_sky", "Reaching the Sky", KnowledgeCategory.SKY)
                .icon(icon(NEItems.weatherGlass))
                .summary("Finding the islands from the ground, and getting up to them.")
                .description(
                    "A Weather Glass (glass round a water bottle, a copper cap) finds the sky from the ground: held, a "
                        + "drift of mist runs from it toward the nearest island overhead, violet if something is built "
                        + "in it; used, it says which way, how far and how high.\n\n"
                        + "A Cloud Condenser gathers Cloud: high in the sky always, and on the ground while rain falls "
                        + "on it (made with a copper coil and stone bricks on the ground, or Stormglass and Skystone). "
                        + "Loose cloud is no floor, but two pressed together at the Loom make Packed Cloud, which bears "
                        + "weight. Used toward open air, Packed Cloud sets itself there touching another, up to "
                        + naturalexceptions.minecraft.block.BlockPackedCloud.REACH
                        + " blocks out from anything solid: a stair can lean out over the drop.\n\n"
                        + "Beside every great island, and some smaller ones, warm air rises in a column from low down "
                        + "to a little over the island's top, just off its rock; wisps of mist drifting up show where. "
                        + "Stand in it and it carries you slowly up; hold jump with a Glider Cloak on and it carries you "
                        + "fast. At the top, step off onto the island: the cloud rim leaves a gap there. Step out on "
                        + "the way up, and you fall.")
                .addRelated("sky_islands")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("wind_gates", "Wind Gates", KnowledgeCategory.SKY)
                .icon(new ItemStack(Blocks.stonebrick))
                .summary("The great stone arches of the land: the wind runs through them.")
                .description(
                    "Walk beneath a Wind Gate's span and the wind through it carries you on: a Tailwind, quicker feet "
                        + "and a higher jump, for " + WindGates.TAILWIND_TICKS / 1200
                        + " minutes. Now and then a gate's keystone holds a sealed Gale Core.\n\n"
                        + "The tall stone spires of the land carry a hermit's lookout on top, with a log of the "
                        + "bearings to the nearest sky dungeons.")
                .addRelated("sky_islands")
                .build());
    }

    // --- Gear -----------------------------------------------------------------------------------------------

    private static void gear(KnowledgeRegistry registry) {
        registry.registerEntry(
            KnowledgeEntry.builder("gear_screen", "Gear", KnowledgeCategory.GEAR)
                .icon(icon(NEItems.TREASURES.get(Treasure.GLIDER_CLOAK)))
                .summary("Worn things with real effects, in the Gear tab of your inventory.")
                .description(
                    "Open your inventory and pick the Gear tab: a hat, a face, a neck, a back, a belt, two gloves, two "
                        + "shoes and ten rings, each slot for its own kind of thing. Some pieces are stronger as a pair "
                        + "(gloves, shoes). One with an ability is used with the Gear Ability key (V).\n\n"
                        + "Gear is found in sky dungeons' chests and vaults, salvaged from the dead city's machines, "
                        + "and left by the guardians as relics; some of it can be made.")
                .addRelated("sky_dungeons")
                .build());
        for (Treasure treasure : Treasure.values()) {
            Item item = NEItems.TREASURES.get(treasure);
            if (item == null) continue;
            String name = title(treasure, new ItemStack(item).getDisplayName());
            registry.registerEntry(
                KnowledgeEntry.builder("gear_" + treasure.registryName(), name, KnowledgeCategory.GEAR)
                    .icon(new ItemStack(item))
                    .summary(treasure.effect())
                    .description(
                        treasure.effect() + "\n\nWorn in its " + slot(treasure.kind())
                            .toLowerCase() + " slot, in the Gear tab of your inventory. " + source(treasure.source()) + ".")
                    .addProperty("Worn", slot(treasure.kind()))
                    .addProperty("Found", source(treasure.source()))
                    .addRelated("gear_screen")
                    .build());
        }
    }

    /** Its name, or, where names are not loaded (a server), one made from its registry name. */
    private static String title(Treasure treasure, String shown) {
        if (shown != null && !shown.startsWith("item.")) return shown;
        StringBuilder out = new StringBuilder();
        for (String word : treasure.registryName()
            .split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1));
        }
        return out.toString();
    }

    private static String slot(GearKind kind) {
        String name = kind.name()
            .toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String source(Treasure.Source source) {
        switch (source) {
            case VAULT:
                return "In a sky dungeon's vault";
            case WARDEN:
                return "Left by the Castellan, a relic";
            case SALVAGE:
                return "Salvaged from the dead city's machines";
            case GATHERED:
                return "Left by the Gathered, a relic";
            default:
                return "In sky dungeons' chests; it can be made";
        }
    }

    // --- Keeping alive --------------------------------------------------------------------------------------

    private static void survival(KnowledgeRegistry registry) {
        registry.registerEntry(
            KnowledgeEntry.builder("thirst", "Thirst", KnowledgeCategory.SURVIVAL)
                .icon(new ItemStack(net.minecraft.init.Items.potionitem))
                .summary("The body needs water, as it needs food.")
                .description(
                    "Run dry, and thirst takes health a little at a time. Drink: a water bottle, milk, a barrel, a "
                        + "fountain.")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("injuries", "Wounds and Treatment", KnowledgeCategory.SURVIVAL)
                .icon(icon(NEItems.bandage))
                .summary("Blows land on a part of the body: cuts bleed, bones break, burns blister.")
                .description(
                    "The Body tab of your inventory shows each part and what is wrong with it. A Field Dressing and "
                        + "Splint, dropped on a wound there, dresses a cut or burn or splints a break; treatment helps "
                        + "the body heal, it does not undo the injury.\n\n"
                        + "Styptic Salve slows every bleeding cut at once and makes it clot fast. An Iron Tonic makes "
                        + "back lost blood over a minute. Smoke is breathed in: a Filter Mask on the face takes out "
                        + "most of it.")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("star_rain", "Star Rain", KnowledgeCategory.SURVIVAL)
                .icon(icon(NEItems.materialSample))
                .summary("Now and then the sky falls: a shower of stars, and a crater where one came down.")
                .description(
                    "A Star Rain comes down over the land on certain nights. Where a star lands it leaves a crater: a "
                        + "ball of Fused Impact Crust round a core of Stellar Matter, still hot. Both are worth "
                        + "digging out: the crust edges steel tools, and the two together make the sword Skyfall. A "
                        + "Weatherwise Hood warns of a Star Rain the day before.")
                .addRelated("tool_edges")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("blood_night", "Blood Night", KnowledgeCategory.SURVIVAL)
                .icon(new ItemStack(net.minecraft.init.Items.rotten_flesh))
                .summary("Once a year the sky goes red, and the dead walk thicker.")
                .description(
                    "Late in each year comes a Blood Night: a red sky, and more of the dead about until morning. A "
                        + "Weatherwise Hood warns of one the day before.")
                .build());
    }
}
