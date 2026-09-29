package naturalexceptions.minecraft.biology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyRegionState;
import naturalexceptions.core.biology.ExposureBurden;
import naturalexceptions.core.biology.ExposureProtection;
import naturalexceptions.core.biology.ExposureRoute;
import naturalexceptions.core.biology.PhysiologyProfile;

/**
 * What a player can read about their own body: per-region damage, open injuries, accumulated exposure, reserves and
 * derived function. A read-only copy of the server's {@link BiologicalState}, small enough to send once a second.
 */
public final class BodyStatusSnapshot {

    /** Bounds on what a snapshot carries; the state itself is bounded well below these. */
    static final int MAX_REGIONS = 16;
    static final int MAX_INJURIES = 32;
    static final int MAX_EXPOSURES = 32;
    /** Condition lost per unit of exposure dose, as in {@link BiologicalState#condition()}. */
    static final double CONDITION_PER_DOSE = 0.0001;
    static final double MAX_EXPOSURE_PENALTY = 0.7;
    /** At or below this integrity a region is broken (as {@link BodyRegionState#fractured()}). */
    static final double FRACTURE_INTEGRITY = 0.20;

    public static final class Region {

        private final String id;
        private final float integrity;

        public Region(String id, float integrity) {
            this.id = id;
            this.integrity = integrity;
        }

        public String id() {
            return id;
        }

        public float integrity() {
            return integrity;
        }

        public boolean fractured() {
            return integrity <= FRACTURE_INTEGRITY;
        }
    }

    public static final class Injury {

        private final BiologicalInjury.Kind kind;
        private final String regionId;
        private final float severity;
        private final float bleeding;
        private final float treatment;
        private final boolean closed;
        /** How many separate injury records this line stands for (see {@link #groupedInjuries()}). */
        private final int count;

        public Injury(BiologicalInjury.Kind kind, String regionId, float severity, float bleeding, float treatment,
            boolean closed) {
            this(kind, regionId, severity, bleeding, treatment, closed, 1);
        }

        Injury(BiologicalInjury.Kind kind, String regionId, float severity, float bleeding, float treatment,
            boolean closed, int count) {
            this.kind = kind;
            this.regionId = regionId;
            this.severity = severity;
            this.bleeding = bleeding;
            this.treatment = treatment;
            this.closed = closed;
            this.count = count;
        }

        public int count() {
            return count;
        }

        public BiologicalInjury.Kind kind() {
            return kind;
        }

        public String regionId() {
            return regionId;
        }

        public float severity() {
            return severity;
        }

        /** Blood lost per second, in millilitres. */
        public float bleeding() {
            return bleeding;
        }

        public float treatment() {
            return treatment;
        }

        public boolean closed() {
            return closed;
        }
    }

    public static final class Exposure {

        private final String material;
        private final ExposureRoute route;
        private final double dose;

        public Exposure(String material, ExposureRoute route, double dose) {
            this.material = material;
            this.route = route;
            this.dose = dose;
        }

        public String material() {
            return material;
        }

        public ExposureRoute route() {
            return route;
        }

        public double dose() {
            return dose;
        }

        /** Share of overall condition this burden currently costs, 0..0.7. */
        public double conditionPenalty() {
            return Math.min(MAX_EXPOSURE_PENALTY, Math.max(0.0, dose * CONDITION_PER_DOSE));
        }
    }

    private final float condition;
    private final float movement;
    private final float handling;
    private final boolean canBleed;
    private final float blood;
    private final float bloodCapacity;
    private final float energy;
    private final float energyCapacity;
    private final float hydration;
    private final float hydrationCapacity;
    private final float inhalationProtection;
    private final List<Region> regions;
    private final List<Injury> injuries;
    private final List<Exposure> exposures;

    public BodyStatusSnapshot(float condition, float movement, float handling, boolean canBleed, float blood,
        float bloodCapacity, float energy, float energyCapacity, float hydration, float hydrationCapacity,
        float inhalationProtection, List<Region> regions, List<Injury> injuries, List<Exposure> exposures) {
        this.condition = condition;
        this.movement = movement;
        this.handling = handling;
        this.canBleed = canBleed;
        this.blood = blood;
        this.bloodCapacity = bloodCapacity;
        this.energy = energy;
        this.energyCapacity = energyCapacity;
        this.hydration = hydration;
        this.hydrationCapacity = hydrationCapacity;
        this.inhalationProtection = inhalationProtection;
        this.regions = Collections.unmodifiableList(new ArrayList<>(bounded(regions, MAX_REGIONS)));
        this.injuries = Collections.unmodifiableList(new ArrayList<>(bounded(injuries, MAX_INJURIES)));
        this.exposures = Collections.unmodifiableList(new ArrayList<>(bounded(exposures, MAX_EXPOSURES)));
    }

    public static BodyStatusSnapshot from(BiologicalState state) {
        PhysiologyProfile physiology = state.physiology();
        List<Region> regions = new ArrayList<>();
        for (BodyRegionState region : state.regions()
            .values()) {
            regions.add(
                new Region(
                    region.definition()
                        .id()
                        .toString(),
                    (float) region.structuralIntegrity()));
        }
        List<Injury> injuries = new ArrayList<>();
        for (BiologicalInjury injury : state.injuries()) {
            if (injury.resolved()) continue;
            injuries.add(
                new Injury(
                    injury.kind(),
                    injury.regionId()
                        .toString(),
                    (float) injury.severity(),
                    (float) injury.bleedingMillilitresPerSecond(),
                    (float) injury.treatment(),
                    injury.closed()));
        }
        List<Exposure> exposures = new ArrayList<>();
        for (ExposureBurden burden : state.exposures()) {
            if (burden.doseMicrounits() <= 0L) continue;
            exposures.add(
                new Exposure(
                    burden.material()
                        .toString(),
                    burden.route(),
                    burden.dose()));
        }
        float inhalation = 0.0F;
        for (ExposureProtection item : state.protection()) {
            if (item.route() == ExposureRoute.INHALATION) inhalation = Math.max(inhalation, (float) item.attenuation());
        }
        return new BodyStatusSnapshot(
            (float) state.condition(),
            (float) state.movementCapability(),
            (float) state.handlingCapability(),
            physiology.canBleed(),
            (float) state.bloodReserveMillilitres(),
            (float) physiology.bloodCapacityMillilitres(),
            (float) state.energyReserveJoules(),
            (float) physiology.energyCapacityJoules(),
            (float) state.hydrationReserveMillilitres(),
            (float) physiology.hydrationCapacityMillilitres(),
            inhalation,
            regions,
            injuries,
            exposures);
    }

    public void write(ByteBuf buf) {
        buf.writeFloat(condition);
        buf.writeFloat(movement);
        buf.writeFloat(handling);
        buf.writeBoolean(canBleed);
        buf.writeFloat(blood);
        buf.writeFloat(bloodCapacity);
        buf.writeFloat(energy);
        buf.writeFloat(energyCapacity);
        buf.writeFloat(hydration);
        buf.writeFloat(hydrationCapacity);
        buf.writeFloat(inhalationProtection);
        buf.writeByte(regions.size());
        for (Region region : regions) {
            ByteBufUtils.writeUTF8String(buf, region.id);
            buf.writeFloat(region.integrity);
        }
        buf.writeByte(injuries.size());
        for (Injury injury : injuries) {
            buf.writeByte(injury.kind.ordinal());
            ByteBufUtils.writeUTF8String(buf, injury.regionId);
            buf.writeFloat(injury.severity);
            buf.writeFloat(injury.bleeding);
            buf.writeFloat(injury.treatment);
            buf.writeBoolean(injury.closed);
        }
        buf.writeByte(exposures.size());
        for (Exposure exposure : exposures) {
            ByteBufUtils.writeUTF8String(buf, exposure.material);
            buf.writeByte(exposure.route.ordinal());
            buf.writeDouble(exposure.dose);
        }
    }

    public static BodyStatusSnapshot read(ByteBuf buf) {
        float condition = buf.readFloat();
        float movement = buf.readFloat();
        float handling = buf.readFloat();
        boolean canBleed = buf.readBoolean();
        float blood = buf.readFloat();
        float bloodCapacity = buf.readFloat();
        float energy = buf.readFloat();
        float energyCapacity = buf.readFloat();
        float hydration = buf.readFloat();
        float hydrationCapacity = buf.readFloat();
        float inhalation = buf.readFloat();
        List<Region> regions = new ArrayList<>();
        int regionCount = Math.min(MAX_REGIONS, buf.readUnsignedByte());
        for (int i = 0; i < regionCount; i++) {
            regions.add(new Region(ByteBufUtils.readUTF8String(buf), buf.readFloat()));
        }
        List<Injury> injuries = new ArrayList<>();
        int injuryCount = Math.min(MAX_INJURIES, buf.readUnsignedByte());
        BiologicalInjury.Kind[] kinds = BiologicalInjury.Kind.values();
        for (int i = 0; i < injuryCount; i++) {
            BiologicalInjury.Kind kind = kinds[Math.min(kinds.length - 1, buf.readUnsignedByte())];
            injuries.add(
                new Injury(
                    kind,
                    ByteBufUtils.readUTF8String(buf),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean()));
        }
        List<Exposure> exposures = new ArrayList<>();
        int exposureCount = Math.min(MAX_EXPOSURES, buf.readUnsignedByte());
        ExposureRoute[] routes = ExposureRoute.values();
        for (int i = 0; i < exposureCount; i++) {
            String material = ByteBufUtils.readUTF8String(buf);
            ExposureRoute route = routes[Math.min(routes.length - 1, buf.readUnsignedByte())];
            exposures.add(new Exposure(material, route, buf.readDouble()));
        }
        return new BodyStatusSnapshot(
            condition,
            movement,
            handling,
            canBleed,
            blood,
            bloodCapacity,
            energy,
            energyCapacity,
            hydration,
            hydrationCapacity,
            inhalation,
            regions,
            injuries,
            exposures);
    }

    /** Coarse fingerprint of what the panel shows, so an unchanged body is not re-sent every second. */
    public long fingerprint() {
        long hash = 1125899906842597L;
        hash = mix(hash, Math.round(condition * 1000.0));
        hash = mix(hash, Math.round(movement * 1000.0));
        hash = mix(hash, Math.round(handling * 1000.0));
        hash = mix(hash, Math.round(blood));
        hash = mix(hash, Math.round(energy / 1000.0));
        hash = mix(hash, Math.round(hydration));
        hash = mix(hash, Math.round(inhalationProtection * 100.0));
        for (Region region : regions) {
            hash = mix(hash, region.id.hashCode());
            hash = mix(hash, Math.round(region.integrity * 1000.0));
        }
        for (Injury injury : injuries) {
            hash = mix(hash, injury.kind.ordinal());
            hash = mix(hash, injury.regionId.hashCode());
            hash = mix(hash, Math.round(injury.severity * 1000.0));
            hash = mix(hash, Math.round(injury.bleeding * 100.0));
            hash = mix(hash, Math.round(injury.treatment * 100.0));
            hash = mix(hash, injury.closed ? 1 : 0);
        }
        for (Exposure exposure : exposures) {
            hash = mix(hash, exposure.material.hashCode());
            hash = mix(hash, exposure.route.ordinal());
            hash = mix(hash, Math.round(exposure.dose * 1000.0));
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        return 31L * hash + value;
    }

    private static <T> List<T> bounded(List<T> values, int max) {
        if (values == null) return Collections.emptyList();
        return values.size() <= max ? values : values.subList(0, max);
    }

    public float condition() {
        return condition;
    }

    public float movement() {
        return movement;
    }

    public float handling() {
        return handling;
    }

    public boolean canBleed() {
        return canBleed;
    }

    public float blood() {
        return blood;
    }

    public float bloodCapacity() {
        return bloodCapacity;
    }

    public float energy() {
        return energy;
    }

    public float energyCapacity() {
        return energyCapacity;
    }

    public float hydration() {
        return hydration;
    }

    public float hydrationCapacity() {
        return hydrationCapacity;
    }

    /** Strongest breathing protection being worn, 0..1. */
    public float inhalationProtection() {
        return inhalationProtection;
    }

    public List<Region> regions() {
        return regions;
    }

    public List<Injury> injuries() {
        return injuries;
    }

    public List<Exposure> exposures() {
        return exposures;
    }

    /**
     * Injuries as a person would describe them: repeated records of the same kind on the same part (four burns from
     * standing on something hot) become one entry with the worst severity, the combined bleeding, and "treated" only
     * if every one of them is. In first-seen order.
     */
    public List<Injury> groupedInjuries() {
        List<Injury> groups = new ArrayList<>();
        for (Injury injury : injuries) {
            int found = -1;
            for (int i = 0; i < groups.size(); i++) {
                Injury group = groups.get(i);
                if (group.kind == injury.kind && group.regionId.equals(injury.regionId)) found = i;
            }
            if (found < 0) {
                groups.add(injury);
                continue;
            }
            Injury group = groups.get(found);
            groups.set(
                found,
                new Injury(
                    group.kind,
                    group.regionId,
                    Math.max(group.severity, injury.severity),
                    group.bleeding + injury.bleeding,
                    Math.min(group.treatment, injury.treatment),
                    group.closed && injury.closed,
                    group.count + injury.count));
        }
        return groups;
    }

    public List<Injury> injuriesIn(String regionId) {
        List<Injury> result = new ArrayList<>();
        for (Injury injury : injuries) if (injury.regionId.equals(regionId)) result.add(injury);
        return result;
    }

    public Region region(String regionId) {
        for (Region region : regions) if (region.id.equals(regionId)) return region;
        return null;
    }

    /** How hurt a region is overall, 0 (sound) to 1: the worst of its structural damage and its open injuries. */
    public float regionHarm(String regionId) {
        Region region = region(regionId);
        float harm = region == null ? 0.0F : 1.0F - region.integrity;
        for (Injury injury : injuriesIn(regionId)) harm = Math.max(harm, injury.severity);
        return Math.max(0.0F, Math.min(1.0F, harm));
    }

    public boolean regionBleeding(String regionId) {
        for (Injury injury : injuriesIn(regionId)) if (injury.bleeding > 0.05F) return true;
        return false;
    }

    public boolean regionBurned(String regionId) {
        for (Injury injury : injuriesIn(regionId)) if (injury.kind == BiologicalInjury.Kind.BURN) return true;
        return false;
    }

    /** Total condition lost to exposure, as {@link BiologicalState#condition()} caps it. */
    public double exposurePenalty() {
        double total = 0.0;
        for (Exposure exposure : exposures) total += exposure.dose;
        return Math.min(MAX_EXPOSURE_PENALTY, total * CONDITION_PER_DOSE);
    }

    // --- Words -------------------------------------------------------------------------------------------------

    /** "naturalexceptions:left_arm" becomes "Left Arm". */
    public static String label(String id) {
        if (id == null || id.isEmpty()) return "";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/]")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1));
        }
        return out.toString();
    }

    public static String injuryName(BiologicalInjury.Kind kind) {
        switch (kind) {
            case CUT:
                return "Cut";
            case BURN:
                return "Burn";
            case STRUCTURAL:
            default:
                return "Bone damage";
        }
    }

    /** e.g. "Cut 35% \u00b7 bleeding 1.4 ml/s \u00b7 dressed". */
    public static String injuryLine(Injury injury) {
        StringBuilder line = new StringBuilder(injuryName(injury.kind)).append(' ')
            .append(percent(injury.severity));
        if (injury.bleeding > 0.05F) line.append(" \u00b7 bleeding ")
            .append(String.format(Locale.ROOT, "%.1f", injury.bleeding))
            .append(" ml/s");
        if (injury.treatment > 0.0F)
            line.append(injury.kind == BiologicalInjury.Kind.STRUCTURAL ? " \u00b7 splinted" : " \u00b7 dressed");
        else if (injury.closed) line.append(" \u00b7 closed");
        return line.toString();
    }

    public static String routeWord(ExposureRoute route) {
        switch (route) {
            case INGESTION:
                return "swallowed";
            case INHALATION:
                return "inhaled";
            case CONTACT:
                return "skin contact";
            case WOUND:
            default:
                return "through a wound";
        }
    }

    /** e.g. "inhaled \u00b7 dose 12.4 \u00b7 -3% condition". */
    public static String exposureDetail(Exposure exposure) {
        return routeWord(exposure.route) + " \u00b7 dose "
            + String.format(Locale.ROOT, exposure.dose < 10.0 ? "%.2f" : "%.0f", exposure.dose)
            + " \u00b7 "
            + (exposure.conditionPenalty() < 0.005 ? "no harm yet"
                : "-" + percent(exposure.conditionPenalty()) + " condition");
    }

    public static String percent(double fraction) {
        return Math.round(Math.max(0.0, fraction) * 100.0) + "%";
    }

    /** Material id to show when no translation exists. */
    public static String materialFallbackName(String materialId) {
        return label(materialId);
    }
}
