package naturalexceptions.minecraft.network;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import naturalexceptions.minecraft.gear.GearSlot;

/** Server-to-owner: everything this player wears, so their client can apply movement gear (jumps, gliding). */
public final class PacketGearSync implements IMessage {

    /** Latest gear received by this client; applied to the local player on the next client tick. */
    public static volatile ItemStack[] clientPending;

    private ItemStack[] stacks = new ItemStack[GearSlot.COUNT];

    public PacketGearSync() {}

    public PacketGearSync(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = Math.min(GearSlot.COUNT, buf.readUnsignedByte());
        stacks = new ItemStack[GearSlot.COUNT];
        for (int i = 0; i < count; i++) stacks[i] = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(stacks.length);
        for (ItemStack stack : stacks) ByteBufUtils.writeItemStack(buf, stack);
    }

    /** Only stores data, so it is safe to register on both physical sides. */
    public static final class Handler implements IMessageHandler<PacketGearSync, IMessage> {

        @Override
        public IMessage onMessage(PacketGearSync message, MessageContext ctx) {
            if (message != null) clientPending = message.stacks;
            return null;
        }
    }
}
