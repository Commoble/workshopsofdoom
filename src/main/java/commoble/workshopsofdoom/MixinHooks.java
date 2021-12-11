package commoble.workshopsofdoom;

import com.google.common.collect.ImmutableMap;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryData;
import net.minecraft.resources.ResourceKey;

public class MixinHooks
{
	public static void whenBuildBuiltinRegistries(ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> builder)
	{
		builder.put(WorkshopsOfDoom.STRUCTURE_NOISE_REGISTRY_KEY, new RegistryData<>(WorkshopsOfDoom.STRUCTURE_NOISE_REGISTRY_KEY, WorkshopsOfDoom.INSTANCE.globalNoiseModifiersDispatcher.getDispatchedCodec(), null));
	}
}
