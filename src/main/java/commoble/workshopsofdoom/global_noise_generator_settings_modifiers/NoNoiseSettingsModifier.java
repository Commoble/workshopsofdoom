package commoble.workshopsofdoom.global_noise_generator_settings_modifiers;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.GlobalNoiseSettingsModifier;
import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class NoNoiseSettingsModifier extends GlobalNoiseSettingsModifier
{
	public static final NoNoiseSettingsModifier INSTANCE = new NoNoiseSettingsModifier(() -> WorkshopsOfDoom.INSTANCE.noNoiseSettingsModifier.get());
	public static final Codec<NoNoiseSettingsModifier> CODEC = Codec.unit(INSTANCE);
	
	public NoNoiseSettingsModifier(Supplier<? extends Serializer<?>> dispatcherGetter)
	{
		super(dispatcherGetter);
	}

	@Override
	public void modify(ResourceKey<NoiseGeneratorSettings> key, NoiseGeneratorSettings settings)
	{
		// noop
	}
}
