package naturalexceptions.minecraft.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server to client: you stand in rising air that tops out at this height. The client (which moves its own player)
 * carries the player up while the word is fresh; the server says it again while they stay in it.
 */
public final class PacketUpdraft implements IMessage {

    /** Where the rising air the client's player stands in tops out, and until when (client world time) it holds. */
    public static volatile int clientTop;
    public static volatile long clientUntil = Long.MIN_VALUE;
    /** How long one word holds, in ticks: a little past the server's next look. */
    public static final int HOLDS = 16;

    private int top;

    public PacketUpdraft() {}

    public PacketUpdraft(int top) {
        this.top = top;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        top = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(top);
    }

    /** Only stores what it was told, so it is safe to register on both physical sides. */
    public static final class Handler implements IMessageHandler<PacketUpdraft, IMessage> {

        @Override
        public IMessage onMessage(PacketUpdraft message, MessageContext ctx) {
            if (message == null) return null;
            clientTop = message.top;
            clientUntil = -1; // set from the client's clock on its next tick
            return null;
        }
    }
}
