package commoble.workshopsofdoom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.LoadableJigsawStructure.LoadableJigsawConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
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
	private final GenerationStage.Decoration generationStage;
	
	public LoadableJigsawStructure(Codec<LoadableJigsawConfig> codec, GenerationStage.Decoration generationStage)
	{
		super(codec);
		this.generationStage = generationStage;
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
	
	// also consider overriding getDefaultSpawnList and getDefaultCreatureSpawnList

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
		public Start(Structure<LoadableJigsawConfig> structure, int chunkX, int chunkZ, MutableBoundingBox mutableBox, int refCount, long seed)
		{
			super(structure, chunkX, chunkZ, mutableBox, refCount, seed);
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
			// from JigsawStructure, but we don't create a VillageConfig until now
			// if we create one when configured structures / StructureFeatures are registered,
			// then the worldgen data loader tries to resolve the jigsaw pool before jigsaw pools are loaded from data
			BlockPos startPos = new BlockPos(chunkX * 16, config.getStartY(), chunkZ * 16);
			
			JigsawPattern startPool = registries.getRegistry(Registry.JIGSAW_POOL_KEY)
				.getOrDefault(config.getStartPool());
			
			// mcp calls this VillageConfig but it's the config used for vanilla jigsaw structures (bastions, pillager outposts, villages)
			VillageConfig villageConfig = new VillageConfig(() -> startPool, config.getSize());
			IPieceFactory pieceFactory = AbstractVillagePiece::new;
			JigsawManager.func_242837_a(registries, villageConfig, pieceFactory, generator, templates, startPos, this.components, this.rand,
				config.getAllowIntersectingPieces(),
				config.getSnapToHeightMap());
			this.recalculateStructureSize();
		}
		
	}
	
	public static class LoadableJigsawConfig implements IFeatureConfig
	{
		public static final Codec<LoadableJigsawConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("start_pool").forGetter(LoadableJigsawConfig::getStartPool),
				Codec.INT.fieldOf("size").forGetter(LoadableJigsawConfig::getSize),
				Codec.INT.optionalFieldOf("start_y",0).forGetter(LoadableJigsawConfig::getStartY),
				Codec.BOOL.optionalFieldOf("allow_intersecting_pieces", true).forGetter(LoadableJigsawConfig::getAllowIntersectingPieces),
				Codec.BOOL.optionalFieldOf("snap_to_height_map", true).forGetter(LoadableJigsawConfig::getSnapToHeightMap)
			).apply(instance, LoadableJigsawConfig::new));
		
		private final ResourceLocation startPool; public ResourceLocation getStartPool() {return this.startPool;}
		private final int size; public int getSize() { return this.size;}
		private final int startY; public int getStartY() {return this.startY;}
		private final boolean allowIntersectingPieces; public boolean getAllowIntersectingPieces() {return this.allowIntersectingPieces;}
		private final boolean snapToHeightMap; public boolean getSnapToHeightMap() {return this.snapToHeightMap;}
		
		public LoadableJigsawConfig(ResourceLocation startPool, int size)
		{
			this(startPool, size, 0, true, true);
		}
		
		public LoadableJigsawConfig(ResourceLocation startPool, int size, int startY, boolean allowIntersectingPieces, boolean snapToHeightMap)
		{
			this.startPool = startPool;
			this.size = size;
			this.startY = startY;
			this.allowIntersectingPieces = allowIntersectingPieces;
			this.snapToHeightMap = snapToHeightMap;
		}
	}

}
