package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.client.renderer.texture.IIconRegister;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.fluid.FluidSourceBoundary;
import naturalexceptions.core.fluid.FluidInteractionRules;
import naturalexceptions.core.fluid.FluidVessel;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.minecraft.persistence.FluidParcelNbtCodec;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.biology.BiologicalProperties;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.ExposureRoute;

/** Player-held finite 1 L vessel for scooping, transferring, and spilling physical liquid parcels. */
public final class ItemFluidVessel extends Item {
    public static final long CAPACITY_MICROLITRES = 1_000_000L;
    private static final String KEY_PARCEL = "FluidParcel";
    @SideOnly(Side.CLIENT) private IIcon emptyIcon;
    @SideOnly(Side.CLIENT) private IIcon filledIcon;
    public ItemFluidVessel() {
        setMaxStackSize(1); setUnlocalizedName("fluidVessel"); setTextureName("naturalexceptions:fluid_vessel_empty");
    }

    @Override @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        emptyIcon = register.registerIcon("naturalexceptions:fluid_vessel_empty");
        filledIcon = register.registerIcon("naturalexceptions:fluid_vessel_filled");
        itemIcon = emptyIcon;
    }

    @Override @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) { return isFilled(stack) ? filledIcon : emptyIcon; }

    /** Affordable early-game 1 L glass vessel recipe. */
    public static net.minecraft.item.crafting.IRecipe survivalRecipe(Item item) {
        return new net.minecraft.item.crafting.ShapedRecipes(3, 2, new ItemStack[] {
            new ItemStack(net.minecraft.init.Blocks.glass), null, new ItemStack(net.minecraft.init.Blocks.glass),
            new ItemStack(net.minecraft.init.Blocks.glass), new ItemStack(net.minecraft.init.Blocks.glass),
            new ItemStack(net.minecraft.init.Blocks.glass) }, new ItemStack(item));
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean isSelected) {
        if (world == null || world.isRemote || !isSelected || !(entity instanceof net.minecraft.entity.EntityLivingBase)
            || Math.floorMod(world.getTotalWorldTime(), 20L) != 0L) return;
        FluidParcel parcel = contents(stack);
        if (parcel != null && parcel.thermalState().temperatureKelvin() >= 373.15) {
            naturalexceptions.minecraft.biology.BiologicalEvents.applyThermalContact(
                (net.minecraft.entity.EntityLivingBase) entity,
                ResourceId.of("naturalexceptions", "right_arm"), parcel.thermalState().temperatureKelvin(), 8.0);
        }
    }

    /** A dropped vessel containing canonical matter must not silently destroy it at vanilla's item timeout. */
    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        if (entityItem != null && isFilled(entityItem.getEntityItem())) {
            entityItem.lifespan = Integer.MAX_VALUE;
            entityItem.age = 0;
        }
        return false;
    }

    public static boolean isFilled(ItemStack stack) {
        return stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(KEY_PARCEL, 10);
    }
    public static FluidParcel contents(ItemStack stack) {
        if (!isFilled(stack)) return null;
        try { return FluidParcelNbtCodec.read(stack.getTagCompound().getCompoundTag(KEY_PARCEL)); }
        catch (RuntimeException invalid) { return null; }
    }
    public static void setContents(ItemStack stack, FluidParcel parcel) {
        if (stack == null) throw new IllegalArgumentException("Vessel item required");
        if (stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
        if (parcel == null) stack.getTagCompound().removeTag(KEY_PARCEL);
        else {
            if (parcel.volumeMicrolitres() > CAPACITY_MICROLITRES)
                throw new IllegalArgumentException("Liquid exceeds the one litre vessel capacity");
            stack.getTagCompound().setTag(KEY_PARCEL, FluidParcelNbtCodec.write(parcel));
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
        int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote || player == null) return true;
        net.minecraft.block.Block hit = world.getBlock(x, y, z);
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        if (saved == null) return false;
        FluidCellKey hitKey = new FluidCellKey(world.provider.dimensionId, x, y, z);
        if (!isFilled(stack)) {
            if (hit instanceof naturalexceptions.minecraft.fluid.BlockFluidCell) {
                FluidParcel parcel = saved.fluids().parcelAt(hitKey);
                if (parcel == null || parcel.phase() != MaterialPhase.LIQUID) return false;
                parcel = saved.fluids().remove(hitKey);
                setContents(stack, parcel); saved.markDirty(); world.setBlockToAir(x, y, z);
                world.markBlockForUpdate(x, y, z); return true;
            }
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityStarterSmelter) {
                TileEntityStarterSmelter smelter = (TileEntityStarterSmelter) tile;
                AggregateLayer extracted = smelter.vesselState().pourLiquidMetal(MaterialQuantity.ofGrams(100.0));
                if (extracted == null || extracted.physicalPhase() != MaterialPhase.LIQUID) return false;
                FluidParcel parcel = FluidParcel.fromAggregateLayer(extracted);
                if (parcel.volumeMicrolitres() > CAPACITY_MICROLITRES) {
                    smelter.vesselState().remelt(extracted);
                    return false;
                }
                setContents(stack, parcel); smelter.markDirty(); saved.markDirty();
                world.markBlockForUpdate(x, y, z);
                return true;
            }
            if (hit == net.minecraft.init.Blocks.water && world.getBlockMetadata(x, y, z) == 0) {
                FluidParcel water = waterParcel();
                setContents(stack, water);
                saved.fluids().registerSourceBoundary(hitKey,
                    FluidSourceBoundary.infinite("minecraft-water:" + hitKey.stableString(), water.materialRef()));
                saved.markDirty();
                return true;
            }
            if (hit == net.minecraft.init.Blocks.lava && world.getBlockMetadata(x, y, z) == 0) {
                FluidParcel lava = lavaParcel();
                setContents(stack, lava);
                saved.fluids().registerSourceBoundary(hitKey,
                    FluidSourceBoundary.infinite("minecraft-lava:" + hitKey.stableString(), lava.materialRef()));
                saved.markDirty();
                return true;
            }
            return false;
        }

        if (hit instanceof naturalexceptions.minecraft.fluid.BlockFluidCell) {
            FluidParcel existing = saved.fluids().parcelAt(hitKey);
            FluidParcel incoming = contents(stack);
            if (existing == null || incoming == null) return false;
            // Spatial cells currently own one parcel. Do not accept an immiscible layer that
            // the cell codec cannot represent, and preview from clones so a rejected pour
            // cannot consume the parcel that is still owned by world state.
            if (!existing.materialRef().equals(incoming.materialRef())
                || !existing.composition().equals(incoming.composition())) {
                player.addChatMessage(new ChatComponentText("This spatial cell can only receive the same liquid material."));
                return false;
            }
            FluidVessel receiver = new FluidVessel(CAPACITY_MICROLITRES, FluidInteractionRules.sameMaterialOnly());
            FluidParcel existingPreview = FluidParcelNbtCodec.read(FluidParcelNbtCodec.write(existing));
            FluidParcel incomingPreview = FluidParcelNbtCodec.read(FluidParcelNbtCodec.write(incoming));
            if (receiver.fill(existingPreview).compareTo(existing.mass()) != 0) return false;
            if (receiver.fill(incomingPreview).compareTo(incoming.mass()) != 0 || receiver.contents().size() != 1) {
                player.addChatMessage(new ChatComponentText("The receiving vessel is full or contains an incompatible liquid."));
                return false;
            }
            saved.fluids().remove(hitKey);
            try {
                saved.fluids().put(hitKey, receiver.contents().get(0));
            } catch (RuntimeException refused) {
                saved.fluids().put(hitKey, existing);
                return false;
            }
            setContents(stack, null); saved.markDirty(); world.markBlockForUpdate(x, y, z);
            return true;
        }

        int tx = x + (side == 4 ? -1 : side == 5 ? 1 : 0);
        int ty = y + (side == 0 ? -1 : side == 1 ? 1 : 0);
        int tz = z + (side == 2 ? -1 : side == 3 ? 1 : 0);
        if (!world.isAirBlock(tx, ty, tz)) return false;
        FluidCellKey target = new FluidCellKey(world.provider.dimensionId, tx, ty, tz);
        FluidParcel parcel = contents(stack);
        if (parcel == null) return false;
        int visualMeta = naturalexceptions.minecraft.fluid.BlockFluidCell.metadataFor(parcel);
        if (!world.setBlock(tx, ty, tz, NEBlocks.fluidCell, visualMeta, 3)) return false;
        try { saved.fluids().put(target, parcel); }
        catch (RuntimeException error) { world.setBlockToAir(tx, ty, tz); return false; }
        setContents(stack, null); saved.markDirty(); world.markBlockForUpdate(tx, ty, tz);
        player.addChatMessage(new ChatComponentText("Spilled " + parcel.mass() + " of "
            + naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(player, parcel.materialRef(),
                parcel.stateKey())));
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || player == null) return stack;
        FluidParcel parcel = contents(stack);
        if (parcel == null) return stack;
        MaterialQuantity amount = parcel.mass().compareTo(MaterialQuantity.ofGrams(250.0)) <= 0
            ? parcel.mass() : MaterialQuantity.ofGrams(250.0);
        FluidParcel ingested = amount.equals(parcel.mass()) ? parcel : parcel.extract(amount);
        ResourceId material = parcel.composition().asMap().keySet().iterator().next();
        BiologicalState body = BiologicalProperties.get(player).state();
        body.expose(material, parcel.thermalState().properties(), ExposureRoute.INGESTION,
            ingested.mass().toGrams(), world.getTotalWorldTime());
        if (parcel.composition().contains(ResourceId.of("minecraft", "water")))
            body.replenishHydrationMillilitres(Math.min(250.0, ingested.volumeMicrolitres() / 1000.0));
        if (amount.equals(parcel.mass())) setContents(stack, null);
        else setContents(stack, parcel);
        player.addChatMessage(new ChatComponentText("Ingested " + ingested.mass() + " of "
            + naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(player,
                ingested.materialRef(), ingested.stateKey())
            + "; biological suitability is evaluated from material traits."));
        return stack;
    }

    private static FluidParcel waterParcel() {
        CanonicalComposition composition = CanonicalComposition.of(ResourceId.of("minecraft", "water"));
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        MaterialProperties properties = MaterialProperties.builder().density(1.0).hardness(0.0)
            .specificHeatSolid(4180.0).specificHeatLiquid(4180.0).meltingPointKelvin(273.15)
            .latentHeatOfFusion(334000.0).build();
        MaterialQuantity mass = MaterialQuantity.ofKilograms(1.0);
        return new FluidParcel(MaterialRef.authored("minecraft", "water"), key, composition, mass,
            ActiveThermalState.ambient(mass.toKilograms(), properties));
    }

    private static FluidParcel lavaParcel() {
        CanonicalComposition composition = CanonicalComposition.of(ResourceId.of("minecraft", "lava"));
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        MaterialProperties properties = MaterialProperties.builder().density(2.8).hardness(0.0)
            .specificHeatSolid(840.0).specificHeatLiquid(1100.0).meltingPointKelvin(1100.0)
            .latentHeatOfFusion(400000.0).electricalResistivity(1.0e8).build();
        MaterialQuantity mass = MaterialQuantity.ofKilograms(1.0);
        return new FluidParcel(MaterialRef.authored("minecraft", "lava"), key, composition, mass,
            ActiveThermalState.atTemperature(1300.0, 1.0, mass.toKilograms(), properties));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        FluidParcel parcel = contents(stack);
        if (parcel == null) { tooltip.add("Empty fluid vessel (1 L capacity)"); return; }
        tooltip.add("Material: " + naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(player,
            parcel.materialRef(), parcel.stateKey()));
        tooltip.add("Quantity: " + parcel.mass() + " | " + parcel.volumeMicrolitres() + " µL");
        tooltip.add("State: " + (parcel.phase() == MaterialPhase.LIQUID ? "liquid" : "solid"));
        double temperature = parcel.thermalState().temperatureKelvin();
        if (temperature >= 1273.15) tooltip.add("Visibly glowing hot; unsafe to handle directly");
        else if (temperature >= 373.15) tooltip.add("Dangerously hot to touch");
        else if (temperature >= 310.15) tooltip.add("Warm to the touch");
        else if (temperature <= 273.15) tooltip.add("Cold to the touch");
        if (advanced) tooltip.add(String.format("Observed temperature: %.1f K", temperature));
    }
}
