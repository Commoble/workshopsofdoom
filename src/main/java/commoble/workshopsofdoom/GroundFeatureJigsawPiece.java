package commoble.workshopsofdoom;

import java.util.Random;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.jigsaw.FeatureJigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.IJigsawDeserializer;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraftforge.registries.ForgeRegistries;

// like FeatureJigsawPiece, but offset downward by 1 block
// useful for making features at the same position as the parent jigsaw
// when snap-to-heightmap isn't viable
public class GroundFeatureJigsawPiece extends FeatureJigsawPiece
{
	public static final Codec<GroundFeatureJigsawPiece> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
			ConfiguredFeature.field_236264_b_.fieldOf("feature").forGetter((piece) -> piece.featureGetter),
			func_236848_d_()
		).apply(instance, GroundFeatureJigsawPiece::new));
	public static final IJigsawDeserializer<GroundFeatureJigsawPiece> DESERIALIZER = () -> CODEC;
	
	private final Supplier<ConfiguredFeature<?, ?>> featureGetter;

	protected GroundFeatureJigsawPiece(Supplier<ConfiguredFeature<?, ?>> feature, JigsawPattern.PlacementBehaviour placement)
	{
		super(feature, placement);
		this.featureGetter = feature;
	}

	@Override
	public boolean func_230378_a_(TemplateManager templates, ISeedReader world, StructureManager structures, ChunkGenerator chunkGenerator, BlockPos posToGenerate,
		BlockPos posB, Rotation rotation, MutableBoundingBox box, Random rand, boolean flag)
	{
		return this.featureGetter.get().generate(world, chunkGenerator, rand, posToGenerate.down());
	}

	@Override
	public IJigsawDeserializer<?> getType()
	{
		return DESERIALIZER;
	}

	@Override
	public String toString()
	{
		return "GroundFeature[" + ForgeRegistries.FEATURES.getKey(this.featureGetter.get().getFeature()) + "]";
	}
}
