package commoble.workshopsofdoom.structure_pieces;

import java.util.Random;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.structures.FeaturePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElementType;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

// like FeatureJigsawPiece, but offset downward by 1 block
// useful for making features at the same position as the parent jigsaw
// when snap-to-heightmap isn't viable
public class GroundFeatureJigsawPiece extends FeaturePoolElement
{
	public static final Codec<GroundFeatureJigsawPiece> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
			PlacedFeature.CODEC.fieldOf("feature").forGetter((piece) -> piece.featureGetter),
			projectionCodec()
		).apply(instance, GroundFeatureJigsawPiece::new));
	public static final StructurePoolElementType<GroundFeatureJigsawPiece> DESERIALIZER = () -> CODEC;
	
	private final Supplier<PlacedFeature> featureGetter;

	protected GroundFeatureJigsawPiece(Supplier<PlacedFeature> feature, StructureTemplatePool.Projection placement)
	{
		super(feature, placement);
		this.featureGetter = feature;
	}

	@Override
	public boolean place(StructureManager templates, WorldGenLevel world, StructureFeatureManager structures, ChunkGenerator chunkGenerator, BlockPos posToGenerate,
		BlockPos posB, Rotation rotation, BoundingBox box, Random rand, boolean flag)
	{
		return this.featureGetter.get().place(world, chunkGenerator, rand, posToGenerate.below());
	}

	@Override
	public StructurePoolElementType<?> getType()
	{
		return DESERIALIZER;
	}

	@Override
	public String toString()
	{
		return "GroundFeature[" + this.featureGetter.get() + "]";
	}
}
