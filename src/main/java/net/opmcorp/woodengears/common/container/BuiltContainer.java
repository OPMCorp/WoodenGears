package net.opmcorp.woodengears.common.container;

import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.opmcorp.woodengears.common.container.sync.SyncableProperty;
import net.opmcorp.woodengears.common.network.ContainerUpdatePacket;
import net.opmcorp.woodengears.common.network.WGPacketHandler;
import org.apache.commons.lang3.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BuiltContainer extends Container
{
    @Getter
    private final String name;

    private final EntityPlayer player;

    private final Predicate<EntityPlayer> canInteract;
    private final List<Range<Integer>>    playerSlotRanges;
    private final List<Range<Integer>>    tileSlotRanges;

    private List<SyncableProperty<?>>         syncablesValues;
    private List<Consumer<InventoryCrafting>> craftEvents;

    private final List<IInventory> inventories;

    BuiltContainer(final String name, final EntityPlayer player, final List<IInventory> inventories,
                   final Predicate<EntityPlayer> canInteract, final List<Range<Integer>> playerSlotRange,
                   final List<Range<Integer>> tileSlotRange)
    {
        this.player = player;
        this.name = name;

        this.canInteract = canInteract;

        this.playerSlotRanges = playerSlotRange;
        this.tileSlotRanges = tileSlotRange;

        this.inventories = inventories;

        this.inventories.forEach(inventory -> inventory.openInventory(player));
    }

    public void setSyncables(final List<SyncableProperty<?>> properties)
    {
        this.syncablesValues = properties;
    }

    public void addSyncable(final SyncableProperty<?> property)
    {
        this.syncablesValues.add(property);
    }

    public void addCraftEvents(final List<Consumer<InventoryCrafting>> craftEvents)
    {
        this.craftEvents = craftEvents;
    }

    public void addCraftEvent(final Consumer<InventoryCrafting> craftEvent)
    {
        if (this.craftEvents == null)
            this.craftEvents = new ArrayList<>();
        this.craftEvents.add(craftEvent);
    }

    public void removeCraftEvent(final Consumer<InventoryCrafting> craftEvent)
    {
        if (this.craftEvents == null)
            this.craftEvents = new ArrayList<>();
        this.craftEvents.remove(craftEvent);
    }

    public void addSlot(final Slot slot)
    {
        this.addSlotToContainer(slot);
    }

    @Override
    public boolean canInteractWith(final EntityPlayer playerIn)
    {
        return this.canInteract.test(playerIn);
    }

    @Override
    public final void onCraftMatrixChanged(final IInventory inv)
    {
        if (this.craftEvents != null && !this.craftEvents.isEmpty())
            this.craftEvents.forEach(consumer -> consumer.accept((InventoryCrafting) inv));
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        if (this.syncablesValues != null && !this.syncablesValues.isEmpty())
        {
            for (final SyncableProperty<?> syncable : this.syncablesValues)
            {
                if (syncable.needRefresh())
                {
                    syncable.updateInternal();
                    WGPacketHandler.INSTANCE.sendTo(
                            new ContainerUpdatePacket(this.windowId,
                                    this.syncablesValues.indexOf(syncable), syncable.toNBT(new NBTTagCompound())),
                            (EntityPlayerMP) this.player);
                }
            }
        }
    }

    public void updateProperty(final int id, final NBTTagCompound property)
    {
        final SyncableProperty<?> syncable = this.syncablesValues.get(id);
        syncable.fromNBT(property);
        syncable.update();
    }

    @Override
    public ItemStack transferStackInSlot(final EntityPlayer player, final int index)
    {
        ItemStack originalStack = ItemStack.EMPTY;

        final Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack())
        {
            final ItemStack stackInSlot = slot.getStack();
            originalStack = stackInSlot.copy();

            boolean shifted = false;

            for (final Range<Integer> range : this.playerSlotRanges)
                if (range.contains(index))
                {
                    if (this.shiftToTile(stackInSlot))
                        shifted = true;
                    break;
                }

            if (!shifted)
                for (final Range<Integer> range : this.tileSlotRanges)
                    if (range.contains(index))
                    {
                        if (this.shiftToPlayer(stackInSlot))
                            shifted = true;
                        break;
                    }

            slot.onSlotChange(stackInSlot, originalStack);
            if (!stackInSlot.isEmpty())
                slot.onSlotChanged();
            if (stackInSlot.getCount() == originalStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, stackInSlot);
        }
        return originalStack;
    }

    protected boolean shiftItemStack(final ItemStack stackToShift, final int start, final int end)
    {
        boolean changed = false;
        if (stackToShift.isStackable())
        {
            for (int slotIndex = start; stackToShift.getCount() > 0 && slotIndex < end; slotIndex++)
            {
                final Slot slot = this.inventorySlots.get(slotIndex);
                final ItemStack stackInSlot = slot.getStack();
                if (!stackInSlot.isEmpty() && ItemStack.areItemStacksEqual(stackInSlot, stackToShift)
                        && slot.isItemValid(stackToShift)
                        && slot.getItemStackLimit(stackToShift) > stackInSlot.getCount())
                {
                    final int resultingStackSize = Math.min(slot.getItemStackLimit(stackToShift),
                            stackInSlot.getCount() + stackToShift.getCount());
                    final int max = Math.min(stackToShift.getMaxStackSize(), slot.getItemStackLimit(stackToShift));

                    if (resultingStackSize <= max)
                    {
                        stackToShift.setCount(0);
                        stackInSlot.setCount(resultingStackSize);
                        slot.onSlotChanged();
                        changed = true;
                    }
                    else if (stackInSlot.getCount() < max)
                    {
                        stackToShift.shrink(max - stackInSlot.getCount());
                        stackInSlot.setCount(max);
                        slot.onSlotChanged();
                        changed = true;
                    }
                }
            }
        }
        if (stackToShift.getCount() > 0)
        {
            for (int slotIndex = start; stackToShift.getCount() > 0 && slotIndex < end; slotIndex++)
            {
                final Slot slot = this.inventorySlots.get(slotIndex);
                ItemStack stackInSlot = slot.getStack();
                if (stackInSlot.isEmpty() && slot.isItemValid(stackToShift))
                {
                    final int max = Math.min(stackToShift.getMaxStackSize(), slot.getSlotStackLimit());

                    stackInSlot = stackToShift.copy();
                    stackInSlot.setCount(Math.min(stackToShift.getCount(), max));
                    stackToShift.shrink(stackInSlot.getCount());
                    slot.putStack(stackInSlot);
                    slot.onSlotChanged();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean shiftToTile(final ItemStack stackToShift)
    {
        for (final Range<Integer> range : this.tileSlotRanges)
            if (this.shiftItemStack(stackToShift, range.getMinimum(), range.getMaximum() + 1))
                return true;
        return false;
    }

    private boolean shiftToPlayer(final ItemStack stackToShift)
    {
        for (final Range<Integer> range : this.playerSlotRanges)
            if (this.shiftItemStack(stackToShift, range.getMinimum(), range.getMaximum() + 1))
                return true;
        return false;
    }

    @Override
    public void onContainerClosed(final EntityPlayer player)
    {
        super.onContainerClosed(player);
        this.inventories.forEach(inventory -> inventory.closeInventory(player));
    }
}