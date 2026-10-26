package naturalexceptions.minecraft.knowledge.provider;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.core.power.Cable;
import naturalexceptions.core.power.PowerTopology;
import naturalexceptions.minecraft.knowledge.IKnowledgeProvider;
import naturalexceptions.minecraft.knowledge.KnowledgeCategory;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.power.Arc;
import naturalexceptions.minecraft.power.BlockFuse;
import naturalexceptions.minecraft.power.BlockPoweredDoor;
import naturalexceptions.minecraft.power.TileArcLamp;
import naturalexceptions.minecraft.power.TileBatteryRack;
import naturalexceptions.minecraft.power.TileCrankDynamo;
import naturalexceptions.minecraft.power.TileElectricHeater;
import naturalexceptions.minecraft.power.TileLightningSpire;
import naturalexceptions.minecraft.power.TileStormCell;
import naturalexceptions.minecraft.power.TileTeslaCoil;
import naturalexceptions.minecraft.power.TileWindmill;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Electricity: what makes it, what carries it, what it runs, and what it does to the careless. */
public final class PowerKnowledgeProvider implements IKnowledgeProvider {

    private static ItemStack icon(Block block) {
        return new ItemStack(block != null ? block : Blocks.iron_block);
    }

    private static ItemStack icon(Item item) {
        return new ItemStack(item != null ? item : net.minecraft.init.Items.iron_ingot);
    }

    @Override
    public void registerEntries(KnowledgeRegistry registry) {
        StringBuilder ratings = new StringBuilder();
        for (Cable cable : Cable.values()) ratings.append(
            String.format(
                "%s%s %.0f A",
                ratings.length() == 0 ? "" : ", ",
                cable.name()
                    .toLowerCase()
                    .replace('_', ' '),
                cable.ratedAmps()));
        registry.registerEntry(
            KnowledgeEntry.builder("power_basics", "Electricity", KnowledgeCategory.POWER)
                .icon(icon(NEBlocks.cable))
                .summary("Sources, cable and machines, joined face to face into a network that is solved as a circuit.")
                .description(
                    "Anything that carries current joins whatever carries current on its six faces: cable, a closed "
                        + "Knife Switch, a Fuse, a machine. A network needs something to drive it (a Crank Dynamo, a "
                        + "Windmill, a Storm Cell, a heart in a Heart Socket) and something to use it.\n\n"
                        + "Everything is solved as a real circuit: copper has resistance, so long thin runs lose "
                        + "voltage; cable warms with the current it carries and fails if it carries too much (cloth "
                        + "chars and leaves bare copper; bare copper melts through). Machines are rated for a voltage: "
                        + "under it they work weakly, far over it they burn out.\n\n"
                        + "Most sources give 24 V. High tension, some 130 V, comes only from Storm Cells racked in "
                        + "series in a Battery Rack, and it is dangerous: see High Tension. A Multimeter shows the "
                        + "volts, amps and state of whatever you use it on.")
                .addProperty("Cable carries", ratings.toString())
                .addProperty("Bare copper bites", String.format("above %.0f V", Cable.SHOCK_VOLTS))
                .addRelated("high_tension")
                .addRelated("power_sources")
                .addRelated("power_machines")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("power_sources", "Power Sources", KnowledgeCategory.POWER)
                .icon(icon(NEBlocks.windmill))
                .summary("A crank, the wind, Stormglass charge, and lightning.")
                .description(
                    "Crank Dynamo: hold use on it to turn it; it gives " + (int) TileCrankDynamo.EMF
                        + " V while you turn, and tires you as you do.\n\n"
                        + "Windmill: it gives " + (int) TileWindmill.VOLTS
                        + " V while the wind turns it, and more current the harder it blows. There is no wind on the "
                        + "ground: it rises with height from y " + naturalexceptions.core.power.Wind.CALM_Y
                        + " to full at y " + naturalexceptions.core.power.Wind.FULL_Y
                        + " (the sky islands stand in it), and blows harder in rain and storms. Keep the air before its "
                        + "sails open (a 5 x 5 disc).\n\n"
                        + "Storm Cell: Stormglass in brine, storing charge (" + (int) (TileStormCell.CAPACITY / 1000)
                        + " kJ). Its voltage runs from " + (int) TileStormCell.EMPTY_VOLTS
                        + " V flat to " + (int) TileStormCell.FULL_VOLTS
                        + " V full, so a 24 V network charges it only part way. Taken up, it keeps its charge.\n\n"
                        + "Heart Socket: holds an Automaton's Stormglass heart, a small steady source while its charge "
                        + "lasts. Broken, it gives the heart back.\n\n"
                        + "Lightning Spire: in a thunderstorm, open to the sky, it calls lightning down, and every "
                        + "Storm Cell on its network (racked ones too) shares the strike, "
                        + (int) (TileLightningSpire.STRIKE_JOULES / 1000)
                        + " kJ. Only lightning fills a cell.")
                .addRelated("battery_rack")
                .addRelated("power_basics")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("battery_rack", "Battery Rack", KnowledgeCategory.POWER)
                .icon(icon(NEBlocks.batteryRack))
                .summary("Up to five Storm Cells, joined side by side or end to end by a lever.")
                .description(
                    "Use it with a Storm Cell to rack it, empty-handed to take one off (it carries its share of the "
                        + "charge), and sneak and use it to throw the lever.\n\n"
                        + "PARALLEL (lever left): the cells stand side by side, at a cell's voltage, very stiff, and "
                        + "a 24 V windmill or dynamo charges them all.\n\n"
                        + "SERIES (lever right): their voltages add, up to "
                        + (int) (TileStormCell.FULL_VOLTS * TileBatteryRack.CELLS)
                        + " V with five full cells: high tension, for the Electric Heater. A rack in series cannot be "
                        + "charged from 24 V, so throw it back to parallel to charge it.\n\n"
                        + "Throwing the lever while it carries a load draws an arc.")
                .addProperty("Cells", TileBatteryRack.CELLS + " Storm Cells")
                .addProperty("Series", "about " + (int) (TileStormCell.FULL_VOLTS * TileBatteryRack.CELLS) + " V")
                .addRelated("high_tension")
                .addRelated("power_sources")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("high_tension", "High Tension", KnowledgeCategory.POWER)
                .icon(icon(NEBlocks.fuse))
                .summary("Some 130 V: what it runs, what it does to you, and how to keep it in its wires.")
                .description(
                    "Bare copper at high tension bites hard whoever walks into it; run insulated cable (rubbered "
                        + "braid: wool and leather or Cloudsilk) where anyone goes.\n\n"
                        + "A high-tension line broken while it carries a load arcs: cutting live cable, opening a "
                        + "Knife Switch, or throwing a Battery Rack's lever under load burns whoever did it and can "
                        + "set what is near alight. Open lines with nothing running on them, or off load.\n\n"
                        + "24 V machines (lamps, doors, plates) burn out at high tension: keep two networks.\n\n"
                        + "A Fuse is a thin copper link in a glass tube: when more than its rating flows for a moment "
                        + "it melts and opens the line there, and the rest is spared. Re-link a blown fuse with a "
                        + "length of copper wire. Its rating comes from the wire it is made with.")
                .addProperty("Arcs at", String.format("%.0f V and %.0f A or more", Arc.VOLTS, Arc.AMPS))
                .addProperty(
                    "Fuses",
                    String.format(
                        "%.0f A (wire), %.0f A (cable), %.0f A (heavy cable)",
                        BlockFuse.RATED_AMPS[0],
                        BlockFuse.RATED_AMPS[1],
                        BlockFuse.RATED_AMPS[2]))
                .addRelated("battery_rack")
                .addRelated("power_machines")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("power_machines", "Electric Machines", KnowledgeCategory.POWER)
                .icon(icon(NEBlocks.arcLamp))
                .summary("Lamps, heat for the smelter, defences, doors, and wireless relays.")
                .description(
                    "Arc Lamp: " + (int) TileArcLamp.RATED_WATTS + " W at " + (int) TileArcLamp.RATED_VOLTS
                        + " V, brighter with more power; driven far past its rating, its carbons burn through.\n\n"
                        + "Electric Heater: set into a Starter Smelter's shell in place of a brick. It is wound for "
                        + "high tension, " + (int) (TileElectricHeater.RATED_WATTS / 1000) + " kW at "
                        + (int) TileElectricHeater.RATED_VOLTS
                        + " V: one keeps a charge past copper's melting point, two on a well-charged rack past iron's. "
                        + "It needs heavy cable. On 24 V it only warms.\n\n"
                        + "Shock Plate: whatever stands on it is jolted, harder with more power behind it.\n\n"
                        + "Tesla Coil: charges up, and arcs at monsters within " + (int) TileTeslaCoil.REACH
                        + " blocks.\n\n"
                        + "Powered Door: an iron door held open while it gets power ("
                        + (int) BlockPoweredDoor.Tile.HOLD_WATTS + " W).\n\n"
                        + "Stormglass Relay: joins the network of every other relay within "
                        + PowerTopology.RELAY_REACH
                        + " blocks, through the air.\n\n"
                        + "Multimeter: use it on anything on a network to read it.")
                .addRelated("power_basics")
                .addRelated("high_tension")
                .build());
        registry.registerEntry(
            KnowledgeEntry.builder("multimeter", "Multimeter", KnowledgeCategory.POWER)
                .icon(icon(NEItems.multimeter))
                .summary("Use it on cable or a machine: volts, amps, charge, heat.")
                .description(
                    "Use it on any cable, switch, fuse or machine on a network. It reads the voltage there, the current "
                        + "through it, and what that part is doing: a cable's heat against what it can take, a cell's "
                        + "or a rack's charge, a lamp's or a heater's watts against its rating.")
                .addRelated("power_basics")
                .build());
    }
}
