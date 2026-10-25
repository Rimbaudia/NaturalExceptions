package naturalexceptions.minecraft.power;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.minecraft.registry.NEItems;

/**
 * A socket for an Automaton's Stormglass heart: a small, steady trickle of power from the charge still in it, running
 * slowly down. The heart keeps its charge when taken out (and a strong enough network charges it back).
 */
public final class TileHeartSocket extends TileBattery {

    /** A fresh heart's charge, and what one holds at most. */
    public static final double HEART_JOULES = 400_000.0;
    private boolean heart;

    @Override
    protected double capacity() {
        return HEART_JOULES;
    }

    @Override
    protected double emptyVolts() {
        return 20.0;
    }

    @Override
    protected double fullVolts() {
        return 25.0;
    }

    @Override
    protected double internalOhms() {
        return 3.0;
    }

    @Override
    protected boolean present() {
        return heart;
    }

    public boolean hasHeart() {
        return heart;
    }

    /** The charge in a heart item (a fresh one, never set, is full). */
    public static double heartCharge(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("Charge") ? stack.getTagCompound()
                .getDouble("Charge") : HEART_JOULES;
    }

    /** Sets a heart in, or takes it out (into the player's hand). */
    public boolean use(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        if (!heart && held != null && held.getItem() == NEItems.stormglassHeart) {
            heart = true;
            charge = heartCharge(held);
            if (!player.capabilities.isCreativeMode && --held.stackSize <= 0)
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            changedHeart();
            return true;
        }
        if (heart && held == null) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, takeHeart());
            return true;
        }
        return false;
    }

    /** Takes the heart out, with what charge is left in it (null if there is none). */
    public ItemStack takeHeart() {
        if (!heart || NEItems.stormglassHeart == null) return null;
        ItemStack out = new ItemStack(NEItems.stormglassHeart);
        out.setTagCompound(new NBTTagCompound());
        out.getTagCompound()
            .setDouble("Charge", charge);
        heart = false;
        charge = 0.0;
        changedHeart();
        return out;
    }

    private void changedHeart() {
        markDirty();
        worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, heart ? 1 : 0, 3);
        PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Heart", heart);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        heart = tag.getBoolean("Heart");
    }

    @Override
    public String readout() {
        if (!heart) return "Heart Socket: empty (set a Stormglass Heart in it)";
        return String.format("Heart Socket: %.0f%% charged, %.1f V, %+.1f A", fraction() * 100.0, volts, amps);
    }
}
