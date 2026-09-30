package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import naturalexceptions.minecraft.registry.NEItems;

/** Island stone split by seams of Stormglass, near a sky island's underside. Mined for Stormglass Shards. */
public final class BlockStormglassVein extends Block {

    public BlockStormglassVein() {
        super(Material.rock);
        setBlockName("stormglassVein");
        setBlockTextureName("naturalexceptions:stormglass_vein");
        setHardness(3.0F);
        setResistance(5.0F);
        setLightLevel(0.3F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return NEItems.stormglassShard != null ? NEItems.stormglassShard : Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1 + random.nextInt(2);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return quantityDropped(random) + (fortune > 0 ? random.nextInt(fortune + 1) : 0);
    }
}
