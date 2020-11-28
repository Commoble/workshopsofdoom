package commoble.workshopsofdoom.structure_pieces;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.jigsaw.IJigsawDeserializer;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern.PlacementBehaviour;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraft.world.gen.feature.template.IStructureProcessorType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.ProcessorLists;
import net.minecraft.world.gen.feature.template.StructureProcessor;
import net.minecraft.world.gen.feature.template.StructureProcessorList;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraft.world.gen.feature.template.TemplateManager;

public class RejiggableJigsawPiece extends SingleJigsawPiece
{
	public static final Codec<RejiggableJigsawPiece> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			// base class fields . . .
			SingleJigsawPiece.func_236846_c_(), // "location"
			SingleJigsawPiece.func_236844_b_(), // "processors"
			SingleJigsawPiece.func_236848_d_(), // "projection"
			// field for the extra jigsaw processors
			IStructureProcessorType.field_242922_m.fieldOf("jigsaw_processors").forGetter(RejiggableJigsawPiece::getJigsawProcessors)
		).apply(instance, RejiggableJigsawPiece::new));
	public static final IJigsawDeserializer<RejiggableJigsawPiece> DESERIALIZER = () -> CODEC;
			
	protected final Supplier<StructureProcessorList> jigsawProcessors;
	public Supplier<StructureProcessorList> getJigsawProcessors() { return this.jigsawProcessors; }

	protected RejiggableJigsawPiece(Either<ResourceLocation, Template> location, Supplier<StructureProcessorList> processors, PlacementBehaviour projection, Supplier<StructureProcessorList> jigsawProcessors)
	{
		super(location, processors, projection);
		this.jigsawProcessors = jigsawProcessors;
	}
	
	protected RejiggableJigsawPiece(Template template)
	{
		this(Either.right(template),
			() -> ProcessorLists.field_244101_a,
			PlacementBehaviour.RIGID,
			() -> ProcessorLists.field_244101_a);
	}

	protected Template getTemplate(TemplateManager p_236843_1_)
	{
		return this.field_236839_c_.map(p_236843_1_::getTemplateDefaulted, Function.identity());
	}

	@Override
	public List<Template.BlockInfo> getJigsawBlocks(TemplateManager templateManager, BlockPos structureOffset, Rotation rotation, Random rand)
	{
		List<BlockInfo> oldJigsaws = super.getJigsawBlocks(templateManager, structureOffset, rotation, rand);
		
		Template template = this.getTemplate(templateManager);
		PlacementSettings placementSettings = new PlacementSettings().setRotation(rotation);
		int size = oldJigsaws.size();
		List<BlockInfo> newJigsaws = new ArrayList<>(size);
		List<StructureProcessor> jigsawProcessorList = this.jigsawProcessors.get().func_242919_a();
		for (int i=0; i<size; i++)
		{
			newJigsaws.add(this.processJigsaw(jigsawProcessorList, structureOffset, oldJigsaws.get(i), placementSettings, template));
		}
		return newJigsaws;
	}
	
	protected Template.BlockInfo processJigsaw(List<StructureProcessor> jigsawProcessorList, BlockPos structureOffset, BlockInfo originalInfo, PlacementSettings placementSettings, Template template)
	{
		int size = jigsawProcessorList.size();
		Template.BlockInfo transformedInfo = originalInfo;
		
		for (int i=0; i<size; i++)
		{
			StructureProcessor processor = jigsawProcessorList.get(i);
			transformedInfo = processor.process(null, structureOffset, null, originalInfo, transformedInfo, placementSettings, template);
		}
		
		return transformedInfo;
	}

}
