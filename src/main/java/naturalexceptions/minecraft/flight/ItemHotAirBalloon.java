package naturalexceptions.minecraft.flight;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * A hot-air balloon, folded: envelope, ropes, basket and brazier. Used on the ground it is laid out and stood up there,
 * where the envelope has room over it.
 */
public final class ItemHotAirBalloon extends Item {

    public ItemHotAirBalloon() {
        setMaxStackSize(1);
        setUnlocalizedName("hotAirBalloon");
        setTextureName("naturalexceptions:hot_air_balloon");
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hx,
        float hy, float hz) {
        if (side != 1) return false;
        if (world.isRemote) return true;
        double bx = x + 0.5, by = y + 1.0, bz = z + 0.5;
        AxisAlignedBB room = AxisAlignedBB.getBoundingBox(
            bx - EntityBalloon.ENVELOPE_RADIUS,
            by,
            bz - EntityBalloon.ENVELOPE_RADIUS,
            bx + EntityBalloon.ENVELOPE_RADIUS,
            by + EntityBalloon.ENVELOPE_TOP,
            bz + EntityBalloon.ENVELOPE_RADIUS);
        if (!world.func_147461_a(room)
            .isEmpty()) {
            player.addChatComponentMessage(
                new ChatComponentText("§7There is no room here to stand the envelope up: it wants open sky over it."));
            return true;
        }
        world.spawnEntityInWorld(new EntityBalloon(world, bx, by, bz));
        world.playSoundEffect(bx, by, bz, "step.cloth", 1.0F, 0.8F);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }
}
