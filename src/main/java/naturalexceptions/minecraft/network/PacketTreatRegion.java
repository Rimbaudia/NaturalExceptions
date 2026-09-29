package naturalexceptions.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.biology.BodyTreatment;

/** Client-to-server: the player dropped the bandage on their cursor onto a body region in the Body panel. */
public final class PacketTreatRegion implements IMessage {

    private String regionId;

    public PacketTreatRegion() {}

    public PacketTreatRegion(String regionId) {
        this.regionId = regionId;
    }

    public String regionId() {
        return regionId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        regionId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, regionId == null ? "" : regionId);
    }

    public static final class Handler implements IMessageHandler<PacketTreatRegion, IMessage> {

        @Override
        public IMessage onMessage(PacketTreatRegion message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null && message != null && message.regionId != null)
                BodyTreatment.applyFromCursor(player, message.regionId);
            return null;
        }
    }
}
