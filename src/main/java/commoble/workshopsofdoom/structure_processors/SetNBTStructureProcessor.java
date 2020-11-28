package commoble.workshopsofdoom.structure_processors;

import java.util.Random;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundNBT;
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

// the rule entry structure processor allows the setting of NBT in template blockinfos,
// but it *requires* that the output state be specified
// which is problematic for things like chests where there are multiple valid states and the state shouldn't change
// so we make this processor that doesn't affect the blockstate, only the nbt
public class SetNBTStructureProcessor extends StructureProcessor
{
	public static final Codec<SetNBTStructureProcessor> CODEC = RecordCodecBuilder.create(instance -> instance
		.group(CompoundNBT.CODEC.fieldOf("nbt").forGetter(SetNBTStructureProcessor::getNBT),
			RuleTest.field_237127_c_.fieldOf("input_predicate").forGetter(SetNBTStructureProcessor::getInputPredicate),
			RuleTest.field_237127_c_.optionalFieldOf("location_predicate", AlwaysTrueRuleTest.INSTANCE).forGetter(SetNBTStructureProcessor::getLocationPredicate),
			PosRuleTest.field_237102_c_.optionalFieldOf("position_predicate", AlwaysTrueTest.field_237100_b_).forGetter(SetNBTStructureProcessor::getPositionPredicate))
		.apply(instance, SetNBTStructureProcessor::new));

	public static final IStructureProcessorType<SetNBTStructureProcessor> DESERIALIZER = () -> CODEC;

	/** The nbt to set for a given blockinfo in a structure template (required) **/
	public CompoundNBT getNBT()
	{
		return this.nbt;
	}

	private final CompoundNBT nbt;

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

	public SetNBTStructureProcessor(CompoundNBT nbt, RuleTest inputPredicate, RuleTest locationPredicate, PosRuleTest positionPredicate)
	{
		this.nbt = nbt;
		this.inputPredicate = inputPredicate;
		this.locationPredicate = locationPredicate;
		this.positionPredicate = positionPredicate;
	}

	@Override
	protected IStructureProcessorType<?> getType()
	{
		return DESERIALIZER;
	}

	@Override
	@Nullable
	public Template.BlockInfo process(@Nullable IWorldReader world, BlockPos offset, @Nullable BlockPos structureOrigin, Template.BlockInfo originalInfo, Template.BlockInfo transformedInfo,
		PlacementSettings placementSettings, @Nullable Template template)
	{
		Random random = new Random(MathHelper.getPositionRandom(transformedInfo.pos));
		if (this.inputPredicate.test(transformedInfo.state, random)
			&& this.locationPredicate.test(world.getBlockState(transformedInfo.pos), random)
			&& this.positionPredicate.func_230385_a_(originalInfo.pos, transformedInfo.pos, structureOrigin, random))
			{
				return new Template.BlockInfo(transformedInfo.pos, transformedInfo.state, this.nbt);
			}
		else
		{
			return transformedInfo;
		}
	}

}
