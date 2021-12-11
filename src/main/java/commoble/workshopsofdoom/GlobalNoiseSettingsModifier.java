package commoble.workshopsofdoom;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatchable;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public abstract class GlobalNoiseSettingsModifier extends Dispatchable<GlobalNoiseSettingsModifier.Serializer<?>>
{
	public GlobalNoiseSettingsModifier(Supplier<? extends Serializer<?>> dispatcherGetter)
	{
		super(dispatcherGetter);
	}
	
	public abstract void modify(ResourceKey<NoiseGeneratorSettings> key, NoiseGeneratorSettings settings);

	/**
	 * This is a subclass of Dispatcher because forge registries allow at most one registry per class
	 */
	public static class Serializer <MODIFIER extends GlobalNoiseSettingsModifier> extends Dispatcher<Serializer<?>, MODIFIER>
	{

		public Serializer(Codec<MODIFIER> subCodec)
		{
			super(subCodec);
		}

	}

}
