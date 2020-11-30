package commoble.workshopsofdoom.structure_processors;

import java.util.List;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.gen.feature.template.AlwaysTrueRuleTest;
import net.minecraft.world.gen.feature.template.AlwaysTrueTest;
import net.minecraft.world.gen.feature.template.IStructureProcessorType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.PosRuleTest;
import net.minecraft.world.gen.feature.template.RuleTest;
import net.minecraft.world.gen.feature.template.StructureProcessor;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraft.world.gen.feature.template.Template.EntityInfo;

// similar to the rules list processor but can be used with other processors
// if predicates match, will apply its list of sub-processors

public class PredicatedStructureProcessor extends StructureProcessor
{
	public static final Codec<PredicatedStructureProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RuleTest.field_237127_c_.optionalFieldOf("input_predicate", AlwaysTrueRuleTest.INSTANCE).forGetter(PredicatedStructureProcessor::getInputpredicate),
			RuleTest.field_237127_c_.optionalFieldOf("location_predicate", AlwaysTrueRuleTest.INSTANCE).forGetter(PredicatedStructureProcessor::getLocationpredicate),
			PosRuleTest.field_237102_c_.optionalFieldOf("position_predicate", AlwaysTrueTest.field_237100_b_).forGetter(PredicatedStructureProcessor::getPositionPredicate),
			IStructureProcessorType.PROCESSOR_TYPE.listOf().fieldOf("processors").forGetter(PredicatedStructureProcessor::getProcessors)
		).apply(instance, PredicatedStructureProcessor::new));
	
	public static final IStructureProcessorType<PredicatedStructureProcessor> DESERIALIZER = () -> CODEC;

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
	protected IStructureProcessorType<?> getType()
	{
		return DESERIALIZER;
	}

	@Override
	public BlockInfo process(IWorldReader world, BlockPos originalPos, BlockPos structureOrigin, BlockInfo originalInfo, BlockInfo transformedInfo, PlacementSettings placement,
		Template template)
	{
		Random random = new Random(MathHelper.getPositionRandom(transformedInfo.pos));
		if (this.inputPredicate.test(transformedInfo.state, random))
		{
			if (this.positionPredicate.func_230385_a_(originalInfo.pos, transformedInfo.pos, structureOrigin, random))
			{
				if (this.locationPredicate.test(world.getBlockState(transformedInfo.pos), random))
				{
					BlockInfo output = transformedInfo;
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
	public EntityInfo processEntity(IWorldReader world, BlockPos seedPos, EntityInfo rawEntityInfo, EntityInfo entityInfo, PlacementSettings placementSettings, Template template)
	{
		// the predicates we use are only for blocks, unfortunately
		return super.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
	}
}
