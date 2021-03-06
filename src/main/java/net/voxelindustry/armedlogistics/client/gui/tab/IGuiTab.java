package net.voxelindustry.armedlogistics.client.gui.tab;

import net.minecraft.item.ItemStack;
import net.voxelindustry.brokkgui.component.GuiNode;

import java.util.List;

public interface IGuiTab
{
    List<GuiNode> getElements();

    ItemStack getIcon();

    String getName();

    default float getTabOffsetX()
    {
        return 0;
    }

    default void toggleVisibility(boolean isVisible)
    {

    }

    default void setButton(TabButton button)
    {

    }
}
