package naturalexceptions.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The player at this client's screen. Only called when the effective side is the client. */
@SideOnly(Side.CLIENT)
public final class ClientPlayer {

    private ClientPlayer() {}

    public static EntityPlayer get() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc == null ? null : mc.thePlayer;
    }
}
