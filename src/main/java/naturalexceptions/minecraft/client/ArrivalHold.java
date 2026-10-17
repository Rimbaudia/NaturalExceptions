package naturalexceptions.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Holds the player where they are while the chunk they stand in has not reached the client yet. The 1.7.10 client
 * counts a missing chunk as empty air and lets the player fall through it, so after a teleport to land that is still
 * generating (a citadel) they dropped below its floor before it arrived, and only a second teleport, with the chunks
 * already there, landed them on it.
 */
@SideOnly(Side.CLIENT)
public final class ArrivalHold {

    private boolean holding;
    private double heldX, heldY, heldZ;

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new ArrivalHold());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            holding = false;
            return;
        }
        boolean missing = mc.theWorld
            .getChunkFromBlockCoords(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posZ))
            .isEmpty();
        if (!missing) {
            holding = false;
            return;
        }
        // Where the player was put (by the server's position packet) is where they wait: moved by more than a
        // tick's fall since the last hold, they were put somewhere new.
        if (!holding || Math.abs(player.posX - heldX) > 1.0
            || Math.abs(player.posZ - heldZ) > 1.0
            || Math.abs(player.posY - heldY) > 4.0) {
            heldX = player.posX;
            heldY = player.posY;
            heldZ = player.posZ;
            holding = true;
        }
        player.setPosition(heldX, heldY, heldZ);
        player.motionX = player.motionY = player.motionZ = 0.0;
        player.fallDistance = 0.0F;
    }
}
