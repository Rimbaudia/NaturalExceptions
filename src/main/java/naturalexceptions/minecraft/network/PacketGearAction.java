package naturalexceptions.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.gear.GearMovement;

/**
 * Client-to-server: the player used a piece of movement gear this tick. The client moves itself (as vanilla
 * does); the server checks the gear is really worn, forgives the fall, and plays the effect for everyone.
 */
public final class PacketGearAction implements IMessage {

    public enum Action {
        DOUBLE_JUMP,
        GLIDE,
        CLIMB
    }

    private Action action = Action.GLIDE;

    public PacketGearAction() {}

    public PacketGearAction(Action action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int index = buf.readUnsignedByte();
        action = Action.values()[Math.min(Action.values().length - 1, index)];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action.ordinal());
    }

    public static final class Handler implements IMessageHandler<PacketGearAction, IMessage> {

        @Override
        public IMessage onMessage(PacketGearAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null && message != null) GearMovement.onAction(player, message.action);
            return null;
        }
    }
}
