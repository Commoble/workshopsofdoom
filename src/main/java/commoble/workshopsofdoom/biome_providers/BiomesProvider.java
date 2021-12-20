package commoble.workshopsofdoom.biome_providers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;

public class BiomesProvider extends BiomeProvider
{
	public static final Codec<BiomesProvider> CODEC = 
		RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.xmap(ResourceKey.elementKey(Registry.BIOME_REGISTRY), ResourceKey::location)
				.listOf().optionalFieldOf("values", ImmutableList.of()).forGetter(BiomesProvider::getBiomeKeys)
		).apply(instance, BiomesProvider::new));
	
	private final List<ResourceKey<Biome>> biomes;
	public List<ResourceKey<Biome>> getBiomeKeys() { return this.biomes; }
	
	public BiomesProvider(List<ResourceKey<Biome>> biomes)
	{
		super(WorkshopsOfDoom.INSTANCE.biomesProvider);
		this.biomes = biomes;
	}

	@Override
	public List<Biome> getBiomes(MinecraftServer server)
	{
		List<Biome> biomeList = new ArrayList<>();
		Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		for (ResourceKey<Biome> key : this.biomes)
		{
			Biome biome = biomeRegistry.get(key);
			if (biome != null)
				biomeList.add(biome);
		}
		return biomeList;
	}
}
