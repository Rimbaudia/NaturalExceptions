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
import naturalexceptions.minecraft.furniture.EntityHoistCounterweight;
import naturalexceptions.minecraft.furniture.EntityHoistLoad;
import naturalexceptions.minecraft.furniture.HoistGeometry;
import naturalexceptions.minecraft.furniture.TileEntityDerrickPivot;
import naturalexceptions.minecraft.furniture.TileEntityHoist;

/**
 * A windlass's moving parts: the drum with its wound rope, turning as the rope pays out; the crank turning with it;
 * the brake lever, up when set and laid over when off; the rope up to the sheave (straight up to a headframe's, or up
 * a derrick's mast and out along its boom to the one at its tip, following the tip while the boom swings), over the
 * sheave's turning wheel and down to the basket; and the counterweight's rope from the back of the drum down into its
 * well. Its parts are placed as its block's are ({@link naturalexceptions.minecraft.block.PartModel} authored with the
 * rope leaving towards -z), so what is drawn is where it is clicked.
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
        double r = HoistGeometry.DRUM;
        IIcon wood = Blocks.planks.getIcon(1, 1), log = Blocks.log.getIcon(2, 1),
            iron = Blocks.iron_block.getIcon(2, 0);
        IIcon cord = HoistGeometry.ropeIcon();
        drum(rope, facing, alongX, r, wood, log, iron);
        lever(hoist.braked(), facing, alongX, wood);
        pawl(hoist.braked(), facing, alongX, r, iron);
        double[] tip = tip(hoist, load, partialTicks);
        if (tip != null) {
            double ax = tip[0] - tile.xCoord, ay = tip[1] - tile.yCoord, az = tip[2] - tile.zCoord, angle = tip[3];
            double w = HoistGeometry.WHEEL, cx = Math.cos(angle), cz = Math.sin(angle);
            // The rope off the top of the drum, by way of the mast for a derrick, up to the near side of the wheel.
            double[] from = point(0.5, 0.6 + r + 0.03, 0.5, facing);
            int[] derrick = hoist.derrickForDrawing();
            if (derrick != null) {
                double px = derrick[0] - tile.xCoord + 0.5, py = derrick[1] - tile.yCoord,
                    pz = derrick[2] - tile.zCoord + 0.5;
                double[] foot = { px, py + 0.55, pz }, top = { px, derrick[3] - tile.yCoord + 0.5, pz };
                HoistDrawing.cord(cord, from, foot, 0.05);
                HoistDrawing.cord(cord, foot, top, 0.05);
                from = top;
            }
            double[] near = { ax - cx * w * 0.7, ay + w * 0.7, az - cz * w * 0.7 };
            double[] crown = { ax, ay + w, az };
            double[] far = { ax + cx * w * 0.7, ay + w * 0.7, az + cz * w * 0.7 };
            double[] edge = { ax + cx * w, ay, az + cz * w };
            HoistDrawing.cord(cord, from, near, 0.05);
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
            // The wheel: two squares a quarter-turn apart make its rim, standing in the rope's way, turning.
            GL11.glPushMatrix();
            GL11.glTranslated(ax, ay, az);
            GL11.glRotated(-Math.toDegrees(angle), 0, 1, 0);
            GL11.glRotated(-Math.toDegrees(rope / w), 0, 0, 1);
            wheel(w, log, wood, iron);
            GL11.glPopMatrix();
        }
        // The counterweight's rope, off the back of the drum and down into its well.
        EntityHoistCounterweight counter = counter(hoist);
        if (counter != null) {
            double cy = counter.lastTickPosY + (counter.posY - counter.lastTickPosY) * partialTicks;
            double[] back = point(0.5, 0.6, 0.5 + r + 0.03, facing);
            double[] bail = { counter.posX - tile.xCoord, cy + 0.95 - tile.yCoord, counter.posZ - tile.zCoord };
            HoistDrawing.cord(cord, back, bail, 0.05);
        }
        GL11.glPopMatrix();
    }

    /** The drum, its wound rope and the crank, all turning with the rope about the axle at (_, 0.6, 0.5). */
    private static void drum(double rope, int facing, boolean alongX, double r, IIcon wood, IIcon log, IIcon iron) {
        double[] hub = point(0.5, 0.6, 0.5, facing);
        GL11.glPushMatrix();
        GL11.glTranslated(hub[0], hub[1], hub[2]);
        rotate(Math.toDegrees(rope / r), alongX);
        // Barrel, the rope wound on it, and a flange either side: each eight-sided, two squares a half-turn apart.
        for (int k = 0; k < 2; k++) {
            GL11.glPushMatrix();
            rotate(45 * k, alongX);
            HoistDrawing.box(log, axial(-0.3, 0.3, r * 0.92, alongX));
            HoistDrawing.box(HoistGeometry.ropeIcon(), axial(-0.21, 0.21, r * 0.92 + 0.03, alongX));
            HoistDrawing.box(log, axial(-0.25, -0.21, r * 0.92 + 0.06, alongX));
            HoistDrawing.box(log, axial(0.21, 0.25, r * 0.92 + 0.06, alongX));
            GL11.glPopMatrix();
        }
        // The ratchet's teeth round the brake end.
        for (int k = 0; k < 8; k++) {
            GL11.glPushMatrix();
            rotate(45 * k, alongX);
            double[] tooth = axial(-0.3, -0.26, 0.025, alongX);
            tooth[1] = r + 0.02;
            tooth[4] = r + 0.1;
            HoistDrawing.box(iron, tooth);
            GL11.glPopMatrix();
        }
        HoistDrawing.box(iron, axial(-0.5, 0.5, 0.04, alongX)); // the axle, out through both posts
        double[] arm = axial(0.47, 0.51, 0.03, alongX);
        arm[4] = 0.28;
        HoistDrawing.box(iron, arm);
        double[] handle = axial(0.47, 0.62, 0.035, alongX);
        handle[1] = 0.24;
        handle[4] = 0.31;
        HoistDrawing.box(wood, handle);
        GL11.glPopMatrix();
    }

    /**
     * The pawl on the brake-end post: down on the ratchet's teeth when the brake is set (the rope can only be wound
     * in), lifted clear when it is off.
     */
    private static void pawl(boolean braked, int facing, boolean alongX, double r, IIcon iron) {
        double[] hinge = point(0.22, 0.6 + r + 0.22, 0.5 + r + 0.05, facing);
        GL11.glPushMatrix();
        GL11.glTranslated(hinge[0], hinge[1], hinge[2]);
        // Lying back towards the drum's top and down onto it when set; swung up off it when not.
        boolean flip = facing == 3 || facing == 4;
        rotate((braked ? 0 : 40) * (flip ? -1 : 1), alongX);
        double[] arm = axial(0.0, 0.04, 0.02, alongX);
        double reach = 0.18;
        if (alongX) {
            arm[1] = -0.12;
            arm[4] = -0.08;
            arm[2] = flip ? 0 : -reach;
            arm[5] = flip ? reach : 0;
        } else {
            arm[1] = -0.12;
            arm[4] = -0.08;
            arm[0] = flip ? 0 : -reach;
            arm[3] = flip ? reach : 0;
        }
        HoistDrawing.box(iron, arm);
        GL11.glPopMatrix();
    }

    /** A sheave's wheel in its own frame (its axle along z): an eight-sided rim, six spokes and a hub. */
    private static void wheel(double w, IIcon log, IIcon wood, IIcon iron) {
        double side = w * Math.tan(Math.PI / 8);
        for (int k = 0; k < 8; k++) {
            GL11.glPushMatrix();
            GL11.glRotated(45 * k + 22.5, 0, 0, 1);
            HoistDrawing.box(log, new double[] { -side, w - 0.07, -0.07, side, w, 0.07 });
            GL11.glPopMatrix();
        }
        for (int k = 0; k < 6; k++) {
            GL11.glPushMatrix();
            GL11.glRotated(60 * k, 0, 0, 1);
            HoistDrawing.box(wood, new double[] { -0.02, 0.05, -0.025, 0.02, w - 0.06, 0.025 });
            GL11.glPopMatrix();
        }
        HoistDrawing.box(iron, new double[] { -0.07, -0.07, -0.08, 0.07, 0.07, 0.08 });
    }

    /** The brake lever on its quadrant at the -x end: up when set, laid over when off. */
    private static void lever(boolean braked, int facing, boolean alongX, IIcon wood) {
        double[] pivot = point(0.025, 0.45, 0.5, facing);
        GL11.glPushMatrix();
        GL11.glTranslated(pivot[0], pivot[1], pivot[2]);
        rotate(braked ? 0 : 50, alongX);
        double[] lever = axial(-0.02, 0.02, 0.025, alongX);
        lever[1] = 0.0;
        lever[4] = 0.5;
        HoistDrawing.box(wood, lever);
        GL11.glPopMatrix();
    }

    /** The sheave's axle where it is now and the way the rope leaves it: {x, y, z, angle}; null if none is known. */
    private static double[] tip(TileEntityHoist hoist, EntityHoistLoad load, float partialTicks) {
        int[] derrick = hoist.derrickForDrawing();
        if (derrick != null) {
            TileEntity t = hoist.getWorldObj()
                .getTileEntity(derrick[0], derrick[1], derrick[2]);
            if (t instanceof TileEntityDerrickPivot && ((TileEntityDerrickPivot) t).slewing()) {
                TileEntityDerrickPivot pivot = (TileEntityDerrickPivot) t;
                double ticks = pivot.ticksIn(partialTicks);
                double[] p = pivot.tip(ticks);
                return new double[] { p[0], p[1], p[2], pivot.angle(ticks) };
            }
        }
        int[] s = hoist.sheaveForDrawing();
        if (s == null) return null;
        double angle;
        if (derrick != null) angle = TileEntityDerrickPivot.angleOf(derrick[4]);
        else {
            double[] a = HoistGeometry.away(hoist.getWorldObj(), hoist.xCoord, hoist.zCoord, s[0], s[1], s[2]);
            angle = Math.atan2(a[2], a[0]);
        }
        return new double[] { s[0] + 0.5, s[1] + 0.5, s[2] + 0.5, angle };
    }

    private static EntityHoistCounterweight counter(TileEntityHoist hoist) {
        if (!hoist.counterHooked()) return null;
        for (Object o : hoist.getWorldObj().loadedEntityList) if (o instanceof EntityHoistCounterweight) {
            EntityHoistCounterweight c = (EntityHoistCounterweight) o;
            if (c.windlassX == hoist.xCoord && c.windlassY == hoist.yCoord && c.windlassZ == hoist.zCoord) return c;
        }
        return null;
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
