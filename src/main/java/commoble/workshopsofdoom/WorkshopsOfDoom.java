package commoble.workshopsofdoom;

import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;

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
import commoble.workshopsofdoom.structures.LoadableJigsawStructure;
import commoble.workshopsofdoom.util.Codecs;
import commoble.workshopsofdoom.util.ConfigHelper;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.PillagerOutpostFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
	
	public final ServerConfig serverConfig;
	
	// forge registry objects
	public final RegistryObject<SpawnEggItem> excavatorSpawnEgg;
	public final RegistryObject<EntityType<ExcavatorEntity>> excavator;
	
	public final RegistryObject<LoadableJigsawStructure> desertQuarry;
	public final RegistryObject<LoadableJigsawStructure> plainsQuarry;
	public final RegistryObject<LoadableJigsawStructure> mountainsMines;
	public final RegistryObject<LoadableJigsawStructure> badlandsMines;
	public final RegistryObject<LoadableJigsawStructure> workshop;
	
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
		DeferredRegister<StructureFeature<?>> structures = registerRegister(modBus, ForgeRegistries.STRUCTURE_FEATURES);
		
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

		// server config needs to be initialized after entity registry objects intialize but before structures
		this.serverConfig = ConfigHelper.register(ModConfig.Type.SERVER, ServerConfig::new);
		
		List<SpawnerData> noSpawnList = ImmutableList.of();
		Supplier<List<SpawnerData>> noSpawns = () -> noSpawnList;

		this.desertQuarry = structures.register(Names.DESERT_QUARRY,
			makePillagerStructureFactory(() -> this.serverConfig.desertQuarryMonsters.get().get(), noSpawns));
		this.plainsQuarry = structures.register(Names.PLAINS_QUARRY,
			makePillagerStructureFactory(() -> this.serverConfig.plainsQuarryMonsters.get().get(), noSpawns));
		this.mountainsMines = structures.register(Names.MOUNTAIN_MINES,
			makePillagerStructureFactory(() -> this.serverConfig.mountainMinesMonsters.get().get(), noSpawns));
		this.badlandsMines = structures.register(Names.BADLANDS_MINES,
			makePillagerStructureFactory(() -> this.serverConfig.badlandsMinesMonsters.get().get(), noSpawns));
		this.workshop = structures.register(Names.WORKSHOP,
			makePillagerStructureFactory(noSpawns, noSpawns));
		
		// add event listeners to event busses
		modBus.addListener(this::onCommonSetup);
		modBus.addListener(this::onRegisterEntityAttributes);
		
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientInit.doClientInit(modBus, forgeBus);
		}
	}
	
	void onCommonSetup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(this::afterCommonSetup);
	}
	
	void afterCommonSetup()
	{
		// add spawn egg behaviours to dispenser
		DefaultDispenseItemBehavior spawnEggBehavior = new DefaultDispenseItemBehavior()
		{
			/**
			 * Dispense the specified stack, play the dispense sound and spawn particles.
			 */
			@Override
			public ItemStack execute(BlockSource source, ItemStack stack)
			{
				Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
				EntityType<?> entitytype = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
				entitytype.spawn(source.getLevel(), stack, (Player) null, source.getPos().relative(direction), MobSpawnType.DISPENSER, direction != Direction.UP, false);
				stack.shrink(1);
				return stack;
			}
		};
		
		DispenserBlock.registerBehavior(this.excavatorSpawnEgg.get(), spawnEggBehavior);
		
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
	
	void onRegisterEntityAttributes(EntityAttributeCreationEvent event)
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
	
	// create and register a forge deferred register
	private static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> registerRegister(IEventBus modBus, IForgeRegistry<T> registry)
	{
		DeferredRegister<T> register = DeferredRegister.create(registry, MODID);
		register.register(modBus);
		return register;
	}
	
	// register a thing to a vanilla Registry (not worldgen registries)
	private static <T> T registerVanilla(Registry<T> registry, String name, T thing)
	{
		return Registry.register(registry, new ResourceLocation(MODID, name), thing);
	}
	
	private static Supplier<LoadableJigsawStructure> makePillagerStructureFactory(Supplier<List<SpawnerData>> monsterSpawnerGetter, Supplier<List<SpawnerData>> creatureSpawnerGetter)
	{
		return () -> new LoadableJigsawStructure(
			Codecs.JUMBO_JIGSAW_CONFIG_CODEC,
			0,
			false,
			true,
			PillagerOutpostFeature::checkLocation,
			GenerationStep.Decoration.SURFACE_STRUCTURES,
			true,
			monsterSpawnerGetter,
			creatureSpawnerGetter,
			false);
	}
	
	/**
	 * Register additional information needed for structures
	 * @param <STRUCTURE> The type of the structure
	 * @param structure A registered Structure instance (LoadableJigsawStructure has extra fields for structure registry)
	 * */
	private static <STRUCTURE extends LoadableJigsawStructure> void setStructureInfo(STRUCTURE structure)
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