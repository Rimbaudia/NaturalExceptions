package naturalexceptions.minecraft.tick;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.electrical.CircuitNetwork;
import naturalexceptions.core.electrical.ResistiveConductor;
import naturalexceptions.core.electrical.ResistiveLoad;
import naturalexceptions.core.electrical.SwitchElement;
import naturalexceptions.core.electrical.VoltageSource;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemFluidVessel;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEItems;

/** Operator-only P9 acceptance controls; these do not participate in normal progression. */
public final class CommandP9 extends CommandBase {
    private static final String CIRCUIT_ID = "p9_acceptance_dc_chain";
    @Override public String getCommandName() { return "ne_p9"; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/ne_p9 <fluid status|inspect|disturb|move [direction]|cool <seconds>|water|vessel|circuit create|inspect|open|close|advance <seconds>>";
    }
    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override public void processCommand(ICommandSender sender, String[] args) {
        World world = sender.getEntityWorld();
        if (world == null || world.isRemote || world.provider.dimensionId != 0) {
            sender.addChatMessage(new ChatComponentText("P9 acceptance tools require the server Overworld.")); return;
        }
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        if (saved == null) return;
        if (args.length < 2) { sender.addChatMessage(new ChatComponentText(getCommandUsage(sender))); return; }
        if ("fluid".equalsIgnoreCase(args[0])) fluid(sender, world, saved, args);
        else if ("circuit".equalsIgnoreCase(args[0])) circuit(sender, saved, args);
        else sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    private void fluid(ICommandSender sender, World world, NEWorldSavedData saved, String[] args) {
        if ("vessel".equalsIgnoreCase(args[1]) || "water".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof EntityPlayer) || NEItems.fluidVessel == null) return;
            ItemStack stack = new ItemStack(NEItems.fluidVessel);
            if ("water".equalsIgnoreCase(args[1])) ItemFluidVessel.setContents(stack, waterParcel());
            EntityPlayer player = (EntityPlayer) sender;
            if (!player.inventory.addItemStackToInventory(stack)) player.dropPlayerItemWithRandomChoice(stack, false);
            player.inventoryContainer.detectAndSendChanges();
            sender.addChatMessage(new ChatComponentText("Created a " + ("water".equalsIgnoreCase(args[1])
                ? "finite 1 L water parcel" : "empty 1 L fluid vessel") + "."));
            return;
        }
        int coordinatesAt = "move".equalsIgnoreCase(args[1]) ? 3 : 2;
        FluidCellKey key = positionKey(sender, world, args, coordinatesAt);
        if (key == null) {
            sender.addChatMessage(new ChatComponentText("Use integer x y z coordinates for this fluid command."));
            return;
        }
        if ("inspect".equalsIgnoreCase(args[1])) {
            FluidParcel parcel = saved.fluids().parcelAt(key);
            sender.addChatMessage(new ChatComponentText(parcel == null ? "No explicit fluid parcel at " + key
                : parcel.materialRef() + " | " + parcel.mass() + " | " + parcel.volumeMicrolitres() + " uL | "
                    + parcel.phase() + " | " + parcel.thermalState().temperatureKelvin() + " K"));
        } else if ("disturb".equalsIgnoreCase(args[1])) {
            saved.fluids().disturb(key); saved.markDirty();
            sender.addChatMessage(new ChatComponentText("Queued local fluid-region rebuild at " + key));
        } else if ("move".equalsIgnoreCase(args[1])) {
            int[] delta = direction(args.length > 2 ? args[2] : "down");
            FluidParcel parcel = saved.fluids().parcelAt(key);
            FluidCellKey target = new FluidCellKey(key.dimension(), key.x() + delta[0], key.y() + delta[1], key.z() + delta[2]);
            if (parcel == null || !world.isAirBlock(target.x(), target.y(), target.z())) {
                sender.addChatMessage(new ChatComponentText("Stand on an explicit fluid cell with an empty destination beside it."));
                return;
            }
            int meta = naturalexceptions.minecraft.fluid.BlockFluidCell.metadataFor(parcel);
            if (!world.setBlock(target.x(), target.y(), target.z(), naturalexceptions.minecraft.registry.NEBlocks.fluidCell,
                meta, 3)) return;
            if (!saved.fluids().move(key, target)) { world.setBlockToAir(target.x(), target.y(), target.z()); return; }
            world.setBlockToAir(key.x(), key.y(), key.z()); saved.markDirty();
            world.markBlockForUpdate(target.x(), target.y(), target.z());
            sender.addChatMessage(new ChatComponentText("Moved the finite parcel to " + target));
        } else if ("cool".equalsIgnoreCase(args[1])) {
            double seconds;
            try { seconds = Double.parseDouble(args.length > 2 ? args[2] : "0"); }
            catch (NumberFormatException bad) { sender.addChatMessage(new ChatComponentText("Use a finite number of seconds.")); return; }
            if (!Double.isFinite(seconds) || seconds < 0.0 || seconds > 10000000.0) {
                sender.addChatMessage(new ChatComponentText("Thermal elapsed time must be between 0 and 10000000 seconds."));
                return;
            }
            int work = saved.fluids().processThermalWork(seconds, 64);
            if (work > 0) saved.markDirty();
            sender.addChatMessage(new ChatComponentText("Applied " + seconds + " s to " + work
                + " queued hot-fluid cells; remaining active=" + saved.fluids().activeThermalCellCount()));
        } else if ("status".equalsIgnoreCase(args[1])) {
            sender.addChatMessage(new ChatComponentText("Fluid mass=" + saved.fluids().totalMass() + ", cells="
                + saved.fluids().snapshotCells().size() + ", regions=" + saved.fluids().regionCount()
                + ", sleeping=" + saved.fluids().sleepingRegionCount() + ", dirty=" + saved.fluids().dirtyCellCount()
                + ", active thermal=" + saved.fluids().activeThermalCellCount()));
        } else sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    private void circuit(ICommandSender sender, NEWorldSavedData saved, String[] args) {
        CircuitNetwork network = saved.electrical().network(CIRCUIT_ID);
        if ("create".equalsIgnoreCase(args[1])) {
            if (network == null) { network = createCircuit(); saved.electrical().register(network); saved.markDirty(); }
            sender.addChatMessage(new ChatComponentText("P9 source → copper conductor → open switch → 1 Ω heater created."));
            return;
        }
        if (network == null) { sender.addChatMessage(new ChatComponentText("Create the acceptance circuit first: /ne_p9 circuit create")); return; }
        if ("open".equalsIgnoreCase(args[1]) || "close".equalsIgnoreCase(args[1])) {
            network.switchElement().setClosed("close".equalsIgnoreCase(args[1])); saved.markDirty();
            CircuitStatus(sender, network, "Switch is now " + (network.switchElement().closed() ? "closed" : "open"));
        } else if ("advance".equalsIgnoreCase(args[1])) {
            double seconds;
            try { seconds = Double.parseDouble(args.length > 2 ? args[2] : "0"); }
            catch (NumberFormatException bad) { sender.addChatMessage(new ChatComponentText("Use a finite number of seconds.")); return; }
            if (!Double.isFinite(seconds) || seconds < 0.0 || seconds > 100000.0) {
                sender.addChatMessage(new ChatComponentText("Elapsed time must be between 0 and 100000 seconds.")); return;
            }
            network.advanceElapsed(seconds); saved.electrical().processWork(0.0, 16); saved.markDirty();
            CircuitStatus(sender, network, "Advanced powered physical state by " + seconds + " s");
        } else if ("inspect".equalsIgnoreCase(args[1])) {
            CircuitStatus(sender, network, "Network " + CIRCUIT_ID);
        } else sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    private static void CircuitStatus(ICommandSender sender, CircuitNetwork network, String heading) {
        naturalexceptions.core.electrical.CircuitSolution s = network.solve();
        sender.addChatMessage(new ChatComponentText(heading + ": V=" + s.voltageVolts() + " V, I=" + s.currentAmps()
            + " A, R=" + s.totalResistanceOhms() + " ohm, P(load)=" + s.loadPowerWatts() + " W, P(wire)="
            + s.conductorPowerWatts() + " W, fractured=" + network.conductors().get(0).damage().isFractured()
            + ", open=" + s.openCircuit() + ", dirty=" + network.isDirty()));
    }

    private static CircuitNetwork createCircuit() {
        MaterialProperties copper = BuiltinMaterials.COPPER_PROPERTIES;
        // Geometry is 1 m × 1e-8 m²; copper density gives 8.96e-5 kg of conductor.
        ActiveThermalState wireHeat = ActiveThermalState.ambient(8.96e-5, copper);
        ActiveThermalState loadHeat = ActiveThermalState.ambient(0.01, copper);
        ResistiveConductor wire = new ResistiveConductor("p9_copper_wire",
            MaterialRef.authored(BuiltinMaterials.COPPER_MATERIAL_ID), copper, 1.0, 1.0e-8, wireHeat,
            new ComponentDamage(), copper.meltingPointKelvin());
        ResistiveLoad load = new ResistiveLoad("p9_heater", 1.0, loadHeat, new ComponentDamage(), 5000.0);
        CircuitNetwork network = new CircuitNetwork(CIRCUIT_ID, new VoltageSource("p9_primitive_source", 12.0),
            new SwitchElement("p9_switch", false), load);
        network.addConductor(wire);
        return network;
    }

    private static FluidParcel waterParcel() {
        ResourceId waterId = ResourceId.of("minecraft", "water");
        naturalexceptions.core.composition.CanonicalComposition composition =
            naturalexceptions.core.composition.CanonicalComposition.of(waterId);
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        MaterialProperties properties = MaterialProperties.builder().density(1.0).specificHeatSolid(4180)
            .specificHeatLiquid(4180).meltingPointKelvin(273.15).latentHeatOfFusion(334000).build();
        MaterialQuantity mass = MaterialQuantity.ofKilograms(1);
        return new FluidParcel(MaterialRef.authored("minecraft", "water"), key, composition, mass,
            ActiveThermalState.ambient(mass.toKilograms(), properties));
    }

    private static FluidCellKey positionKey(ICommandSender sender, World world, String[] args, int coordinatesAt) {
        if (args.length >= coordinatesAt + 3) {
            try {
                return new FluidCellKey(world.provider.dimensionId, Integer.parseInt(args[coordinatesAt]),
                    Integer.parseInt(args[coordinatesAt + 1]), Integer.parseInt(args[coordinatesAt + 2]));
            } catch (NumberFormatException invalid) { return null; }
        }
        int x = 0, y = 64, z = 0;
        if (sender instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) sender; x = (int) Math.floor(p.posX);
            y = (int) Math.floor(p.posY); z = (int) Math.floor(p.posZ);
        }
        return new FluidCellKey(world.provider.dimensionId, x, y, z);
    }

    private static int[] direction(String name) {
        if ("up".equalsIgnoreCase(name)) return new int[] { 0, 1, 0 };
        if ("north".equalsIgnoreCase(name)) return new int[] { 0, 0, -1 };
        if ("south".equalsIgnoreCase(name)) return new int[] { 0, 0, 1 };
        if ("east".equalsIgnoreCase(name)) return new int[] { 1, 0, 0 };
        if ("west".equalsIgnoreCase(name)) return new int[] { -1, 0, 0 };
        return new int[] { 0, -1, 0 };
    }
}
