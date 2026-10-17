package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import naturalexceptions.core.hoisting.HoistDynamics;
import naturalexceptions.core.hoisting.HoistMasses;
import naturalexceptions.minecraft.item.ItemHoistRope;
import naturalexceptions.minecraft.multiblock.DerrickDefinition;
import naturalexceptions.minecraft.multiblock.HoistFrame;
import naturalexceptions.minecraft.multiblock.HoistFrames;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A windlass at work: the drum, the rope wound on it, the pawl and brake, the basket on the rope's end with its cargo
 * (the store's nine slots, which the basket carries), and a counterweight if one is hung. It works the frame it stands
 * by ({@link HoistFrames}): a headframe (the rope runs up over its sheave and down the shaft) or a derrick (up the
 * mast, out along the boom, over the sheave at its tip and down).
 * <p>
 * The rope is fitted by hand, a coil of Hoist Rope (or a length of Cloudsilk Rope) at a time. With rope enough to
 * reach it, a basket set under the sheave is hooked on; from then the windlass hangs it on the rope
 * ({@link EntityHoistLoad}) and runs the rope each tick ({@link HoistDynamics}): wound up at the crank, let down by
 * easing the brake, or running free with the brake off.
 * <p>
 * A Counterweight Box set in a well dug straight down behind the windlass hangs from the drum the other way round
 * ({@link EntityHoistCounterweight}): as the basket goes down it comes up, as the basket comes up it goes down. Its
 * blocks of stone weigh against the basket; its well's depth limits how far the basket may go either way.
 */
public final class TileEntityHoist extends TileEntityStorage {

    /** The basket never comes nearer the sheave's axle than this: a rider's head clears the wheel. */
    static final double TOP = 2.2;
    /** How long one turn of the crank (one right-click while held) keeps the rope working, in ticks. */
    private static final int WORK_TICKS = 6;
    /** The most blocks of stone a counterweight box takes. */
    public static final int COUNTER_CAPACITY = 16;

    private int coils, silk;
    private boolean brake = true, hooked;
    private final HoistDynamics rope = new HoistDynamics(TOP);
    /** The counterweight: hung, its stone, and the rope out at which it would be at the well's mouth. */
    private boolean counterHooked;
    private int counterBlocks;
    private double counterRef;

    private EntityHoistLoad load;
    private EntityHoistCounterweight counter;
    private HoistFrame frame;
    private int recheck, floorCheck;
    private double floor = Double.MAX_VALUE, lastTop = TOP;
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

    public HoistFrame frame() {
        return frame;
    }

    EntityHoistLoad load() {
        return load;
    }

    /** Whether the basket is hoisted right up. */
    boolean atTop() {
        return rope.paidOut() <= lastTop + 0.3;
    }

    /** Whether this windlass works the derrick on the pivot at (x, y, z). */
    boolean worksPivot(int x, int y, int z) {
        return frame != null && frame.kind == HoistFrame.Kind.DERRICK
            && frame.pivotX == x
            && frame.pivotY == y
            && frame.pivotZ == z;
    }

    public int counterBlocks() {
        return counterBlocks;
    }

    public boolean counterHooked() {
        return counterHooked;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    /** The well a counterweight hangs in: the column behind the windlass, {x, z}. */
    int[] well() {
        int[] d = DerrickDefinition.step(BlockHoist.facing(getBlockMetadata()));
        return new int[] { xCoord - d[0], zCoord - d[1] };
    }

    /** How deep the well behind the windlass is, from the ground at its mouth down: clear blocks. */
    private int wellDepth() {
        int[] w = well();
        int depth = 0;
        while (depth < 64 && HoistGeometry.clearFor(worldObj, w[0], yCoord - 1 - depth, w[1])) depth++;
        return depth;
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

    /** Stone loaded into the counterweight at once, by worldgen. */
    public void weightCounterBlocks(int n) {
        counterBlocks = Math.min(COUNTER_CAPACITY, counterBlocks + n);
        markDirty();
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
        int[] c = cargo();
        return String.format(
            "Basket: %d block%s and %d other thing%s of cargo, %.0f kg with no one in it%s.",
            c[0],
            c[0] == 1 ? "" : "s",
            c[1],
            c[1] == 1 ? "" : "s",
            HoistMasses.basket(0, c[0], c[1]),
            counterHooked ? String.format("; the counterweight %.0f kg", HoistMasses.counterweight(counterBlocks))
                : "");
    }

    /** {blocks, other items} in the basket's cargo. */
    private int[] cargo() {
        int blocks = 0, items = 0;
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack s = getStackInSlot(i);
            if (s == null) continue;
            if (s.getItem() instanceof ItemBlock) blocks += s.stackSize;
            else items += s.stackSize;
        }
        return new int[] { blocks, items };
    }

    /** Weights the counterweight with a block from {@code held}; or with none held, takes one back. */
    void weightCounter(EntityPlayer player, ItemStack held) {
        if (held != null && held.getItem() instanceof ItemBlock) {
            if (counterBlocks >= COUNTER_CAPACITY) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText("The counterweight box is full."));
                return;
            }
            counterBlocks++;
            if (!player.capabilities.isCreativeMode) held.stackSize--;
            if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        } else if (held == null && counterBlocks > 0) {
            counterBlocks--;
            ItemStack stone = new ItemStack(net.minecraft.init.Blocks.cobblestone);
            if (!player.inventory.addItemStackToInventory(stone))
                worldObj.spawnEntityInWorld(new EntityItem(worldObj, player.posX, player.posY, player.posZ, stone));
        }
        markDirty();
        player.addChatMessage(
            new net.minecraft.util.ChatComponentText(
                String.format(
                    "Counterweight: %d block%s, %.0f kg.",
                    counterBlocks,
                    counterBlocks == 1 ? "" : "s",
                    HoistMasses.counterweight(counterBlocks))));
    }

    /** Takes the basket off the rope where it hangs: a block again there if it can be, else the player's. */
    void unhook(EntityPlayer player) {
        if (!hooked || load == null || Math.abs(rope.speed()) > 1e-4) return;
        place(
            player,
            (int) Math.floor(load.posX),
            (int) Math.floor(load.posY + 0.5),
            (int) Math.floor(load.posZ),
            NEBlocks.hoistBasket);
        hooked = false;
        load.setDead();
        load = null;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /** Takes the counterweight box off its rope: a block again where it hangs, and its stone back to the player. */
    void unhookCounter(EntityPlayer player) {
        if (!counterHooked || counter == null || Math.abs(rope.speed()) > 1e-4) return;
        place(
            player,
            (int) Math.floor(counter.posX),
            (int) Math.floor(counter.posY + 0.5),
            (int) Math.floor(counter.posZ),
            NEBlocks.counterweightBox);
        if (counterBlocks > 0) {
            ItemStack stone = new ItemStack(net.minecraft.init.Blocks.cobblestone, counterBlocks);
            if (!player.inventory.addItemStackToInventory(stone))
                worldObj.spawnEntityInWorld(new EntityItem(worldObj, player.posX, player.posY, player.posZ, stone));
        }
        counterBlocks = 0;
        counterHooked = false;
        counter.setDead();
        counter = null;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    private void place(EntityPlayer player, int x, int y, int z, Block block) {
        if (block == null) return;
        if (worldObj.isAirBlock(x, y, z)) worldObj.setBlock(x, y, z, block);
        else if (!player.inventory.addItemStackToInventory(new ItemStack(block))) worldObj
            .spawnEntityInWorld(new EntityItem(worldObj, player.posX, player.posY, player.posZ, new ItemStack(block)));
    }

    /** Broken: the rope comes off with it, and what hung on it drops. */
    void taken() {
        drop(load);
        drop(counter);
        ItemStack[] drops = { coils > 0 && NEItems.hoistRope != null ? new ItemStack(NEItems.hoistRope, coils) : null,
            silk > 0 && NEBlocks.cloudsilkRope != null ? new ItemStack(NEBlocks.cloudsilkRope, silk) : null,
            hooked && NEBlocks.hoistBasket != null ? new ItemStack(NEBlocks.hoistBasket) : null,
            counterHooked && NEBlocks.counterweightBox != null ? new ItemStack(NEBlocks.counterweightBox) : null,
            counterBlocks > 0 ? new ItemStack(net.minecraft.init.Blocks.cobblestone, counterBlocks) : null };
        for (ItemStack s : drops) if (s != null)
            worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, s));
        coils = silk = counterBlocks = 0;
        hooked = counterHooked = false;
    }

    // --- At work -------------------------------------------------------------------------------------------------

    /** The derrick pivot this windlass works, if its boom is swinging now. */
    private TileEntityDerrickPivot swinging() {
        if (frame == null || frame.kind != HoistFrame.Kind.DERRICK) return null;
        TileEntity t = worldObj.getTileEntity(frame.pivotX, frame.pivotY, frame.pivotZ);
        return t instanceof TileEntityDerrickPivot && ((TileEntityDerrickPivot) t).slewing()
            ? (TileEntityDerrickPivot) t
            : null;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        TileEntityDerrickPivot swing = swinging();
        if (swing == null && (frame == null || --recheck <= 0)) {
            HoistFrame was = frame;
            frame = HoistFrames.find(worldObj, xCoord, yCoord, zCoord);
            recheck = 40;
            if (frame == null ? was != null : !frame.sameAs(was)) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord); // clients draw the rope to it
                floorCheck = 0;
            }
        }
        if (frame == null) {
            drop(load);
            drop(counter);
            load = null;
            counter = null;
            return;
        }
        // Where the rope comes down: the sheave, or the tip of a boom swinging round.
        double[] sheave = swing != null ? swing.tip(swing.ticksIn(0))
            : new double[] { frame.sheaveX + 0.5, frame.sheaveY + 0.5, frame.sheaveZ + 0.5 };
        double axle = sheave[1];
        if (swing != null || --floorCheck <= 0) {
            floor = floorUnder((int) Math.floor(sheave[0]), (int) Math.floor(sheave[2]), axle);
            floorCheck = 10;
        }
        // Up, it stops level with the ground the frame stands on (a headframe's collar), to step off onto.
        double top = Math.max(TOP, axle - (frame.groundY + 1));
        double bottom = Math.max(top, Math.min(ropeLength(), floor));
        // The counterweight is hung once the basket is: its reference is the rope the basket hangs on.
        if (hooked && !counterHooked) hookCounter();
        if (counterHooked) {
            // The box can come up no higher than the well's mouth, nor go down past its floor.
            bottom = Math.max(top, Math.min(bottom, counterRef));
            top = Math.min(bottom, Math.max(top, counterRef - (wellDepth() - 1)));
            if (counter == null || counter.isDead) {
                counter = new EntityHoistCounterweight(
                    worldObj,
                    new int[] { xCoord, yCoord, zCoord },
                    well(),
                    yCoord - 1,
                    counterRef - rope.paidOut());
                worldObj.spawnEntityInWorld(counter);
            }
        }
        lastTop = top;
        if (!hooked) {
            hookBasket((int) Math.floor(sheave[0]), (int) Math.floor(sheave[2]), axle, top, bottom);
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
        load.setSheave(sheave[0], sheave[2], away(swing));
        int[] c = cargo();
        HoistDynamics.Crank by = working > 0 ? crank : HoistDynamics.Crank.NONE;
        int way = working > 0 ? winding : 0;
        if (working > 0) working--;
        double counterMass = counterHooked ? HoistMasses.counterweight(counterBlocks) : 0.0;
        boolean moved = rope.step(
            HoistMasses.basket(load.riddenByEntity != null ? 1 : 0, c[0], c[1]),
            counterMass,
            by,
            way,
            brake,
            top,
            bottom);
        load.setRope(rope.paidOut());
        if (counter != null) counter.setDepth(counterRef - rope.paidOut());
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

    private static void drop(net.minecraft.entity.Entity e) {
        if (e == null) return;
        if (e.riddenByEntity != null) e.riddenByEntity.mountEntity(null);
        e.setDead();
    }

    /** The way the rope leaves the sheave's wheel to go down, as an angle (radians, from +x towards +z). */
    private double away(TileEntityDerrickPivot swing) {
        if (swing != null) return swing.angle(swing.ticksIn(0));
        if (frame.kind == HoistFrame.Kind.DERRICK) return TileEntityDerrickPivot.angleOf(frame.boomFacing);
        double[] a = HoistGeometry.away(worldObj, xCoord, zCoord, frame.sheaveX, frame.sheaveY, frame.sheaveZ);
        return Math.atan2(a[2], a[0]);
    }

    /** Rope from the axle down to where the basket would rest on what is under it, in the column (x, z). */
    private double floorUnder(int x, int z, double axle) {
        for (int y = (int) Math.floor(axle) - 1; y > 0 && y > axle - 128; y--) {
            if (!HoistGeometry.clearFor(worldObj, x, y, z)) return axle - (y + 1);
        }
        return Double.MAX_VALUE;
    }

    /** A basket block set under the sheave, within reach of the rope, is hooked on and taken up off the floor. */
    private void hookBasket(int x, int z, double axle, double top, double bottom) {
        if (ropeLength() < TOP || NEBlocks.hoistBasket == null) return;
        for (int y = (int) Math.floor(axle) - 1; y > 0 && axle - y <= ropeLength() + 0.01; y--) {
            Block block = worldObj.getBlock(x, y, z);
            if (block == NEBlocks.hoistBasket) {
                worldObj.setBlockToAir(x, y, z);
                hooked = true;
                rope.set(Math.max(top, Math.min(bottom, axle - y)), 0.0);
                markDirty();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return;
            }
            if (!HoistGeometry.clearFor(worldObj, x, y, z)) return;
        }
    }

    /** A Counterweight Box set in the well behind the windlass is hooked onto the drum, where it is. */
    private void hookCounter() {
        if (NEBlocks.counterweightBox == null) return;
        int[] w = well();
        for (int depth = 0; depth < 64; depth++) {
            int y = yCoord - 1 - depth;
            Block block = worldObj.getBlock(w[0], y, w[1]);
            if (block == NEBlocks.counterweightBox) {
                worldObj.setBlockToAir(w[0], y, w[1]);
                counterHooked = true;
                counterRef = rope.paidOut() + depth;
                markDirty();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return;
            }
            if (!HoistGeometry.clearFor(worldObj, w[0], y, w[1])) return;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null && !worldObj.isRemote) {
            drop(load);
            drop(counter);
            load = null;
            counter = null;
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
        tag.setBoolean("CounterHooked", counterHooked);
        tag.setInteger("CounterBlocks", counterBlocks);
        tag.setDouble("CounterRef", counterRef);
        if (frame != null) {
            tag.setIntArray("Sheave", new int[] { frame.sheaveX, frame.sheaveY, frame.sheaveZ });
            if (frame.kind == HoistFrame.Kind.DERRICK) tag.setIntArray(
                "Derrick",
                new int[] { frame.pivotX, frame.pivotY, frame.pivotZ, frame.mastTop, frame.boomFacing });
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        coils = tag.getInteger("RopeCoils");
        silk = tag.getInteger("RopeSilk");
        brake = !tag.hasKey("Brake") || tag.getBoolean("Brake");
        hooked = tag.getBoolean("Hooked");
        rope.set(tag.hasKey("PaidOut") ? tag.getDouble("PaidOut") : TOP, 0.0);
        counterHooked = tag.getBoolean("CounterHooked");
        counterBlocks = tag.getInteger("CounterBlocks");
        counterRef = tag.getDouble("CounterRef");
        int[] s = tag.getIntArray("Sheave");
        clientSheave = s.length == 3 ? s : null;
        int[] d = tag.getIntArray("Derrick");
        clientDerrick = d.length == 5 ? d : null;
    }

    /** The client's copy of where the sheave is, from the server's word: for drawing the rope to it. */
    int[] clientSheave;
    /** And for a derrick: {pivot x, y, z, mast top, boom facing}. */
    int[] clientDerrick;
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

    public int[] derrickForDrawing() {
        return clientDerrick;
    }
}
