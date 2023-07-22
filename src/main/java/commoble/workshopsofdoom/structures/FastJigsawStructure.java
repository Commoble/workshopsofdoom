package commoble.workshopsofdoom.structures;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.OctreeJigsawPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public class FastJigsawStructure extends JigsawStructure
{
	// same as the vanilla codec but doesn't cap size to 7 and doesn't use the expansion hack
	public static final Codec<FastJigsawStructure> CODEC = RecordCodecBuilder.<FastJigsawStructure>mapCodec((builder) -> builder.group(
				settingsCodec(builder),
				StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
				ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("size").forGetter(structure -> structure.maxDepth),
				HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
				Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
				Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
			).apply(builder, FastJigsawStructure::new)).codec();

	public FastJigsawStructure(
		Structure.StructureSettings settings,
		Holder<StructureTemplatePool> startPool,
		Optional<ResourceLocation> startJigsawName,
		int size,
		HeightProvider startHeight,
		Optional<Heightmap.Types> projectStartToHeightmap,
		int maxDistanceFromCenter)
	{
		super(settings, startPool, startJigsawName, size, startHeight, false, projectStartToHeightmap, maxDistanceFromCenter);
	}
	
	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context)
	{
		ChunkPos chunkpos = context.chunkPos();
		int i = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
		BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), i, chunkpos.getMinBlockZ());
//		Pools.forceBootstrap();
		return OctreeJigsawPlacer.addPieces(context, this.startPool, this.startJigsawName, this.maxDepth, blockpos, this.useExpansionHack, this.projectStartToHeightmap, this.maxDistanceFromCenter);
	}

	@Override
	public StructureType<?> type()
	{
		return WorkshopsOfDoom.INSTANCE.fastJigsawStructure.get();
	}
}
