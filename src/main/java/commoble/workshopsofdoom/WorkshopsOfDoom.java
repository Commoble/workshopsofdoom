package commoble.workshopsofdoom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import commoble.workshopsofdoom.LoadableJigsawStructure.LoadableJigsawConfig;
import commoble.workshopsofdoom.features.BlockMoundFeature;
import net.minecraft.block.Blocks;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.BlockStateProvidingFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod(WorkshopsOfDoom.MODID)
public class WorkshopsOfDoom
{
	public static final String MODID = "workshopsofdoom";
	public static final Logger logger = LogManager.getLogger();
	public static WorkshopsOfDoom INSTANCE;
	
	// forge registry objects
	public final RegistryObject<BlockMoundFeature> blockMound;
	
	public final RegistryObject<LoadableJigsawStructure> quarry;
	
	// vanilla registry objects that we can't register in the mod constructor due to being off-thread
	public StructureFeature<LoadableJigsawConfig, ? extends Structure<LoadableJigsawConfig>> desertQuarry = null;
	
	public WorkshopsOfDoom() // invoked by forge due to @Mod
	{
		INSTANCE = this;
		
		// mod bus has modloading init events and registry events
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		// forge bus is for server starting events and in-game events
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// create and register deferred registers
		DeferredRegister<Feature<?>> features = registerRegister(modBus, ForgeRegistries.FEATURES);
		DeferredRegister<Structure<?>> structures = registerRegister(modBus, ForgeRegistries.STRUCTURE_FEATURES);
		
		// get a temporary list of structures to finagle vanilla registries with later
		Map<RegistryKey<World>, List<RegistryObject<LoadableJigsawStructure>>> structuresByWorld = new HashMap<>();
		
		// register registry objects
		this.blockMound = features.register(Names.BLOCK_MOUND, () -> new BlockMoundFeature(BlockStateProvidingFeatureConfig.field_236453_a_));
		
		// structures are added to a list for later vanilla-registry-finagling
		this.quarry = registerStructure(structures, structuresByWorld, Names.QUARRY,
			() -> new LoadableJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES),
			World.OVERWORLD);
//		this.quarry = structureReg.apply(Names.QUARRY, () -> new LoadableJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES));
		
		// add event listeners to event busses
		modBus.addListener(this::onCommonSetup);

		// per forge recommendations, adding things to biomes should be done in the HIGH phase
		forgeBus.addListener(EventPriority.HIGH, this::addThingsToBiomeOnBiomeLoad);
		
		Consumer<WorldEvent.Load> addStructuresToWorldListener = event -> this.addStructuresToWorld(event,structuresByWorld);
		forgeBus.addListener(addStructuresToWorldListener);
	}
	
	void onCommonSetup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(this::afterCommonSetup);
	}
	
	void afterCommonSetup()
	{
		// this needs to be called for each Structure instance
		// structures use this weird placement info per-world instead of feature placements
		setStructureInfo(this.quarry.get(), false, 8, 4, 892348929);
		
		// register to forgeless vanilla registries
		Registry.register(Registry.STRUCTURE_POOL_ELEMENT, new ResourceLocation(MODID, Names.GROUND_FEATURE), GroundFeatureJigsawPiece.DESERIALIZER);
		
		// register to vanilla worldgen registries
		registerConfiguredFeature(Names.DIRT_MOUND,
			new ConfiguredFeature<>(this.blockMound.get(),
				new BlockStateProvidingFeatureConfig(new SimpleBlockStateProvider(Blocks.DIRT.getDefaultState()))));
		
		registerConfiguredFeature(Names.SAND_MOUND,
			new ConfiguredFeature<>(this.blockMound.get(),
				new BlockStateProvidingFeatureConfig(new SimpleBlockStateProvider(Blocks.SAND.getDefaultState()))));
		
		registerConfiguredFeature(Names.GRAVEL_MOUND,
			new ConfiguredFeature<>(this.blockMound.get(),
				new BlockStateProvidingFeatureConfig(new SimpleBlockStateProvider(Blocks.GRAVEL.getDefaultState()))));
		
		registerConfiguredFeature(Names.COBBLESTONE_MOUND,
			new ConfiguredFeature<>(this.blockMound.get(),
				new BlockStateProvidingFeatureConfig(new SimpleBlockStateProvider(Blocks.COBBLESTONE.getDefaultState()))));
		
		registerConfiguredFeature(Names.STONE_MOUND,
			new ConfiguredFeature<>(this.blockMound.get(),
				new BlockStateProvidingFeatureConfig(new SimpleBlockStateProvider(Blocks.STONE.getDefaultState()))));
		
		registerConfiguredFeature(Names.SANDSTONE_MOUND,
			new ConfiguredFeature<>(this.blockMound.get(),
				new BlockStateProvidingFeatureConfig(new SimpleBlockStateProvider(Blocks.SANDSTONE.getDefaultState()))));
		
		// register configured features and structures
		this.desertQuarry = registerConfiguredStructure(
			Names.DESERT_QUARRY,
			this.quarry.get(),
			this.quarry.get()
				.withConfiguration(new LoadableJigsawConfig(new ResourceLocation(MODID, Names.DESERT_QUARRY_START), 7, 0, false, true)));
	}

	// called for each biome loaded when biomes are loaded
	void addThingsToBiomeOnBiomeLoad(BiomeLoadingEvent event)
	{
		if (event.getCategory() != Category.OCEAN)
		{
			// beware! Only one configured structure per structure instance can be added to a given biome
			event.getGeneration()
				.getStructures()
				.add(() -> this.desertQuarry);
		}
	}
	
	// TODO replace map with config later
	void addStructuresToWorld(WorldEvent.Load event, Map<RegistryKey<World>, List<RegistryObject<LoadableJigsawStructure>>> structuresByWorld)
	{
		// structures are weird and need a seperation setting registered for any worlds they generate in
		IWorld world = event.getWorld();
		if (world instanceof ServerWorld)
		{
			@SuppressWarnings("resource")
			ServerWorld serverWorld = (ServerWorld)world;
			ChunkGenerator chunkGenerator = serverWorld.getChunkProvider().getChunkGenerator();
			List<RegistryObject<LoadableJigsawStructure>> empty = new ArrayList<>();
			
			// make sure we don't spawn in flat worlds or other dimensions
			if (!(chunkGenerator instanceof FlatChunkGenerator))
//				&& serverWorld.getDimensionKey().equals(World.OVERWORLD))
			{
				List<RegistryObject<LoadableJigsawStructure>> structures = structuresByWorld.getOrDefault(serverWorld.getDimensionKey(), empty);
				// the separations map may be immutable,
				// so to add our separations, we need to copy the map and replace it
				Map<Structure<?>, StructureSeparationSettings> tempMap = new HashMap<>(chunkGenerator.func_235957_b_().field_236193_d_);
				for (RegistryObject<LoadableJigsawStructure> registeredStructure : structures)
				{
					LoadableJigsawStructure structure = registeredStructure.get();
					tempMap.put(structure, DimensionStructuresSettings.field_236191_b_.get(structure));
				}
				chunkGenerator.func_235957_b_().field_236193_d_ = tempMap;
			}
		}
	}
	
	// create and register a forge deferred register
	static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> registerRegister(IEventBus modBus, IForgeRegistry<T> registry)
	{
		DeferredRegister<T> register = DeferredRegister.create(registry, MODID);
		register.register(modBus);
		return register;
	}
	
	@SafeVarargs
	static RegistryObject<LoadableJigsawStructure> registerStructure(DeferredRegister<Structure<?>> structures,
		Map<RegistryKey<World>, List<RegistryObject<LoadableJigsawStructure>>> structuresByWorld,
		String name,
		Supplier<? extends LoadableJigsawStructure> factory,
		RegistryKey<World>...validWorlds)
	{
		RegistryObject<LoadableJigsawStructure> obj = structures.register(name, factory);
		for (RegistryKey<World> key : validWorlds)
		{
			structuresByWorld.computeIfAbsent(key, WorkshopsOfDoom::makeList)
				.add(obj);
		}
		return obj;
	}
	
	// saves us a lambda in the above method
	static List<RegistryObject<LoadableJigsawStructure>> makeList(Object foobar)
	{
		return new ArrayList<>();
	}
	
	static <FC extends IFeatureConfig, F extends Feature<FC>, CF extends ConfiguredFeature<FC,F>> CF registerConfiguredFeature(String name, CF feature)
	{
		WorldGenRegistries.register(WorldGenRegistries.CONFIGURED_FEATURE, new ResourceLocation(MODID, name), feature);
		return feature;
	}
	
	static <C extends IFeatureConfig, S extends Structure<C>, SF extends StructureFeature<C,? extends S>> SF registerConfiguredStructure(
		String name,
		S structure,
		SF configuredStructure)
	{
		WorldGenRegistries.register(WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE, new ResourceLocation(MODID, name), configuredStructure);
		
		// allegedly helps prevent custom chunk generators from crashing
		FlatGenerationSettings.STRUCTURES.put(structure, configuredStructure);
		
		return configuredStructure;
	}
	
	// register additional information needed for structures
	/**
	 * Register additional information needed for structures
	 * @param <STRUCTURE> The type of the structure
	 * @param structure A registered Structure instance
	 * @param transformSurroundingLand If true:<br>
	 * " Will add land at the base of the structure like it does for Villages and Outposts. "<br>
	 * " Doesn't work well on structure that have pieces stacked vertically or change in heights. "<br>
	 * ~TelepathicGrunt
	 * @param maxSpacing Maximum farapartness (in number of chunks) the structure placer will attempt to place this structure
	 * @param minSeparation Minimum farapartness (in number of chunks) the structure placer will attempt to place this structure
	 * @param salt A large, unique number to be added to the generation seed to prevent different structures from always being placed in the same spot
	 */
	static <STRUCTURE extends Structure<?>> void setStructureInfo(STRUCTURE structure, boolean transformSurroundingLand, int maxSpacing, int minSeparation, int salt)
	{
		Structure.NAME_STRUCTURE_BIMAP.put(structure.getRegistryName().toString(), structure);
		
		// " Will add land at the base of the structure like it does for Villages and Outposts. "
		// " Doesn't work well on structure that have pieces stacked vertically or change in heights. "
		// ~TelepathicGrunt
		// (vanilla only uses this for villages, outposts, and Nether Fossils)
		if (transformSurroundingLand)
		{
			Structure.field_236384_t_ = ImmutableList.<Structure<?>>builder()
				.addAll(Structure.field_236384_t_)
				.add(structure)
				.build();
		}
		
		StructureSeparationSettings seperation = new StructureSeparationSettings(maxSpacing, minSeparation, salt);
		
		DimensionStructuresSettings.field_236191_b_ =
			ImmutableMap.<Structure<?>, StructureSeparationSettings>builder()
				.putAll(DimensionStructuresSettings.field_236191_b_)
				.put(structure, seperation)
				.build();
	}
}