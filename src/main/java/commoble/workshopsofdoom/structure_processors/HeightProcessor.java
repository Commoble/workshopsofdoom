package commoble.workshopsofdoom.structure_processors;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.gen.feature.template.IStructureProcessorType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.StructureProcessor;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraft.world.gen.feature.template.Template.EntityInfo;

// run a sub-processor if height is within a specified range
// we make a specific thing just for this because jigsaw processors can't use world context or the structure origin
public class HeightProcessor extends StructureProcessor
{
	public static final Codec<HeightProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("min", 0).forGetter(HeightProcessor::getMin),
			Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(HeightProcessor::getMax),
			IStructureProcessorType.PROCESSOR_TYPE.listOf().fieldOf("processors").forGetter(HeightProcessor::getProcessors)
		).apply(instance, HeightProcessor::new));

	public static final IStructureProcessorType<HeightProcessor> DESERIALIZER = () -> CODEC;

	private final int min;	public int getMin() { return this.min; }
	private final int max;	public int getMax() { return this.max; }
	private final List<StructureProcessor> processors;	public List<StructureProcessor> getProcessors() { return this.processors; }

	public HeightProcessor(int min, int max, List<StructureProcessor> processors)
	{
		this.min = min;
		this.max = max;
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
		int y = transformedInfo.pos.getY();
		if( y >= this.min && y <= this.max)
		{
			// we pass the test, run each subprocess
			BlockInfo output = transformedInfo;
			for (StructureProcessor processor : this.processors)
			{
				output = processor.process(world, originalPos, structureOrigin, originalInfo, output, placement, template);
			}
			return output;
		}
		
		return transformedInfo; //no-op
	}

	@Override
	public EntityInfo processEntity(IWorldReader world, BlockPos seedPos, EntityInfo rawEntityInfo, EntityInfo entityInfo, PlacementSettings placementSettings, Template template)
	{
		int y = entityInfo.blockPos.getY();
		if( y >= this.min && y <= this.max)
		{
			// we pass the test, run each subprocess
			EntityInfo output = entityInfo;
			for (StructureProcessor processor : this.processors)
			{
				output = processor.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
			}
			return output;
		}
		
		return entityInfo; //no-op
	}

}