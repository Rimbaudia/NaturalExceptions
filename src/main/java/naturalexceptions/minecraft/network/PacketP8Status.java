package naturalexceptions.minecraft.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server-to-client summary of the world events a player is standing in, for restrained sky and water cues. */
public final class PacketP8Status implements IMessage {

    /** Latest status received by this client; read by client-only presentation code. */
    public static volatile boolean clientBloodNight;

    private boolean bloodNight;

    public PacketP8Status() {}

    public PacketP8Status(boolean bloodNight) {
        this.bloodNight = bloodNight;
    }

    public boolean bloodNight() {
        return bloodNight;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        bloodNight = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(bloodNight);
    }

    /** Only stores a flag, so it is safe to register on both physical sides. */
    public static final class Handler implements IMessageHandler<PacketP8Status, IMessage> {

        @Override
        public IMessage onMessage(PacketP8Status message, MessageContext ctx) {
            if (message != null) clientBloodNight = message.bloodNight;
            return null;
        }
    }
}
