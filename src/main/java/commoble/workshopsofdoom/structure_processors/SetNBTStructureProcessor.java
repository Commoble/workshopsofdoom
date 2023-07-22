package commoble.workshopsofdoom.structure_processors;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// the rule entry structure processor allows the setting of NBT in template blockinfos,
// but it *requires* that the output state be specified
// which is problematic for things like chests where there are multiple valid states and the state shouldn't change
// so we make this processor that doesn't affect the blockstate, only the nbt
public class SetNBTStructureProcessor extends StructureProcessor
{
	public static final Codec<SetNBTStructureProcessor> CODEC = RecordCodecBuilder.create(instance -> instance
		.group(CompoundTag.CODEC.fieldOf("nbt").forGetter(SetNBTStructureProcessor::getNBT),
			RuleTest.CODEC.fieldOf("input_predicate").forGetter(SetNBTStructureProcessor::getInputPredicate),
			RuleTest.CODEC.optionalFieldOf("location_predicate", AlwaysTrueTest.INSTANCE).forGetter(SetNBTStructureProcessor::getLocationPredicate),
			PosRuleTest.CODEC.optionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(SetNBTStructureProcessor::getPositionPredicate))
		.apply(instance, SetNBTStructureProcessor::new));

	/** The nbt to set for a given blockinfo in a structure template (required) **/
	public CompoundTag getNBT()
	{
		return this.nbt;
	}

	private final CompoundTag nbt;

	// The predicate for an input blockinfo in a structure template (required) **/
	public RuleTest getInputPredicate()
	{
		return this.inputPredicate;
	}

	private final RuleTest inputPredicate;

	// The predicate for the existing block at the position a structure template's
	// blockinfo is overwriting (optional) **/
	public RuleTest getLocationPredicate()
	{
		return this.locationPredicate;
	}

	private final RuleTest locationPredicate;

	// The predicate for the positional data of a structure template's input
	// blockinfo **/
	public PosRuleTest getPositionPredicate()
	{
		return this.positionPredicate;
	}

	private final PosRuleTest positionPredicate;

	public SetNBTStructureProcessor(CompoundTag nbt, RuleTest inputPredicate, RuleTest locationPredicate, PosRuleTest positionPredicate)
	{
		this.nbt = nbt;
		this.inputPredicate = inputPredicate;
		this.locationPredicate = locationPredicate;
		this.positionPredicate = positionPredicate;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.setNbtStructureProcessor.get();
	}

	@Override
	@Nullable
	public StructureTemplate.StructureBlockInfo process(@Nullable LevelReader world, BlockPos offset, @Nullable BlockPos structureOrigin, StructureTemplate.StructureBlockInfo originalInfo, StructureTemplate.StructureBlockInfo transformedInfo,
		StructurePlaceSettings placementSettings, @Nullable StructureTemplate template)
	{
		RandomSource random = placementSettings.getRandom(transformedInfo.pos());
		if (this.inputPredicate.test(transformedInfo.state(), random)
			&& this.locationPredicate.test(world.getBlockState(transformedInfo.pos()), random)
			&& this.positionPredicate.test(originalInfo.pos(), transformedInfo.pos(), structureOrigin, random))
			{
				return new StructureTemplate.StructureBlockInfo(transformedInfo.pos(), transformedInfo.state(), this.nbt);
			}
		else
		{
			return transformedInfo;
		}
	}

}
