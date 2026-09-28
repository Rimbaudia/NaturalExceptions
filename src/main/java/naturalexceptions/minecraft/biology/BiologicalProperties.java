package naturalexceptions.minecraft.biology;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.IExtendedEntityProperties;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.minecraft.persistence.BiologicalStateNbtCodec;

/** Host adapter. Player and organisms use the same core state; ordinary entities keep only compact live state. */
public final class BiologicalProperties implements IExtendedEntityProperties {
    public static final String KEY = "naturalexceptions.biology.v1";
    private static final String PERSISTENT_MARKER = "naturalexceptions.persistent_biology";
    private BiologicalState state;
    private boolean persistent;

    public static BiologicalProperties get(EntityLivingBase entity) {
        if (entity == null) return null;
        IExtendedEntityProperties current = entity.getExtendedProperties(KEY);
        if (current instanceof BiologicalProperties) {
            BiologicalProperties existing = (BiologicalProperties) current;
            if (existing.state == null) existing.state = new BiologicalState(BodyPlanRegistry.standard().require(
                naturalexceptions.core.biology.BuiltinBodyPlans.HUMAN_ID),
                Math.max(0, entity.worldObj != null ? entity.worldObj.getTotalWorldTime() : entity.ticksExisted));
            return existing;
        }
        BiologicalProperties created = new BiologicalProperties();
        entity.registerExtendedProperties(KEY, created);
        created.state = new BiologicalState(BodyPlanRegistry.standard().require(
            naturalexceptions.core.biology.BuiltinBodyPlans.HUMAN_ID),
            Math.max(0, entity.worldObj != null ? entity.worldObj.getTotalWorldTime() : entity.ticksExisted));
        return created;
    }
    public BiologicalState state() { return state; }
    public void state(BiologicalState value) { state = value; }
    public static void markPersistent(EntityLivingBase entity) {
        if (entity != null) {
            entity.getEntityData().setBoolean(PERSISTENT_MARKER, true);
            IExtendedEntityProperties current = entity.getExtendedProperties(KEY);
            if (current instanceof BiologicalProperties) ((BiologicalProperties) current).persistent = true;
        }
    }
    @Override public void saveNBTData(NBTTagCompound compound) {
        if (compound != null && persistent && state != null) compound.setTag("State", BiologicalStateNbtCodec.write(state));
    }
    @Override public void loadNBTData(NBTTagCompound compound) {
        if (compound == null || !compound.hasKey("State", 10)) return;
        BiologicalState loaded = BiologicalStateNbtCodec.read(compound.getCompoundTag("State"), BodyPlanRegistry.standard(), 0L);
        if (loaded != null) state = loaded;
    }
    @Override public void init(Entity entity, net.minecraft.world.World world) {
        persistent = entity instanceof EntityPlayer || (entity != null && entity.getEntityData().getBoolean(PERSISTENT_MARKER));
    }
    public void copyFrom(BiologicalProperties source, boolean preserve) {
        if (!preserve || source == null || source.state == null) {
            state = null;
            return;
        }
        persistent = true;
        NBTTagCompound copy = BiologicalStateNbtCodec.write(source.state);
        state = BiologicalStateNbtCodec.read(copy, BodyPlanRegistry.standard(), 0L);
    }
}
