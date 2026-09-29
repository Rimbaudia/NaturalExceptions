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
        BiologicalInjury injury = pick(state, regionId);
        if (injury == null) return false;
        state.treatInjury(injury.id(), treatmentFor(injury.kind()));
        if (!player.capabilities.isCreativeMode) {
            held.stackSize--;
            player.inventory.setItemStack(held.stackSize <= 0 ? null : held);
            player.updateHeldItem();
        }
        player.worldObj.playSoundAtEntity(player, "step.cloth", 1.0F, 0.9F);
        BodyStatusSync.sendNow(player, state, now);
        return true;
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
