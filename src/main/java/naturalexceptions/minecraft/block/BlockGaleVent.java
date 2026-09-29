package naturalexceptions.minecraft.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A grated vent that breathes the island's wind. It gusts on a steady beat (two seconds on, two off) and carries
 * anything in front of it up to six blocks: a lift up a shaft, a shove across a bridge, a launch pad when built by
 * a player. Meta is {@link #metaFor} the side it blows towards: the vanilla side number with its lowest bit flipped,
 * so that meta 0 (and the item) blows upwards.
 */
public final class BlockGaleVent extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon face;

    public BlockGaleVent() {
        super(Material.rock);
        setBlockName("galeVent");
        setBlockTextureName("naturalexceptions:gale_vent_side");
        setHardness(2.5F);
        setResistance(30.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityGaleVent();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        world.setBlockMetadataWithNotify(
            x,
            y,
            z,
            metaFor(BlockPistonBase.determineOrientation(world, x, y, z, placer)),
            2);
    }

    /** Meta for a vent blowing towards a vanilla side (0 down, 1 up, 2..5 north, south, west, east). */
    public static int metaFor(int side) {
        return side ^ 1;
    }

    public static int sideOf(int meta) {
        return (meta & 7) ^ 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("naturalexceptions:gale_vent_side");
        face = register.registerIcon("naturalexceptions:gale_vent_face");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side == sideOf(meta) ? face : blockIcon;
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }
}
