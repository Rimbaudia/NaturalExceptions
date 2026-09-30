package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.worldgen.DungeonBeacons;

/**
 * A surveyor's desk with a chart spread on it and an ink pot. In or near a sky dungeon, using it shows that
 * dungeon's chart as a Surveyor's Chart would; holding paper (with a Stormglass Shard to hand for the ink's glow), it
 * draws you a chart of your own.
 */
public final class BlockChartTable extends BlockParts {

    /** The board, four legs, the chart laid on it, the ink pot. */
    private static final double[][] PARTS = { { 0.0, 0.8125, 0.0, 1.0, 0.9375, 1.0 },
        { 0.0625, 0.0, 0.0625, 0.1875, 0.8125, 0.1875 }, { 0.8125, 0.0, 0.0625, 0.9375, 0.8125, 0.1875 },
        { 0.0625, 0.0, 0.8125, 0.1875, 0.8125, 0.9375 }, { 0.8125, 0.0, 0.8125, 0.9375, 0.8125, 0.9375 },
        { 0.12, 0.9375, 0.15, 0.78, 0.95, 0.85 }, { 0.82, 0.9375, 0.7, 0.92, 1.0, 0.8 } };

    public BlockChartTable() {
        super(Material.wood);
        setBlockName("chartTable");
        setBlockTextureName("planks_spruce");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote || !(player instanceof EntityPlayerMP)) return true;
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() == Items.paper && NEItems.surveyorsChart != null) {
            boolean free = player.capabilities.isCreativeMode;
            if (!free && !player.inventory.hasItem(NEItems.stormglassShard)) {
                player.addChatComponentMessage(
                    new ChatComponentText("§7A chart needs a Stormglass Shard for its ink as well as the paper."));
                return true;
            }
            if (!free) {
                player.inventory.consumeInventoryItem(NEItems.stormglassShard);
                if (--held.stackSize <= 0)
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
            ItemStack chart = new ItemStack(NEItems.surveyorsChart);
            if (!player.inventory.addItemStackToInventory(chart)) player.dropPlayerItemWithRandomChoice(chart, false);
            world.playSoundEffect(x + 0.5, y + 1.0, z + 0.5, "step.cloth", 0.8F, 1.2F);
            return true;
        }
        if (!DungeonBeacons.showChart((EntityPlayerMP) player))
            player.addChatComponentMessage(new ChatComponentText("§7The charts here are of no dungeon near."));
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index == 5) return Blocks.wool.getIcon(1, 4); // the chart: yellowed paper
        if (index == 6) return Blocks.wool.getIcon(1, 15); // the ink
        return index == 0 ? getIcon(1, 0) : Blocks.log.getIcon(2, 1);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }
}
