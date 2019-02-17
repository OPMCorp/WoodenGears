package net.vi.woodengears.common.tile;

import fr.ourten.teabeans.value.BaseProperty;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.IItemHandler;
import net.vi.woodengears.common.grid.logistic.ProviderType;
import net.vi.woodengears.common.grid.logistic.node.BaseItemProvider;
import net.vi.woodengears.common.grid.logistic.node.InventoryBuffer;
import net.voxelindustry.steamlayer.container.BuiltContainer;
import net.voxelindustry.steamlayer.container.ContainerBuilder;
import net.voxelindustry.steamlayer.tile.ITileInfoList;

public class TileProvider extends TileLogicisticNode implements ITickable
{
    @Getter
    private BaseItemProvider provider;

    @Getter
    private BaseProperty<IItemHandler> cachedInventoryProperty;
    private WrappedInventory           wrappedInventory;
    private InventoryBuffer            buffer;

    public TileProvider()
    {
        super("provider");

        this.cachedInventoryProperty = new BaseProperty<>(null, "cachedInventoryProperty");

        this.wrappedInventory = new WrappedInventory();
        this.buffer = new InventoryBuffer(8, 8 * 64);

        this.provider = new BaseItemProvider(this, ProviderType.PASSIVE_PROVIDER, this.wrappedInventory, this.buffer);

        this.getConnectedInventoryProperty().addListener(obs -> wrappedInventory.setWrapped(getConnectedInventory()));
    }

    @Override
    public void addInfo(ITileInfoList list)
    {
        super.addInfo(list);

        if (!this.buffer.isEmpty())
        {
            list.addText("Buffer:");
            this.buffer.getStacks().forEach(stack ->
            {
                if (!stack.isEmpty())
                    list.addItem(stack);
            });
        }
    }

    @Override
    public void update()
    {
        if (this.isClient())
            return;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);

        this.buffer.writeNBT(tag);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.buffer.readNBT(tag);
    }

    @Override
    public BuiltContainer createContainer(EntityPlayer player)
    {
        return new ContainerBuilder("provider", player)
                .player(player).inventory(8, 94).hotbar(8, 152)
                .addInventory()
                .syncBooleanValue(getConnectedInventoryProperty()::getValue, getConnectedInventoryProperty()::setValue)
                .syncInventory(this::getConnectedInventory, cachedInventoryProperty::setValue, 10)
                .create();
    }

    public void dropBuffer()
    {
        for (ItemStack stack : this.buffer.getStacks())
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
    }
}
