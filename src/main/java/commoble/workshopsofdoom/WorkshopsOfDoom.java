package commoble.workshopsofdoom;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;

import commoble.workshopsofdoom.biome_providers.BiomeCategoryProvider;
import commoble.workshopsofdoom.biome_providers.BiomeProvider;
import commoble.workshopsofdoom.biome_providers.BiomeTypeProvider;
import commoble.workshopsofdoom.biome_providers.BiomesProvider;
import commoble.workshopsofdoom.client.ClientInit;
import commoble.workshopsofdoom.data.FakeTagManager;
import commoble.workshopsofdoom.entities.ExcavatorEntity;
import commoble.workshopsofdoom.features.BlockMoundFeature;
import commoble.workshopsofdoom.features.SpawnEntityFeature;
import commoble.workshopsofdoom.features.SpawnLeashedEntityFeature;
import commoble.workshopsofdoom.loot_modifiers.AddTableModifier;
import commoble.workshopsofdoom.noise_settings_modifiers.AddStructureNoiseSettingsModifier;
import commoble.workshopsofdoom.noise_settings_modifiers.ApplyIfDimensionNoiseSettingsModifier;
import commoble.workshopsofdoom.noise_settings_modifiers.NoNoiseSettingsModifier;
import commoble.workshopsofdoom.noise_settings_modifiers.NoiseSettingsModifier;
import commoble.workshopsofdoom.pos_rule_tests.HeightInWorldTest;
import commoble.workshopsofdoom.rule_tests.AndRuleTest;
import commoble.workshopsofdoom.rule_tests.ChanceRuleTest;
import commoble.workshopsofdoom.structure_pieces.GroundFeatureJigsawPiece;
import commoble.workshopsofdoom.structure_pieces.RejiggableJigsawPiece;
import commoble.workshopsofdoom.structure_processors.EditPoolStructureProcessor;
import commoble.workshopsofdoom.structure_processors.HeightProcessor;
import commoble.workshopsofdoom.structure_processors.ItemFrameLootProcessor;
import commoble.workshopsofdoom.structure_processors.PredicatedStructureProcessor;
import commoble.workshopsofdoom.structure_processors.SetNBTStructureProcessor;
import commoble.workshopsofdoom.structures.FastJigsawStructure;
import commoble.workshopsofdoom.util.CodecJsonDataManager;
import commoble.workshopsofdoom.util.Codecs;
import commoble.workshopsofdoom.util.RegistryDispatcher;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.PillagerOutpostFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryObject;

@Mod(WorkshopsOfDoom.MODID)
public class WorkshopsOfDoom
{
	public static final String MODID = "workshopsofdoom";
	public static final Logger logger = LogManager.getLogger();
	public static WorkshopsOfDoom INSTANCE;
	
	// custom registries
	public static final RegistryDispatcher<NoiseSettingsModifier.Serializer<?>, NoiseSettingsModifier> NOISE_SETTINGS_MODIFIER_DISPATCHER = 
		RegistryDispatcher.<NoiseSettingsModifier.Serializer<?>, NoiseSettingsModifier>makeDispatchForgeRegistry(
			NoiseSettingsModifier.Serializer.class,
			new ResourceLocation(MODID, Names.NOISE_SETTINGS_MODIFIER),
			builder -> builder
				.disableSaving()
				.disableSync()
			);
	public static final RegistryDispatcher<BiomeProvider.Serializer<?>, BiomeProvider> BIOME_PROVIDER_DISPATCHER = 
		RegistryDispatcher.<BiomeProvider.Serializer<?>, BiomeProvider>makeDispatchForgeRegistry(
			BiomeProvider.Serializer.class,
			new ResourceLocation(MODID, Names.BIOME_PROVIDER),
			builder -> builder
				.disableSaving()
				.disableSync()
			);
	
	// data loaders
	public static final FakeTagManager<ResourceKey<Level>> DIMENSION_TAGS = new FakeTagManager<>(
		ResourceKey.elementKey(Registry.DIMENSION_REGISTRY),
		"tags/dimensions");
	public static final CodecJsonDataManager<NoiseSettingsModifier> NOISE_SETTINGS_MODIFIER_LOADER = new CodecJsonDataManager<>(
		Names.CONFIGURED_NOISE_SETTINGS_MODIFIERS,
		NoiseSettingsModifier.CODEC,
		logger);
	
	// forge registry objects
	public final RegistryObject<SpawnEggItem> excavatorSpawnEgg;
	public final RegistryObject<EntityType<ExcavatorEntity>> excavator;
	
	public final RegistryObject<FastJigsawStructure> desertQuarry;
	public final RegistryObject<FastJigsawStructure> plainsQuarry;
	public final RegistryObject<FastJigsawStructure> mountainsMines;
	public final RegistryObject<FastJigsawStructure> badlandsMines;
	public final RegistryObject<FastJigsawStructure> workshop;
	
	public final RegistryObject<NoiseSettingsModifier.Serializer<NoNoiseSettingsModifier>> noNoiseSettingsModifier;
	public final RegistryObject<NoiseSettingsModifier.Serializer<ApplyIfDimensionNoiseSettingsModifier>> applyIfDimensionNoiseSettingsModifer;
	public final RegistryObject<NoiseSettingsModifier.Serializer<AddStructureNoiseSettingsModifier>> addStructureNoiseSettingsModifier;
	
	public final RegistryObject<BiomeProvider.Serializer<BiomesProvider>> biomesProvider;
	public final RegistryObject<BiomeProvider.Serializer<BiomeCategoryProvider>> biomeCategoryProvider;
	public final RegistryObject<BiomeProvider.Serializer<BiomeTypeProvider>> biomeTypeProvider;
	
	public WorkshopsOfDoom() // invoked by forge due to @Mod
	{
		INSTANCE = this;
		
		// mod bus has modloading init events and registry events
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		// forge bus is for server starting events and in-game events
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// custom registries
		NOISE_SETTINGS_MODIFIER_DISPATCHER.subscribe(modBus);
		BIOME_PROVIDER_DISPATCHER.subscribe(modBus);
		
		// create and register deferred registers
		DeferredRegister<Item> items = registerRegister(modBus, ForgeRegistries.ITEMS);
		DeferredRegister<EntityType<?>> entities = registerRegister(modBus, ForgeRegistries.ENTITIES);
		DeferredRegister<Feature<?>> features = registerRegister(modBus, ForgeRegistries.FEATURES);
		DeferredRegister<StructureFeature<?>> structures = registerRegister(modBus, ForgeRegistries.STRUCTURE_FEATURES);
		DeferredRegister<NoiseSettingsModifier.Serializer<?>> noiseSettingsModifiers = registerRegister(modBus, NOISE_SETTINGS_MODIFIER_DISPATCHER);
		DeferredRegister<BiomeProvider.Serializer<?>> biomeProviders = registerRegister(modBus, BIOME_PROVIDER_DISPATCHER);
		DeferredRegister<GlobalLootModifierSerializer<?>> lootModifierSerializers = registerRegister(modBus, ForgeRegistries.LOOT_MODIFIER_SERIALIZERS);
		
		// register registry objects
		
		// register entities
		this.excavator = entities.register(Names.EXCAVATOR, () -> EntityType.Builder.<ExcavatorEntity>of(ExcavatorEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.clientTrackingRange(8)
			.build(new ResourceLocation(MODID, Names.EXCAVATOR).toString()));
		
		// register items
		this.excavatorSpawnEgg = items.register(Names.EXCAVATOR_SPAWN_EGG, () ->
			new ForgeSpawnEggItem(this.excavator, 0x884724, 0xacf228, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
		
		// register features
		features.register(Names.BLOCK_MOUND, () -> new BlockMoundFeature(BlockPileConfiguration.CODEC));
		features.register(Names.SPAWN_ENTITY, () -> new SpawnEntityFeature(SpawnEntityFeature.EntityConfig.CODEC));
		features.register(Names.SPAWN_LEASHED_ENTITY, () -> new SpawnLeashedEntityFeature(SpawnLeashedEntityFeature.LeashedEntityConfig.CODEC));
		
		// loot modifiers
		lootModifierSerializers.register(Names.ADD_TABLE, () -> new AddTableModifier.Serializer());
		
		// global noise modifiers
		this.noNoiseSettingsModifier = noiseSettingsModifiers.register(Names.NONE, () -> new NoiseSettingsModifier.Serializer<>(NoNoiseSettingsModifier.CODEC));
		this.applyIfDimensionNoiseSettingsModifer = noiseSettingsModifiers.register(Names.APPLY_IF_DIMENSION, () -> new NoiseSettingsModifier.Serializer<>(ApplyIfDimensionNoiseSettingsModifier.CODEC));
		this.addStructureNoiseSettingsModifier = noiseSettingsModifiers.register(Names.ADD_STRUCTURE, () -> new NoiseSettingsModifier.Serializer<>(AddStructureNoiseSettingsModifier.CODEC));

		// biome providers
		this.biomesProvider = biomeProviders.register(Names.BIOMES, () -> new BiomeProvider.Serializer<>(BiomesProvider.CODEC));
		this.biomeCategoryProvider = biomeProviders.register(Names.BIOME_CATEGORY, () -> new BiomeProvider.Serializer<>(BiomeCategoryProvider.CODEC));
		this.biomeTypeProvider = biomeProviders.register(Names.BIOME_TYPE, () -> new BiomeProvider.Serializer<>(BiomeTypeProvider.CODEC));
				

		Supplier<Supplier<FastJigsawStructure>> pillagerStructureFactory = () -> () -> new FastJigsawStructure(
			Codecs.JUMBO_JIGSAW_CONFIG_CODEC,
			0,
			true,
			PillagerOutpostFeature::checkLocation,
			GenerationStep.Decoration.SURFACE_STRUCTURES,
			false,
			true);

		this.desertQuarry = structures.register(Names.DESERT_QUARRY, pillagerStructureFactory.get());
		this.plainsQuarry = structures.register(Names.PLAINS_QUARRY, pillagerStructureFactory.get());
		this.mountainsMines = structures.register(Names.MOUNTAIN_MINES, pillagerStructureFactory.get());
		this.badlandsMines = structures.register(Names.BADLANDS_MINES, pillagerStructureFactory.get());
		this.workshop = structures.register(Names.WORKSHOP, pillagerStructureFactory.get());
		
		// add event listeners to event busses
		modBus.addListener(this::onCommonSetup);
		modBus.addListener(this::onRegisterEntityAttributes);
		
		forgeBus.addListener(this::onAddReloadListeners);
		forgeBus.addListener(this::onServerAboutToStart);
		forgeBus.addListener(EventPriority.HIGH, this::onWorldLoad);
		
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientInit.doClientInit(modBus, forgeBus);
		}
	}
	
	private void onCommonSetup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(this::afterCommonSetup);
	}
	
	private void afterCommonSetup()
	{
		// this needs to be called for each Structure instance
		// structures use this weird placement info per-world instead of feature placements
		setStructureInfo(this.desertQuarry.get());
		setStructureInfo(this.plainsQuarry.get());
		setStructureInfo(this.mountainsMines.get());
		setStructureInfo(this.badlandsMines.get());
		setStructureInfo(this.workshop.get());
		
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
	}
	
	private void onRegisterEntityAttributes(EntityAttributeCreationEvent event)
	{
		event.put(this.excavator.get(),
			Monster.createMonsterAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.35F)
				.add(Attributes.FOLLOW_RANGE, 12.0D)
				.add(Attributes.MAX_HEALTH, 24.0D)
				.add(Attributes.ATTACK_DAMAGE, 4.0D)
				.add(Attributes.ARMOR, 2.0D)
				.build());
	}
	
	private void onAddReloadListeners(AddReloadListenerEvent event)
	{
		event.addListener(DIMENSION_TAGS);
		event.addListener(NOISE_SETTINGS_MODIFIER_LOADER);
	}
	
	private void onServerAboutToStart(ServerAboutToStartEvent event)
	{
//		MinecraftServer server = event.getServer();
//		RegistryAccess registryAccess = server.registryAccess();
//		
//		NoiseGeneratorSettings overworldNoise = registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).get(NoiseGeneratorSettings.OVERWORLD);
//		var overworldStructures = overworldNoise.structureSettings();
//		NoiseGeneratorSettings netherNoise = registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).get(NoiseGeneratorSettings.NETHER);
//		var netherStructures = netherNoise.structureSettings();
//		NoiseGeneratorSettings largeNoise = registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).get(NoiseGeneratorSettings.LARGE_BIOMES);
//		var largeStructures = largeNoise.structureSettings();
//		Biome desert = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(Biomes.DESERT);
//		
//		// get all the noise settings and all the noise settings modifiers and apply all of the modifiers to all of the settings
//		for (var noiseSettingsEntry : registryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).entrySet())
//		{
//			var noiseSettingsKey = noiseSettingsEntry.getKey();
//			var noiseSettings = noiseSettingsEntry.getValue();
//			for (var modifier : registryAccess.registryOrThrow(WorkshopsOfDoom.CONFIGURED_NOISE_SETTINGS_MODIFIER_REGISTRY_KEY))
//			{
//				modifier.modify(noiseSettingsKey, noiseSettings);
//			}
//		}
	}
	
	// noise settings are still jank
	// they're not deep-copied like other dynamic registry objects on server start,
	// chunkgenerators use them from builtinregistries for some reason (unless, presumably, a datapack overrides them)
	// so modifications will persist across multiple world create/joins
	// we don't want this!
	// so we'll continue doing it on world load for now,
	// where we can at least give each chunk generator its own copy
	private void onWorldLoad(WorldEvent.Load event)
	{
		var world = event.getWorld();
		if (world instanceof ServerLevel serverLevel)
		{			
			for (var modifier : NOISE_SETTINGS_MODIFIER_LOADER.values())
			{
				modifier.modify(serverLevel);
			}
		}
	}
	
	// create and register a forge deferred register
	private static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> registerRegister(IEventBus modBus, IForgeRegistry<T> registry)
	{
		DeferredRegister<T> register = DeferredRegister.create(registry, MODID);
		register.register(modBus);
		return register;
	}
	
	private static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> registerRegister(IEventBus modBus, RegistryDispatcher<T,?> dispatcher)
	{
		DeferredRegister<T> register = dispatcher.makeDeferredRegister(MODID);
		register.register(modBus);
		return register;
	}
	
	// register a thing to a vanilla Registry (not worldgen registries)
	private static <T> T registerVanilla(Registry<T> registry, String name, T thing)
	{
		return Registry.register(registry, new ResourceLocation(MODID, name), thing);
	}
	
	/**
	 * Register additional information needed for structures
	 * @param <STRUCTURE> The type of the structure
	 * @param structure A registered Structure instance (LoadableJigsawStructure has extra fields for structure registry)
	 * */
	private static <STRUCTURE extends FastJigsawStructure> void setStructureInfo(STRUCTURE structure)
	{
		StructureFeature.STRUCTURES_REGISTRY.put(structure.getRegistryName().toString(), structure);
		
		// " Will add land at the base of the structure like it does for Villages and Outposts. "
		// " Doesn't work well on structure that have pieces stacked vertically or change in heights. "
		// ~TelepathicGrunt
		// (vanilla only uses this for villages, outposts, and Nether Fossils)
		if (structure.transformSurroundingLand)
		{
			StructureFeature.NOISE_AFFECTING_FEATURES = ImmutableList.<StructureFeature<?>>builder()
				.addAll(StructureFeature.NOISE_AFFECTING_FEATURES)
				.add(structure)
				.build();
		}
	}
}