package commoble.workshopsofdoom.biome_providers;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.world.level.biome.Biome;

public class BiomesProvider extends BiomeProvider
{
	public static final Codec<BiomesProvider> CODEC = Biome.LIST_CODEC
		.optionalFieldOf("values", ImmutableList.of())
		.xmap(BiomesProvider::new, BiomesProvider::getBiomeSuppliers)
		.codec(); // always end with a MapCodecCodec when defining dispatch sub-codecs to ensure proper field splicing
	
	private final List<Supplier<Biome>> biomes;
	public List<Supplier<Biome>> getBiomeSuppliers() { return this.biomes; }
	
	public BiomesProvider(List<Supplier<Biome>> biomes)
	{
		super(WorkshopsOfDoom.INSTANCE.biomesProvider);
		this.biomes = biomes;
	}

	@Override
	public List<Biome> getBiomes()
	{
		return this.getBiomeSuppliers().stream().map(Supplier::get).toList();
	}
}
