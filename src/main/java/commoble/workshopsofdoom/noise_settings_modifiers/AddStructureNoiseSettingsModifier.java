package commoble.workshopsofdoom.noise_settings_modifiers;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.biome_providers.BiomeProvider;
import commoble.workshopsofdoom.util.ReflectionUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;

public class AddStructureNoiseSettingsModifier extends NoiseSettingsModifier
{
	public static final Logger LOGGER = LogManager.getLogger();
	public static final Codec<AddStructureNoiseSettingsModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("structure").forGetter(AddStructureNoiseSettingsModifier::getStructure),
			StructureFeatureConfiguration.CODEC.fieldOf("config").forGetter(AddStructureNoiseSettingsModifier::getConfig),
			BiomeProvider.CODEC.fieldOf("biomes").forGetter(AddStructureNoiseSettingsModifier::getBiomeProvider)
		).apply(instance, AddStructureNoiseSettingsModifier::new));
	
	private final ResourceLocation structure;
	public ResourceLocation getStructure() { return this.structure; }
	
	private final StructureFeatureConfiguration config;
	public StructureFeatureConfiguration getConfig() { return this.config; }
	
	private final BiomeProvider biomeProvider;
	public BiomeProvider getBiomeProvider() { return this.biomeProvider; }
	
	public AddStructureNoiseSettingsModifier(ResourceLocation structure, StructureFeatureConfiguration config, BiomeProvider biomeProvider)
	{
		super(WorkshopsOfDoom.INSTANCE.addStructureNoiseSettingsModifier);
		this.structure = structure;
		this.config = config;
		this.biomeProvider = biomeProvider;
	}

	@Override
	public void modify(ServerLevel level)
	{
		MinecraftServer server = level.getServer();
		ConfiguredStructureFeature<?,?> configuredStructure = level.getServer()
			.registryAccess()
			.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)
			.get(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, this.getStructure()));
		StructureFeature<?> structure = configuredStructure.feature;
		ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
		StructureSettings structureSettings = chunkGenerator.getSettings();

		// don't run mojang's constructor because it's jank
		// (can we mixin in a new constructor?)
		StructureSettings newStructureSettings;
		try
		{
			newStructureSettings = (StructureSettings) ReflectionUtils.UNSAFE.allocateInstance(StructureSettings.class);
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
			return;
		}
		newStructureSettings.stronghold = structureSettings.stronghold;
		
		// we need to modify two immutable maps (by replacing them with slightly larger maps)
		Map<StructureFeature<?>, StructureFeatureConfiguration> spacingMap = structureSettings.structureConfig();
		ImmutableMap<StructureFeature<?>, ImmutableMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>>> structureBiomes = structureSettings.configuredStructures;
		
		// we'll do the spacing map first since it's a bit simpler
		// ImmutableMap builder doesn't allow replacing of duplicates, we'll use a regular hashmap to build
		Map<StructureFeature<?>, StructureFeatureConfiguration> builder = new HashMap<>();
		builder.putAll(spacingMap);
		builder.put(structure, this.getConfig());
		newStructureSettings.structureConfig = ImmutableMap.copyOf(builder);
		
		// then the biomes
		Map<StructureFeature<?>, ImmutableMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>>> structureBiomeBuilder = new HashMap<>();
		structureBiomeBuilder.putAll(structureBiomes);
		
		ImmutableMultimap.Builder<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>> multiBuilder = ImmutableMultimap.builder();
		this.biomeProvider.getBiomes(server).forEach(biome -> multiBuilder.put(configuredStructure, ResourceKey.create(Registry.BIOME_REGISTRY, biome.getRegistryName())));
		
		structureBiomeBuilder.put(structure, multiBuilder.build());
		newStructureSettings.configuredStructures = ImmutableMap.copyOf(structureBiomeBuilder);
		chunkGenerator.settings = newStructureSettings;
	}
}
