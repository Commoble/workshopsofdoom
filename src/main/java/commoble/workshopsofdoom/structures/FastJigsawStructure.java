package commoble.workshopsofdoom.structures;

import java.util.Optional;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import commoble.workshopsofdoom.util.OctreeJigsawPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureFeature;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class FastJigsawStructure extends NoiseAffectingStructureFeature<JigsawConfiguration>
{	
	static final Logger LOGGER = LogManager.getLogger();
	private final GenerationStep.Decoration generationStage;
	private final boolean restrictSpawnBoxes;
	public final boolean transformSurroundingLand;
	
	/**
	 * 
	 * @param codec The deserializer for the config
	 * @param generationStage When to generate the structure
	 * @param restrictSpawnBoxes If true, the structure's spawn entries are only used in the bounding boxes of the structure pieces.
	 * If false, the structure's spawn entries will be used inside the structure's entire AABB cuboid.
	 * @param placementSalt A salt added to the location seed by the structure placer; for best results this should be large and not the same as any other structure's seed (see DimensionStructuresSettings for vanilla values)
	 * @param transformSurroundingLand <br>
		// " Will add land at the base of the structure like it does for Villages and Outposts. "<br>
		// " Doesn't work well on structure that have pieces stacked vertically or change in heights. "<br>
		// ~TelepathicGrunt<br>
		// (vanilla only uses this for villages, outposts, and Nether Fossils)
	 */
	public FastJigsawStructure(Codec<JigsawConfiguration> codec, int startY, boolean snapToHeightMap, Predicate<PieceGeneratorSupplier.Context<JigsawConfiguration>> placementPredicate, GenerationStep.Decoration generationStage, boolean restrictSpawnBoxes, boolean transformSurroundingLand)
	{
		super(codec, createGenerator(startY, snapToHeightMap, placementPredicate));
		this.generationStage = generationStage;
		this.restrictSpawnBoxes = restrictSpawnBoxes;
		this.transformSurroundingLand = transformSurroundingLand;
	}
	
	public static PieceGeneratorSupplier<JigsawConfiguration> createGenerator(int startY, boolean snapToHeightMap, Predicate<PieceGeneratorSupplier.Context<JigsawConfiguration>> placementPredicate)
	{
		return (context) -> {
			if (!placementPredicate.test(context))
			{
				return Optional.empty();
			}
			else
			{
				BlockPos blockpos = new BlockPos(context.chunkPos().getMinBlockX(), startY, context.chunkPos().getMinBlockZ());
				Pools.bootstrap();
				return OctreeJigsawPlacer.addPieces(context, PoolElementStructurePiece::new, blockpos, snapToHeightMap);
			}
		};
	}
	
	@Override
	public GenerationStep.Decoration step()
	{
		return this.generationStage;
	}

	@Override
	public boolean getDefaultRestrictsSpawnsToInside()
	{
		return this.restrictSpawnBoxes;
	}	
}
