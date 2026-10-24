package naturalexceptions.minecraft.worldgen.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.minecraft.network.PacketDungeonChart;
import naturalexceptions.minecraft.worldgen.DungeonChart;

/**
 * A Surveyor's Chart, open: one level of the dungeon on a sheet of parchment. Walked rooms are inked in, the rooms
 * they open onto sketched round with dots, the ways walked drawn between them; marks show the ways down (▼) and up
 * (▲), the entrance, the Warden's gate and the vault, and the old surveyor's key mark. You are the red arrow. The
 * arrows at the foot turn to the other levels.
 */
@SideOnly(Side.CLIENT)
public final class GuiDungeonChart extends GuiScreen {

    private static final int PAPER = 0xFFE9DCB5, PAPER_EDGE = 0xFF8C6A40, INK = 0xFF4A3520, SKETCH = 0xFF9C8058,
        ROOM = 0xFFC8B084, KEY = 0xFFB0302A, YOU = 0xFFD02020;
    static final String[] LEVEL_NAMES = { "Upper Halls", "Overgrown", "Forgotten", "Storm-struck" };

    private final DungeonChart chart;
    private int level;
    private int left, top, sheetW, sheetH;

    public GuiDungeonChart(DungeonChart chart) {
        this.chart = chart;
        this.level = Math.max(0, Math.min(chart.levels - 1, chart.playerLevel));
    }

    /** Opens a chart the server sent, on the client's own thread. */
    public static final class Opener {

        public static void register() {
            FMLCommonHandler.instance()
                .bus()
                .register(new Opener());
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || PacketDungeonChart.pending == null) return;
            DungeonChart chart = PacketDungeonChart.pending;
            PacketDungeonChart.pending = null;
            net.minecraft.client.Minecraft.getMinecraft()
                .displayGuiScreen(new GuiDungeonChart(chart));
        }
    }

    @Override
    public void initGui() {
        sheetW = Math.min(width - 20, 300);
        sheetH = Math.min(height - 20, 230);
        left = (width - sheetW) / 2;
        top = (height - sheetH) / 2;
        buttonList.clear();
        buttonList.add(new GuiButton(0, left + 8, top + sheetH - 26, 20, 20, "<"));
        buttonList.add(new GuiButton(1, left + sheetW - 28, top + sheetH - 26, 20, 20, ">"));
        updateButtons();
    }

    private void updateButtons() {
        ((GuiButton) buttonList.get(0)).enabled = level > 0;
        ((GuiButton) buttonList.get(1)).enabled = level < chart.levels - 1;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        level = Math.max(0, Math.min(chart.levels - 1, level + (button.id == 0 ? -1 : 1)));
        updateButtons();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** "Level −2 · Overgrown". */
    static String levelTitle(int level, int levels) {
        String name = LEVEL_NAMES[SkyDungeon.Theme.of(level, levels)
            .ordinal()];
        return "Level −" + (level + 1) + " · " + name;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left - 2, top - 2, left + sheetW + 2, top + sheetH + 2, PAPER_EDGE);
        drawRect(left, top, left + sheetW, top + sheetH, PAPER);
        drawCenteredString(fontRendererObj, levelTitle(level, chart.levels), width / 2, top + 6, 0xFF3A2A18);
        // The old surveyor's line about the key.
        String keyLine = chart.keyLevel < 0 ? "No key was ever kept here."
            : "The surveyor marked the key on level −" + (chart.keyLevel + 1) + ".";
        fontRendererObj
            .drawString(keyLine, (width - fontRendererObj.getStringWidth(keyLine)) / 2, top + sheetH - 20, 0x7A2A20);

        List<DungeonChart.Room> rooms = new ArrayList<>();
        for (DungeonChart.Room r : chart.rooms) if (r.level == level) rooms.add(r);
        int mapTop = top + 20, mapBottom = top + sheetH - 30, mapLeft = left + 10, mapRight = left + sheetW - 10;
        if (rooms.isEmpty()) {
            String none = "You have not been down here yet.";
            fontRendererObj.drawString(
                none,
                (width - fontRendererObj.getStringWidth(none)) / 2,
                (mapTop + mapBottom) / 2,
                0x6A5030);
        } else {
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (DungeonChart.Room r : rooms) {
                minX = Math.min(minX, r.minX);
                minZ = Math.min(minZ, r.minZ);
                maxX = Math.max(maxX, r.maxX + 1);
                maxZ = Math.max(maxZ, r.maxZ + 1);
            }
            if (chart.playerLevel == level) {
                minX = Math.min(minX, chart.playerX - 2);
                maxX = Math.max(maxX, chart.playerX + 2);
                minZ = Math.min(minZ, chart.playerZ - 2);
                maxZ = Math.max(maxZ, chart.playerZ + 2);
            }
            double scale = Math
                .min((mapRight - mapLeft) / (double) (maxX - minX), (mapBottom - mapTop) / (double) (maxZ - minZ));
            scale = Math.min(scale, 4.0);
            int ox = (int) ((mapLeft + mapRight) / 2 - (minX + maxX) / 2.0 * scale);
            int oz = (int) ((mapTop + mapBottom) / 2 - (minZ + maxZ) / 2.0 * scale);
            // The ways walked, under the rooms.
            for (int[] link : chart.links) {
                DungeonChart.Room a = chart.rooms.get(link[0]), b = chart.rooms.get(link[1]);
                if (a.level != level) continue;
                line(
                    ox + (int) ((a.minX + a.maxX + 1) / 2.0 * scale),
                    oz + (int) ((a.minZ + a.maxZ + 1) / 2.0 * scale),
                    ox + (int) ((b.minX + b.maxX + 1) / 2.0 * scale),
                    oz + (int) ((b.minZ + b.maxZ + 1) / 2.0 * scale),
                    INK);
            }
            for (DungeonChart.Room r : rooms) {
                int x0 = ox + (int) (r.minX * scale), z0 = oz + (int) (r.minZ * scale);
                int x1 = ox + (int) ((r.maxX + 1) * scale), z1 = oz + (int) ((r.maxZ + 1) * scale);
                if (r.visited) {
                    drawRect(x0, z0, x1, z1, INK);
                    drawRect(x0 + 1, z0 + 1, x1 - 1, z1 - 1, ROOM);
                } else dotted(x0, z0, x1, z1, r.mark == DungeonChart.KEY ? KEY : SKETCH);
                String mark = mark(r.mark);
                if (!mark.isEmpty()) fontRendererObj.drawString(
                    mark,
                    (x0 + x1 - fontRendererObj.getStringWidth(mark)) / 2,
                    (z0 + z1) / 2 - 4,
                    r.mark == DungeonChart.KEY ? 0xB0302A : r.mark == DungeonChart.WARDEN ? 0x5A2A80 : 0x3A2A18);
            }
            if (chart.playerLevel == level) {
                int px = ox + (int) ((chart.playerX + 0.5) * scale), pz = oz + (int) ((chart.playerZ + 0.5) * scale);
                double yaw = Math.toRadians(chart.playerYaw);
                // Minecraft faces south at yaw 0.
                int fx = px - (int) Math.round(Math.sin(yaw) * 6), fz = pz + (int) Math.round(Math.cos(yaw) * 6);
                line(px, pz, fx, fz, YOU);
                drawRect(px - 2, pz - 2, px + 2, pz + 2, YOU);
            }
        }
        String legend = "▼ down   ▲ up   E entrance   K key   C Castellan   V vault";
        fontRendererObj
            .drawString(legend, (width - fontRendererObj.getStringWidth(legend)) / 2, top + sheetH - 10, 0x6A5030);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static String mark(int mark) {
        switch (mark) {
            case DungeonChart.DOWN:
                return "▼";
            case DungeonChart.UP:
                return "▲";
            case DungeonChart.ENTRANCE:
                return "E";
            case DungeonChart.KEY:
                return "K";
            case DungeonChart.WARDEN:
                return "C";
            case DungeonChart.VAULT:
                return "V";
            default:
                return "";
        }
    }

    /** A room only glimpsed: its outline in dots. */
    private static void dotted(int x0, int z0, int x1, int z1, int colour) {
        for (int x = x0; x < x1; x += 2) {
            Gui.drawRect(x, z0, x + 1, z0 + 1, colour);
            Gui.drawRect(x, z1 - 1, x + 1, z1, colour);
        }
        for (int z = z0; z < z1; z += 2) {
            Gui.drawRect(x0, z, x0 + 1, z + 1, colour);
            Gui.drawRect(x1 - 1, z, x1, z + 1, colour);
        }
    }

    /** A one-pixel line, stepped. */
    private static void line(int x0, int z0, int x1, int z1, int colour) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (steps == 0 ? 0 : (x1 - x0) * i / steps), z = z0 + (steps == 0 ? 0 : (z1 - z0) * i / steps);
            Gui.drawRect(x, z, x + 1, z + 1, colour);
        }
    }
}
