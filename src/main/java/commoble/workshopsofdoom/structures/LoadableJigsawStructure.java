package commoble.workshopsofdoom.structures;

import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.structures.LoadableJigsawStructure.LoadableJigsawConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager.IPieceFactory;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.structure.VillageConfig;
import net.minecraft.world.gen.feature.template.TemplateManager;

public class LoadableJigsawStructure extends Structure<LoadableJigsawConfig>
{	
	static final Logger LOGGER = LogManager.getLogger();
	private final GenerationStage.Decoration generationStage;
	private final boolean restrictSpawnBoxes;
	private final Supplier<List<Spawners>> monsterSpawnerGetter;
	private final Supplier<List<Spawners>> creatureSpawnerGetter;
	
	public final int placementSalt;
	public final boolean transformSurroundingLand;
	
	/**
	 * 
	 * @param codec The deserializer for the config
	 * @param generationStage When to generate the structure
	 * @param restrictSpawnBoxes If true, the structure's spawn entries are only used in the bounding boxes of the structure pieces.
	 * If false, the structure's spawn entries will be used inside the structure's entire AABB cuboid.
	 * @param monsterSpawnerGetter Monster spawns, only have monsters in these (zombies, illagers, guardians, etc)
	 * @param creatureSpawnerGetter Creature spawns, only have creatures (pigs, cows, bears, etc) in these
	 * @param placementSalt A salt added to the location seed by the structure placer; for best results this should be large and not the same as any other structure's seed (see DimensionStructuresSettings for vanilla values)
	 * @param transformSurroundingLand <br>
		// " Will add land at the base of the structure like it does for Villages and Outposts. "<br>
		// " Doesn't work well on structure that have pieces stacked vertically or change in heights. "<br>
		// ~TelepathicGrunt<br>
		// (vanilla only uses this for villages, outposts, and Nether Fossils)
	 */
	public LoadableJigsawStructure(Codec<LoadableJigsawConfig> codec, GenerationStage.Decoration generationStage, boolean restrictSpawnBoxes, Supplier<List<Spawners>> monsterSpawnerGetter, Supplier<List<Spawners>> creatureSpawnerGetter, int placementSalt, boolean transformSurroundingLand)
	{
		super(codec);
		this.generationStage = generationStage;
		this.restrictSpawnBoxes = restrictSpawnBoxes;
		this.monsterSpawnerGetter = monsterSpawnerGetter;
		this.creatureSpawnerGetter = creatureSpawnerGetter;
		this.placementSalt = placementSalt;
		this.transformSurroundingLand = transformSurroundingLand;
	}

	@Override
	public IStartFactory<LoadableJigsawConfig> getStartFactory()
	{
		return Start::new;
	}
	
	@Override
	public GenerationStage.Decoration getDecorationStage()
	{
		return this.generationStage;
	}
	
	
	

	@Override
	public List<Spawners> getDefaultSpawnList()
	{
		return this.monsterSpawnerGetter.get();
	}

	@Override
	public List<Spawners> getDefaultCreatureSpawnList()
	{
		return this.creatureSpawnerGetter.get();
	}

	@Override
	public boolean getDefaultRestrictsSpawnsToInside()
	{
		return this.restrictSpawnBoxes;
	}

	// shouldGenerate
	@Override
	protected boolean func_230363_a_(ChunkGenerator generator, BiomeProvider biomeProvider, long seed, SharedSeedRandom rand, int chunkX, int chunkZ,
		Biome biome, ChunkPos chunkPos, LoadableJigsawConfig config)
	{
		return super.func_230363_a_(generator, biomeProvider, seed, rand, chunkX, chunkZ, biome, chunkPos, config);
		// PillagerOutpostStructure checks the separation settings of villages here as well
		// -- this prevents it from spawning close to villages
	}

	static class Start extends StructureStart<LoadableJigsawConfig>
	{
		protected final Structure<LoadableJigsawConfig> structure;
		
		public Start(Structure<LoadableJigsawConfig> structure, int chunkX, int chunkZ, MutableBoundingBox mutableBox, int refCount, long seed)
		{
			super(structure, chunkX, chunkZ, mutableBox, refCount, seed);
			this.structure = structure;
		}
		
		// this method is responsible for:
		// -- creating and configuring a structure piece serializer
		// -- loading templates if needed
		// -- adding structure pieces to this object's components
		// -- calling recalculateStructureSize
		@Override
		public void func_230364_a_(DynamicRegistries registries, ChunkGenerator generator, TemplateManager templates, int chunkX, int chunkZ, Biome biome,
			LoadableJigsawConfig config)
		{
			int depth = config.size;
			String logSuffix = depth < 20 ? "" : " (this may take some time)";
			LOGGER.debug("Running initial structure generation for structure {} at chunk position {}, {}, with jigsaw depth of {}{}",
				this.getStructure().getRegistryName().toString(),
				chunkX,
				chunkZ,
				depth,
				logSuffix);
			long start = System.currentTimeMillis();
			// from JigsawStructure, but we don't create a VillageConfig until now
			// if we create one when configured structures / StructureFeatures are registered,
			// then the worldgen data loader tries to resolve the jigsaw pool before jigsaw pools are loaded from data
			BlockPos startPos = new BlockPos(chunkX * 16, config.getStartY(), chunkZ * 16);
			
			JigsawPattern startPool = registries.getRegistry(Registry.JIGSAW_POOL_KEY)
				.getOrDefault(config.getStartPool());
			
			if (startPool == null)
			{
				throw new NullPointerException(String.format("A configured structure %s is missing required initial template pool file: %s", this.structure.getRegistryName().toString(), config.getStartPool().toString()));
			}
			
			// mcp calls this VillageConfig but it's the config used for vanilla jigsaw structures (bastions, pillager outposts, villages)
			VillageConfig villageConfig = new VillageConfig(() -> startPool, config.getSize());
  			IPieceFactory pieceFactory = AbstractVillagePiece::new;
			JigsawManager.func_242837_a(registries, villageConfig, pieceFactory, generator, templates, startPos, this.components, this.rand,
				config.getAllowIntersectingPieces(),
				config.getSnapToHeightMap());
			this.recalculateStructureSize();
			long end = System.currentTimeMillis();
			LOGGER.debug("Structure generation completed, elapsed time = {} ms", end-start);
		}
		
	}
	
	public static class LoadableJigsawConfig implements IFeatureConfig
	{
		public static final Codec<LoadableJigsawConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("start_pool").forGetter(LoadableJigsawConfig::getStartPool),
				Codec.INT.fieldOf("size").forGetter(LoadableJigsawConfig::getSize),
				Codec.INT.fieldOf("min_seperation").forGetter(LoadableJigsawConfig::getMinSeparation),
				Codec.INT.fieldOf("max_separation").forGetter(LoadableJigsawConfig::getMaxSeparation),
				Codec.INT.optionalFieldOf("start_y",0).forGetter(LoadableJigsawConfig::getStartY),
				Codec.BOOL.optionalFieldOf("enable_legacy_piece_intersactions", false).forGetter(LoadableJigsawConfig::getAllowIntersectingPieces),
				Codec.BOOL.optionalFieldOf("snap_to_height_map", true).forGetter(LoadableJigsawConfig::getSnapToHeightMap)
			).apply(instance, LoadableJigsawConfig::new));
		
		private final ResourceLocation startPool; public ResourceLocation getStartPool() {return this.startPool;}
		private final int size; public int getSize() { return this.size;}
		private final int minSeparation; public int getMinSeparation() { return this.minSeparation; }
		private final int maxSeparation; public int getMaxSeparation() { return this.maxSeparation; }
		private final int startY; public int getStartY() {return this.startY;}
		private final boolean enableLegacyIntersectionHack; public boolean getAllowIntersectingPieces() {return this.enableLegacyIntersectionHack;}
		private final boolean snapToHeightMap; public boolean getSnapToHeightMap() {return this.snapToHeightMap;}
		
		/**
		 * As with the other constructor but with recommended params.
		 * Snap-to-height is set to TRUE, and the intersection hack for legacy structures is disabled.
		 * @param startPool The name of the template pool for the initial jigsaw piece,
		 * e.g. "workshopsofdoom:start" => data/workshopsofdoom/worldgen/template_pool/start
		 * @param minSeparation Minimum farapartness for generating instances of this structure
		 * @param maxSeparation Maximum farapartness for generating instances of this structure
		 * @param size How many pieces deep the structure can generate beyond the initial piece.
		 * The first piece is iteration 0, size must be greater than 0 to have more than one piece in the structure.
		 */
		public LoadableJigsawConfig(ResourceLocation startPool, int size, int minSeparation, int maxSeparation)
		{
			this(startPool, size, minSeparation, maxSeparation, 0, false, true);
		}
		
		/**
		 * 
		 * @param startPool The name of the template pool for the initial jigsaw piece,
		 * e.g. "workshopsofdoom:start" => data/workshopsofdoom/worldgen/template_pool/start
		 * @param size How many pieces deep the structure can generate beyond the initial piece.
		 * The first piece is iteration 0, size must be greater than 0 to have more than one piece in the structure.
		 * @param minSeparation Minimum farapartness for generating instances of this structure
		 * @param maxSeparation Maximum farapartness for generating instances of this structure
		 * @param startY What y-level to start the structure at. Ignored if snapToHeightMap is true.
		 * @param enableLegacyIntersectionHack Affects the way in which the jigsaw placer decides
		 * whether jigsaw pieces are intersecting in strange ways. If this is disabled, then
		 * interior-pointing jigsaws' pieces must be entirely contained by their parent structure's cuboids,
		 * and exterior-pointing jigsaw's pieces cannot overlap with previously-placed pieces. If this is enabled,
		 * then the y-levels of the voxels used for collision-checking are offset in strange ways. Enabled for
		 * villages and pillager outposts, but not bastions.
		 * @param snapToHeightMap If true, the starting position will snap to the local heightmap
		 */
		public LoadableJigsawConfig(ResourceLocation startPool, int size, int minSeparation, int maxSeparation, int startY, boolean enableLegacyIntersectionHack, boolean snapToHeightMap)
		{
			this.startPool = startPool;
			this.size = size;
			this.minSeparation = minSeparation;
			this.maxSeparation = maxSeparation;
			this.startY = startY;
			this.enableLegacyIntersectionHack = enableLegacyIntersectionHack;
			this.snapToHeightMap = snapToHeightMap;
		}
	}

}
