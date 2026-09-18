package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public final class BlockRefractoryBrick extends Block {

    public BlockRefractoryBrick() {
        super(Material.rock);
        setBlockName("refractoryBrick");
        setBlockTextureName("naturalexceptions:refractory_brick_block");
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(Block.soundTypeStone);
    }
}
