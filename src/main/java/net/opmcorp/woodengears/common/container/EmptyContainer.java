package net.opmcorp.woodengears.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class EmptyContainer extends Container
{
    @Override
    public boolean canInteractWith(final EntityPlayer player)
    {
        return false;
    }
}
