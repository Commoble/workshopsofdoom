package commoble.workshopsofdoom.structures;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.JigsawFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class LoadableJigsawStructure extends JigsawFeature
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
	public LoadableJigsawStructure(Codec<JigsawConfiguration> codec, int startY, boolean enableLegacyJigsawHack, boolean snapToHeightMap, Predicate<PieceGeneratorSupplier.Context<JigsawConfiguration>> placementPredicate, GenerationStep.Decoration generationStage, boolean restrictSpawnBoxes, boolean transformSurroundingLand)
	{
		super(codec, startY, enableLegacyJigsawHack, snapToHeightMap, placementPredicate);
		this.generationStage = generationStage;
		this.restrictSpawnBoxes = restrictSpawnBoxes;
		this.transformSurroundingLand = transformSurroundingLand;
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
