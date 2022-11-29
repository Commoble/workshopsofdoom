package commoble.workshopsofdoom.rule_tests;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;

// composite rule test
// returns true if all of its subrules return true
// returns false if any subrules return false
// returns true if list of subrules is empty
public class AndRuleTest extends RuleTest
{
	public static final Codec<AndRuleTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RuleTest.CODEC.listOf().fieldOf("predicates").forGetter(AndRuleTest::getPredicates)
		).apply(instance, AndRuleTest::new));

	private final List<RuleTest> predicates;	public List<RuleTest> getPredicates() { return this.predicates; }

	public AndRuleTest(List<RuleTest> predicates)
	{
		this.predicates = predicates;
	}

	@Override
	public boolean test(BlockState state, RandomSource random)
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
	protected RuleTestType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.andRuleTest.get();
	}
}
