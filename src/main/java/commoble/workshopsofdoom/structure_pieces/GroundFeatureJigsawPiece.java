package commoble.workshopsofdoom.structure_pieces;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

// like FeatureJigsawPiece, but offset downward by 1 block
// useful for making features at the same position as the parent jigsaw
// when snap-to-heightmap isn't viable
public class GroundFeatureJigsawPiece extends FeaturePoolElement
{
	public static final Codec<GroundFeatureJigsawPiece> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
			PlacedFeature.CODEC.fieldOf("feature").forGetter((piece) -> piece.feature),
			projectionCodec()
		).apply(instance, GroundFeatureJigsawPiece::new));
	public static final StructurePoolElementType<GroundFeatureJigsawPiece> DESERIALIZER = () -> CODEC;
	
	private final Supplier<PlacedFeature> featureGetter;

	protected GroundFeatureJigsawPiece(Holder<PlacedFeature> feature, StructureTemplatePool.Projection placement)
	{
		super(feature, placement);
		this.featureGetter = feature;
	}

	@Override
	public boolean place(StructureTemplateManager templates, WorldGenLevel world, StructureManager structures, ChunkGenerator chunkGenerator, BlockPos posToGenerate,
		BlockPos posB, Rotation rotation, BoundingBox box, RandomSource rand, boolean flag)
	{
		return this.featureGetter.get().place(world, chunkGenerator, rand, posToGenerate.below());
	}

	@Override
	public StructurePoolElementType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.groundFeatureJigsawPiece.get();
	}

	@Override
	public String toString()
	{
		return "GroundFeature[" + this.featureGetter.get() + "]";
	}
}
