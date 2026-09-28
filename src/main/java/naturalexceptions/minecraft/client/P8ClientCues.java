package naturalexceptions.minecraft.client;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.event.P8WorldPresence;
import naturalexceptions.minecraft.network.PacketP8Status;

/**
 * Restrained environmental cues for world events a player is standing in: during a Blood Night the night fog
 * shifts toward rust and still water nearby stirs. Nothing is drawn as an overlay.
 */
@SideOnly(Side.CLIENT)
public final class P8ClientCues {

    /** 0 = ordinary night, 1 = full Blood Night tint; eased so the change is gradual. */
    private float tint;

    private static boolean bloodNightVisible(World world) {
        return PacketP8Status.clientBloodNight && world != null
            && world.provider.dimensionId == 0
            && P8WorldPresence.isNight(world.getWorldTime());
    }

    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        if (tint <= 0.001F) return;
        float t = tint * 0.55F;
        event.red = event.red * (1.0F - t) + 0.34F * t;
        event.green = event.green * (1.0F - t) + 0.04F * t;
        event.blue = event.blue * (1.0F - t) + 0.03F * t;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        EntityPlayer player = mc.thePlayer;
        if (world == null || player == null) {
            tint = 0.0F;
            return;
        }
        float target = bloodNightVisible(world) ? 1.0F : 0.0F;
        tint += (target - tint) * 0.02F;
        if (target < 0.5F || mc.isGamePaused()) return;
        // Still water within a few blocks stirs: a handful of dark bubbles per second, never a flood of particles.
        for (int i = 0; i < 2; i++) {
            int x = MathHelper.floor_double(player.posX) + world.rand.nextInt(17) - 8;
            int z = MathHelper.floor_double(player.posZ) + world.rand.nextInt(17) - 8;
            int y = MathHelper.floor_double(player.posY) + world.rand.nextInt(7) - 3;
            if (world.getBlock(x, y, z)
                .getMaterial() == Material.water && world.isAirBlock(x, y + 1, z)) {
                world.spawnParticle("reddust", x + world.rand.nextDouble(), y + 1.02, z + world.rand.nextDouble(),
                    0.45, 0.02, 0.02);
                world.spawnParticle("bubble", x + world.rand.nextDouble(), y + 0.9, z + world.rand.nextDouble(), 0.0,
                    0.04, 0.0);
            }
        }
    }
}
