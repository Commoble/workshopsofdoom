package commoble.workshopsofdoom.biome_providers;

import java.util.List;
import java.util.Set;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.Codecs;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;

public class BiomeCategoryProvider extends BiomeProvider
{
	public static final Codec<BiomeCategoryProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codecs.makeEnumSetCodec(Biome.BiomeCategory.CODEC).fieldOf("categories").forGetter(BiomeCategoryProvider::getCategories)
		).apply(instance, BiomeCategoryProvider::new));
	
	private final Set<BiomeCategory> categories;
	public Set<BiomeCategory> getCategories() { return this.categories; }
	

	public BiomeCategoryProvider(Set<BiomeCategory> categories)
	{
		super(WorkshopsOfDoom.INSTANCE.biomeCategoryProvider);
		this.categories = categories;
	}
	
	public List<Biome> getBiomes(MinecraftServer server)
	{
		return server.registryAccess()
			.registryOrThrow(Registry.BIOME_REGISTRY)
			.stream()
			.filter(biome -> this.getCategories().contains(biome.getBiomeCategory()))
			.toList();
	}

}
