package naturalexceptions.minecraft.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Foundry Colossus's own crucible, with the draft-heart that kept its furnace roaring. Set into a Starter
 * Smelter's refractory shell in place of a brick, it breathes on the fire: the smelter burns as if half-pumped without
 * the bellows, and hotter yet with them.
 */
public final class BlockColossusCrucible extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockColossusCrucible() {
        super(Material.iron);
        setBlockName("colossusCrucible");
        setBlockTextureName("naturalexceptions:colossus_crucible");
        setHardness(5.0F);
        setResistance(30.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        top = register.registerIcon("naturalexceptions:colossus_crucible_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side <= 1 ? top : blockIcon;
    }

    /** Its item, with what it does. */
    public static final class Item extends ItemBlock {

        public Item(Block block) {
            super(block);
        }

        @Override
        public net.minecraft.item.EnumRarity getRarity(ItemStack stack) {
            return net.minecraft.item.EnumRarity.epic;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
            lines.add("§7The Foundry Colossus's crucible; its draft-heart still breathes.");
            lines.add("§8Set it into a Starter Smelter's shell in place of a brick:");
            lines.add("§8the fire burns as if half-pumped, hotter with bellows.");
        }
    }
}
