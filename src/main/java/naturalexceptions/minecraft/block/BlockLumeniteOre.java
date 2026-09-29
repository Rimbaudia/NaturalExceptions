package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;

import naturalexceptions.minecraft.registry.NEItems;

/** Stone shot through with Lumenite; lit faintly from within. Breaks into Lumenite Shards. */
public final class BlockLumeniteOre extends Block {

    public BlockLumeniteOre() {
        super(Material.rock);
        setBlockName("lumeniteOre");
        setBlockTextureName("naturalexceptions:lumenite_ore");
        setHardness(3.0F);
        setResistance(5.0F);
        setLightLevel(0.4F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return NEItems.lumeniteShard != null ? NEItems.lumeniteShard : Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random random) {
        return 2 + random.nextInt(3);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return quantityDropped(random) + (fortune > 0 ? random.nextInt(fortune + 1) : 0);
    }

    @Override
    public int getExpDrop(IBlockAccess world, int metadata, int fortune) {
        return 2
            + (world instanceof net.minecraft.world.World ? ((net.minecraft.world.World) world).rand.nextInt(4) : 0);
    }
}
