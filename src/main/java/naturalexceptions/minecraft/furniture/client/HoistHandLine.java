package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.EntityHoistLoad;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketHoistHaul;

/**
 * A rider in a hoist's basket works its hand-line by holding use: looking up hauls the basket up, looking down lets it
 * down. Sent a few times a second while held.
 */
@SideOnly(Side.CLIENT)
public final class HoistHandLine {

    private int cooldown;

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new HoistHandLine());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null || !(mc.thePlayer.ridingEntity instanceof EntityHoistLoad))
            return;
        if (cooldown > 0) cooldown--;
        if (!mc.gameSettings.keyBindUseItem.getIsKeyPressed() || cooldown > 0) return;
        float pitch = mc.thePlayer.rotationPitch;
        if (Math.abs(pitch) < 20.0F) return; // looking ahead: neither
        NENetwork.CHANNEL.sendToServer(new PacketHoistHaul(pitch < 0 ? -1 : 1));
        cooldown = 3;
    }
}
