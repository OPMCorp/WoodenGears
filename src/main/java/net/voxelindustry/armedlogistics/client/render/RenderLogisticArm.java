package net.voxelindustry.armedlogistics.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.voxelindustry.armedlogistics.ArmedLogistics;
import net.voxelindustry.armedlogistics.common.entity.EntityLogisticArm;

import javax.annotation.Nullable;

public class RenderLogisticArm extends EntityRenderer<EntityLogisticArm>
{
    private       ModelLogisticArm modelLogisticArm = new ModelLogisticArm();
    private final ItemRenderer     itemRenderer;

    public RenderLogisticArm(EntityRendererManager renderManager)
    {
        super(renderManager);
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void doRender(EntityLogisticArm logisticArm, double x, double y, double z, float entityYaw,
                         float partialTicks)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translated(x + 0.25, y + 0.75, z);

        GlStateManager.rotatef(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scalef(0.5F, 0.5F, 0.5F);

        bindEntityTexture(logisticArm);

        modelLogisticArm.renderPedestal(0.0625F);
        modelLogisticArm.renderFirstPiston(0.0625F);

        if (logisticArm.getPickupCount() != 0)
        {
            if (logisticArm.getPickupCount() > 40)
            {
                double delta = 1 - (interp(logisticArm.getPickupCount(), logisticArm.getPickupCount() + 1, partialTicks) / 80D);
                GlStateManager.translated(0, 2.35 * delta, 0);
                modelLogisticArm.renderSecondPiston(0.0625F);
                GlStateManager.translated(0, 2 * (1 - (logisticArm.getPickupCount() / 40D) / 2D), 0);
                modelLogisticArm.renderHead(0.0625F);
            }
            else
            {
                double delta = interp(logisticArm.getPickupCount(), logisticArm.getPickupCount() + 1, partialTicks) / 40D;
                GlStateManager.translated(0, 1.175 * delta, 0);
                modelLogisticArm.renderSecondPiston(0.0625F);
                GlStateManager.translated(0, (logisticArm.getPickupCount() / 40D), 0);
                modelLogisticArm.renderHead(0.0625F);
            }
        }
        else
        {
            modelLogisticArm.renderSecondPiston(0.0625F);
            modelLogisticArm.renderHead(0.0625F);
        }

        GlStateManager.translated(0.5D, 22 / 16D, 0.0D);
        renderItem(logisticArm);

        GlStateManager.popMatrix();
    }

    private void renderItem(EntityLogisticArm logisticArm)
    {
        ItemStack itemStack = logisticArm.getStack();

        if (!itemStack.isEmpty())
        {
            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.scalef(0.5F, 0.5F, 0.5F);
            GlStateManager.rotatef(180, 1, 0, 0);
            GlStateManager.pushLightingAttributes();
            RenderHelper.enableStandardItemLighting();
            itemRenderer.renderItem(itemStack, ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popAttributes();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityLogisticArm entity)
    {
        return new ResourceLocation(ArmedLogistics.MODID, "textures/models/logistic_arm.png");
    }

    private double interp(double previous, double next, double partialTicks)
    {
        return previous + (next - previous) * partialTicks;
    }
}