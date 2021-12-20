package commoble.workshopsofdoom.noise_settings_modifiers;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.Level;

public class ApplyIfDimensionNoiseSettingsModifier extends NoiseSettingsModifier
{
	public static final Codec<ApplyIfDimensionNoiseSettingsModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("tag").forGetter(ApplyIfDimensionNoiseSettingsModifier::getTagId),
				NoiseSettingsModifier.CODEC.fieldOf("delegate").forGetter(ApplyIfDimensionNoiseSettingsModifier::getDelegate)
			).apply(instance, ApplyIfDimensionNoiseSettingsModifier::new));
	
	private final ResourceLocation tagId;
	public ResourceLocation getTagId() { return this.tagId; };
	
	private final NoiseSettingsModifier delegate;
	public NoiseSettingsModifier getDelegate() { return this.delegate; }
	
	private final Supplier<Tag<ResourceKey<Level>>> tagLookup;
	
	public ApplyIfDimensionNoiseSettingsModifier(ResourceLocation tagId, NoiseSettingsModifier delegate)
	{
		super(WorkshopsOfDoom.INSTANCE.applyIfDimensionNoiseSettingsModifer);
		this.tagId = tagId;
		this.delegate = delegate;
		this.tagLookup = Suppliers.memoize(() -> WorkshopsOfDoom.DIMENSION_TAGS.getTag(tagId));
	}

	@Override
	public void modify(ServerLevel level)
	{
		if (this.tagLookup.get().contains(level.dimension()))
		{
			this.delegate.modify(level);
		}
	}
	
}
