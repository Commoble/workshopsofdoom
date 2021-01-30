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

import commoble.workshopsofdoom.client.ClientInit;
import commoble.workshopsofdoom.entities.ExcavatorEntity;
import commoble.workshopsofdoom.features.BlockMoundFeature;
import commoble.workshopsofdoom.features.SpawnEntityFeature;
import commoble.workshopsofdoom.features.SpawnLeashedEntityFeature;
import commoble.workshopsofdoom.pos_rule_tests.HeightInWorldTest;
import commoble.workshopsofdoom.rule_tests.AndRuleTest;
import commoble.workshopsofdoom.rule_tests.ChanceRuleTest;
import commoble.workshopsofdoom.structure_pieces.GroundFeatureJigsawPiece;
import commoble.workshopsofdoom.structure_pieces.ItemFrameLootProcessor;
import commoble.workshopsofdoom.structure_pieces.RejiggableJigsawPiece;
import commoble.workshopsofdoom.structure_processors.EditPoolStructureProcessor;
import commoble.workshopsofdoom.structure_processors.HeightProcessor;
import commoble.workshopsofdoom.structure_processors.PredicatedStructureProcessor;
import commoble.workshopsofdoom.structure_processors.SetNBTStructureProcessor;
import commoble.workshopsofdoom.structures.LoadableJigsawStructure;
import commoble.workshopsofdoom.structures.LoadableJigsawStructure.LoadableJigsawConfig;
import commoble.workshopsofdoom.structures.PillagerJigsawStructure;
import commoble.workshopsofdoom.util.ConfigHelper;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.BlockStateProvidingFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
	
	public final ServerConfig serverConfig;
	public final CommonConfig commonConfig;
	
	// forge registry objects
	public final RegistryObject<SpawnEggItem> excavatorSpawnEgg;
	public final RegistryObject<EntityType<ExcavatorEntity>> excavator;
	
	public final RegistryObject<PillagerJigsawStructure> desertQuarry;
	public final RegistryObject<PillagerJigsawStructure> plainsQuarry;
	public final RegistryObject<PillagerJigsawStructure> mountainsMines;
	public final RegistryObject<PillagerJigsawStructure> badlandsMines;
	public final RegistryObject<PillagerJigsawStructure> workshop;
	
	// vanilla registry objects that we can't register in the mod constructor due to being off-thread
	public StructureFeature<LoadableJigsawConfig, ? extends Structure<LoadableJigsawConfig>> configuredDesertQuarry = null;
	public StructureFeature<LoadableJigsawConfig, ? extends Structure<LoadableJigsawConfig>> configuredPlainsQuarry = null;
	public StructureFeature<LoadableJigsawConfig, ? extends Structure<LoadableJigsawConfig>> configuredMountainsMines = null;
	public StructureFeature<LoadableJigsawConfig, ? extends Structure<LoadableJigsawConfig>> configuredBadlandsMines = null;
	public StructureFeature<LoadableJigsawConfig, ? extends Structure<LoadableJigsawConfig>> configuredWorkshop = null;
	
	public WorkshopsOfDoom() // invoked by forge due to @Mod
	{
		INSTANCE = this;
		
		// mod bus has modloading init events and registry events
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		// forge bus is for server starting events and in-game events
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// create and register deferred registers
		DeferredRegister<Item> items = registerRegister(modBus, ForgeRegistries.ITEMS);
		DeferredRegister<EntityType<?>> entities = registerRegister(modBus, ForgeRegistries.ENTITIES);
		DeferredRegister<Feature<?>> features = registerRegister(modBus, ForgeRegistries.FEATURES);
		DeferredRegister<Structure<?>> structures = registerRegister(modBus, ForgeRegistries.STRUCTURE_FEATURES);
		
		// get a temporary list of structures to finagle vanilla registries with later
		Map<RegistryKey<World>, List<RegistryObject<? extends LoadableJigsawStructure>>> structuresByWorld = new HashMap<>();
		
		// static init entities so we can make spawn eggs for them
		EntityType<ExcavatorEntity> initExcavator = EntityType.Builder.<ExcavatorEntity>create(ExcavatorEntity::new, EntityClassification.MONSTER)
			.size(0.6F, 1.95F)
			.trackingRange(8)
			.build(new ResourceLocation(MODID, Names.EXCAVATOR).toString());
		
		// register registry objects
		// register items
			// for spawn eggs, we need the instance of the entity type before the entity type registry event
		this.excavatorSpawnEgg = items.register(Names.EXCAVATOR_SPAWN_EGG, () ->
			new SpawnEggItem(initExcavator, 0x884724, 0xacf228, new Item.Properties().group(ItemGroup.MISC)));
		// register entities -- just use the existing entity types we already made for spawn eggs
		this.excavator = entities.register(Names.EXCAVATOR, () -> initExcavator);
		
		// register features
		features.register(Names.BLOCK_MOUND, () -> new BlockMoundFeature(BlockStateProvidingFeatureConfig.field_236453_a_));
		features.register(Names.SPAWN_ENTITY, () -> new SpawnEntityFeature(SpawnEntityFeature.EntityConfig.CODEC));
		features.register(Names.SPAWN_LEASHED_ENTITY, () -> new SpawnLeashedEntityFeature(SpawnLeashedEntityFeature.LeashedEntityConfig.CODEC));

		// server config needs to be initialized after entity registry objects intialize but before structures
		this.commonConfig = ConfigHelper.register(ModConfig.Type.COMMON, CommonConfig::new);
		this.serverConfig = ConfigHelper.register(ModConfig.Type.SERVER, ServerConfig::new);
		
		List<Spawners> noSpawnList = ImmutableList.of();
		Supplier<List<Spawners>> noSpawns = () -> noSpawnList;
		
		// structures are added to a list for later vanilla-registry-finagling
		// (the big numbers we use for the placement salts here were generated
		// by taking the hashcode of their registry name
		this.desertQuarry = registerStructure(structures, structuresByWorld, Names.DESERT_QUARRY,
			() -> new PillagerJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES, true,
				() -> this.serverConfig.desertQuarryMonsters.get().get(),
				noSpawns,
				420764282,
				false),
			World.OVERWORLD);
		this.plainsQuarry = registerStructure(structures, structuresByWorld, Names.PLAINS_QUARRY,
			() -> new PillagerJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES, true,
				() -> this.serverConfig.plainsQuarryMonsters.get().get(),
				noSpawns,
				489846822,
				false),
			World.OVERWORLD);
		this.mountainsMines = registerStructure(structures, structuresByWorld, Names.MOUNTAIN_MINES,
			() -> new PillagerJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES, true,
				() -> this.serverConfig.mountainMinesMonsters.get().get(),
				noSpawns,
				305511170,
				false),
			World.OVERWORLD);
		this.badlandsMines = registerStructure(structures, structuresByWorld, Names.BADLANDS_MINES,
			() -> new PillagerJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES, true,
				() -> this.serverConfig.badlandsMinesMonsters.get().get(),
				noSpawns,
				219011832,
				false),
			World.OVERWORLD);
		this.workshop = registerStructure(structures, structuresByWorld, Names.WORKSHOP,
			() -> new PillagerJigsawStructure(LoadableJigsawConfig.CODEC, GenerationStage.Decoration.SURFACE_STRUCTURES, true,
				noSpawns,
				noSpawns,
				567764539,
				false),
			World.OVERWORLD);
		
		// add event listeners to event busses
		modBus.addListener(this::onCommonSetup);

		// per forge recommendations, adding things to biomes should be done in the HIGH phase
		forgeBus.addListener(EventPriority.HIGH, this::addThingsToBiomeOnBiomeLoad);
		
		Consumer<WorldEvent.Load> addStructuresToWorldListener = event -> this.addStructuresToWorld(event,structuresByWorld);
		forgeBus.addListener(addStructuresToWorldListener);
//		forgeBus.addListener(this::onChunkLoad);
		
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientInit.doClientInit(modBus, forgeBus);
		}
	}
	
//	void onChunkLoad(ChunkEvent.Load event)
//	{
//		IChunk chunk = event.getChunk();
//		chunk.getTileEntitiesPos().forEach(pos ->{
//			TileEntity te = chunk.getTileEntity(pos);
//			if (te instanceof StructureBlockTileEntity)
//			{
//				StructureBlockTileEntity block = (StructureBlockTileEntity) te;
//				block.setName(new ResourceLocation(block.getName().toString().replace("desert", "__biome__")));
////				JigsawTileEntity jigsaw = (JigsawTileEntity) te;
////				String oldPool = jigsaw.func_235670_g_().toString(); //
////				String newPool = oldPool.replace("default", "__biome__");
////				jigsaw.func_235667_c_(new ResourceLocation(newPool));
//			}
//		});
//	}
	
	void onCommonSetup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(this::afterCommonSetup);
	}
	
	void afterCommonSetup()
	{
		// set entity attributes
		GlobalEntityTypeAttributes.put(this.excavator.get(), ExcavatorEntity.getAttributes().create());

		// add spawn egg behaviours to dispenser
		DefaultDispenseItemBehavior spawnEggBehavior = new DefaultDispenseItemBehavior()
		{
			/**
			 * Dispense the specified stack, play the dispense sound and spawn particles.
			 */
			@Override
			public ItemStack dispenseStack(IBlockSource source, ItemStack stack)
			{
				Direction direction = source.getBlockState().get(DispenserBlock.FACING);
				EntityType<?> entitytype = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
				entitytype.spawn(source.getWorld(), stack, (PlayerEntity) null, source.getBlockPos().offset(direction), SpawnReason.DISPENSER, direction != Direction.UP, false);
				stack.shrink(1);
				return stack;
			}
		};
		
		DispenserBlock.registerDispenseBehavior(this.excavatorSpawnEgg.get(), spawnEggBehavior);
		
		// this needs to be called for each Structure instance
		// structures use this weird placement info per-world instead of feature placements
		setStructureInfo(this.desertQuarry.get(), this.commonConfig.desertQuarry.get());
		setStructureInfo(this.plainsQuarry.get(), this.commonConfig.plainsQuarry.get());
		setStructureInfo(this.mountainsMines.get(), this.commonConfig.mountainsMines.get());
		setStructureInfo(this.badlandsMines.get(), this.commonConfig.badlandsMines.get());
		setStructureInfo(this.workshop.get(), this.commonConfig.workshop.get());
		
		// register to forgeless vanilla registries
		registerVanilla(Registry.STRUCTURE_POOL_ELEMENT, Names.GROUND_FEATURE_POOL_ELEMENT, GroundFeatureJigsawPiece.DESERIALIZER);
		registerVanilla(Registry.STRUCTURE_POOL_ELEMENT, Names.REJIGGABLE_POOL_ELEMENT, RejiggableJigsawPiece.DESERIALIZER);
		registerVanilla(Registry.STRUCTURE_PROCESSOR, Names.EDIT_POOL, EditPoolStructureProcessor.DESERIALIZER);
		registerVanilla(Registry.STRUCTURE_PROCESSOR, Names.SET_NBT, SetNBTStructureProcessor.DESERIALIZER);
		registerVanilla(Registry.STRUCTURE_PROCESSOR, Names.PREDICATE, PredicatedStructureProcessor.DESERIALIZER);
		registerVanilla(Registry.STRUCTURE_PROCESSOR, Names.HEIGHT_PROCESSOR, HeightProcessor.DESERIALIZER);
		registerVanilla(Registry.STRUCTURE_PROCESSOR, Names.ITEM_FRAME_LOOT, ItemFrameLootProcessor.DESERIALIZER);
		registerVanilla(Registry.RULE_TEST, Names.RANDOM_CHANCE, ChanceRuleTest.DESERIALIZER);
		registerVanilla(Registry.RULE_TEST, Names.AND, AndRuleTest.DESERIALIZER);
		registerVanilla(Registry.POS_RULE_TEST, Names.HEIGHT, HeightInWorldTest.DESERIALIZER);
		
		// register configured structures
		this.configuredDesertQuarry = registerConfiguredStructure(
			Names.DESERT_QUARRY,
			this.desertQuarry.get(),
			this.desertQuarry.get()
				.withConfiguration(this.commonConfig.desertQuarry.get()));
		
		this.configuredPlainsQuarry = registerConfiguredStructure(
			Names.PLAINS_QUARRY,
			this.plainsQuarry.get(),
			this.plainsQuarry.get()
				.withConfiguration(this.commonConfig.plainsQuarry.get()));
		
		this.configuredMountainsMines = registerConfiguredStructure(
			Names.MOUNTAIN_MINES,
			this.mountainsMines.get(),
			this.mountainsMines.get()
				.withConfiguration(this.commonConfig.mountainsMines.get()));
		
		this.configuredBadlandsMines = registerConfiguredStructure(
			Names.BADLANDS_MINES,
			this.badlandsMines.get(),
			this.badlandsMines.get()
				.withConfiguration(this.commonConfig.badlandsMines.get()));
		this.configuredWorkshop = registerConfiguredStructure(
			Names.WORKSHOP,
			this.workshop.get(),
			this.workshop.get()
				.withConfiguration(this.commonConfig.workshop.get()));
	}

	// called for each biome loaded when biomes are loaded
	// TODO replace with config
	void addThingsToBiomeOnBiomeLoad(BiomeLoadingEvent event)
	{
		Consumer<StructureFeature<?,?>> structureAdder = structure -> 
			event.getGeneration()
			.getStructures()
			.add(() -> structure);
			
		// beware! Only one configured structure per structure instance can be added to a given biome
		if (event.getCategory() == Category.DESERT)
		{
			structureAdder.accept(this.configuredDesertQuarry);
		}
		else if (event.getCategory() == Category.PLAINS)
		{
			structureAdder.accept(this.configuredPlainsQuarry);
		}
		else if (event.getCategory() == Category.EXTREME_HILLS)
		{
			structureAdder.accept(this.configuredMountainsMines);
		}
		else if (event.getCategory() == Category.MESA)
		{
			structureAdder.accept(this.configuredBadlandsMines);
		}
		else if (event.getCategory() == Category.ICY)
		{
			structureAdder.accept(this.configuredWorkshop);
		}
	}
	
	// TODO replace map with config later
	void addStructuresToWorld(WorldEvent.Load event, Map<RegistryKey<World>, List<RegistryObject<? extends LoadableJigsawStructure>>> structuresByWorld)
	{
		// structures are weird and need a seperation setting registered for any worlds they generate in
		IWorld world = event.getWorld();
		if (world instanceof ServerWorld)
		{
			@SuppressWarnings("resource")
			ServerWorld serverWorld = (ServerWorld)world;
			ChunkGenerator chunkGenerator = serverWorld.getChunkProvider().getChunkGenerator();
			List<RegistryObject<? extends LoadableJigsawStructure>> empty = new ArrayList<>();
			
			// make sure we don't spawn in flat worlds or other dimensions
			if (!(chunkGenerator instanceof FlatChunkGenerator))
//				&& serverWorld.getDimensionKey().equals(World.OVERWORLD))
			{
				List<RegistryObject<? extends LoadableJigsawStructure>> structures = structuresByWorld.getOrDefault(serverWorld.getDimensionKey(), empty);
				// the separations map may be immutable,
				// so to add our separations, we need to copy the map and replace it
				Map<Structure<?>, StructureSeparationSettings> tempMap = new HashMap<>(chunkGenerator.func_235957_b_().field_236193_d_);
				for (RegistryObject<? extends LoadableJigsawStructure> registeredStructure : structures)
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
	
	// register a thing to a vanilla Registry (not worldgen registries)
	static <T> T registerVanilla(Registry<T> registry, String name, T thing)
	{
		return Registry.register(registry, new ResourceLocation(MODID, name), thing);
	}
	
	@SafeVarargs
	static <STRUCTURE extends LoadableJigsawStructure> RegistryObject<STRUCTURE> registerStructure(DeferredRegister<Structure<?>> structures,
		Map<RegistryKey<World>, List<RegistryObject<? extends LoadableJigsawStructure>>> structuresByWorld,
		String name,
		Supplier<STRUCTURE> factory,
		RegistryKey<World>...validWorlds)
	{
		RegistryObject<STRUCTURE> obj = structures.register(name, factory);
		for (RegistryKey<World> key : validWorlds)
		{
			structuresByWorld.computeIfAbsent(key, WorkshopsOfDoom::makeList)
				.add(obj);
		}
		return obj;
	}
	
	// saves us a lambda in the above method
	static List<RegistryObject<? extends LoadableJigsawStructure>> makeList(Object foobar)
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
	 * 
	 * */
	static <STRUCTURE extends LoadableJigsawStructure> void setStructureInfo(STRUCTURE structure, LoadableJigsawConfig config)
	{
		Structure.NAME_STRUCTURE_BIMAP.put(structure.getRegistryName().toString(), structure);
		
		// " Will add land at the base of the structure like it does for Villages and Outposts. "
		// " Doesn't work well on structure that have pieces stacked vertically or change in heights. "
		// ~TelepathicGrunt
		// (vanilla only uses this for villages, outposts, and Nether Fossils)
		if (structure.transformSurroundingLand)
		{
			Structure.field_236384_t_ = ImmutableList.<Structure<?>>builder()
				.addAll(Structure.field_236384_t_)
				.add(structure)
				.build();
		}
		
		StructureSeparationSettings seperation = new StructureSeparationSettings(config.getMaxSeparation(), config.getMinSeparation(), structure.placementSalt);
		
		DimensionStructuresSettings.field_236191_b_ =
			ImmutableMap.<Structure<?>, StructureSeparationSettings>builder()
				.putAll(DimensionStructuresSettings.field_236191_b_)
				.put(structure, seperation)
				.build();
	}
}