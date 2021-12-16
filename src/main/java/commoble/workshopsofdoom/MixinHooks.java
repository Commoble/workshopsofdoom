package commoble.workshopsofdoom;

import com.google.common.collect.ImmutableMap;

import commoble.workshopsofdoom.noise_settings_modifiers.NoiseSettingsModifier;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryData;
import net.minecraft.resources.ResourceKey;

public class MixinHooks
{
	public static void whenBuildBuiltinRegistries(ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> builder)
	{
		builder.put(WorkshopsOfDoom.CONFIGURED_NOISE_SETTINGS_MODIFIER_REGISTRY_KEY, new RegistryData<>(WorkshopsOfDoom.CONFIGURED_NOISE_SETTINGS_MODIFIER_REGISTRY_KEY, NoiseSettingsModifier.DIRECT_CODEC, null));
	}
}
