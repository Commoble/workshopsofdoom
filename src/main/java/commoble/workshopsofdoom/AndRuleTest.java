package commoble.workshopsofdoom;

import java.util.List;
import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.template.IRuleTestType;
import net.minecraft.world.gen.feature.template.RuleTest;

// composite rule test
// returns true if all of its subrules return true
// returns false if any subrules return false
// returns true if list of subrules is empty
public class AndRuleTest extends RuleTest
{
	public static final Codec<AndRuleTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RuleTest.field_237127_c_.listOf().fieldOf("predicates").forGetter(AndRuleTest::getPredicates)
		).apply(instance, AndRuleTest::new));
	
	public static final IRuleTestType<AndRuleTest> DESERIALIZER = () -> CODEC;

	private final List<RuleTest> predicates;	public List<RuleTest> getPredicates() { return this.predicates; }

	public AndRuleTest(List<RuleTest> predicates)
	{
		this.predicates = predicates;
	}

	@Override
	public boolean test(BlockState state, Random random)
	{
		for (RuleTest test : this.predicates)
		{
			if (!test.test(state, random))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	protected IRuleTestType<?> getType()
	{
		return DESERIALIZER;
	}
}
