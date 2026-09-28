package naturalexceptions.minecraft.fabrication;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.ConditioningHistory;
import naturalexceptions.core.material.Microstructure;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemBlacksmithHammer;
import naturalexceptions.minecraft.item.ItemBlacksmithTongs;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;

/**
 * TileEntity controlling the physical forging anvil and hosted workpiece.
 *
 * <p>
 * Simulates real hot-working and cold-working deformation:
 * <ul>
 * <li>Conserves exact component mass and material composition across operations.</li>
 * <li>Hot-working above recrystallization threshold (0.45 T_melt) maintains dynamic, unhardened grains.</li>
 * <li>Cold-working below recrystallization threshold causes strain hardening, WORK_HARDENED microstructure,
 * and COLD_WORKED conditioning history, accumulating micro-cracking until fracture or anneal recovery.</li>
 * </ul>
 */
public class TileEntityForgingAnvil extends TileEntity implements IInventory {

    public static final GeometryId[] FORGEABLE_TARGETS = new GeometryId[] { GeometryId.PICK_HEAD, GeometryId.BLADE,
        GeometryId.PLATE, GeometryId.ROD, GeometryId.HANDLE };

    private ItemStack workpiece = null;
    private GeometryId targetGeometry = GeometryId.PLATE;

    public TileEntityForgingAnvil() {}

    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public GeometryId getTargetGeometry() {
        return targetGeometry != null ? targetGeometry : GeometryId.PLATE;
    }

    public void setTargetGeometry(GeometryId target) {
        if (target != null) {
            this.targetGeometry = target;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    public GeometryId cycleTargetGeometry(EntityPlayer player) {
        int idx = 0;
        for (int i = 0; i < FORGEABLE_TARGETS.length; i++) {
            if (FORGEABLE_TARGETS[i] == getTargetGeometry()) {
                idx = i;
                break;
            }
        }
        targetGeometry = FORGEABLE_TARGETS[(idx + 1) % FORGEABLE_TARGETS.length];
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, "random.click", 0.8F, 1.6F);
        }
        return targetGeometry;
    }

    public void setWorkpiece(ItemStack stack) {
        this.workpiece = stack;
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && worldObj.isRemote) {
            return;
        }

        if (workpiece != null) {
            coolWorkpiece(0.05); // 1 tick
            if (worldObj != null && worldObj.getTotalWorldTime() % 20L == 0L) {
                markDirty();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    public void coolWorkpiece(double deltaSeconds) {
        if (workpiece == null) return;
        ActiveThermalState thermal = ItemGenericComponent.getThermalState(workpiece);
        if (thermal != null && thermal.temperatureKelvin() > 293.15) {
            double temp = thermal.temperatureKelvin();
            double surfaceArea = 0.03; // Workpiece surface area on anvil
            double qRad = 0.80 * 5.670374419e-8
                * surfaceArea
                * (Math.pow(temp, 4.0) - Math.pow(293.15, 4.0))
                * deltaSeconds;
            // Additional conduction to massive cold anvil block
            double qCondAnvil = 35.0 * surfaceArea * (temp - 293.15) * deltaSeconds;
            double qConv = 15.0 * surfaceArea * (temp - 293.15) * deltaSeconds;
            double totalLoss = Math.max(0.0, qRad + qConv + qCondAnvil);
            double ambientEnergy = naturalexceptions.core.thermal.ThermodynamicModel
                .energyForTemperature(293.15, 0.0, thermal.massKg(), thermal.properties());
            if (thermal.energyJoules() - totalLoss < ambientEnergy) {
                thermal.setEnergyJoules(ambientEnergy);
            } else {
                thermal.removeEnergyJoules(totalLoss);
            }

            ItemGenericComponent.checkAnneal(workpiece);
            ItemGenericComponent.saveThermalState(workpiece, thermal);
        }
    }

    public boolean hammerStrike(EntityPlayer player, ItemStack hammerStack) {
        if (workpiece == null) {
            if (player != null && (worldObj == null || !worldObj.isRemote)) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7e[Forging Anvil] Anvil is empty. Place a blank, component, or ingot on it."));
            }
            return false;
        }

        // Convert standard ingot/feedstock to blank component if placed directly
        naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry.ResolvedInput input = naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry
            .resolve(workpiece);
        if (input != null && input.kind()
            == naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry.InputKind.METALLIC_PRODUCT) {
            ComponentInstance blankComp = new ComponentInstance(
                GeometryId.BLANK,
                input.materialRef(),
                CanonicalMaterialKey.of(input.composition()),
                input.inputMass(),
                ManufacturingState.AS_CAST);
            ItemStack converted = ItemGenericComponent.createStack(blankComp);
            ActiveThermalState ingotThermal = ItemGenericComponent.getThermalState(workpiece);
            if (ingotThermal != null) {
                ItemGenericComponent.saveThermalState(converted, ingotThermal);
            }
            this.workpiece = converted;
            markDirty();
        }

        ComponentInstance comp = ItemGenericComponent.getComponent(workpiece);
        if (comp == null) {
            if (player != null && (worldObj == null || !worldObj.isRemote)) {
                player.addChatMessage(
                    new ChatComponentText("\u00a7c[Forging Anvil] Workpiece has no forgeable component structure."));
            }
            return false;
        }

        if (comp.damage()
            .isFractured()) {
            if (player != null && (worldObj == null || !worldObj.isRemote)) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a74[Forging Anvil] Workpiece is fractured and cannot be forged! Remelt it in a smelter."));
            }
            return false;
        }

        MaterialProperties props = new MaterialPropertyResolver().resolve(
            comp.materialRef(),
            comp.stateKey()
                .composition(),
            comp.stateKey()
                .phase())
            .properties();
        if (props == null) {
            props = BuiltinMaterials.COPPER_PROPERTIES;
        }

        double meltPoint = props.meltingPointKelvin();
        double tRx = props.recrystallizationTemperatureKelvin();

        ActiveThermalState thermal = ItemGenericComponent.getThermalState(workpiece);
        double currentTemp = thermal != null ? thermal.temperatureKelvin() : 293.15;

        if (currentTemp >= meltPoint) {
            if (player != null && (worldObj == null || !worldObj.isRemote)) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Forging Anvil] Workpiece is too hot and partially liquid! Let it cool to solid before forging."));
            }
            return false;
        }

        boolean isHotWorking = currentTemp >= tRx;
        boolean hasManualOp = (hammerStack != null && hammerStack.hasTagCompound()
            && hammerStack.getTagCompound()
                .hasKey(ItemBlacksmithHammer.KEY_OPERATION));

        GeometryId currentGeom = comp.geometryId();
        GeometryId targetGeom = currentGeom;
        ItemBlacksmithHammer.HammerOperation op;
        boolean isPlanishing = false;

        if (hasManualOp) {
            op = ItemBlacksmithHammer.getOperation(hammerStack);
            isPlanishing = (op == ItemBlacksmithHammer.HammerOperation.PLANISH);

            switch (op) {
                case FLATTEN:
                    if (currentGeom == GeometryId.BLANK) {
                        targetGeom = GeometryId.PLATE;
                    } else if (currentGeom == GeometryId.PLATE) {
                        targetGeom = GeometryId.PLATE;
                        isPlanishing = true;
                    } else {
                        if (player != null && (worldObj == null || !worldObj.isRemote)) {
                            player.addChatMessage(
                                new ChatComponentText(
                                    "\u00a7e[Forging Anvil] Flattening compresses stock thickness (Blanks -> Plates)."));
                        }
                        return false;
                    }
                    break;

                case DRAW_OUT:
                    if (currentGeom == GeometryId.BLANK) {
                        targetGeom = GeometryId.ROD;
                    } else if (currentGeom == GeometryId.ROD) {
                        targetGeom = GeometryId.HANDLE;
                    } else {
                        if (player != null && (worldObj == null || !worldObj.isRemote)) {
                            player.addChatMessage(
                                new ChatComponentText(
                                    "\u00a7e[Forging Anvil] Drawing Out elongates stock (Blanks -> Rods -> Handles)."));
                        }
                        return false;
                    }
                    break;

                case BEVEL:
                    if (currentGeom == GeometryId.PLATE) {
                        targetGeom = GeometryId.BLADE;
                    } else if (currentGeom == GeometryId.ROD) {
                        targetGeom = GeometryId.PICK_HEAD;
                    } else {
                        if (player != null && (worldObj == null || !worldObj.isRemote)) {
                            player.addChatMessage(
                                new ChatComponentText(
                                    "\u00a7e[Forging Anvil] Beveling shapes edges and points (Plates -> Blades, Rods -> Pick Heads)."));
                        }
                        return false;
                    }
                    break;

                case PLANISH:
                default:
                    targetGeom = currentGeom;
                    isPlanishing = true;
                    break;
            }
        } else {
            // Target-driven forging: derived physical operation towards established target geometry
            GeometryId target = getTargetGeometry();
            if (currentGeom == target) {
                if (comp.manufacturingState() == ManufacturingState.FINISHED) {
                    if (worldObj != null) {
                        worldObj.playSoundEffect(
                            xCoord + 0.5D,
                            yCoord + 0.8D,
                            zCoord + 0.5D,
                            "random.anvil_land",
                            1.0F,
                            1.8F);
                    }
                    return true;
                }
                targetGeom = target;
                op = ItemBlacksmithHammer.HammerOperation.PLANISH;
                isPlanishing = true;
            } else if (target == GeometryId.PICK_HEAD) {
                if (currentGeom == GeometryId.BLANK) {
                    targetGeom = GeometryId.ROD;
                    op = ItemBlacksmithHammer.HammerOperation.DRAW_OUT;
                } else if (currentGeom == GeometryId.ROD) {
                    targetGeom = GeometryId.PICK_HEAD;
                    op = ItemBlacksmithHammer.HammerOperation.BEVEL;
                } else {
                    targetGeom = GeometryId.PICK_HEAD;
                    op = ItemBlacksmithHammer.HammerOperation.BEVEL;
                }
            } else if (target == GeometryId.BLADE) {
                if (currentGeom == GeometryId.BLANK) {
                    targetGeom = GeometryId.PLATE;
                    op = ItemBlacksmithHammer.HammerOperation.FLATTEN;
                } else if (currentGeom == GeometryId.PLATE) {
                    targetGeom = GeometryId.BLADE;
                    op = ItemBlacksmithHammer.HammerOperation.BEVEL;
                } else {
                    targetGeom = GeometryId.PLATE;
                    op = ItemBlacksmithHammer.HammerOperation.FLATTEN;
                }
            } else if (target == GeometryId.PLATE) {
                if (currentGeom == GeometryId.BLANK) {
                    targetGeom = GeometryId.PLATE;
                    op = ItemBlacksmithHammer.HammerOperation.FLATTEN;
                } else {
                    targetGeom = GeometryId.PLATE;
                    op = ItemBlacksmithHammer.HammerOperation.PLANISH;
                    isPlanishing = true;
                }
            } else if (target == GeometryId.ROD) {
                if (currentGeom == GeometryId.BLANK) {
                    targetGeom = GeometryId.ROD;
                    op = ItemBlacksmithHammer.HammerOperation.DRAW_OUT;
                } else {
                    targetGeom = GeometryId.ROD;
                    op = ItemBlacksmithHammer.HammerOperation.PLANISH;
                    isPlanishing = true;
                }
            } else if (target == GeometryId.HANDLE) {
                if (currentGeom == GeometryId.BLANK) {
                    targetGeom = GeometryId.ROD;
                    op = ItemBlacksmithHammer.HammerOperation.DRAW_OUT;
                } else if (currentGeom == GeometryId.ROD) {
                    targetGeom = GeometryId.HANDLE;
                    op = ItemBlacksmithHammer.HammerOperation.DRAW_OUT;
                } else {
                    targetGeom = GeometryId.HANDLE;
                    op = ItemBlacksmithHammer.HammerOperation.PLANISH;
                    isPlanishing = true;
                }
            } else {
                targetGeom = target;
                op = ItemBlacksmithHammer.HammerOperation.PLANISH;
                isPlanishing = true;
            }
        }

        // Advance geometry while strictly conserving mass and material identity
        comp.work(targetGeom, isPlanishing ? ManufacturingState.FINISHED : ManufacturingState.WORKED);

        if (isHotWorking) {
            // Dynamic grain recovery / recrystallization occurs continuously during hot work
            comp.setStateKey(
                comp.stateKey()
                    .withMicrostructure(Microstructure.ANNEALED)
                    .withConditioning(ConditioningHistory.HOT_ROLLED));
            comp.setManufacturingState(isPlanishing ? ManufacturingState.FINISHED : ManufacturingState.WORKED);
        } else {
            // Cold working below recrystallization threshold causes strain hardening
            comp.setStateKey(
                comp.stateKey()
                    .withMicrostructure(Microstructure.WORK_HARDENED)
                    .withConditioning(ConditioningHistory.COLD_WORKED));
            comp.setManufacturingState(ManufacturingState.COLD_WORKED);
            comp.damage()
                .addDeformation(0.08);
            comp.damage()
                .addCracking(0.06);

            if (player != null && (worldObj == null || !worldObj.isRemote)
                && comp.damage()
                    .isFractured()) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a74[Forging Anvil] FRACTURE! Component shattered from cold-working strain without annealing!"));
            }
        }

        // Save updated component back to workpiece
        AssemblyNbtCodec.writeComponentToNbt(comp, workpiece.getTagCompound());

        // Heat loss from contact with hammer and anvil
        if (thermal != null) {
            thermal.removeEnergyJoules(250.0);
            ItemGenericComponent.saveThermalState(workpiece, thermal);
        }

        if (hammerStack != null && player != null && player.capabilities != null) {
            hammerStack.damageItem(1, player);
        }

        if (worldObj != null) {
            if (isPlanishing) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, "random.anvil_land", 1.0F, 1.6F);
                for (int i = 0; i < 12; i++) {
                    worldObj.spawnParticle(
                        "crit",
                        xCoord + 0.5D,
                        yCoord + 0.9D,
                        zCoord + 0.5D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.35D,
                        0.15D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.35D);
                }
            } else if (isHotWorking) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, "random.anvil_use", 0.9F, 1.0F);
                for (int i = 0; i < 8; i++) {
                    worldObj.spawnParticle(
                        "crit",
                        xCoord + 0.5D,
                        yCoord + 0.9D,
                        zCoord + 0.5D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.25D,
                        0.12D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.25D);
                }
            } else {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, "random.anvil_use", 0.8F, 1.5F);
                for (int i = 0; i < 6; i++) {
                    worldObj.spawnParticle(
                        "smoke",
                        xCoord + 0.5D,
                        yCoord + 0.9D,
                        zCoord + 0.5D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.15D,
                        0.05D,
                        (worldObj.rand.nextDouble() - 0.5D) * 0.15D);
                }
            }
            markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        return true;
    }

    public boolean tongsInteract(EntityPlayer player, ItemStack tongs) {
        if (tongs == null || !(tongs.getItem() instanceof ItemBlacksmithTongs)) {
            return false;
        }

        // 1. Tongs holding workpiece & Anvil empty -> Place workpiece on anvil
        if (ItemBlacksmithTongs.hasWorkpiece(tongs) && workpiece == null) {
            ItemStack wp = ItemBlacksmithTongs.getWorkpiece(tongs);
            ItemBlacksmithTongs.clearWorkpiece(tongs);
            setWorkpiece(wp);
            if (player != null) {
                player.inventoryContainer.detectAndSendChanges();
                if (worldObj == null || !worldObj.isRemote) {
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a7a[Forging Anvil] Placed \u00a7f" + wp.getDisplayName() + " \u00a7aon anvil."));
                }
            }
            if (worldObj != null) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, "step.stone", 1.0F, 1.0F);
            }
            return true;
        }

        // 2. Tongs empty & Anvil has workpiece -> Grab workpiece into tongs
        if (!ItemBlacksmithTongs.hasWorkpiece(tongs) && workpiece != null) {
            ItemStack wp = workpiece;
            setWorkpiece(null);
            ItemBlacksmithTongs.setWorkpiece(tongs, wp);
            if (player != null) {
                player.inventoryContainer.detectAndSendChanges();
                if (worldObj == null || !worldObj.isRemote) {
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a7e[Forging Anvil] Grabbed \u00a7f" + wp.getDisplayName()
                                + " \u00a7ewith blacksmith tongs."));
                }
            }
            if (worldObj != null) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.8D, zCoord + 0.5D, "step.stone", 1.0F, 1.2F);
            }
            return true;
        }

        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Workpiece", 10)) {
            this.workpiece = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Workpiece"));
        } else {
            this.workpiece = null;
        }
        if (tag.hasKey("TargetGeometry")) {
            try {
                this.targetGeometry = GeometryId.valueOf(tag.getString("TargetGeometry"));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("TargetGeometry", getTargetGeometry().name());
        if (workpiece != null) {
            NBTTagCompound wpTag = new NBTTagCompound();
            workpiece.writeToNBT(wpTag);
            tag.setTag("Workpiece", wpTag);
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    // --- IInventory Implementation (1 slot) ---

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? workpiece : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (slot == 0 && workpiece != null) {
            ItemStack stack = workpiece;
            workpiece = null;
            markDirty();
            return stack;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return decrStackSize(slot, 1);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) {
            setWorkpiece(stack);
        }
    }

    @Override
    public String getInventoryName() {
        return "container.forgingAnvil";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null || worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() instanceof ItemGenericComponent) return true;
        naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry.ResolvedInput input = naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry
            .resolve(stack);
        return input != null
            && input.kind() == naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry.InputKind.METALLIC_PRODUCT;
    }
}
