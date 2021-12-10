package commoble.workshopsofdoom.rule_tests;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class ChanceRuleTest extends RuleTest
{
	public static final Codec<ChanceRuleTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("probability").forGetter(ChanceRuleTest::getProbability)
		).apply(instance, ChanceRuleTest::new));
	
	public static final RuleTestType<ChanceRuleTest> DESERIALIZER = () -> CODEC;

	private final float probability;	public float getProbability() { return this.probability; }

	public ChanceRuleTest(float probability)
	{
		this.probability = probability;
	}

	@Override
	public boolean test(BlockState state, Random random)
	{
		return random.nextFloat() < this.probability;
	}

	@Override
	protected RuleTestType<?> getType()
	{
		return DESERIALIZER;
	}
}
