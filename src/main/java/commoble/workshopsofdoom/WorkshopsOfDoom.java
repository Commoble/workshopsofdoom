package commoble.workshopsofdoom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import commoble.workshopsofdoom.client.ClientInit;
import commoble.workshopsofdoom.entities.ExcavatorEntity;
import commoble.workshopsofdoom.features.BlockMoundFeature;
import commoble.workshopsofdoom.features.SpawnEntityFeature;
import commoble.workshopsofdoom.features.SpawnLeashedEntityFeature;
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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(WorkshopsOfDoom.MODID)
public class WorkshopsOfDoom
{
	public static final String MODID = "workshopsofdoom";
	public static final Logger logger = LogManager.getLogger();
	public static WorkshopsOfDoom INSTANCE;
	
	public static class ResourceLocations
	{
		private static final ResourceLocation wsdrl(String name) { return new ResourceLocation(MODID, name); }
		
		public static final ResourceLocation DESERT_QUARRY = wsdrl(Names.DESERT_QUARRY);
		public static final ResourceLocation PLAINS_QUARRY = wsdrl(Names.PLAINS_QUARRY);
		public static final ResourceLocation MOUNTAIN_MINES = wsdrl(Names.MOUNTAIN_MINES);
		public static final ResourceLocation BADLANDS_MINES = wsdrl(Names.BADLANDS_MINES);
		public static final ResourceLocation WORKSHOP = wsdrl(Names.WORKSHOP);
		
		public static final ResourceLocation HAS_DESERT_QUARRY = wsdrl(Names.HAS_DESERT_QUARRY);
		public static final ResourceLocation HAS_PLAINS_QUARRY = wsdrl(Names.HAS_PLAINS_QUARRY);
		public static final ResourceLocation HAS_MOUNTAIN_MINES = wsdrl(Names.HAS_MOUNTAIN_MINES);
		public static final ResourceLocation HAS_BADLANDS_MINES = wsdrl(Names.HAS_BADLANDS_MINES);
		public static final ResourceLocation HAS_WORKSHOP = wsdrl(Names.WORKSHOP);
		
		public static final ResourceLocation DESERT_QUARRY_START = wsdrl(Names.DESERT_QUARRY_START);
		public static final ResourceLocation PLAINS_QUARRY_START = wsdrl(Names.PLAINS_QUARRY_START);
		public static final ResourceLocation MOUNTAIN_MINES_START = wsdrl(Names.MOUNTAIN_MINES_START);
		public static final ResourceLocation BADLANDS_MINES_START = wsdrl(Names.BADLANDS_MINES_START);
		public static final ResourceLocation WORKSHOP_START = wsdrl(Names.WORKSHOP_START);
		
		public static final ResourceLocation QUARRIES = wsdrl(Names.QUARRIES);
		public static final ResourceLocation MINES = wsdrl(Names.MINES);
		public static final ResourceLocation EXCAVATIONS = wsdrl(Names.EXCAVATIONS);
		public static final ResourceLocation WORKSHOPS = wsdrl(Names.WORKSHOPS);
	}
	
	public static class Tags
	{		
		public static class Biomes
		{
			private static TagKey<Biome> tag(ResourceLocation id)
			{
				return TagKey.create(Registries.BIOME, id);
			}
			
			public static final TagKey<Biome> HAS_DESERT_QUARRY = tag(ResourceLocations.HAS_DESERT_QUARRY);
			public static final TagKey<Biome> HAS_PLAINS_QUARRY = tag(ResourceLocations.HAS_PLAINS_QUARRY);
			public static final TagKey<Biome> HAS_MOUNTAIN_MINES = tag(ResourceLocations.HAS_MOUNTAIN_MINES);
			public static final TagKey<Biome> HAS_BADLANDS_MINES = tag(ResourceLocations.HAS_BADLANDS_MINES);
			public static final TagKey<Biome> HAS_WORKSHOP = tag(ResourceLocations.HAS_WORKSHOP);
		}
		
		public static class Structures
		{
			private static TagKey<Structure> tag(ResourceLocation id)
			{
				return TagKey.create(Registries.STRUCTURE, id);
			}
			
			public static final TagKey<Structure> QUARRIES = tag(ResourceLocations.QUARRIES);
			public static final TagKey<Structure> MINES = tag(ResourceLocations.MINES);
			public static final TagKey<Structure> EXCAVATIONS = tag(ResourceLocations.EXCAVATIONS);
			public static final TagKey<Structure> WORKSHOPS = tag(ResourceLocations.WORKSHOPS);
		}
	}
	
	public static class Keys
	{
		public static class Structures
		{
			private static final ResourceKey<Structure> k(ResourceLocation id)
			{
				return ResourceKey.create(Registries.STRUCTURE, id);
			}
			
			public static final ResourceKey<Structure> DESERT_QUARRY = k(ResourceLocations.DESERT_QUARRY);
			public static final ResourceKey<Structure> PLAINS_QUARRY = k(ResourceLocations.PLAINS_QUARRY);
			public static final ResourceKey<Structure> MOUNTAIN_MINES = k(ResourceLocations.MOUNTAIN_MINES);
			public static final ResourceKey<Structure> BADLANDS_MINES = k(ResourceLocations.BADLANDS_MINES);
			public static final ResourceKey<Structure> WORKSHOP = k(ResourceLocations.WORKSHOP);
		}
		
		public static class StructureSets
		{
			private static final ResourceKey<StructureSet> k(ResourceLocation id)
			{
				return ResourceKey.create(Registries.STRUCTURE_SET, id);
			}
			
			public static final ResourceKey<StructureSet> QUARRIES = k(ResourceLocations.QUARRIES);
			public static final ResourceKey<StructureSet> MINES = k(ResourceLocations.MINES);
			public static final ResourceKey<StructureSet> WORKSHOPS = k(ResourceLocations.WORKSHOPS);
		}
	}
	
	
	// forge registry objects
	public final RegistryObject<SpawnEggItem> excavatorSpawnEgg;
	public final RegistryObject<EntityType<ExcavatorEntity>> excavator;
	public final RegistryObject<StructureType<FastJigsawStructure>> fastJigsawStructure;
	public final RegistryObject<StructurePoolElementType<GroundFeatureJigsawPiece>> groundFeatureJigsawPiece;
	public final RegistryObject<StructurePoolElementType<RejiggableJigsawPiece>> rejiggableJigsawPiece;
	public final RegistryObject<StructureProcessorType<EditPoolStructureProcessor>> editPoolStructureProcessor;
	public final RegistryObject<StructureProcessorType<SetNBTStructureProcessor>> setNbtStructureProcessor;
	public final RegistryObject<StructureProcessorType<PredicatedStructureProcessor>> predicatedStructureProcessor;
	public final RegistryObject<StructureProcessorType<HeightProcessor>> heightProcessor;
	public final RegistryObject<StructureProcessorType<ItemFrameLootProcessor>> itemFrameLootProcessor;
	public final RegistryObject<RuleTestType<ChanceRuleTest>> chanceRuleTest;
	public final RegistryObject<RuleTestType<AndRuleTest>> andRuleTest;
	public final RegistryObject<PosRuleTestType<HeightInWorldTest>> heightInWorldTest;
	
	public WorkshopsOfDoom() // invoked by forge due to @Mod
	{
		INSTANCE = this;
		
		// mod bus has modloading init events and registry events
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		// forge bus is for server starting events and in-game events
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// create and register deferred registers
		DeferredRegister<Item> items = registerRegister(modBus, Registries.ITEM);
		DeferredRegister<EntityType<?>> entities = registerRegister(modBus, Registries.ENTITY_TYPE);
		DeferredRegister<Feature<?>> features = registerRegister(modBus, Registries.FEATURE);
		DeferredRegister<StructureType<?>> structures = registerRegister(modBus, Registries.STRUCTURE_TYPE);
		DeferredRegister<StructurePoolElementType<?>> structurePoolElements = registerRegister(modBus, Registries.STRUCTURE_POOL_ELEMENT);
		DeferredRegister<StructureProcessorType<?>> structureProcessors = registerRegister(modBus, Registries.STRUCTURE_PROCESSOR);
		DeferredRegister<RuleTestType<?>> ruleTests = registerRegister(modBus, Registries.RULE_TEST);
		DeferredRegister<PosRuleTestType<?>> posRuleTests = registerRegister(modBus, Registries.POS_RULE_TEST);
		
		// register registry objects
		
		// register entities
		this.excavator = entities.register(Names.EXCAVATOR, () -> EntityType.Builder.<ExcavatorEntity>of(ExcavatorEntity::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.clientTrackingRange(8)
			.build(new ResourceLocation(MODID, Names.EXCAVATOR).toString()));
		
		// register items
		this.excavatorSpawnEgg = items.register(Names.EXCAVATOR_SPAWN_EGG, () ->
			new ForgeSpawnEggItem(this.excavator, 0x884724, 0xacf228, new Item.Properties()));
		
		// register features
		features.register(Names.BLOCK_MOUND, () -> new BlockMoundFeature(BlockPileConfiguration.CODEC));
		features.register(Names.SPAWN_ENTITY, () -> new SpawnEntityFeature(SpawnEntityFeature.EntityConfig.CODEC));
		features.register(Names.SPAWN_LEASHED_ENTITY, () -> new SpawnLeashedEntityFeature(SpawnLeashedEntityFeature.LeashedEntityConfig.CODEC));
		
		this.fastJigsawStructure = structures.<StructureType<FastJigsawStructure>>register(Names.FAST_JIGSAW, () -> () -> FastJigsawStructure.CODEC);
		
		this.groundFeatureJigsawPiece = structurePoolElements.register(Names.GROUND_FEATURE_POOL_ELEMENT, () -> () -> GroundFeatureJigsawPiece.CODEC);
		this.rejiggableJigsawPiece = structurePoolElements.register(Names.REJIGGABLE_POOL_ELEMENT, () -> () -> RejiggableJigsawPiece.CODEC);
		this.editPoolStructureProcessor = structureProcessors.register(Names.EDIT_POOL, () -> () -> EditPoolStructureProcessor.CODEC);
		this.setNbtStructureProcessor = structureProcessors.register(Names.SET_NBT, () -> () -> SetNBTStructureProcessor.CODEC);
		this.predicatedStructureProcessor = structureProcessors.register(Names.PREDICATE, () -> () -> PredicatedStructureProcessor.CODEC);
		this.heightProcessor = structureProcessors.register(Names.HEIGHT_PROCESSOR, () -> () -> HeightProcessor.CODEC);
		this.itemFrameLootProcessor = structureProcessors.register(Names.ITEM_FRAME_LOOT, () -> () -> ItemFrameLootProcessor.CODEC);
		this.chanceRuleTest = ruleTests.register(Names.RANDOM_CHANCE, () -> () -> ChanceRuleTest.CODEC);
		this.andRuleTest = ruleTests.register(Names.AND, () -> () -> AndRuleTest.CODEC);
		this.heightInWorldTest = posRuleTests.register(Names.HEIGHT, () -> () -> HeightInWorldTest.CODEC);
		
		// add event listeners to event busses
		modBus.addListener(this::onRegisterEntityAttributes);
		modBus.addListener(this::onBuildTabContents);
		
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientInit.doClientInit(modBus, forgeBus);
		}
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
	
	private void onBuildTabContents(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS)
		{
			event.accept(this.excavatorSpawnEgg);
		}
	}
	
	// create and register a forge deferred register
	private static <T> DeferredRegister<T> registerRegister(IEventBus modBus, ResourceKey<Registry<T>> registryKey)
	{
		DeferredRegister<T> register = DeferredRegister.create(registryKey, MODID);
		register.register(modBus);
		return register;
	}
}