package naturalexceptions.minecraft.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.biology.BodyStatusSnapshot;

/** Server-to-client copy of the player's own body state, for the inventory Body panel. */
public final class PacketBodyStatus implements IMessage {

    /** Latest body state received by this client; read by client-only presentation code. */
    public static volatile BodyStatusSnapshot clientLatest;

    private BodyStatusSnapshot snapshot;

    public PacketBodyStatus() {}

    public PacketBodyStatus(BodyStatusSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public BodyStatusSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        snapshot = BodyStatusSnapshot.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        snapshot.write(buf);
    }

    /** Only stores the snapshot, so it is safe to register on both physical sides. */
    public static final class Handler implements IMessageHandler<PacketBodyStatus, IMessage> {

        @Override
        public IMessage onMessage(PacketBodyStatus message, MessageContext ctx) {
            if (message != null && message.snapshot != null) clientLatest = message.snapshot;
            return null;
        }
    }
}
