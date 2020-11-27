package commoble.workshopsofdoom.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.IllagerModel;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.util.ResourceLocation;

public class ExcavatorRenderer extends VindicatorRenderer
{
	public static final ResourceLocation VINDICATOR_TEXTURE = new ResourceLocation("textures/entity/illager/vindicator.png");
	public static final ResourceLocation EXCAVATOR_OVERLAY = new ResourceLocation(WorkshopsOfDoom.MODID, "textures/entity/excavator.png");

	public ExcavatorRenderer(EntityRendererManager manager)
	{
		super(manager);
		this.addLayer(new ExcavatorOverlay<>(this));
	}

	static class ExcavatorOverlay<E extends AbstractIllagerEntity, M extends IllagerModel<E>> extends LayerRenderer<E, M>
	{

		public ExcavatorOverlay(IEntityRenderer<E,M> renderer)
		{
			super(renderer);
		}

		@Override
		public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, E entitylivingbaseIn, float limbSwing, float limbSwingAmount,
			float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
		{
			if (!entitylivingbaseIn.isInvisible())
			{
				M model = this.getEntityModel();
				renderCutoutModel(model, EXCAVATOR_OVERLAY, matrixStackIn, bufferIn, packedLightIn, entitylivingbaseIn, 1.0F, 1.0F, 1.0F);

			}
		}

	}
}
