package commoble.workshopsofdoom.global_noise_generator_settings_modifiers;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.GlobalNoiseSettingsModifier;
import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class ApplyIfTaggedNoiseSettingsModifier extends GlobalNoiseSettingsModifier
{
	public static final Codec<ApplyIfTaggedNoiseSettingsModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("tag").forGetter(ApplyIfTaggedNoiseSettingsModifier::getTagId),
				WorkshopsOfDoom.INSTANCE.globalNoiseModifiersDispatcher.getDispatchedCodec().fieldOf("delegate").forGetter(ApplyIfTaggedNoiseSettingsModifier::getDelegate)
			).apply(instance, ApplyIfTaggedNoiseSettingsModifier::new));
	
	private final ResourceLocation tagId;
	public ResourceLocation getTagId() { return this.tagId; };
	
	private final GlobalNoiseSettingsModifier delegate;
	public GlobalNoiseSettingsModifier getDelegate() { return this.delegate; }
	
	private final Supplier<Tag<ResourceKey<NoiseGeneratorSettings>>> tagLookup;
	
	public ApplyIfTaggedNoiseSettingsModifier(ResourceLocation tagId, GlobalNoiseSettingsModifier delegate)
	{
		super(WorkshopsOfDoom.INSTANCE.applyIfTaggedNoiseSettingsModifer);
		this.tagId = tagId;
		this.delegate = delegate;
		this.tagLookup = Suppliers.memoize(() -> WorkshopsOfDoom.NOISE_GEN_SETTINGS_TAGS.getTag(tagId));
	}

	@Override
	public void modify(ResourceKey<NoiseGeneratorSettings> key, NoiseGeneratorSettings settings)
	{
		if (this.tagLookup.get().contains(key))
		{
			this.delegate.modify(key, settings);
		}
	}
	
}
