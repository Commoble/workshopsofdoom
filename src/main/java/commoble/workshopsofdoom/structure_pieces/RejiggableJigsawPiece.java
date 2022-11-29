package commoble.workshopsofdoom.structure_pieces;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class RejiggableJigsawPiece extends SinglePoolElement
{
	public static final Codec<RejiggableJigsawPiece> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			// base class fields . . .
			SinglePoolElement.templateCodec(), // "location"
			SinglePoolElement.processorsCodec(), // "processors"
			SinglePoolElement.projectionCodec(), // "projection"
			// field for the extra jigsaw processors
			StructureProcessorType.LIST_CODEC.fieldOf("jigsaw_processors").forGetter(RejiggableJigsawPiece::getJigsawProcessors)
		).apply(instance, RejiggableJigsawPiece::new));
			
	protected final Holder<StructureProcessorList> jigsawProcessors;
	public Holder<StructureProcessorList> getJigsawProcessors() { return this.jigsawProcessors; }

	protected RejiggableJigsawPiece(Either<ResourceLocation, StructureTemplate> location, Holder<StructureProcessorList> processors, Projection projection, Holder<StructureProcessorList> jigsawProcessors)
	{
		super(location, processors, projection);
		this.jigsawProcessors = jigsawProcessors;
	}
	
	protected RejiggableJigsawPiece(StructureTemplate template)
	{
		this(Either.right(template),
			ProcessorLists.EMPTY,
			Projection.RIGID,
			ProcessorLists.EMPTY);
	}

	protected StructureTemplate getTemplate(StructureTemplateManager templates)
	{
		return this.template.map(templates::getOrCreate, Function.identity());
	}

	@Override
	public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager templateManager, BlockPos structureOffset, Rotation rotation, RandomSource rand)
	{
		List<StructureBlockInfo> oldJigsaws = super.getShuffledJigsawBlocks(templateManager, structureOffset, rotation, rand);
		
		StructureTemplate template = this.getTemplate(templateManager);
		StructurePlaceSettings placementSettings = new StructurePlaceSettings().setRotation(rotation);
		int size = oldJigsaws.size();
		List<StructureBlockInfo> newJigsaws = new ArrayList<>(size);
		List<StructureProcessor> jigsawProcessorList = this.jigsawProcessors.get().list();
		for (int i=0; i<size; i++)
		{
			newJigsaws.add(this.processJigsaw(jigsawProcessorList, structureOffset, oldJigsaws.get(i), placementSettings, template));
		}
		return newJigsaws;
	}
	
	protected StructureTemplate.StructureBlockInfo processJigsaw(List<StructureProcessor> jigsawProcessorList, BlockPos structureOffset, StructureBlockInfo originalInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
	{
		int size = jigsawProcessorList.size();
		StructureTemplate.StructureBlockInfo transformedInfo = originalInfo;
		
		for (int i=0; i<size; i++)
		{
			StructureProcessor processor = jigsawProcessorList.get(i);
			transformedInfo = processor.process(null, structureOffset, null, originalInfo, transformedInfo, placementSettings, template);
		}
		
		return transformedInfo;
	}

	@Override
	public StructurePoolElementType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.rejiggableJigsawPiece.get();
	}
}
