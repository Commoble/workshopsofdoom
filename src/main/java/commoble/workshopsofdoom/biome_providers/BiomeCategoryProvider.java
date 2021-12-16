package commoble.workshopsofdoom.biome_providers;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.Codecs;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;

public class BiomeCategoryProvider extends BiomeProvider
{
	public static final Codec<BiomeCategoryProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codecs.makeEnumSetCodec(Biome.BiomeCategory.CODEC).fieldOf("categories").forGetter(BiomeCategoryProvider::getCategories),
			RegistryLookupCodec.create(Registry.BIOME_REGISTRY).forGetter(BiomeCategoryProvider::getBiomeRegistry)
		).apply(instance, BiomeCategoryProvider::new));
	
	private final Set<BiomeCategory> categories;
	public Set<BiomeCategory> getCategories() { return this.categories; }
	
	private final Registry<Biome> biomeRegistry;
	public Registry<Biome> getBiomeRegistry() { return this.biomeRegistry; }
	
	private final Supplier<List<Biome>> biomes;
	

	public BiomeCategoryProvider(Set<BiomeCategory> categories, Registry<Biome> biomeRegistry)
	{
		super(WorkshopsOfDoom.INSTANCE.biomeCategoryProvider);
		this.categories = categories;
		this.biomeRegistry = biomeRegistry;
		this.biomes = Suppliers.memoize(() -> this.biomeRegistry.stream().filter(biome -> this.getCategories().contains(biome.getBiomeCategory())).toList());
	}
	
	public List<Biome> getBiomes()
	{
		return this.biomes.get();
	}

}
