package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.BlockHoist;
import naturalexceptions.minecraft.furniture.BlockSheave;
import naturalexceptions.minecraft.furniture.EntityHoistLoad;
import naturalexceptions.minecraft.furniture.HoistGeometry;
import naturalexceptions.minecraft.furniture.TileEntityHoist;

/**
 * A windlass's moving parts: the drum with its wound rope, turning as the rope pays out; the crank turning with it;
 * the brake lever, up when set and down when off; the rope from the drum up over the sheave; and the sheave's wheel,
 * turning with the rope. Its parts are placed as its block's are ({@link naturalexceptions.minecraft.block.PartModel}
 * authored with the rope leaving towards -z), so what is drawn is where it is clicked.
 */
@SideOnly(Side.CLIENT)
public final class RenderWindlass extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityHoist)) return;
        TileEntityHoist hoist = (TileEntityHoist) tile;
        int facing = BlockHoist.facing(tile.getBlockMetadata());
        EntityHoistLoad load = hoist.clientLoad();
        double rope = load == null ? hoist.paidOut()
            : load.sheave()[1] - (load.lastTickPosY + (load.posY - load.lastTickPosY) * partialTicks);
        int light = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(tile.xCoord, tile.yCoord, tile.zCoord, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        boolean alongX = facing == 2 || facing == 3; // the drum's axle
        double turn = Math.toDegrees(rope / HoistGeometry.DRUM);
        IIcon wood = Blocks.planks.getIcon(1, 1), log = Blocks.log.getIcon(2, 1),
            iron = Blocks.iron_block.getIcon(2, 0);
        // The drum, its wound rope, and the crank: all turning about the axle at (_, 0.6, 0.5).
        double[] hub = point(0.5, 0.6, 0.5, facing);
        GL11.glPushMatrix();
        GL11.glTranslated(hub[0], hub[1], hub[2]);
        rotate(turn, alongX);
        double r = HoistGeometry.DRUM;
        HoistDrawing.box(log, axial(-0.3, 0.3, r, alongX));
        HoistDrawing.box(HoistGeometry.ropeIcon(), axial(-0.22, 0.22, r + 0.03, alongX));
        HoistDrawing.box(iron, axial(-0.5, 0.5, 0.04, alongX)); // the axle, out through both posts
        // The crank on the +x end: an arm out from the axle and the handle at its end.
        double[] arm = axial(0.47, 0.51, 0.03, alongX);
        arm[alongX ? 4 : 4] = 0.28;
        HoistDrawing.box(iron, arm);
        double[] handle = axial(0.47, 0.62, 0.035, alongX);
        handle[1] = 0.24;
        handle[4] = 0.31;
        HoistDrawing.box(wood, handle);
        GL11.glPopMatrix();
        // The brake lever, on its quadrant at the -x end: up when set, laid over when off.
        double[] pivot = point(0.025, 0.45, 0.5, facing);
        GL11.glPushMatrix();
        GL11.glTranslated(pivot[0], pivot[1], pivot[2]);
        rotate(hoist.braked() ? 0 : 50, alongX);
        double[] lever = axial(-0.02, 0.02, 0.025, alongX);
        lever[1] = 0.0;
        lever[4] = 0.5;
        HoistDrawing.box(wood, lever);
        GL11.glPopMatrix();
        // The rope up to the sheave and the sheave's wheel.
        int[] sheave = hoist.sheaveForDrawing();
        if (sheave != null && tile.getWorldObj()
            .getBlock(sheave[0], sheave[1], sheave[2]) instanceof BlockSheave) {
            boolean wheelAlongZ = BlockSheave.axleAlongX(
                tile.getWorldObj()
                    .getBlockMetadata(sheave[0], sheave[1], sheave[2]));
            double[] away = HoistGeometry
                .away(tile.getWorldObj(), tile.xCoord, tile.zCoord, sheave[0], sheave[1], sheave[2]);
            double ax = sheave[0] - tile.xCoord + 0.5, ay = sheave[1] - tile.yCoord + 0.5,
                az = sheave[2] - tile.zCoord + 0.5, w = HoistGeometry.WHEEL;
            // Off the top of the drum, up to the near side of the wheel, over its top, and down its far side.
            double[] drumTop = point(0.5, 0.6 + r + 0.03, 0.5, facing);
            double[] near = { ax - away[0] * w * 0.7, ay + w * 0.7, az - away[2] * w * 0.7 };
            double[] crown = { ax, ay + w, az };
            double[] far = { ax + away[0] * w * 0.7, ay + w * 0.7, az + away[2] * w * 0.7 };
            double[] edge = { ax + away[0] * w, ay, az + away[2] * w };
            IIcon cord = HoistGeometry.ropeIcon();
            HoistDrawing.cord(cord, drumTop, near, 0.05);
            HoistDrawing.cord(cord, near, crown, 0.05);
            HoistDrawing.cord(cord, crown, far, 0.05);
            HoistDrawing.cord(cord, far, edge, 0.05);
            if (!hoist.hooked()) {
                // No basket on it: the rope's end hangs a little way down, its hook on it.
                double[] end = { edge[0], ay - 1.5, edge[2] };
                HoistDrawing.cord(cord, edge, end, 0.05);
                HoistDrawing.box(
                    iron,
                    new double[] { end[0] - 0.06, end[1] - 0.2, end[2] - 0.06, end[0] + 0.06, end[1], end[2] + 0.06 });
            }
            // The wheel: two squares a quarter-turn apart make its rim, turning about the sheave's axle.
            GL11.glPushMatrix();
            GL11.glTranslated(ax, ay, az);
            rotate(-Math.toDegrees(rope / w), wheelAlongZ);
            for (int k = 0; k < 2; k++) {
                GL11.glPushMatrix();
                rotate(45 * k, wheelAlongZ);
                double t = 0.06;
                HoistDrawing.box(
                    log,
                    wheelAlongZ ? new double[] { -t, -w, -w, t, w, w } : new double[] { -w, -w, -t, w, w, t });
                GL11.glPopMatrix();
            }
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    /** A point of the windlass as authored (rope leaving towards -z), where it is for this facing. */
    private static double[] point(double x, double y, double z, int facing) {
        switch (facing) {
            case 3:
                return new double[] { x, y, 1 - z };
            case 4:
                return new double[] { z, y, x };
            case 5:
                return new double[] { 1 - z, y, x };
            default:
                return new double[] { x, y, z };
        }
    }

    /** A box along the axle (from {@code from} to {@code to} along it), {@code r} either side of it. */
    private static double[] axial(double from, double to, double r, boolean alongX) {
        return alongX ? new double[] { from, -r, -r, to, r, r } : new double[] { -r, -r, from, r, r, to };
    }

    private static void rotate(double degrees, boolean aboutX) {
        if (aboutX) GL11.glRotated(degrees, 1, 0, 0);
        else GL11.glRotated(degrees, 0, 0, 1);
    }
}
