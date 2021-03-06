package net.voxelindustry.armedlogistics.client.gui;

import fr.ourten.teabeans.binding.BaseExpression;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.voxelindustry.armedlogistics.client.gui.component.EditableName;
import net.voxelindustry.armedlogistics.client.gui.component.InventoryView;
import net.voxelindustry.armedlogistics.client.gui.tab.FacingTab;
import net.voxelindustry.armedlogistics.client.gui.tab.IGuiTab;
import net.voxelindustry.armedlogistics.client.gui.tab.TabButton;
import net.voxelindustry.armedlogistics.common.tile.TileLogicisticNode;
import net.voxelindustry.brokkgui.component.GuiNode;
import net.voxelindustry.brokkgui.data.RelativeBindingHelper;
import net.voxelindustry.brokkgui.element.input.GuiToggleGroup;
import net.voxelindustry.brokkgui.panel.GuiAbsolutePane;
import net.voxelindustry.brokkgui.shape.Rectangle;
import net.voxelindustry.brokkgui.sprite.Texture;
import net.voxelindustry.brokkgui.wrapper.container.BrokkGuiContainer;
import net.voxelindustry.steamlayer.container.BuiltContainer;

public abstract class GuiLogisticNode<T extends TileLogicisticNode> extends BrokkGuiContainer<BuiltContainer> implements IGuiTab
{
    public static final float TAB_HEIGHT  = 32;
    public static final float GUI_WIDTH   = 176;
    public static final float GUI_HEIGHT  = 216;
    public static final int   FONT_HEIGHT = Minecraft.getInstance().fontRenderer.FONT_HEIGHT;


    @Getter
    private final T tile;

    protected GuiAbsolutePane tabHeaders;
    protected GuiAbsolutePane body;
    private   GuiAbsolutePane mainPanel;

    private final Rectangle survivalInventory;

    private final IGuiTab[] tabs;

    private IGuiTab lastTab = null;

    protected EditableName title;

    public GuiLogisticNode(BuiltContainer container)
    {
        super(container);

        tile = (T) container.getMainTile();

        mainPanel = new GuiAbsolutePane();
        setMainPanel(mainPanel);

        body = new GuiAbsolutePane();
        body.setBackgroundTexture(getBackgroundTexture());
        mainPanel.addChild(body, 0, TAB_HEIGHT);

        tabs = new IGuiTab[]{new FacingTab(this)};
        setupTabs();

        title = new EditableName(tile.getDisplayName()::getFormattedText, tile::setCustomName);
        title.getWidthProperty().bind(BaseExpression.transform(body.getWidthProperty(), parentWidth -> parentWidth - 6 - 3));
        body.addChild(title, 6, 6);

        survivalInventory = new Rectangle();
        survivalInventory.setSize(162, 76);
        survivalInventory.setID("survival-inventory");

        body.addChild(survivalInventory, 7, 0);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        if (lastTab == null)
            refreshOffset(getTabOffsetX());
    }

    protected abstract Texture getBackgroundTexture();

    protected abstract int getSurvivalInventoryOffset();

    private void refreshSize(float width, float height, float xOffset)
    {
        body.setSize(width - xOffset, height);
        setSize(width, height + TAB_HEIGHT);

        mainPanel.setChildPos(tabHeaders, xOffset, 0);
        body.setxTranslate(xOffset);
        setxOffset((int) -xOffset / 2);

        survivalInventory.setyTranslate(height - getSurvivalInventoryOffset());
    }

    private void refreshOffset(float xOffset)
    {
        refreshSize(GUI_WIDTH + xOffset, GUI_HEIGHT, xOffset);
    }

    @Override
    public GuiAbsolutePane getMainPanel()
    {
        return mainPanel;
    }

    @Override
    public String getName()
    {
        return getTile().getDisplayName().getFormattedText();
    }

    public IGuiTab getCurrentTab()
    {
        return lastTab == null ? this : lastTab;
    }

    private void setupTabs()
    {
        tabHeaders = new GuiAbsolutePane();
        tabHeaders.setSize(176, 32);

        GuiToggleGroup tabHeaderGroup = new GuiToggleGroup();

        TabButton mainTabButton = new TabButton(getIcon(), getName());
        mainTabButton.setToggleGroup(tabHeaderGroup);
        tabHeaderGroup.setSelectedButton(mainTabButton);
        tabHeaders.addChild(mainTabButton, 0, 4);

        mainTabButton.setOnSelectEvent(e ->
        {
            if (!e.isSelected())
                return;
            getElements().forEach(node -> node.setVisible(true));
            getContainer().showAllSlots();
            survivalInventory.setVisible(true);
            title.setVisible(true);
            lastTab.getElements().forEach(node -> node.setVisible(false));

            if (lastTab.getTabOffsetX() != getTabOffsetX())
                refreshOffset(getTabOffsetX());
            lastTab = null;
        });

        for (int i = 0; i < tabs.length; i++)
        {
            TabButton tabButton = new TabButton(tabs[i].getIcon(), tabs[i].getName());
            tabButton.setToggleGroup(tabHeaderGroup);
            tabs[i].setButton(tabButton);

            int finalIndex = i;
            tabButton.setOnSelectEvent(e ->
            {
                if (!e.isSelected())
                    return;

                IGuiTab newTab = tabs[finalIndex];

                if (lastTab == null)
                {
                    getElements().forEach(node -> node.setVisible(false));

                    getContainer().hideAllSlots();
                    survivalInventory.setVisible(false);
                    title.setVisible(false);

                    if (newTab.getTabOffsetX() != getTabOffsetX())
                        refreshOffset(newTab.getTabOffsetX());
                }
                else
                {
                    lastTab.getElements().forEach(node -> node.setVisible(false));

                    if (newTab.getTabOffsetX() != lastTab.getTabOffsetX())
                        refreshOffset(newTab.getTabOffsetX());
                }
                newTab.getElements().forEach(node -> node.setVisible(true));

                lastTab = newTab;
            });

            tabHeaders.addChild(tabButton, (i + 1) * 28, 4);

            tabs[i].getElements().forEach(element ->
            {
                mainPanel.addChild(element, 0, 0);
                RelativeBindingHelper.bindToPos(element, body);
            });

            RelativeBindingHelper.bindSizeRelative((GuiNode) tabs[i], body, 1, 1);
            tabs[i].getElements().forEach(node -> node.setVisible(false));
        }
        tabHeaderGroup.setAllowNothing(false);

        mainPanel.addChild(tabHeaders);
    }

    protected void updateStatusStyle()
    {
        if (tile.getConnectedInventoryProperty().getValue())
        {
            TileEntity tile =
                    Minecraft.getInstance().world.getTileEntity(this.tile.getPos().offset(this.tile.getFacing()));

            if(!(tile instanceof INamedContainerProvider))
                return;

            String name = ((INamedContainerProvider) tile).getDisplayName() != null ? ((INamedContainerProvider) tile).getDisplayName().getFormattedText() :
                    I18n.format("armedlogistics.gui.inventory.genericname");

            getInventoryView().setInvStatus(I18n.format("armedlogistics.gui.inventory.where", name,
                    I18n.format("armedlogistics.gui.facing." + this.tile.getFacing())));
            getInventoryView().setInvValid(true);
        }
        else
        {
            getInventoryView().setInvStatus(I18n.format("armedlogistics.gui.inventory.notwhere",
                    I18n.format("armedlogistics.gui.facing." + tile.getFacing())));
            getInventoryView().setInvValid(false);
        }
    }

    protected abstract InventoryView getInventoryView();
}
