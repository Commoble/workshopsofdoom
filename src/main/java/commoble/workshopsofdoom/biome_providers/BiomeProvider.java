package commoble.workshopsofdoom.biome_providers;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatchable;
import commoble.workshopsofdoom.util.RegistryDispatcher.Dispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;

/**
 * Base class for json-serializable providers of biome objects
 */
public abstract class BiomeProvider extends Dispatchable<BiomeProvider.Serializer<?>>
{
	public static final Codec<BiomeProvider> CODEC = WorkshopsOfDoom.BIOME_PROVIDER_DISPATCHER.getDispatchedCodec();
	
	public BiomeProvider(Supplier<? extends Serializer<?>> dispatcherGetter)
	{
		super(dispatcherGetter);
	}
	
	/**
	 * Returns a list of biome objects. These must be present in the server's biome registry.
	 * (Use RegistryLookupCodec.create(Registry.BIOME_REGISTRY) to create a fake codec field for the server biome registry)
	 * TODO that doesn't actually work right now, temporarily monkey-patched this to work via reloadable data,
	 * so you can't use RegistryLookupCodec 
	 * @return list of valid biomes
	 */
	public abstract List<Biome> getBiomes(MinecraftServer server);

	public static class Serializer<BIOMEPROVIDER extends BiomeProvider> extends Dispatcher<Serializer<?>, BIOMEPROVIDER>
	{

		public Serializer(Codec<BIOMEPROVIDER> subCodec)
		{
			super(subCodec);
		}
		
	}
}
