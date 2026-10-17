package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.minecraft.multiblock.DerrickDefinition;
import naturalexceptions.minecraft.multiblock.HoistFrame;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A derrick's pivot turning its boom. To swing, the boom (its logs and the sheave at its tip) is lifted out of the
 * world and drawn turning about the mast for {@link #TICKS} ticks, the basket on its rope going round with the tip;
 * then it is set down again pointing the new way. It swings only if the boom's new line and the quarter it sweeps
 * are clear at its height, and the basket is hoisted up to the top or its own way round is clear too.
 */
public final class TileEntityDerrickPivot extends TileEntity {

    public static final int TICKS = 60;

    private boolean slewing;
    private int from, to, length, mastTop;
    private long start;
    private Block boomBlock;
    private int boomWood;

    public boolean slewing() {
        return slewing;
    }

    public int mastTop() {
        return mastTop;
    }

    public int length() {
        return length;
    }

    /** The boom's direction as an angle (radians, from +x towards +z) {@code ticks} into a swing. */
    public double angle(double ticks) {
        double a = angleOf(from), b = angleOf(to), d = b - a;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        double t = Math.max(0.0, Math.min(1.0, ticks / TICKS));
        return a + d * (t * t * (3 - 2 * t));
    }

    public double ticksIn(float partialTicks) {
        return worldObj == null ? 0 : worldObj.getTotalWorldTime() - start + partialTicks;
    }

    public static double angleOf(int facing) {
        int[] d = DerrickDefinition.step(facing);
        return Math.atan2(d[1], d[0]);
    }

    /** The sheave's axle where the turning boom has it now: {x, y, z}, block centres. */
    public double[] tip(double ticks) {
        double a = angle(ticks);
        return new double[] { xCoord + 0.5 + Math.cos(a) * length, mastTop + 0.5, zCoord + 0.5 + Math.sin(a) * length };
    }

    /** Swings the boom a quarter-turn, if it is a whole derrick and its way round is clear. */
    void slew(boolean clockwise, EntityPlayer player) {
        if (slewing) return;
        HoistFrame frame = DerrickDefinition.INSTANCE.frame(worldObj, xCoord, yCoord, zCoord);
        if (frame == null) {
            player.addChatMessage(
                new ChatComponentText(
                    "No whole derrick on this pivot: a log mast with a log boom out from its top, a Sheave at the tip."));
            return;
        }
        int next = DerrickDefinition.turn(frame.boomFacing, clockwise);
        String blocked = blocked(frame, next);
        if (blocked != null) {
            player.addChatMessage(new ChatComponentText(blocked));
            return;
        }
        int[] d = DerrickDefinition.step(frame.boomFacing);
        boomBlock = worldObj.getBlock(xCoord + d[0], frame.mastTop, zCoord + d[1]);
        boomWood = worldObj.getBlockMetadata(xCoord + d[0], frame.mastTop, zCoord + d[1]) & 3;
        for (int k = 1; k <= frame.boomLength; k++)
            worldObj.setBlockToAir(xCoord + d[0] * k, frame.mastTop, zCoord + d[1] * k);
        from = frame.boomFacing;
        to = next;
        length = frame.boomLength;
        mastTop = frame.mastTop;
        start = worldObj.getTotalWorldTime();
        slewing = true;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        worldObj.playSoundEffect(xCoord + 0.5, mastTop, zCoord + 0.5, "random.door_open", 0.8F, 0.5F);
    }

    /** Why the boom cannot swing from where it is to {@code next}; null if it can. */
    private String blocked(HoistFrame frame, int next) {
        double a = angleOf(frame.boomFacing), b = angleOf(next), d = b - a;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        int[] here = DerrickDefinition.step(frame.boomFacing);
        for (int s = 1; s <= 12; s++) {
            double ang = a + d * s / 12.0;
            for (int r = 1; r <= frame.boomLength; r++) {
                int x = (int) Math.floor(xCoord + 0.5 + Math.cos(ang) * r),
                    z = (int) Math.floor(zCoord + 0.5 + Math.sin(ang) * r);
                boolean ownBoom = (x - xCoord) * here[1] == (z - zCoord) * here[0]
                    && (x - xCoord) * here[0] + (z - zCoord) * here[1] > 0;
                if (!ownBoom && !worldObj.isAirBlock(x, frame.mastTop, z))
                    return "The boom's way round is blocked at " + x + ", " + frame.mastTop + ", " + z + ".";
            }
        }
        // The basket goes round too: hoisted up to the top it clears everything; lower, its own way must be clear.
        for (TileEntityHoist hoist : windlasses()) {
            EntityHoistLoad load = hoist.load();
            if (load == null || hoist.atTop()) continue;
            int y = (int) Math.floor(load.posY + 0.5);
            for (int s = 1; s <= 12; s++) {
                double ang = a + d * s / 12.0;
                int x = (int) Math.floor(xCoord + 0.5 + Math.cos(ang) * frame.boomLength),
                    z = (int) Math.floor(zCoord + 0.5 + Math.sin(ang) * frame.boomLength);
                for (int dy = 0; dy <= 1; dy++) if (!HoistGeometry.clearFor(worldObj, x, y + dy, z))
                    return "Hoist the basket up before swinging the boom: its way round is blocked.";
            }
        }
        return null;
    }

    /** The windlasses working this derrick. */
    java.util.List<TileEntityHoist> windlasses() {
        java.util.List<TileEntityHoist> out = new java.util.ArrayList<>();
        int r = DerrickDefinition.REACH;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            TileEntity t = worldObj.getTileEntity(xCoord + dx, yCoord, zCoord + dz);
            if (t instanceof TileEntityHoist && ((TileEntityHoist) t).worksPivot(xCoord, yCoord, zCoord))
                out.add((TileEntityHoist) t);
        }
        return out;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || !slewing || worldObj.getTotalWorldTime() - start < TICKS) return;
        // Set down pointing the new way: the logs lying along it, the sheave at the tip turning along it too.
        int[] d = DerrickDefinition.step(to);
        Block log = boomBlock != null ? boomBlock : net.minecraft.init.Blocks.log;
        int axis = d[0] != 0 ? 4 : 8;
        for (int k = 1; k < length; k++)
            worldObj.setBlock(xCoord + d[0] * k, mastTop, zCoord + d[1] * k, log, boomWood | axis, 3);
        if (NEBlocks.sheave != null) worldObj
            .setBlock(xCoord + d[0] * length, mastTop, zCoord + d[1] * length, NEBlocks.sheave, d[0] != 0 ? 0 : 1, 3);
        slewing = false;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        worldObj.playSoundEffect(xCoord + 0.5, mastTop, zCoord + 0.5, "random.wood_click", 0.8F, 0.7F);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Slewing", slewing);
        tag.setInteger("From", from);
        tag.setInteger("To", to);
        tag.setInteger("Length", length);
        tag.setInteger("MastTop", mastTop);
        tag.setLong("Start", start);
        tag.setInteger("BoomBlock", boomBlock == null ? -1 : Block.getIdFromBlock(boomBlock));
        tag.setInteger("BoomWood", boomWood);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        slewing = tag.getBoolean("Slewing");
        from = tag.getInteger("From");
        to = tag.getInteger("To");
        length = tag.getInteger("Length");
        mastTop = tag.getInteger("MastTop");
        start = tag.getLong("Start");
        int id = tag.getInteger("BoomBlock");
        boomBlock = id < 0 ? null : Block.getBlockById(id);
        boomWood = tag.getInteger("BoomWood");
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }

    /** Its turning boom is drawn far out from it. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
