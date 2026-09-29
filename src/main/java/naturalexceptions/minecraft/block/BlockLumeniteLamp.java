package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Lumenite set in a copper frame: full light with no flame, so it burns nothing and works under water. */
public final class BlockLumeniteLamp extends Block {

    public BlockLumeniteLamp() {
        super(Material.glass);
        setBlockName("lumeniteLamp");
        setBlockTextureName("naturalexceptions:lumenite_lamp");
        setHardness(0.8F);
        setLightLevel(1.0F);
        setStepSound(soundTypeGlass);
    }
}
