package naturalexceptions.minecraft.biology;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BiologicalTreatment;

/** What a field bandage does when dropped on a body region: treat that region's worst untreated injury. */
public final class BodyTreatment {

    private BodyTreatment() {}

    /** The injury in {@code regionId} a bandage would treat, or null if nothing there needs it. */
    public static BiologicalInjury pick(BiologicalState state, String regionId) {
        BiologicalInjury worst = null;
        for (BiologicalInjury injury : state.injuries()) {
            if (injury.resolved() || injury.treatment() > 0.0) continue;
            if (!injury.regionId()
                .toString()
                .equals(regionId)) continue;
            if (worst == null || injury.severity() > worst.severity()) worst = injury;
        }
        return worst;
    }

    /**
     * Uses one bandage from the player's cursor on the worst untreated injury in {@code regionId}. Returns true if
     * something was treated; otherwise nothing is used up.
     */
    public static boolean applyFromCursor(EntityPlayerMP player, String regionId) {
        ItemStack held = player.inventory.getItemStack();
        if (held == null || !(held.getItem() instanceof naturalexceptions.minecraft.item.ItemBandage)) return false;
        BiologicalState state = BiologicalProperties.get(player)
            .state();
        long now = player.worldObj.getTotalWorldTime();
        state.advanceTo(Math.max(state.lastResolvedTick(), now));
        if (treatGroup(state, regionId) == 0) return false;
        if (!player.capabilities.isCreativeMode) {
            held.stackSize--;
            player.inventory.setItemStack(held.stackSize <= 0 ? null : held);
            player.updateHeldItem();
        }
        player.worldObj.playSoundAtEntity(player, "step.cloth", 1.0F, 0.9F);
        BodyStatusSync.sendNow(player, state, now);
        return true;
    }

    /** Stanches every bleeding cut on the player (a styptic salve). Returns how many; nothing changes if none bleed. */
    public static int stanch(EntityPlayerMP player) {
        BiologicalState state = BiologicalProperties.get(player)
            .state();
        long now = player.worldObj.getTotalWorldTime();
        state.advanceTo(Math.max(state.lastResolvedTick(), now));
        int stanched = state.stanchBleeding();
        if (stanched > 0) BodyStatusSync.sendNow(player, state, now);
        return stanched;
    }

    /** Gives the player blood to be made over the next while, and water (an iron tonic). */
    public static void fortify(EntityPlayerMP player, double bloodMillilitres, double waterMillilitres) {
        BiologicalState state = BiologicalProperties.get(player)
            .state();
        long now = player.worldObj.getTotalWorldTime();
        state.advanceTo(Math.max(state.lastResolvedTick(), now));
        state.addBloodRegeneration(bloodMillilitres);
        state.replenishHydrationMillilitres(waterMillilitres);
        BodyStatusSync.sendNow(player, state, now);
    }

    /**
     * Treats the worst untreated injury in {@code regionId} together with every other untreated injury of the same
     * kind there: one dressing covers a burned arm however many times it was burned. Returns how many were treated.
     */
    public static int treatGroup(BiologicalState state, String regionId) {
        BiologicalInjury worst = pick(state, regionId);
        if (worst == null) return 0;
        BiologicalTreatment treatment = treatmentFor(worst.kind());
        java.util.List<BiologicalInjury> group = new java.util.ArrayList<>();
        for (BiologicalInjury injury : state.injuries()) {
            if (injury.kind() == worst.kind() && injury.treatment() <= 0.0
                && !injury.resolved()
                && injury.regionId()
                    .toString()
                    .equals(regionId))
                group.add(injury);
        }
        for (BiologicalInjury injury : group) state.treatInjury(injury.id(), treatment);
        return group.size();
    }

    /** Broken bones are splinted; cuts and burns are dressed. */
    public static BiologicalTreatment treatmentFor(BiologicalInjury.Kind kind) {
        return kind == BiologicalInjury.Kind.STRUCTURAL ? BiologicalTreatment.SPLINT : BiologicalTreatment.DRESS;
    }

    /** Whether the client should invite a bandage onto this region (it has an untreated injury). */
    public static boolean treatable(BodyStatusSnapshot body, String regionId) {
        for (BodyStatusSnapshot.Injury injury : body.injuriesIn(regionId)) {
            if (injury.treatment() <= 0.0F) return true;
        }
        return false;
    }
}
