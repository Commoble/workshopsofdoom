package commoble.workshopsofdoom.biome_providers;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

public class BiomeTypeProvider extends BiomeProvider
{
	public static final Codec<BiomeTypeProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.xmap(BiomeDictionary.Type::getType, BiomeDictionary.Type::getName).fieldOf("biome_type").forGetter(BiomeTypeProvider::getBiomeType)
		).apply(instance, BiomeTypeProvider::new));
	
	private final BiomeDictionary.Type biomeType;
	public BiomeDictionary.Type getBiomeType() { return this.biomeType; }

	public BiomeTypeProvider(BiomeDictionary.Type biomeType)
	{
		super(WorkshopsOfDoom.INSTANCE.biomeTypeProvider);
		this.biomeType = biomeType;
	}

	@Override
	public List<Biome> getBiomes(MinecraftServer server)
	{
		Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		List<Biome> biomeList = new ArrayList<>();
		for (var key : BiomeDictionary.getBiomes(this.getBiomeType()))
		{
			Biome biome = biomeRegistry.get(key);
			if (key != null)
				biomeList.add(biome);
		}
		return biomeList;
	}

}
