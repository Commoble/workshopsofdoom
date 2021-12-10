package commoble.workshopsofdoom.client;

import com.mojang.blaze3d.vertex.PoseStack;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;

public class ExcavatorRenderer extends VindicatorRenderer
{
	public static final ResourceLocation VINDICATOR_TEXTURE = new ResourceLocation("textures/entity/illager/vindicator.png");
	public static final ResourceLocation EXCAVATOR_OVERLAY = new ResourceLocation(WorkshopsOfDoom.MODID, "textures/entity/excavator.png");

	public ExcavatorRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		this.addLayer(new ExcavatorOverlay<>(this));
	}

	static class ExcavatorOverlay<E extends AbstractIllager, M extends IllagerModel<E>> extends RenderLayer<E, M>
	{

		public ExcavatorOverlay(RenderLayerParent<E,M> renderer)
		{
			super(renderer);
		}

		@Override
		public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, E entitylivingbaseIn, float limbSwing, float limbSwingAmount,
			float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
		{
			if (!entitylivingbaseIn.isInvisible())
			{
				M model = this.getParentModel();
				renderColoredCutoutModel(model, EXCAVATOR_OVERLAY, matrixStackIn, bufferIn, packedLightIn, entitylivingbaseIn, 1.0F, 1.0F, 1.0F);

			}
		}

	}
}
