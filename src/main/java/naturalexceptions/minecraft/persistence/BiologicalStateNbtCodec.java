package naturalexceptions.minecraft.persistence;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import naturalexceptions.core.biology.ArmorLayer;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyPlan;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.ExposureBurden;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;

/** Bounded semantic persistence for meaningful organism biology. Solver/caches/equipment are not stored here. */
public final class BiologicalStateNbtCodec {

    private static final int VERSION = 1;
    private static final int MAX_INJURIES = 64;
    private static final int MAX_EXPOSURES = 64;

    private BiologicalStateNbtCodec() {}

    public static NBTTagCompound write(BiologicalState state) {
        NBTTagCompound root = new NBTTagCompound();
        if (state == null) return root;
        root.setInteger("Version", VERSION);
        root.setString(
            "Organism",
            state.organismId()
                .hex());
        root.setString(
            "Plan",
            state.bodyPlan()
                .id()
                .toString());
        root.setLong("Resolved", state.lastResolvedTick());
        root.setDouble("Energy", state.energyReserveJoules());
        root.setDouble("Hydration", state.hydrationReserveMillilitres());
        root.setDouble("Blood", state.bloodReserveMillilitres());
        root.setDouble("BloodRegen", state.bloodRegenerationMillilitres());
        NBTTagCompound regions = new NBTTagCompound();
        state.regions()
            .forEach((id, region) -> regions.setDouble(id.toString(), region.structuralIntegrity()));
        root.setTag("Regions", regions);
        NBTTagList injuries = new NBTTagList();
        int count = 0;
        for (BiologicalInjury injury : state.injuries()) {
            if (count++ >= MAX_INJURIES) break;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(
                "Id",
                injury.id()
                    .hex());
            tag.setString(
                "Kind",
                injury.kind()
                    .name());
            tag.setString(
                "Region",
                injury.regionId()
                    .toString());
            tag.setDouble("Severity", injury.severity());
            tag.setDouble("Bleeding", injury.bleedingMillilitresPerSecond());
            tag.setDouble("Treatment", injury.treatment());
            tag.setBoolean("Closed", injury.closed());
            tag.setLong("Resolved", injury.lastResolvedTick());
            tag.setDouble("Clotting", injury.clotting());
            injuries.appendTag(tag);
        }
        root.setTag("Injuries", injuries);
        NBTTagList exposures = new NBTTagList();
        count = 0;
        for (ExposureBurden burden : state.exposures()) {
            if (count++ >= MAX_EXPOSURES) break;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(
                "Material",
                burden.material()
                    .toString());
            tag.setString(
                "Route",
                burden.route()
                    .name());
            tag.setLong("DoseMicro", burden.doseMicrounits());
            tag.setLong("Resolved", burden.lastResolvedTick());
            exposures.appendTag(tag);
        }
        root.setTag("Exposures", exposures);
        NBTTagList armor = new NBTTagList();
        count = 0;
        for (ArmorLayer layer : state.armor()) {
            if (count++ >= 8) break;
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound component = new NBTTagCompound();
            AssemblyNbtCodec.writeComponentToNbt(layer.component(), component);
            tag.setTag("Component", component);
            tag.setDouble("Thickness", layer.thicknessMetres());
            NBTTagCompound values = new NBTTagCompound();
            for (Map.Entry<String, Double> value : layer.material()
                .asMap()
                .entrySet()) if (Double.isFinite(value.getValue())) values.setDouble(value.getKey(), value.getValue());
            tag.setTag("Properties", values);
            NBTTagList traits = new NBTTagList();
            for (String trait : layer.material()
                .biologicalTraits()) traits.appendTag(new NBTTagString(trait));
            tag.setTag("Traits", traits);
            NBTTagList coverage = new NBTTagList();
            for (ResourceId region : layer.coveredRegions()) coverage.appendTag(new NBTTagString(region.toString()));
            tag.setTag("Coverage", coverage);
            armor.appendTag(tag);
        }
        root.setTag("Armor", armor);
        return root;
    }

    public static BiologicalState read(NBTTagCompound root, BodyPlanRegistry registry, long fallbackTick) {
        if (root == null || registry == null || root.getInteger("Version") > VERSION) return null;
        try {
            BodyPlan plan = registry.require(ResourceId.parse(root.getString("Plan")));
            DeterministicId id = DeterministicId.parse(root.getString("Organism"));
            long tick = Math.max(0L, root.getLong("Resolved"));
            BiologicalState state = new BiologicalState(plan, id, tick);
            state.restoreReserves(
                safe(root.getDouble("Energy")),
                safe(root.getDouble("Hydration")),
                safe(root.getDouble("Blood")),
                tick);
            state.restoreBloodRegeneration(safe(root.getDouble("BloodRegen")));
            if (root.hasKey("Regions", 10)) {
                NBTTagCompound regions = root.getCompoundTag("Regions");
                for (naturalexceptions.core.id.ResourceId region : plan.regions()
                    .keySet()) {
                    String key = region.toString();
                    if (regions.hasKey(key, 6)) state.region(region)
                        .restoreIntegrity(unit(regions.getDouble(key)));
                }
            }
            if (root.hasKey("Injuries", 9)) {
                NBTTagList list = root.getTagList("Injuries", 10);
                for (int i = 0; i < Math.min(MAX_INJURIES, list.tagCount()); i++) try {
                    NBTTagCompound tag = list.getCompoundTagAt(i);
                    ResourceId region = ResourceId.parse(tag.getString("Region"));
                    if (!plan.regions()
                        .containsKey(region)) continue;
                    BiologicalInjury injury = new BiologicalInjury(
                        DeterministicId.parse(tag.getString("Id")),
                        BiologicalInjury.Kind.valueOf(tag.getString("Kind")),
                        region,
                        unit(tag.getDouble("Severity")),
                        safe(tag.getDouble("Bleeding")),
                        Math.max(0L, tag.getLong("Resolved")));
                    injury.restoreProgress(
                        unit(tag.getDouble("Treatment")),
                        tag.getBoolean("Closed"),
                        Math.max(0L, tag.getLong("Resolved")));
                    if (tag.hasKey("Clotting", 6)) injury.restoreClotting(tag.getDouble("Clotting"));
                    state.addInjury(injury);
                } catch (RuntimeException ignored) {}
            }
            if (root.hasKey("Exposures", 9)) {
                NBTTagList list = root.getTagList("Exposures", 10);
                for (int i = 0; i < Math.min(MAX_EXPOSURES, list.tagCount()); i++) try {
                    NBTTagCompound tag = list.getCompoundTagAt(i);
                    double dose = tag.hasKey("DoseMicro", 99)
                        ? Math.max(0.0, Math.min(ExposureBurden.MAX_DOSE_MICRO_UNITS, tag.getLong("DoseMicro")))
                            / ExposureBurden.DOSE_MICRO_UNITS_PER_UNIT
                        : safe(tag.getDouble("Dose"));
                    if (dose > 0.0) state.addExposureBurden(
                        new ExposureBurden(
                            ResourceId.parse(tag.getString("Material")),
                            ExposureRoute.valueOf(tag.getString("Route")),
                            dose,
                            Math.max(0L, tag.getLong("Resolved"))));
                } catch (RuntimeException ignored) {}
            }
            if (root.hasKey("Armor", 9)) {
                NBTTagList list = root.getTagList("Armor", 10);
                for (int i = 0; i < Math.min(8, list.tagCount()); i++) try {
                    NBTTagCompound tag = list.getCompoundTagAt(i);
                    ComponentInstance component = AssemblyNbtCodec
                        .readComponentFromNbt(tag.getCompoundTag("Component"));
                    if (component == null) continue;
                    Map<String, Double> values = new HashMap<>();
                    if (tag.hasKey("Properties", 10)) {
                        NBTTagCompound props = tag.getCompoundTag("Properties");
                        for (Object rawKey : props.func_150296_c()) {
                            String key = String.valueOf(rawKey);
                            if (props.hasKey(key, 6) && Double.isFinite(props.getDouble(key)))
                                values.put(key, props.getDouble(key));
                        }
                    }
                    Set<String> traits = new LinkedHashSet<>();
                    if (tag.hasKey("Traits", 9)) {
                        NBTTagList traitList = tag.getTagList("Traits", 8);
                        for (int j = 0; j < Math.min(32, traitList.tagCount()); j++)
                            traits.add(traitList.getStringTagAt(j));
                    }
                    Set<ResourceId> coverage = new LinkedHashSet<>();
                    if (tag.hasKey("Coverage", 9)) {
                        NBTTagList regionList = tag.getTagList("Coverage", 8);
                        for (int j = 0; j < Math.min(8, regionList.tagCount()); j++) {
                            ResourceId region = ResourceId.parse(regionList.getStringTagAt(j));
                            if (plan.regions()
                                .containsKey(region)) coverage.add(region);
                        }
                    }
                    if (!coverage.isEmpty()) state.equipArmor(
                        new ArmorLayer(
                            component,
                            new MaterialProperties(values, traits),
                            safe(tag.getDouble("Thickness")),
                            coverage));
                } catch (RuntimeException ignored) {}
            }
            return state;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static double safe(double v) {
        return Double.isFinite(v) && v >= 0.0 ? Math.min(v, 1.0e12) : 0.0;
    }

    private static double unit(double v) {
        return Double.isFinite(v) ? Math.max(0.0, Math.min(1.0, v)) : 0.0;
    }
}
