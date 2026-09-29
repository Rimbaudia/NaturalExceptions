package naturalexceptions.minecraft.block;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.minecraft.entity.EntitySkyWarden;
import naturalexceptions.minecraft.registry.NEBlocks;

/** Watches for players; wakes the Sky Warden once, then becomes plain Skystone. */
public final class TileEntityWardenSeal extends TileEntity {

    static final double WAKE_RANGE = 8.0;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || worldObj.getTotalWorldTime() % 10 != 0) return;
        EntityPlayer near = worldObj.getClosestPlayer(xCoord + 0.5, yCoord + 1.0, zCoord + 0.5, WAKE_RANGE);
        if (near == null) return;
        EntitySkyWarden warden = new EntitySkyWarden(worldObj);
        warden.setLocationAndAngles(xCoord + 0.5, yCoord + 1.0, zCoord + 0.5, worldObj.rand.nextFloat() * 360.0F, 0.0F);
        warden.setHomeArea(xCoord, yCoord, zCoord, 14);
        worldObj.spawnEntityInWorld(warden);
        worldObj.playSoundAtEntity(warden, "naturalexceptions:warden.roar", 2.5F, 1.0F);
        @SuppressWarnings("unchecked")
        List<EntityPlayer> players = worldObj.playerEntities;
        for (EntityPlayer player : players) {
            if (player.getDistanceSq(xCoord + 0.5, yCoord, zCoord + 0.5) < 40.0 * 40.0)
                player.addChatMessage(new ChatComponentText("\u00a75\u00a7lThe Sky Warden wakes."));
        }
        worldObj.setBlock(xCoord, yCoord, zCoord, NEBlocks.skystoneBricks, 0, 3);
    }
}
