package naturalexceptions.minecraft.furniture;

import net.minecraft.tileentity.TileEntity;

/** Only there so the Orrery's turning arms can be drawn ({@code RenderOrrery}); it holds nothing. */
public final class TileEntityOrrery extends TileEntity {

    @Override
    public boolean canUpdate() {
        return false;
    }
}
