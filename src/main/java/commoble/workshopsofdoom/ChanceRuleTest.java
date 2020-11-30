package commoble.workshopsofdoom;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.template.IRuleTestType;
import net.minecraft.world.gen.feature.template.RuleTest;

public class ChanceRuleTest extends RuleTest
{
	public static final Codec<ChanceRuleTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("probability").forGetter(ChanceRuleTest::getProbability)
		).apply(instance, ChanceRuleTest::new));
	
	public static final IRuleTestType<ChanceRuleTest> DESERIALIZER = () -> CODEC;

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
	protected IRuleTestType<?> getType()
	{
		return DESERIALIZER;
	}
}
