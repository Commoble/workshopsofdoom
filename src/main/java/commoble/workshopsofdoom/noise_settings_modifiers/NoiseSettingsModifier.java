package commoble.workshopsofdoom.noise_settings_modifiers;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatchable;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatcher;
import net.minecraft.server.level.ServerLevel;

public abstract class NoiseSettingsModifier extends Dispatchable<NoiseSettingsModifier.Serializer<?>>
{
	public static final Codec<NoiseSettingsModifier> CODEC = WorkshopsOfDoom.NOISE_SETTINGS_MODIFIER_DISPATCHER.getDispatchedCodec();
	
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
