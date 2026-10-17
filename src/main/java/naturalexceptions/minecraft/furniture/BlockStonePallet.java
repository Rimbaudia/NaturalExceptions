package naturalexceptions.minecraft.furniture;

import java.util.ArrayList;

import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A quarry's cut-stone pallet: a low timber pallet with dressed Skystone stacked on it, two layers of four. Its
 * metadata is how many blocks it holds (0 to {@link #FULL}). Right-click takes one Skystone Bricks block; with Skystone
 * Bricks in hand, stacks one back on. Broken, it drops itself and what it held.
 */
public final class BlockStonePallet extends BlockParts {

    public static final int FULL = 8;

    private static final double P = 1.0 / 16.0;
    /** The pallet: two runners and the deck across them. */
    private static final double[][] PALLET = { { 1, 0, 1, 3, 1, 15 }, { 13, 0, 1, 15, 1, 15 }, { 0, 1, 0, 16, 2, 16 } };
    /** The stone, in the order it is stacked: the bottom layer's four, then the top's. */
    private static final double[][] STONES = { { 1, 2, 1, 7.5, 8, 7.5 }, { 8.5, 2, 1, 15, 8, 7.5 },
        { 1, 2, 8.5, 7.5, 8, 15 }, { 8.5, 2, 8.5, 15, 8, 15 }, { 1, 8, 1, 7.5, 14, 7.5 }, { 8.5, 8, 1, 15, 14, 7.5 },
        { 1, 8, 8.5, 7.5, 14, 15 }, { 8.5, 8, 8.5, 15, 14, 15 } };

    public BlockStonePallet() {
        super(Material.wood);
        setBlockName("stonePallet");
        setBlockTextureName("planks_spruce");
        setHardness(1.5F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    /** How many blocks a pallet holds, from its metadata. */
    public static int count(int metadata) {
        return Math.min(FULL, metadata & 15);
    }

    @Override
    public double[][] parts(int metadata) {
        int held = count(metadata);
        double[][] out = new double[PALLET.length + STONES.length][];
        for (int i = 0; i < out.length; i++) {
            double[] b = i < PALLET.length ? PALLET[i] : STONES[i - PALLET.length];
            // A stone not there yet: left out (an empty part), so every part keeps its place and texture.
            double top = i >= PALLET.length && i - PALLET.length >= held ? b[1] : b[4];
            out[i] = new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, top * P, b[5] * P };
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index < PALLET.length) return Blocks.planks.getIcon(1, 1);
        return NEBlocks.skystoneBricks != null ? NEBlocks.skystoneBricks.getIcon(2, 0)
            : Blocks.stonebrick.getIcon(2, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        Item bricks = NEBlocks.skystoneBricks == null ? null : Item.getItemFromBlock(NEBlocks.skystoneBricks);
        if (bricks == null) return false;
        if (world.isRemote) return true;
        int meta = world.getBlockMetadata(x, y, z), held = count(meta);
        ItemStack hand = player.getHeldItem();
        if (hand != null && hand.getItem() == bricks && hand.getItemDamage() == 0) {
            if (held >= FULL) {
                player.addChatMessage(new ChatComponentText("The pallet is stacked full."));
                return true;
            }
            if (!player.capabilities.isCreativeMode) hand.stackSize--;
            world.setBlockMetadataWithNotify(x, y, z, held + 1, 3);
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "dig.stone", 0.6F, 0.9F);
            return true;
        }
        if (held == 0) {
            player.addChatMessage(new ChatComponentText("The pallet is empty."));
            return true;
        }
        world.setBlockMetadataWithNotify(x, y, z, held - 1, 3);
        ItemStack taken = new ItemStack(bricks);
        if (!player.inventory.addItemStackToInventory(taken))
            world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, taken));
        else player.inventoryContainer.detectAndSendChanges();
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "dig.stone", 0.6F, 1.1F);
        return true;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(this));
        int held = count(metadata);
        if (held > 0 && NEBlocks.skystoneBricks != null) drops.add(new ItemStack(NEBlocks.skystoneBricks, held));
        return drops;
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }
}
