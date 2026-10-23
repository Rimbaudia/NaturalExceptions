package naturalexceptions.minecraft.gear.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.BlockCrumblingSkystone;
import naturalexceptions.minecraft.block.BlockSealedAlcove;
import naturalexceptions.minecraft.block.BlockSkystoneBricks;
import naturalexceptions.minecraft.block.BlockTriggerStone;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;

/**
 * The Warden's Visor: outlines what a dungeon hides within twelve blocks, seen through walls. Trigger Stones and
 * buried TNT in red, crumbling floor in amber, Sealed Alcoves in violet, loose secret walls in sky blue.
 */
@SideOnly(Side.CLIENT)
public final class VisorSight {

    static final int RANGE = 12;
    private static final int SCAN_EVERY = 10;

    private final List<int[]> seen = new ArrayList<>();
    private int timer;

    public static void register() {
        VisorSight sight = new VisorSight();
        MinecraftForge.EVENT_BUS.register(sight);
        FMLCommonHandler.instance()
            .bus()
            .register(sight);
    }

    /** Outline colour for a hidden thing, or 0 if the block hides nothing. */
    static int colour(Block block, int meta) {
        if (block instanceof BlockTriggerStone || block == Blocks.tnt) return 0xFF5040;
        if (block instanceof BlockCrumblingSkystone) return 0xFFB030;
        if (block instanceof BlockSealedAlcove) return 0xC060FF;
        if (block instanceof BlockSkystoneBricks && meta == BlockSkystoneBricks.LOOSE) return 0x60D0FF;
        return 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++timer < SCAN_EVERY) return;
        timer = 0;
        seen.clear();
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null || ItemTreasureGear.worn(player, Treasure.WARDENS_VISOR) == 0) return;
        World world = player.worldObj;
        int px = MathHelper.floor_double(player.posX), py = MathHelper.floor_double(player.posY),
            pz = MathHelper.floor_double(player.posZ);
        // Section by section, straight from the chunks' storage: an empty (all-air) section is skipped whole, and the
        // rest is read as arrays rather than a world lookup per block.
        int y0 = Math.max(0, py - RANGE), y1 = Math.min(255, py + RANGE);
        for (int cx = (px - RANGE) >> 4; cx <= (px + RANGE) >> 4; cx++)
            for (int cz = (pz - RANGE) >> 4; cz <= (pz + RANGE) >> 4; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) continue;
                net.minecraft.world.chunk.storage.ExtendedBlockStorage[] sections = world
                    .getChunkFromChunkCoords(cx, cz)
                    .getBlockStorageArray();
                int xa = Math.max(px - RANGE, cx << 4), xb = Math.min(px + RANGE, (cx << 4) + 15);
                int za = Math.max(pz - RANGE, cz << 4), zb = Math.min(pz + RANGE, (cz << 4) + 15);
                for (int sy = y0 >> 4; sy <= y1 >> 4; sy++) {
                    net.minecraft.world.chunk.storage.ExtendedBlockStorage section = sections[sy];
                    if (section == null || section.isEmpty()) continue;
                    int ya = Math.max(y0, sy << 4), yb = Math.min(y1, (sy << 4) + 15);
                    for (int x = xa; x <= xb; x++) for (int y = ya; y <= yb; y++) for (int z = za; z <= zb; z++) {
                        Block block = section.getBlockByExtId(x & 15, y & 15, z & 15);
                        if (block == Blocks.air || block == Blocks.stone || block == Blocks.dirt) continue;
                        int colour = colour(block, section.getExtBlockMetadata(x & 15, y & 15, z & 15));
                        if (colour != 0) seen.add(new int[] { x, y, z, colour });
                    }
                }
            }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (seen.isEmpty()) return;
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        float t = event.partialTicks;
        double cx = player.lastTickPosX + (player.posX - player.lastTickPosX) * t;
        double cy = player.lastTickPosY + (player.posY - player.lastTickPosY) * t;
        double cz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * t;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);
        float pulse = 0.55F + 0.25F * (float) Math.sin((player.ticksExisted + t) * 0.15);
        for (int[] s : seen) {
            GL11.glColor4f(
                ((s[3] >> 16) & 0xFF) / 255.0F,
                ((s[3] >> 8) & 0xFF) / 255.0F,
                (s[3] & 0xFF) / 255.0F,
                pulse);
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                s[0] - cx + 0.02,
                s[1] - cy + 0.02,
                s[2] - cz + 0.02,
                s[0] - cx + 0.98,
                s[1] - cy + 0.98,
                s[2] - cz + 0.98);
            RenderGlobal.drawOutlinedBoundingBox(box, -1);
        }
        GL11.glPopAttrib();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
