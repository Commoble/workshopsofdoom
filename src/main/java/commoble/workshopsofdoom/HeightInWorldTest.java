package commoble.workshopsofdoom;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.IPosRuleTests;
import net.minecraft.world.gen.feature.template.PosRuleTest;

public class HeightInWorldTest extends PosRuleTest
{
	public static final Codec<HeightInWorldTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("min", 0).forGetter(HeightInWorldTest::getMin),
			Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(HeightInWorldTest::getMax)
		).apply(instance, HeightInWorldTest::new));
	
	public static final IPosRuleTests<HeightInWorldTest> DESERIALIZER = () -> CODEC;

	private final int min;	public int getMin() { return this.min; }
	private final int max;	public int getMax() { return this.max; }

	public HeightInWorldTest(int min, int max)
	{
		this.min = min;
		this.max = max;
	}

	@Override
	public boolean func_230385_a_(BlockPos originalPos, BlockPos transformedPos, BlockPos structureOrigin, Random rand)
	{
		int y = transformedPos.getY();
		return y >= this.min && y <= this.max;
	}

	@Override
	protected IPosRuleTests<?> func_230384_a_()
	{
		return DESERIALIZER;
	}
}
