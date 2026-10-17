package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.hoisting.HoistDynamics;
import naturalexceptions.core.hoisting.HoistMasses;
import naturalexceptions.minecraft.item.ItemHoistRope;
import naturalexceptions.minecraft.multiblock.HeadframeDefinition;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A windlass at work: the drum, the rope wound on it, the pawl and brake, and the basket on the rope's end with its
 * cargo (the store's nine slots, which the basket carries). It works the headframe it stands by
 * ({@link HeadframeDefinition}): the rope runs up over the headframe's sheave and down the shaft.
 * <p>
 * The rope is fitted by hand, a coil of Hoist Rope (or a length of Cloudsilk Rope) at a time. With rope enough to
 * reach it, a basket set in the shaft is hooked on; from then the windlass hangs it on the rope
 * ({@link EntityHoistLoad})
 * and runs the rope each tick ({@link HoistDynamics}): wound up at the crank, let down by easing the brake, or
 * running free with the brake off.
 */
public final class TileEntityHoist extends TileEntityStorage {

    /** The basket never comes nearer the sheave's axle than this: a rider's head clears the wheel. */
    static final double TOP = 2.2;
    /** How long one turn of the crank (one right-click while held) keeps the rope working, in ticks. */
    private static final int WORK_TICKS = 6;

    private int coils, silk;
    private boolean brake = true, hooked;
    private final HoistDynamics rope = new HoistDynamics(TOP);

    private EntityHoistLoad load;
    private HeadframeDefinition.Frame frame;
    private int recheck, floorCheck;
    private double floor = Double.MAX_VALUE;
    private int working, winding;
    private HoistDynamics.Crank crank = HoistDynamics.Crank.NONE;
    private double lastSent;

    public TileEntityHoist() {
        super(9, "container.naturalexceptions.hoist");
    }

    /** Rope on the drum, in blocks. */
    public int ropeLength() {
        return coils * ItemHoistRope.BLOCKS_PER_COIL + silk;
    }

    public boolean braked() {
        return brake;
    }

    public boolean hooked() {
        return hooked;
    }

    /** The rope paid out, sheave to basket (blocks). */
    public double paidOut() {
        return rope.paidOut();
    }

    public HeadframeDefinition.Frame frame() {
        return frame;
    }

    EntityHoistLoad load() {
        return load;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    // --- What a player does with it --------------------------------------------------------------------------------

    /** Fits rope from {@code held} (a Hoist Rope coil or Cloudsilk Rope), a piece a time; true if it took some. */
    boolean fitRope(ItemStack held) {
        if (held == null) return false;
        if (held.getItem() instanceof ItemHoistRope) coils++;
        else if (NEBlocks.cloudsilkRope != null && held.getItem() == Item.getItemFromBlock(NEBlocks.cloudsilkRope))
            silk++;
        else return false;
        held.stackSize--;
        markDirty();
        return true;
    }

    /** Rope fitted at once, by worldgen: {@code n} coils. */
    public void fitCoils(int n) {
        coils += n;
        markDirty();
    }

    /** The crank turned (or the hand-line hauled) one way for a moment: -1 winding up, +1 letting down. */
    void work(HoistDynamics.Crank by, int way) {
        crank = by;
        winding = way;
        working = WORK_TICKS;
    }

    /** A rider on the basket's hand-line. */
    public void haul(int way) {
        work(HoistDynamics.Crank.HAND_LINE, way);
    }

    void toggleBrake() {
        brake = !brake;
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /** Loads {@code stack} into the basket's cargo, as much as there is room for; returns what is left. */
    ItemStack load(ItemStack stack) {
        if (stack == null) return null;
        ItemStack left = stack.copy();
        for (int i = 0; i < getSizeInventory() && left.stackSize > 0; i++) {
            ItemStack slot = getStackInSlot(i);
            if (slot == null) {
                setInventorySlotContents(i, left);
                return null;
            }
            if (slot.isItemEqual(left) && ItemStack.areItemStackTagsEqual(slot, left)) {
                int room = Math.min(slot.getMaxStackSize(), getInventoryStackLimit()) - slot.stackSize;
                int moved = Math.min(room, left.stackSize);
                slot.stackSize += moved;
                left.stackSize -= moved;
            }
        }
        markDirty();
        return left.stackSize > 0 ? left : null;
    }

    /** What the basket holds and weighs, for its rider or loader. */
    String describeCargo() {
        int blocks = 0, items = 0;
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack s = getStackInSlot(i);
            if (s == null) continue;
            if (s.getItem() instanceof ItemBlock) blocks += s.stackSize;
            else items += s.stackSize;
        }
        return String.format(
            "Basket: %d block%s and %d other thing%s of cargo, %.0f kg with no one in it.",
            blocks,
            blocks == 1 ? "" : "s",
            items,
            items == 1 ? "" : "s",
            HoistMasses.basket(0, blocks, items));
    }

    /** Takes the basket off the rope where it hangs: a block again there if it can be, else the player's. */
    void unhook(EntityPlayer player) {
        if (!hooked || load == null || Math.abs(rope.speed()) > 1e-4) return;
        int x = (int) Math.floor(load.posX), y = (int) Math.floor(load.posY + 0.5), z = (int) Math.floor(load.posZ);
        if (worldObj.isAirBlock(x, y, z) && NEBlocks.hoistBasket != null)
            worldObj.setBlock(x, y, z, NEBlocks.hoistBasket);
        else if (NEBlocks.hoistBasket != null
            && !player.inventory.addItemStackToInventory(new ItemStack(NEBlocks.hoistBasket)))
            worldObj.spawnEntityInWorld(
                new EntityItem(worldObj, player.posX, player.posY, player.posZ, new ItemStack(NEBlocks.hoistBasket)));
        hooked = false;
        load.setDead();
        load = null;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /** Broken: the rope comes off with it, and the basket on it falls back to being a block where it hung. */
    void taken() {
        if (load != null) {
            if (load.riddenByEntity != null) load.riddenByEntity.mountEntity(null);
            load.setDead();
        }
        ItemStack[] rope = { coils > 0 && NEItems.hoistRope != null ? new ItemStack(NEItems.hoistRope, coils) : null,
            silk > 0 && NEBlocks.cloudsilkRope != null ? new ItemStack(NEBlocks.cloudsilkRope, silk) : null,
            hooked && NEBlocks.hoistBasket != null ? new ItemStack(NEBlocks.hoistBasket) : null };
        for (ItemStack s : rope) if (s != null)
            worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, s));
        coils = silk = 0;
        hooked = false;
    }

    // --- At work -------------------------------------------------------------------------------------------------

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (frame == null || --recheck <= 0) {
            HeadframeDefinition.Frame was = frame;
            frame = HeadframeDefinition.INSTANCE.find(worldObj, xCoord, yCoord, zCoord);
            recheck = 40;
            boolean changed = was == null ? frame != null
                : frame == null || was.sheaveX != frame.sheaveX
                    || was.sheaveY != frame.sheaveY
                    || was.sheaveZ != frame.sheaveZ;
            if (changed) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord); // clients draw the rope to it
        }
        if (frame == null) {
            if (load != null) {
                if (load.riddenByEntity != null) load.riddenByEntity.mountEntity(null);
                load.setDead();
                load = null;
            }
            return;
        }
        double axle = frame.sheaveY + 0.5;
        if (--floorCheck <= 0) {
            floor = floorUnder(axle);
            floorCheck = 10;
        }
        // Up, it stops at the collar, its floor level with the ground the headframe stands on, to step off onto.
        double top = Math.max(TOP, axle - (frame.groundY + 1));
        double bottom = Math.max(top, Math.min(ropeLength(), floor));
        if (!hooked) {
            hookBasket(axle, top, bottom);
            return;
        }
        if (load == null || load.isDead) {
            load = new EntityHoistLoad(
                worldObj,
                new int[] { xCoord, yCoord, zCoord },
                new int[] { frame.sheaveX, frame.sheaveY, frame.sheaveZ },
                rope.paidOut());
            worldObj.spawnEntityInWorld(load);
        }
        int riders = load.riddenByEntity != null ? 1 : 0, blocks = 0, items = 0;
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack s = getStackInSlot(i);
            if (s == null) continue;
            if (s.getItem() instanceof ItemBlock) blocks += s.stackSize;
            else items += s.stackSize;
        }
        HoistDynamics.Crank by = working > 0 ? crank : HoistDynamics.Crank.NONE;
        int way = working > 0 ? winding : 0;
        if (working > 0) working--;
        boolean moved = rope.step(HoistMasses.basket(riders, blocks, items), 0.0, by, way, brake, top, bottom);
        load.setRope(rope.paidOut());
        if (moved) {
            if (worldObj.getTotalWorldTime() % 8 == 0) worldObj.playSoundEffect(
                xCoord + 0.5,
                yCoord + 0.5,
                zCoord + 0.5,
                brake && rope.speed() < 0 ? "random.click" : "random.door_close",
                0.25F,
                0.6F + worldObj.rand.nextFloat() * 0.1F);
            if (Math.abs(rope.paidOut() - lastSent) > 0.5) {
                lastSent = rope.paidOut();
                markDirty();
            }
        }
    }

    /**
     * Rope from the sheave's axle down to where the basket would rest on the shaft's floor: the first block under the
     * sheave that is neither air nor a basket.
     */
    private double floorUnder(double axle) {
        for (int y = frame.sheaveY - 1; y > 0 && y > frame.sheaveY - 128; y--) {
            if (!HeadframeDefinition.clear(worldObj, frame.sheaveX, y, frame.sheaveZ)) return axle - (y + 1);
        }
        return Double.MAX_VALUE;
    }

    /** A basket block set in the shaft, within reach of the rope, is hooked on and taken up off the floor. */
    private void hookBasket(double axle, double top, double bottom) {
        if (ropeLength() < TOP || NEBlocks.hoistBasket == null) return;
        for (int y = frame.sheaveY - 1; y > 0 && axle - y <= ropeLength() + 0.01; y--) {
            Block block = worldObj.getBlock(frame.sheaveX, y, frame.sheaveZ);
            if (block == NEBlocks.hoistBasket) {
                worldObj.setBlockToAir(frame.sheaveX, y, frame.sheaveZ);
                hooked = true;
                rope.set(Math.max(top, Math.min(bottom, axle - y)), 0.0);
                markDirty();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return;
            }
            if (!HeadframeDefinition.clear(worldObj, frame.sheaveX, y, frame.sheaveZ)) return;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (load != null && !worldObj.isRemote) {
            if (load.riddenByEntity != null) load.riddenByEntity.mountEntity(null);
            load.setDead();
            load = null;
        }
    }

    @Override
    public void onChunkUnload() {
        invalidate();
    }

    // --- Drawing -----------------------------------------------------------------------------------------------------

    /** The rope runs far from the windlass, up to its sheave: drawn whenever the windlass is anywhere near view. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("RopeCoils", coils);
        tag.setInteger("RopeSilk", silk);
        tag.setBoolean("Brake", brake);
        tag.setBoolean("Hooked", hooked);
        tag.setDouble("PaidOut", rope.paidOut());
        if (frame != null) tag.setIntArray("Sheave", new int[] { frame.sheaveX, frame.sheaveY, frame.sheaveZ });
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        coils = tag.getInteger("RopeCoils");
        silk = tag.getInteger("RopeSilk");
        brake = !tag.hasKey("Brake") || tag.getBoolean("Brake");
        hooked = tag.getBoolean("Hooked");
        rope.set(tag.hasKey("PaidOut") ? tag.getDouble("PaidOut") : TOP, 0.0);
        int[] s = tag.getIntArray("Sheave");
        clientSheave = s.length == 3 ? s : null;
    }

    /** The client's copy of where the sheave is, from the server's word: for drawing the rope to it. */
    int[] clientSheave;
    private EntityHoistLoad clientLoad;
    private int clientLook;

    /** On a client: the basket hanging on this windlass's rope, if one is near enough to be known. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public EntityHoistLoad clientLoad() {
        if (clientLoad != null && !clientLoad.isDead) return clientLoad;
        clientLoad = null;
        if (worldObj == null || !hooked || clientLook-- > 0) return null;
        clientLook = 20;
        for (Object o : worldObj.loadedEntityList) if (o instanceof EntityHoistLoad) {
            EntityHoistLoad l = (EntityHoistLoad) o;
            if (l.windlassX == xCoord && l.windlassY == yCoord && l.windlassZ == zCoord) return clientLoad = l;
        }
        return null;
    }

    public int[] sheaveForDrawing() {
        return clientSheave;
    }
}
