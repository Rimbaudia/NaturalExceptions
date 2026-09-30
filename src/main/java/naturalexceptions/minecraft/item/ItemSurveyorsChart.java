package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.minecraft.worldgen.DungeonBeacons;

/**
 * A surveyor's chart of a sky dungeon, left in its entrance hall. Opened, it shows the level you are on: the rooms
 * you have walked, the ways between them and down, and the old surveyor's mark on where the key was kept. It fills
 * itself in as you go (whether or not you carry it).
 */
public final class ItemSurveyorsChart extends Item {

    public ItemSurveyorsChart() {
        setMaxStackSize(1);
        setUnlocalizedName("surveyorsChart");
        setTextureName("naturalexceptions:surveyors_chart");
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || !(player instanceof EntityPlayerMP)) return stack;
        if (!DungeonBeacons.showChart((EntityPlayerMP) player))
            player.addChatComponentMessage(new ChatComponentText("§7No sky dungeon near enough to chart."));
        else world.playSoundAtEntity(player, "step.cloth", 0.6F, 1.4F);
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Use it in a sky dungeon to see the rooms");
        lines.add("§7you have walked, the ways down, and");
        lines.add("§7where the old surveyor marked the key.");
    }
}
