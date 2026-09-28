package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.minecraft.metallurgy.BlockSmelterTuyere;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

/** Reusable manual air supply. Pumping never creates fuel or thermal energy. */
public final class ItemBellows extends Item {

    public static final int STROKE_TICKS = 100;

    public ItemBellows() {
        setMaxStackSize(1);
        setUnlocalizedName("bellows");
        setTextureName("naturalexceptions:bellows");
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!(world.getBlock(x, y, z) instanceof BlockSmelterTuyere)) return false;
        if (world.isRemote) return true;
        BlockSmelterTuyere nozzle = (BlockSmelterTuyere) world.getBlock(x, y, z);
        TileEntityStarterSmelter smelter = nozzle.findAdjacentSmelter(world, x, y, z);
        String feedback;
        if (smelter == null) feedback = "Place the tuyere against a vessel's horizontal face, pointing inward.";
        else if (side != (world.getBlockMetadata(x, y, z) ^ 1))
            feedback = "Pump the outer end of the tuyere, opposite the vessel.";
        else if (!smelter.vesselState()
            .fuelState()
            .isBurning()) feedback = "The vessel needs burning fuel before bellows can strengthen its fire.";
        else if (smelter.getForcedDraftTicks() > STROKE_TICKS - 20) return true;
        else {
            smelter.boostForcedDraft(STROKE_TICKS);
            world.markBlockForUpdate(smelter.xCoord, smelter.yCoord, smelter.zCoord);
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.fizz", 0.5F, 0.7F);
            feedback = "Bellows pumped: five seconds of draft. Burns fuel 60% faster; pump again to maintain.";
        }
        player.addChatMessage(new ChatComponentText(feedback));
        return true;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("Use on the outer end of a tuyere pointing into a vessel.");
        lines.add("Each stroke refreshes 5 seconds of draft; 1 second between strokes.");
        lines.add("Requires burning fuel and consumes it 60% faster. Reusable.");
    }
}
