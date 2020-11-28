package commoble.workshopsofdoom.structure_processors;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.gen.feature.template.IStructureProcessorType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.StructureProcessor;
import net.minecraft.world.gen.feature.template.Template;

// structure processor that performs a string-replace on the jigsaw's pool target field for all jigsaw blocks in the structure
// cannot be used with standard jigsaw pieces, use a rejiggable pool element
public class EditPoolStructureProcessor extends StructureProcessor
{
	public static final Codec<EditPoolStructureProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("replace").forGetter(EditPoolStructureProcessor::getInput),
			Codec.STRING.fieldOf("with").forGetter(EditPoolStructureProcessor::getOutput)
		).apply(instance, EditPoolStructureProcessor::new));
	public static final IStructureProcessorType<EditPoolStructureProcessor> DESERIALIZER = () -> CODEC;
	public static final String POOL = "pool"; // name of the field in jigsaw block NBT
	
	private final String input; public String getInput() { return this.input; }
	private final String output; public String getOutput() { return this.output; }
	
	// we are doing frequent-ish string operations on the same strings,
	// use a cache to improve structure generation times
	private final Map<String,StringNBT> cache = new HashMap<>();
	
	public EditPoolStructureProcessor(String input, String output)
	{
		this.input = input;
		this.output = output;
	}

	@Override
	protected IStructureProcessorType<?> getType()
	{
		return DESERIALIZER;
	}

	// This is only ran on jigsaw blockinfos
	// Beware! World and the second blockpos argument are null
	@Override
	@Nullable
	public Template.BlockInfo process(@Nullable IWorldReader world, BlockPos offset, @Nullable BlockPos structureOrigin, Template.BlockInfo originalInfo, Template.BlockInfo transformedInfo,
		PlacementSettings placementSettings, @Nullable Template template)
	{
		
		if (transformedInfo.state.getBlock() == Blocks.JIGSAW)
		{
			CompoundNBT nbt = transformedInfo.nbt;
			if (nbt != null)
			{
				String oldPool = nbt.getString(POOL);
				StringNBT newPool = this.cache.computeIfAbsent(oldPool, this::editPoolName);
				CompoundNBT newNBT = nbt.copy();
				newNBT.put(POOL, newPool);
				return new Template.BlockInfo(transformedInfo.pos, transformedInfo.state, newNBT);
			}
		}

		return transformedInfo;
	}
	
	protected StringNBT editPoolName(String oldPool)
	{
		return StringNBT.valueOf(oldPool.replace(this.input, this.output));
	}

}
