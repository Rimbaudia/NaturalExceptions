package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public final class BlockCopperBlock extends Block {

    public BlockCopperBlock() {
        super(Material.iron);
        setBlockName("copperBlock");
        setBlockTextureName("naturalexceptions:copper_block");
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(Block.soundTypeMetal);
    }
}
