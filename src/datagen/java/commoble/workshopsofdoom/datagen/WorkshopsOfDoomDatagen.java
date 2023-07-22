package commoble.workshopsofdoom.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import commoble.workshopsofdoom.Names;
import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.structures.FastJigsawStructure;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.worldgen.BootstapContext;
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
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
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
		PackOutput packOutput = generator.getPackOutput();
		var lookup = event.getLookupProvider();
		
		BiFunction<BootstapContext<?>, TagKey<Biome>, HolderSet<Biome>> biomeTagHolderSet = (context, tag) -> context.lookup(Registries.BIOME).getOrThrow(tag);
		BiFunction<BootstapContext<?>, TagKey<Biome>, HolderSet<Biome>> overworldBiomes = (context, biomes) -> new AndHolderSet<>(List.of(biomeTagHolderSet.apply(context, biomes), biomeTagHolderSet.apply(context, BiomeTags.IS_OVERWORLD)));
		BiFunction<BootstapContext<?>, ResourceKey<Structure>, Holder<Structure>> structureHolder = (context, id) -> context.lookup(Registries.STRUCTURE).getOrThrow(id);
		
		// tags
		
		generator.addProvider(true, new TagsProvider<Biome>(packOutput, Registries.BIOME, lookup, WorkshopsOfDoom.MODID, efh)
		{
			@SuppressWarnings("unchecked")
			@Override
			protected void addTags(HolderLookup.Provider provider)
			{
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_DESERT_QUARRY).addTag(Tags.Biomes.IS_DESERT);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_PLAINS_QUARRY).addTag(Tags.Biomes.IS_PLAINS);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_MOUNTAIN_MINES).addTags(Tags.Biomes.IS_MOUNTAIN, BiomeTags.IS_HILL);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_BADLANDS_MINES).addTag(BiomeTags.IS_BADLANDS);
				this.tag(WorkshopsOfDoom.Tags.Biomes.HAS_WORKSHOP).add(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES);
			}
		});
		
		generator.addProvider(true, new TagsProvider<Structure>(packOutput, Registries.STRUCTURE, lookup, WorkshopsOfDoom.MODID, efh)
		{
			@Override
			protected void addTags(HolderLookup.Provider provider)
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

		RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder()
		{
			public HolderLookup.Provider buildPatch(RegistryAccess registries, HolderLookup.Provider lookup)
			{
				RegistrySetBuilder.BuildState state = this.createState(registries);
				Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> map = new HashMap<>();
				state.collectReferencedRegistries().forEach((p_272339_) -> {
					map.put(p_272339_.key(), p_272339_);
				});
				this.entries.stream().map((RegistryStub<?> stub) -> stub.collectChanges(state)).forEach((contents) -> map.put(contents.key(), contents));
				Stream<HolderLookup.RegistryLookup<?>> stream = registries.registries().map((entry) -> entry.value().asLookup());
				HolderLookup.Provider holderlookup$provider = HolderLookup.Provider
					.create(Stream.concat(stream, map.values().stream().map(RegistrySetBuilder.RegistryContents::buildAsLookup).peek(state::addOwner)));
				state.fillMissingHolders(lookup);
				// don't validate missing holder values
				state.throwOnError();
				return holderlookup$provider;
			}
		}
			.add(Registries.STRUCTURE, context -> {
				context.register(WorkshopsOfDoom.Keys.Structures.DESERT_QUARRY,
					structure(context, overworldBiomes.apply(context, Tags.Biomes.IS_DESERT), WorkshopsOfDoom.ResourceLocations.DESERT_QUARRY_START, 15, standardMonsters, desertZombies, standardSkeletons, excavationPillagers));
				context.register(WorkshopsOfDoom.Keys.Structures.PLAINS_QUARRY,
					structure(context, overworldBiomes.apply(context, Tags.Biomes.IS_PLAINS), WorkshopsOfDoom.ResourceLocations.PLAINS_QUARRY_START, 15, standardMonsters, standardZombies, standardSkeletons, excavationPillagers));
				context.register(WorkshopsOfDoom.Keys.Structures.MOUNTAIN_MINES,
					structure(context, overworldBiomes.apply(context, Tags.Biomes.IS_MOUNTAIN), WorkshopsOfDoom.ResourceLocations.MOUNTAIN_MINES_START, 25, standardMonsters, standardZombies, standardSkeletons, excavationPillagers));
				context.register(WorkshopsOfDoom.Keys.Structures.BADLANDS_MINES,
					structure(context, overworldBiomes.apply(context, BiomeTags.IS_BADLANDS), WorkshopsOfDoom.ResourceLocations.BADLANDS_MINES_START, 25, standardMonsters, standardZombies, standardSkeletons, excavationPillagers));
				context.register(WorkshopsOfDoom.Keys.Structures.WORKSHOP,
					structure(context, biomeTagHolderSet.apply(context, WorkshopsOfDoom.Tags.Biomes.HAS_WORKSHOP), WorkshopsOfDoom.ResourceLocations.WORKSHOP_START, 30, standardMonsters, standardZombies, coldSkeletons, workshopPillagers));
			})
			.add(Registries.STRUCTURE_SET, context -> {
				context.register(WorkshopsOfDoom.Keys.StructureSets.QUARRIES, new StructureSet(
					List.of(
						new StructureSelectionEntry(structureHolder.apply(context, WorkshopsOfDoom.Keys.Structures.DESERT_QUARRY), 1),
						new StructureSelectionEntry(structureHolder.apply(context, WorkshopsOfDoom.Keys.Structures.PLAINS_QUARRY), 1)),
					outpostLike(context, 1398115502)));
				context.register(WorkshopsOfDoom.Keys.StructureSets.MINES, new StructureSet(
					List.of(
						new StructureSelectionEntry(structureHolder.apply(context, WorkshopsOfDoom.Keys.Structures.MOUNTAIN_MINES), 1),
						new StructureSelectionEntry(structureHolder.apply(context, WorkshopsOfDoom.Keys.Structures.BADLANDS_MINES), 1)),
					outpostLike(context, 635902772)));
				context.register(WorkshopsOfDoom.Keys.StructureSets.WORKSHOPS, new StructureSet(
					List.of(new StructureSelectionEntry(structureHolder.apply(context, WorkshopsOfDoom.Keys.Structures.WORKSHOP), 1)),
					outpostLike(context, 1640641664)));
			});
		DatapackBuiltinEntriesProvider dbep = new DatapackBuiltinEntriesProvider(packOutput, lookup, registrySetBuilder, Set.of(WorkshopsOfDoom.MODID));
		generator.addProvider(true, dbep);
	}
	
	private static ResourceLocation rl(String id)
	{
		return new ResourceLocation(WorkshopsOfDoom.MODID, id);
	}
	
	@SafeVarargs
	private static FastJigsawStructure structure(BootstapContext<Structure> registries, HolderSet<Biome> biomes, ResourceLocation pool, int depth, List<SpawnerData>... monsters)
	{
		return new FastJigsawStructure(
			new StructureSettings(
				biomes,
				monsters.length > 0 ? Map.of(MobCategory.MONSTER, new StructureSpawnOverride(BoundingBoxType.STRUCTURE, WeightedRandomList.create(Stream.of(monsters).flatMap(List::stream).toList()))) : Map.of(),
				GenerationStep.Decoration.SURFACE_STRUCTURES,
				TerrainAdjustment.BEARD_BOX),
			registries.lookup(Registries.TEMPLATE_POOL).getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, pool)),
			Optional.empty(),
			depth,
			ConstantHeight.ZERO,
			Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
			80);
	}
	
	@SuppressWarnings("deprecation")
	private static RandomSpreadStructurePlacement outpostLike(BootstapContext<?> context, int seed)
	{
		return new RandomSpreadStructurePlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 0.2F, seed, Optional.of(new StructurePlacement.ExclusionZone(context.lookup(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.VILLAGES), 10)), 32, 8, RandomSpreadType.LINEAR);
	}
}
