package commoble.workshopsofdoom.datagen;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import commoble.workshopsofdoom.Names;
import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.structures.FastJigsawStructure;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.holdersets.AndHolderSet;

@Mod(WorkshopsOfDoomDatagen.DATAGEN_MODID)
@EventBusSubscriber(modid=WorkshopsOfDoomDatagen.DATAGEN_MODID, bus=Bus.MOD)
public class WorkshopsOfDoomDatagen
{
	public static final String DATAGEN_MODID = "workshopsofdoom_datagen";
	
	public static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.create();
	
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event)
	{
		DataGenerator generator = event.getGenerator();
		ExistingFileHelper efh = event.getExistingFileHelper();
		
		RegistryAccess registries = RegistryAccess.builtinCopy();
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
		
		Registry<Structure> structures = registries.registryOrThrow(Registry.STRUCTURE_REGISTRY);
		
		Function<TagKey<Biome>, HolderSet<Biome>> biomeTagHolderSet = (tag) -> new HolderSet.Named<>(registries.registryOrThrow(Registry.BIOME_REGISTRY), tag);
		Function<HolderSet<Biome>, HolderSet<Biome>> overworldBiomes = (biomes) -> new AndHolderSet<>(List.of(biomes, biomeTagHolderSet.apply(BiomeTags.IS_OVERWORLD)));
		Function<String, ResourceKey<Structure>> structureKey = id -> ResourceKey.create(Registry.STRUCTURE_REGISTRY, new ResourceLocation(WorkshopsOfDoom.MODID, id));
		Function<String, Holder<Structure>> structureHolder = id -> structures.getOrCreateHolderOrThrow(structureKey.apply(id));
		
		// tags
		
		generator.addProvider(true, new TagsProvider<Biome>(generator, registries.registryOrThrow(Registry.BIOME_REGISTRY), WorkshopsOfDoom.MODID, efh)
		{
			@SuppressWarnings("unchecked")
			@Override
			protected void addTags()
			{
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_DESERT_QUARRY).addTag(Tags.Biomes.IS_DESERT);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_PLAINS_QUARRY).addTag(Tags.Biomes.IS_PLAINS);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_MOUNTAIN_MINES).addTags(Tags.Biomes.IS_MOUNTAIN, BiomeTags.IS_HILL);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_BADLANDS_MINES).addTag(BiomeTags.IS_BADLANDS);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_WORKSHOP).add(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES);
			}
		});
		
		generator.addProvider(true, new TagsProvider<Structure>(generator, registries.registryOrThrow(Registry.STRUCTURE_REGISTRY), WorkshopsOfDoom.MODID, efh)
		{
			@Override
			protected void addTags()
			{
				// using optional references here due to a forge bug that causes false-negative missing-reference-in-tag logspam when creating singleplayer worlds
				this.tag(WorkshopsOfDoom.Tags.Structures.QUARRIES).addOptional(rl(Names.DESERT_QUARRY)).addOptional(rl(Names.PLAINS_QUARRY));
				this.tag(WorkshopsOfDoom.Tags.Structures.MINES).addOptional(rl(Names.MOUNTAIN_MINES)).addOptional(rl(Names.BADLANDS_MINES));
				this.tag(WorkshopsOfDoom.Tags.Structures.EXCAVATIONS).addOptionalTag(WorkshopsOfDoom.Tags.Structures.QUARRIES.location()).addOptionalTag(WorkshopsOfDoom.Tags.Structures.MINES.location());
				this.tag(WorkshopsOfDoom.Tags.Structures.WORKSHOPS).addOptional(rl(Names.WORKSHOP));
			}
		});
		
		List<SpawnerData> standardMonsters = List.of(
			new SpawnerData(EntityType.SPIDER, 100, 4, 4),
			new SpawnerData(EntityType.CREEPER, 100, 4, 4),
			new SpawnerData(EntityType.SLIME, 100, 4, 4),
			new SpawnerData(EntityType.ENDERMAN, 10, 1, 4),
			new SpawnerData(EntityType.WITCH, 10, 1, 1)
			);
		List<SpawnerData> standardZombies = List.of(
			new SpawnerData(EntityType.ZOMBIE, 95, 4, 4),
			new SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 4, 4)
			);
		List<SpawnerData> desertZombies = List.of(
			new SpawnerData(EntityType.ZOMBIE, 19, 4, 4),
			new SpawnerData(EntityType.ZOMBIE_VILLAGER, 1, 1, 1),
			new SpawnerData(EntityType.HUSK, 80, 4, 4)
			);
		List<SpawnerData> standardSkeletons = List.of(
			new SpawnerData(EntityType.SKELETON, 100, 4, 4)
			);
		List<SpawnerData> coldSkeletons = List.of(
			new SpawnerData(EntityType.SKELETON, 20, 4, 4),
			new SpawnerData(EntityType.STRAY, 80, 4, 4)
			);
		List<SpawnerData> excavationPillagers = List.of(
			new SpawnerData(EntityType.PILLAGER, 1000, 1, 1),
			new SpawnerData(WorkshopsOfDoom.INSTANCE.excavator.get(), 1000, 1, 1)
			);
		List<SpawnerData> workshopPillagers = List.of(
			new SpawnerData(EntityType.PILLAGER, 500, 1, 1),
			new SpawnerData(WorkshopsOfDoom.INSTANCE.excavator.get(), 500, 1, 1),
			new SpawnerData(EntityType.VINDICATOR, 1, 1, 1)
			);
		
		// structures
		
		generator.addProvider(true, JsonCodecProvider.forDatapackRegistry(generator, efh, WorkshopsOfDoom.MODID, ops, Registry.STRUCTURE_REGISTRY, Map.of(
			rl(Names.DESERT_QUARRY), structure(registries, overworldBiomes.apply(biomeTagHolderSet.apply(Tags.Biomes.IS_DESERT)), rl(Names.DESERT_QUARRY_START), 15, standardMonsters, desertZombies, standardSkeletons, excavationPillagers),
			rl(Names.PLAINS_QUARRY), structure(registries, overworldBiomes.apply(biomeTagHolderSet.apply(Tags.Biomes.IS_PLAINS)), rl(Names.PLAINS_QUARRY_START), 15, standardMonsters, standardZombies, standardSkeletons, excavationPillagers),
			rl(Names.MOUNTAIN_MINES), structure(registries, overworldBiomes.apply(biomeTagHolderSet.apply(Tags.Biomes.IS_MOUNTAIN)), rl(Names.MOUNTAIN_MINES_START), 25, standardMonsters, standardZombies, standardSkeletons, excavationPillagers),
			rl(Names.BADLANDS_MINES), structure(registries, overworldBiomes.apply(biomeTagHolderSet.apply(BiomeTags.IS_BADLANDS)), rl(Names.BADLANDS_MINES_START), 25, standardMonsters, standardZombies, standardSkeletons, excavationPillagers),
			rl(Names.WORKSHOP), structure(registries, biomeTagHolderSet.apply(WorkshopsOfDoom.Tags.Biomes.HAS_WORKSHOP), rl(Names.WORKSHOP_START), 30, standardMonsters, standardZombies, coldSkeletons, workshopPillagers)
			)));
		
		// structure sets
		
		generator.addProvider(true, JsonCodecProvider.forDatapackRegistry(generator, efh, WorkshopsOfDoom.MODID, ops, Registry.STRUCTURE_SET_REGISTRY, Map.of(
			rl(Names.QUARRIES), new StructureSet(
				List.of(new StructureSelectionEntry(structureHolder.apply(Names.DESERT_QUARRY), 1), new StructureSelectionEntry(structureHolder.apply(Names.PLAINS_QUARRY), 1)),
				new RandomSpreadStructurePlacement(24, 10, RandomSpreadType.LINEAR, 1398115502)),
			rl(Names.MINES), new StructureSet(
				List.of(new StructureSelectionEntry(structureHolder.apply(Names.MOUNTAIN_MINES), 1), new StructureSelectionEntry(structureHolder.apply(Names.BADLANDS_MINES), 1)),
				new RandomSpreadStructurePlacement(32, 12, RandomSpreadType.LINEAR, 635902772)),
			rl(Names.WORKSHOPS), new StructureSet(
				List.of(new StructureSelectionEntry(structureHolder.apply(Names.WORKSHOP), 1)),
				new RandomSpreadStructurePlacement(32, 12, RandomSpreadType.LINEAR, 1640641664))
			)));
	}
	
	private static ResourceLocation rl(String id)
	{
		return new ResourceLocation(WorkshopsOfDoom.MODID, id);
	}
	
	@SafeVarargs
	private static FastJigsawStructure structure(RegistryAccess registries, HolderSet<Biome> biomes, ResourceLocation pool, int depth, List<SpawnerData>... monsters)
	{
		return new FastJigsawStructure(
			new StructureSettings(
				biomes,
				monsters.length > 0 ? Map.of(MobCategory.MONSTER, new StructureSpawnOverride(BoundingBoxType.STRUCTURE, WeightedRandomList.create(Stream.of(monsters).flatMap(List::stream).toList()))) : Map.of(),
				GenerationStep.Decoration.SURFACE_STRUCTURES,
				TerrainAdjustment.BEARD_THIN),
			registries.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY).getOrCreateHolderOrThrow(ResourceKey.create(Registry.TEMPLATE_POOL_REGISTRY, pool)),
			Optional.empty(),
			depth,
			ConstantHeight.ZERO,
			Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
			80);
	}
}
