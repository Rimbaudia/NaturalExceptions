package naturalexceptions.minecraft.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.worldgen.DungeonChart;

/** Server-to-client: a dungeon's chart as the player knows it, to be shown at once. */
public final class PacketDungeonChart implements IMessage {

    /** The chart waiting to be shown; the client's tick opens it (never from the network thread). */
    public static volatile DungeonChart pending;

    private DungeonChart chart;

    public PacketDungeonChart() {}

    public PacketDungeonChart(DungeonChart chart) {
        this.chart = chart;
    }

    public DungeonChart chart() {
        return chart;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        chart = DungeonChart.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        chart.write(buf);
    }

    public static final class Handler implements IMessageHandler<PacketDungeonChart, IMessage> {

        @Override
        public IMessage onMessage(PacketDungeonChart message, MessageContext ctx) {
            if (message != null && message.chart != null) pending = message.chart;
            return null;
        }
    }
}
