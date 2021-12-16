package commoble.workshopsofdoom.noise_settings_modifiers;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.server.level.ServerLevel;

public final class NoNoiseSettingsModifier extends NoiseSettingsModifier
{
	public static final NoNoiseSettingsModifier INSTANCE = new NoNoiseSettingsModifier(() -> WorkshopsOfDoom.INSTANCE.noNoiseSettingsModifier.get());
	public static final Codec<NoNoiseSettingsModifier> CODEC = Codec.unit(INSTANCE);
	
	public NoNoiseSettingsModifier(Supplier<? extends Serializer<?>> dispatcherGetter)
	{
		super(dispatcherGetter);
	}

	@Override
	public void modify(ServerLevel level)
	{
		// noop
	}
}
