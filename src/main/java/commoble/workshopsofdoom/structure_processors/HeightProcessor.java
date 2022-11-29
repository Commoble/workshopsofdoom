package commoble.workshopsofdoom.structure_processors;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;

// run a sub-processor if height is within a specified range
// we make a specific thing just for this because jigsaw processors can't use world context or the structure origin
public class HeightProcessor extends StructureProcessor
{
	public static final Codec<HeightProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("min", Integer.MIN_VALUE).forGetter(HeightProcessor::getMin),
			Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(HeightProcessor::getMax),
			StructureProcessorType.SINGLE_CODEC.listOf().fieldOf("processors").forGetter(HeightProcessor::getProcessors)
		).apply(instance, HeightProcessor::new));

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
	protected StructureProcessorType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.heightProcessor.get();
	}

	@Override
	public StructureBlockInfo process(LevelReader world, BlockPos originalPos, BlockPos structureOrigin, StructureBlockInfo originalInfo, StructureBlockInfo transformedInfo, StructurePlaceSettings placement,
		StructureTemplate template)
	{
		int y = transformedInfo.pos.getY();
		if( y >= this.min && y <= this.max)
		{
			// we pass the test, run each subprocess
			StructureBlockInfo output = transformedInfo;
			for (StructureProcessor processor : this.processors)
			{
				output = processor.process(world, originalPos, structureOrigin, originalInfo, output, placement, template);
			}
			return output;
		}
		
		return transformedInfo; //no-op
	}

	@Override
	public StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos, StructureEntityInfo rawEntityInfo, StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
	{
		int y = entityInfo.blockPos.getY();
		if( y >= this.min && y <= this.max)
		{
			// we pass the test, run each subprocess
			StructureEntityInfo output = entityInfo;
			for (StructureProcessor processor : this.processors)
			{
				output = processor.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
			}
			return output;
		}
		
		return entityInfo; //no-op
	}

}