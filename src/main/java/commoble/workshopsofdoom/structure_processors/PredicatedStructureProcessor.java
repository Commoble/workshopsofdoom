package commoble.workshopsofdoom.structure_processors;

import java.util.List;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;

// similar to the rules list processor but can be used with other processors
// if predicates match, will apply its list of sub-processors

public class PredicatedStructureProcessor extends StructureProcessor
{
	public static final Codec<PredicatedStructureProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RuleTest.CODEC.optionalFieldOf("input_predicate", AlwaysTrueTest.INSTANCE).forGetter(PredicatedStructureProcessor::getInputpredicate),
			RuleTest.CODEC.optionalFieldOf("location_predicate", AlwaysTrueTest.INSTANCE).forGetter(PredicatedStructureProcessor::getLocationpredicate),
			PosRuleTest.CODEC.optionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(PredicatedStructureProcessor::getPositionPredicate),
			StructureProcessorType.SINGLE_CODEC.listOf().fieldOf("processors").forGetter(PredicatedStructureProcessor::getProcessors)
		).apply(instance, PredicatedStructureProcessor::new));
	
	public static final StructureProcessorType<PredicatedStructureProcessor> DESERIALIZER = () -> CODEC;

	// input blockinfo in the structure file
	private final RuleTest inputPredicate;	public RuleTest getInputpredicate() { return this.inputPredicate; }
	// existing block info in the world where this block is being placed
	private final RuleTest locationPredicate;	public RuleTest getLocationpredicate() { return this.locationPredicate; }
	// test of original position, transformed position, and structure origin position
	private final PosRuleTest positionPredicate; public PosRuleTest getPositionPredicate() { return this.positionPredicate; }
	// sub-processors to run if these predicates pass
	private final List<StructureProcessor> processors;	public List<StructureProcessor> getProcessors() { return this.processors; }

	public PredicatedStructureProcessor(RuleTest inputPredicate, RuleTest locationPredicate, PosRuleTest positionPredicate, List<StructureProcessor> processors)
	{
		this.inputPredicate = inputPredicate;
		this.locationPredicate = locationPredicate;
		this.positionPredicate = positionPredicate;
		this.processors = processors;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return DESERIALIZER;
	}

	@Override
	public StructureBlockInfo process(LevelReader world, BlockPos originalPos, BlockPos structureOrigin, StructureBlockInfo originalInfo, StructureBlockInfo transformedInfo, StructurePlaceSettings placement,
		StructureTemplate template)
	{
		Random random = new Random(Mth.getSeed(transformedInfo.pos));
		if (this.inputPredicate.test(transformedInfo.state, random))
		{
			if (this.positionPredicate.test(originalInfo.pos, transformedInfo.pos, structureOrigin, random))
			{
				if (this.locationPredicate.test(world.getBlockState(transformedInfo.pos), random))
				{
					StructureBlockInfo output = transformedInfo;
					for (StructureProcessor processor : this.processors)
					{
						output = processor.process(world, originalPos, structureOrigin, originalInfo, output, placement, template);
					}
					return output;
				}
			}
		}
		return transformedInfo;
	}

	@Override
	public StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos, StructureEntityInfo rawEntityInfo, StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
	{
		// the predicates we use are only for blocks, unfortunately
		return super.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
	}
}
