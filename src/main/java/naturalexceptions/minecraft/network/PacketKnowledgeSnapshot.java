package naturalexceptions.minecraft.network;

import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeNbtCodec;
import naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties;

/** Server-to-owning-client snapshot; never broadcast. */
public final class PacketKnowledgeSnapshot implements IMessage {
    private NBTTagCompound payload = new NBTTagCompound();

    public PacketKnowledgeSnapshot() { }
    public PacketKnowledgeSnapshot(PlayerKnowledgeProperties properties) {
        if (properties != null) payload = PlayerKnowledgeNbtCodec.write(properties.knowledge());
    }

    public NBTTagCompound payload() { return (NBTTagCompound) payload.copy(); }

    @Override public void fromBytes(ByteBuf buf) {
        NBTTagCompound decoded = ByteBufUtils.readTag(buf);
        payload = decoded == null ? new NBTTagCompound() : decoded;
    }
    @Override public void toBytes(ByteBuf buf) { ByteBufUtils.writeTag(buf, payload); }

    /** Server-side placeholder: registers the shared discriminator without referencing client classes. */
    public static final class ServerRegistrationHandler implements IMessageHandler<PacketKnowledgeSnapshot, IMessage> {
        @Override public IMessage onMessage(PacketKnowledgeSnapshot message, MessageContext context) { return null; }
    }
}
