package com.mcjty.rftools.blocks.dimletconstruction;

import com.mcjty.container.InventoryHelper;
import com.mcjty.entity.GenericEnergyHandlerTileEntity;
import com.mcjty.rftools.blocks.BlockTools;
import com.mcjty.rftools.blocks.dimlets.DimletConfiguration;
import com.mcjty.rftools.items.ModItems;
import com.mcjty.rftools.items.dimlets.DimletEntry;
import com.mcjty.rftools.items.dimlets.KnownDimletConfiguration;
import com.mcjty.rftools.network.Argument;
import com.mcjty.rftools.network.PacketHandler;
import com.mcjty.rftools.network.PacketRequestIntegerFromServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Map;

public class DimletWorkbenchTileEntity extends GenericEnergyHandlerTileEntity implements ISidedInventory {
    public static final String CMD_STARTEXTRACT = "startExtract";
    public static final String CMD_GETEXTRACTING = "getExtracting";
    public static final String CLIENTCMD_GETEXTRACTING = "getExtracting";

    private InventoryHelper inventoryHelper = new InventoryHelper(this, DimletWorkbenchContainer.factory, DimletWorkbenchContainer.SIZE_BUFFER + 9);

    private int extracting = 0;
    private int idToExtract = -1;

    public int getExtracting() {
        return extracting;
    }


    public DimletWorkbenchTileEntity() {
        super(DimletConfiguration.WORKBENCH_MAXENERGY, DimletConfiguration.WORKBENCH_RECEIVEPERTICK);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return DimletWorkbenchContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canInsertItem(int index, ItemStack item, int side) {
        if (index == DimletWorkbenchContainer.SLOT_OUTPUT) {
            return false;
        }
        return DimletWorkbenchContainer.factory.isInputSlot(index) || DimletWorkbenchContainer.factory.isSpecificItemSlot(index);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack item, int side) {
        if (index == DimletWorkbenchContainer.SLOT_INPUT) {
            return false;
        }
        if (index == DimletWorkbenchContainer.SLOT_OUTPUT) {
            return true;
        }

        return DimletWorkbenchContainer.factory.isOutputSlot(index);
    }

    @Override
    public int getSizeInventory() {
        return inventoryHelper.getStacks().length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventoryHelper.getStacks()[index];
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        return inventoryHelper.decrStackSize(index, amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
    }

    @Override
    public String getInventoryName() {
        return "Workbench Inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    protected void checkStateServer() {
        if (extracting > 0) {
            extracting--;
            if (extracting == 0) {
                if (!doExtract()) {
                    // We failed due to not enough power. Try again later.
                    extracting = 10;
                }
            }
            markDirty();
        }
    }

    private void startExtracting() {
        if (extracting > 0) {
            // Already extracting
            return;
        }
        ItemStack stack = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_INPUT];
        if (stack != null) {
            if (ModItems.knownDimlet.equals(stack.getItem())) {
                int id = stack.getItemDamage();
                if (!KnownDimletConfiguration.craftableDimlets.contains(id)) {
                    extracting = 64;
                    idToExtract = id;
                    inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_INPUT, 1);
                    markDirty();
                }
            }
        }
    }

    private boolean doExtract() {
        int rf = DimletConfiguration.rfExtractOperation;
        rf = (int) (rf * (2.0f - getInfusedFactor()) / 2.0f);

        if (getEnergyStored(ForgeDirection.DOWN) < rf) {
            // Not enough energy.
            return false;
        }
        extractEnergy(ForgeDirection.DOWN, rf, false);

        DimletEntry entry = KnownDimletConfiguration.idToDimlet.get(idToExtract);
        mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletBaseItem));
        int rarity = entry.getRarity();
        mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletControlCircuitItem, 1, rarity));
        mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletTypeControllerItem, 1, entry.getKey().getType().ordinal()));
        int level;
        if (rarity <= 1) {
            level = 0;
        } else if (rarity <= 3) {
            level = 1;
        } else {
            level = 2;
        }
        mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletMemoryUnitItem, 1, level));
        mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletEnergyModuleItem, 1, level));

        idToExtract = -1;
        markDirty();

        return true;
    }

    private void mergeItemOrThrowInWorld(ItemStack stack) {
        int notInserted = inventoryHelper.mergeItemStack(this, stack, DimletWorkbenchContainer.SLOT_BUFFER, DimletWorkbenchContainer.SLOT_BUFFER + DimletWorkbenchContainer.SIZE_BUFFER, null);
        if (notInserted > 0) {
            BlockTools.spawnItemStack(worldObj, xCoord, yCoord, zCoord, stack);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound);
        extracting = tagCompound.getInteger("extracting");
        idToExtract = tagCompound.getInteger("idToExtract");
    }

    private void readBufferFromNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = tagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            inventoryHelper.getStacks()[i] = ItemStack.loadItemStackFromNBT(nbtTagCompound);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound);
        tagCompound.setInteger("extracting", extracting);
        tagCompound.setInteger("idToExtract", idToExtract);
    }

    private void writeBufferToNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = new NBTTagList();
        for (int i = 0 ; i < inventoryHelper.getStacks().length ; i++) {
            ItemStack stack = inventoryHelper.getStacks()[i];
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(nbtTagCompound);
            }
            bufferTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("Items", bufferTagList);
    }

    @Override
    public boolean execute(String command, Map<String, Argument> args) {
        boolean rc = super.execute(command, args);
        if (rc) {
            return true;
        }
        if (CMD_STARTEXTRACT.equals(command)) {
            startExtracting();
            return true;
        }
        return false;
    }

    // Request the extracting amount from the server. This has to be called on the client side.
    public void requestExtractingFromServer() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestIntegerFromServer(xCoord, yCoord, zCoord,
                CMD_GETEXTRACTING,
                CLIENTCMD_GETEXTRACTING));
    }

    @Override
    public Integer executeWithResultInteger(String command, Map<String, Argument> args) {
        Integer rc = super.executeWithResultInteger(command, args);
        if (rc != null) {
            return rc;
        }
        if (CMD_GETEXTRACTING.equals(command)) {
            return extracting;
        }
        return null;
    }

    @Override
    public boolean execute(String command, Integer result) {
        boolean rc = super.execute(command, result);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_GETEXTRACTING.equals(command)) {
            extracting = result;
            return true;
        }
        return false;
    }

}