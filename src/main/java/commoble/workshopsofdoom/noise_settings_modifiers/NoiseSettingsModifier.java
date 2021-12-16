package commoble.workshopsofdoom.noise_settings_modifiers;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatchable;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatcher;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.level.ServerLevel;

public abstract class NoiseSettingsModifier extends Dispatchable<NoiseSettingsModifier.Serializer<?>>
{
	public static final Codec<NoiseSettingsModifier> DIRECT_CODEC = WorkshopsOfDoom.NOISE_SETTINGS_MODIFIER_DISPATCHER.getDispatchedCodec();
	public static final Codec<Supplier<NoiseSettingsModifier>> REGISTRY_LOOKUP_CODEC = RegistryFileCodec.create(WorkshopsOfDoom.CONFIGURED_NOISE_SETTINGS_MODIFIER_REGISTRY_KEY, DIRECT_CODEC);
	public static final Codec<List<Supplier<NoiseSettingsModifier>>> LIST_CODEC = RegistryFileCodec.homogeneousList(WorkshopsOfDoom.CONFIGURED_NOISE_SETTINGS_MODIFIER_REGISTRY_KEY, DIRECT_CODEC);
	
	public NoiseSettingsModifier(Supplier<? extends Serializer<?>> dispatcherGetter)
	{
		super(dispatcherGetter);
	}
	
	public abstract void modify(ServerLevel level);

	/**
	 * This is a subclass of Dispatcher because forge registries allow at most one registry per class
	 */
	public static class Serializer <MODIFIER extends NoiseSettingsModifier> extends Dispatcher<Serializer<?>, MODIFIER>
	{

		public Serializer(Codec<MODIFIER> subCodec)
		{
			super(subCodec);
		}

	}

}
