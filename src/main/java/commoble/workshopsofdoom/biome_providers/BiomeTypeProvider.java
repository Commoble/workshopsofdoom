package commoble.workshopsofdoom.biome_providers;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

public class BiomeTypeProvider extends BiomeProvider
{
	public static final Codec<BiomeTypeProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.xmap(BiomeDictionary.Type::getType, BiomeDictionary.Type::getName).fieldOf("biome_type").forGetter(BiomeTypeProvider::getBiomeType),
			RegistryLookupCodec.create(Registry.BIOME_REGISTRY).forGetter(BiomeTypeProvider::getBiomeRegistry)
		).apply(instance, BiomeTypeProvider::new));
	
	private final BiomeDictionary.Type biomeType;
	public BiomeDictionary.Type getBiomeType() { return this.biomeType; }
	
	private final Registry<Biome> biomeRegistry;
	public Registry<Biome> getBiomeRegistry() { return this.biomeRegistry; }
	
	private final Supplier<List<Biome>> biomes;

	public BiomeTypeProvider(BiomeDictionary.Type biomeType, Registry<Biome> biomeRegistry)
	{
		super(WorkshopsOfDoom.INSTANCE.biomeTypeProvider);
		this.biomeType = biomeType;
		this.biomeRegistry = biomeRegistry;
		this.biomes = Suppliers.memoize(() -> this.biomeRegistry.entrySet().stream()
			.filter(entry -> BiomeDictionary.hasType(entry.getKey(), this.biomeType))
			.map(Map.Entry::getValue)
			.toList());
	}

	@Override
	public List<Biome> getBiomes()
	{
		return this.biomes.get();
	}

}
