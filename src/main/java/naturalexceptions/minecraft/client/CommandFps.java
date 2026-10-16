package naturalexceptions.minecraft.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * {@code /ne_fps}: ten seconds of where this machine's frames go, written to the log for reading later. Frame times
 * (average, worst, how many were stutters), the integrated server's tick times, what is loaded and drawn, and
 * vanilla's profiler breakdown of the frame. The profiler only runs while the F3 pie chart is up, so the chart is
 * shown for the ten seconds; it costs a little itself.
 */
@SideOnly(Side.CLIENT)
public final class CommandFps extends CommandBase {

    private static final long SPAN_NANOS = 10_000_000_000L;
    /** A frame this long is a visible hitch. */
    private static final long STUTTER_NANOS = 50_000_000L;
    /** Profiler sections smaller than this share of the whole frame are left out of the report. */
    private static final double SHOWN_SHARE = 1.0;

    private static boolean running;
    private static long start, lastFrame, worst, frames, stutters;
    private static boolean hadDebug, hadChart;
    private static final List<String> chunkUpdates = new ArrayList<>();

    public static void register() {
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new CommandFps());
        FMLCommonHandler.instance()
            .bus()
            .register(new Recorder());
    }

    @Override
    public String getCommandName() {
        return "ne_fps";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_fps - records ten seconds of frame and server timings into the log";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (running) return;
        Minecraft mc = Minecraft.getMinecraft();
        hadDebug = mc.gameSettings.showDebugInfo;
        hadChart = mc.gameSettings.showDebugProfilerChart;
        mc.gameSettings.showDebugInfo = true;
        mc.gameSettings.showDebugProfilerChart = true;
        start = lastFrame = System.nanoTime();
        worst = frames = stutters = 0;
        chunkUpdates.clear();
        running = true;
        sender.addChatMessage(
            new ChatComponentText("Recording 10 seconds of frame timings; hold still or play normally."));
    }

    public static final class Recorder {

        private long nextSecond;

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent event) {
            if (!running || event.phase != TickEvent.Phase.END) return;
            long now = System.nanoTime(), frame = now - lastFrame;
            lastFrame = now;
            frames++;
            worst = Math.max(worst, frame);
            if (frame > STUTTER_NANOS) stutters++;
            Minecraft mc = Minecraft.getMinecraft();
            if (now >= nextSecond) {
                nextSecond = now + 1_000_000_000L;
                chunkUpdates.add(mc.debug);
            }
            if (now - start >= SPAN_NANOS) finish(mc, now);
        }

        private static void finish(Minecraft mc, long now) {
            running = false;
            List<String> out = new ArrayList<>();
            double seconds = (now - start) / 1.0e9;
            out.add(
                String.format(
                    "frames: %.1f fps, avg %.1f ms, worst %.1f ms, %d stutters over 50 ms",
                    frames / seconds,
                    seconds * 1000.0 / Math.max(1, frames),
                    worst / 1.0e6,
                    stutters));
            IntegratedServer server = mc.getIntegratedServer();
            if (server != null && server.tickTimeArray != null) {
                long sum = 0, max = 0;
                for (long t : server.tickTimeArray) {
                    sum += t;
                    max = Math.max(max, t);
                }
                out.add(
                    String.format(
                        "server tick: avg %.1f ms, worst %.1f ms (of the last 100)",
                        sum / 1.0e6 / server.tickTimeArray.length,
                        max / 1.0e6));
            }
            out.add(
                "loaded: " + mc.theWorld.loadedEntityList.size()
                    + " entities, "
                    + mc.theWorld.loadedTileEntityList.size()
                    + " tile entities; "
                    + mc.renderGlobal.getDebugInfoRenders()
                    + " | "
                    + mc.renderGlobal.getDebugInfoEntities());
            out.add("each second: " + String.join(" / ", chunkUpdates));
            out.add("where the frame goes (share of the whole frame):");
            section(mc.mcProfiler, "root", 0, out);
            mc.gameSettings.showDebugInfo = hadDebug;
            mc.gameSettings.showDebugProfilerChart = hadChart;
            for (String line : out) FMLLog.info("[ne_fps] %s", line);
            if (mc.thePlayer != null) mc.thePlayer.addChatMessage(
                new ChatComponentText("ne_fps: " + out.get(0) + " - full report in the log (logs/latest.log)."));
        }

        private static void section(Profiler profiler, String name, int depth, List<String> out) {
            if (depth > 6) return;
            List<?> results = profiler.getProfilingData(name);
            if (results == null) return;
            for (int i = 1; i < results.size(); i++) {
                Profiler.Result r = (Profiler.Result) results.get(i);
                if (r.field_76330_b < SHOWN_SHARE) continue;
                StringBuilder indent = new StringBuilder();
                for (int d = 0; d <= depth; d++) indent.append("  ");
                out.add(String.format("%s%s %.1f%%", indent, r.field_76331_c, r.field_76330_b));
                if (!"unspecified".equals(r.field_76331_c))
                    section(profiler, name + "." + r.field_76331_c, depth + 1, out);
            }
        }
    }
}
