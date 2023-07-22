package commoble.workshopsofdoom.structure_processors;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// structure processor that performs a string-replace on the jigsaw's pool target field for all jigsaw blocks in the structure
// cannot be used with standard jigsaw pieces, use a rejiggable pool element
public class EditPoolStructureProcessor extends StructureProcessor
{
	public static final Codec<EditPoolStructureProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("replace").forGetter(EditPoolStructureProcessor::getInput),
			Codec.STRING.fieldOf("with").forGetter(EditPoolStructureProcessor::getOutput)
		).apply(instance, EditPoolStructureProcessor::new));
	public static final String POOL = "pool"; // name of the field in jigsaw block NBT
	
	private final String input; public String getInput() { return this.input; }
	private final String output; public String getOutput() { return this.output; }
	
	// we are doing frequent-ish string operations on the same strings,
	// use a cache to improve structure generation times
	private final Map<String,StringTag> cache = new HashMap<>();
	
	public EditPoolStructureProcessor(String input, String output)
	{
		this.input = input;
		this.output = output;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.editPoolStructureProcessor.get();
	}

	// This is only ran on jigsaw blockinfos
	// Beware! World and the second blockpos argument are null
	@Override
	@Nullable
	public StructureTemplate.StructureBlockInfo process(@Nullable LevelReader world, BlockPos offset, @Nullable BlockPos structureOrigin, StructureTemplate.StructureBlockInfo originalInfo, StructureTemplate.StructureBlockInfo transformedInfo,
		StructurePlaceSettings placementSettings, @Nullable StructureTemplate template)
	{
		
		if (transformedInfo.state().getBlock() == Blocks.JIGSAW)
		{
			CompoundTag nbt = transformedInfo.nbt();
			if (nbt != null)
			{
				String oldPool = nbt.getString(POOL);
				StringTag newPool = this.cache.computeIfAbsent(oldPool, this::editPoolName);
				CompoundTag newNBT = nbt.copy();
				newNBT.put(POOL, newPool);
				return new StructureTemplate.StructureBlockInfo(transformedInfo.pos(), transformedInfo.state(), newNBT);
			}
		}

		return transformedInfo;
	}
	
	protected StringTag editPoolName(String oldPool)
	{
		return StringTag.valueOf(oldPool.replace(this.input, this.output));
	}

}
