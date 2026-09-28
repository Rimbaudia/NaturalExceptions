package naturalexceptions.minecraft.biology;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalInteractionResolver;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BiologicalTreatment;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.biology.ExposureBurden;
import naturalexceptions.core.biology.ExposureProtection;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.biology.PhysicalInteraction;
import naturalexceptions.core.biology.ArmorLayer;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;

/** Developer-only P10 acceptance controls; no command is part of progression. */
public final class CommandBiology extends CommandBase {
    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    @Override public String getCommandName() { return "ne_biology"; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/ne_biology <inspect|armor|cut <joules>|expose <route> <grams> [potency]|protect <route> <attenuation>|unprotect|treat|advance <seconds>|compare|hazard_cell>";
    }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        try { execute(sender, args); }
        catch (RuntimeException invalid) { sender.addChatMessage(new ChatComponentText("Invalid biology command. " + getCommandUsage(sender))); }
    }
    private void execute(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) { sender.addChatMessage(new ChatComponentText("Run this command as a player.")); return; }
        EntityPlayer player = (EntityPlayer) sender;
        World world = player.worldObj;
        if (world.isRemote) return;
        if (args.length == 0) { sender.addChatMessage(new ChatComponentText(getCommandUsage(sender))); return; }
        BiologicalState state = BiologicalProperties.get(player).state();
        if ("inspect".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText("Plan=" + state.bodyPlan().id() + " condition=" + state.condition()
                + " energy=" + state.energyReserveJoules() + " J hydration=" + state.hydrationReserveMillilitres()
                + " mL blood=" + state.bloodReserveMillilitres() + " mL injuries=" + state.injuries().size()
                + " exposure=" + state.exposureEffect() + " active=" + state.hasActiveProcesses()));
            for (BiologicalInjury wound : state.injuries()) sender.addChatMessage(new ChatComponentText(wound.kind()
                + " " + wound.regionId() + " severity=" + wound.severity() + " bleed="
                + wound.bleedingMillilitresPerSecond() + " mL/s treatment=" + wound.treatment()));
            for (ExposureBurden burden : state.exposures()) sender.addChatMessage(new ChatComponentText("Dose "
                + burden.material() + " via " + burden.route() + " = " + burden.dose() + " ("
                + burden.doseMicrounits() + " microdose units)"));
            for (ArmorLayer layer : state.armor()) sender.addChatMessage(new ChatComponentText("Armor "
                + layer.component().materialRef() + " " + layer.component().geometryId() + " cracking="
                + layer.component().damage().cracking() + " fractured=" + layer.component().damage().isFractured()));
        } else if ("status".equalsIgnoreCase(args[0])) {
            naturalexceptions.core.biology.BiologicalDiagnostics.Snapshot stats = naturalexceptions.core.biology.BiologicalDiagnostics.snapshot();
            sender.addChatMessage(new ChatComponentText("P10 work counters: interactions=" + stats.interactions
                + ", exposure deposits=" + stats.exposureDeposits + ", elapsed resolutions=" + stats.elapsedResolutions
                + ", active resolutions=" + stats.activeResolutions + ", sleeping resolutions=" + stats.sleepingResolutions
                + "; this is event/coarse-time work, not per-region polling."));
        } else if ("armor".equalsIgnoreCase(args[0])) {
            if (state.armor().isEmpty()) {
                ComponentInstance plate = new ComponentInstance(DeterministicId.derive("p10-armor", player.getUniqueID().toString()),
                    GeometryId.PLATE, MaterialRef.authored("naturalexceptions", "iron"),
                    CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION, MaterialPhase.SOLID, null, null),
                    MaterialQuantity.ofGrams(156), ManufacturingState.FINISHED, new ComponentDamage());
                state.equipArmor(new ArmorLayer(plate, BuiltinMaterials.IRON_PROPERTIES, 0.01,
                    java.util.Collections.singleton(TORSO)));
                sender.addChatMessage(new ChatComponentText("Equipped a torso-covering iron P5 component. Use /ne_biology armor again to remove it."));
            } else { state.clearArmor(); sender.addChatMessage(new ChatComponentText("Removed P10 acceptance armor.")); }
        } else if ("cut".equalsIgnoreCase(args[0])) {
            double joules = number(args, 1, 100.0, 0.0, 100000.0);
            BiologicalInteractionResolver.apply(state, new PhysicalInteraction(PhysicalInteraction.Type.CUT,
                TORSO, joules, 1.0, 0.9, 20.0), world.getTotalWorldTime());
            sender.addChatMessage(new ChatComponentText("Applied localized torso cutting interaction: " + joules + " J."));
        } else if ("expose".equalsIgnoreCase(args[0])) {
            ExposureRoute route = route(args, 1);
            double grams = number(args, 2, 10.0, 0.0, 1000000.0);
            double potency = number(args, 3, 2.0, 0.0, 1000.0);
            double dose = state.expose(ResourceId.of("naturalexceptions", "acceptance_hazard"),
                MaterialProperties.builder().biologicalHazard(potency).addTrait("respiratory_hazard").build(),
                route, grams, world.getTotalWorldTime());
            sender.addChatMessage(new ChatComponentText("Accumulated " + dose + " dose by " + route + " route."));
        } else if ("protect".equalsIgnoreCase(args[0])) {
            ExposureRoute route = route(args, 1);
            double attenuation = number(args, 2, 0.8, 0.0, 1.0);
            state.addExposureProtection(new ExposureProtection(route, attenuation));
            sender.addChatMessage(new ChatComponentText("Added route barrier; attenuation=" + attenuation + " for " + route));
        } else if ("unprotect".equalsIgnoreCase(args[0])) {
            state.clearExposureProtection(); sender.addChatMessage(new ChatComponentText("Cleared temporary dev protection."));
        } else if ("treat".equalsIgnoreCase(args[0])) {
            if (state.injuries().isEmpty()) { sender.addChatMessage(new ChatComponentText("No active injury to treat.")); return; }
            BiologicalInjury injury = state.injuries().get(state.injuries().size() - 1);
            state.treatInjury(injury.id(), injury.kind() == BiologicalInjury.Kind.CUT
                ? BiologicalTreatment.DRESS : BiologicalTreatment.SPLINT);
            sender.addChatMessage(new ChatComponentText("Applied " + (injury.kind() == BiologicalInjury.Kind.CUT ? "dressing" : "splint")
                + " to " + injury.regionId() + "; the injury remains and will recover over time."));
        } else if ("advance".equalsIgnoreCase(args[0])) {
            double seconds = number(args, 1, 60.0, 0.0, 1000000.0);
            long ticks = (long) Math.min(Long.MAX_VALUE - world.getTotalWorldTime(), seconds * 20.0);
            state.advanceTo(world.getTotalWorldTime() + ticks);
            sender.addChatMessage(new ChatComponentText("Resolved " + seconds + " s elapsed; active=" + state.hasActiveProcesses()));
        } else if ("compare".equalsIgnoreCase(args[0])) {
            BiologicalState human = new BiologicalState(BuiltinBodyPlans.humanLike(), DeterministicId.derive("acceptance", "human"), 0L);
            BiologicalState crystal = new BiologicalState(BuiltinBodyPlans.crystalline(), DeterministicId.derive("acceptance", "crystal"), 0L);
            PhysicalInteraction test = new PhysicalInteraction(PhysicalInteraction.Type.CUT, TORSO, 100.0, 1.0, 0.9, 20.0);
            BiologicalInteractionResolver.apply(human, test, 0L); BiologicalInteractionResolver.apply(crystal, test, 0L);
            sender.addChatMessage(new ChatComponentText("Same 100 J cut: human=" + human.injuries().get(0).kind()
                + ", bleeding=" + human.injuries().get(0).bleedingMillilitresPerSecond() + " mL/s; crystalline="
                + crystal.injuries().get(0).kind() + ", blood reserve=" + crystal.bloodReserveMillilitres() + " mL."));
        } else if ("hazard_cell".equalsIgnoreCase(args[0])) {
            createHazardCell(player, world, sender);
        } else sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    private static void createHazardCell(EntityPlayer player, World world, ICommandSender sender) {
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        int x = net.minecraft.util.MathHelper.floor_double(player.posX) + 1;
        int y = net.minecraft.util.MathHelper.floor_double(player.posY);
        int z = net.minecraft.util.MathHelper.floor_double(player.posZ);
        if (saved == null || !world.isAirBlock(x, y, z)) {
            sender.addChatMessage(new ChatComponentText("Clear the adjacent block space before creating the test parcel.")); return;
        }
        MaterialQuantity mass = MaterialQuantity.ofGrams(900.0);
        MaterialProperties lead = BuiltinMaterials.LEAD_PROPERTIES.forPhase(MaterialPhase.LIQUID);
        FluidParcel parcel = new FluidParcel(MaterialRef.authored("naturalexceptions", "lead"),
            CanonicalMaterialKey.of(BuiltinMaterials.LEAD_COMPOSITION, MaterialPhase.LIQUID, null, null),
            BuiltinMaterials.LEAD_COMPOSITION, mass,
            ActiveThermalState.atTemperature(800.0, 1.0, mass.toKilograms(), lead));
        int meta = naturalexceptions.minecraft.fluid.BlockFluidCell.metadataFor(parcel);
        if (!world.setBlock(x, y, z, naturalexceptions.minecraft.registry.NEBlocks.fluidCell, meta, 3)) return;
        try {
            saved.fluids().put(new FluidCellKey(world.provider.dimensionId, x, y, z), parcel);
            saved.markDirty(); world.markBlockForUpdate(x, y, z);
            sender.addChatMessage(new ChatComponentText("Created a finite 900 g molten lead P9 cell beside you. Right-click it for material contact exposure."));
        } catch (RuntimeException invalid) {
            world.setBlockToAir(x, y, z);
            sender.addChatMessage(new ChatComponentText("P9 refused the finite hazard cell."));
        }
    }
    private static ExposureRoute route(String[] args, int index) {
        if (index >= args.length) throw new IllegalArgumentException("Missing exposure route");
        return ExposureRoute.valueOf(args[index].toUpperCase(java.util.Locale.ROOT));
    }
    private static double number(String[] args, int index, double fallback, double min, double max) {
        double value = index >= args.length ? fallback : Double.parseDouble(args[index]);
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Number out of range");
        return value;
    }
}
