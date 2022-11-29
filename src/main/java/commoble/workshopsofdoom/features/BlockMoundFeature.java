package commoble.workshopsofdoom.features;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

// similar to BlockPileFeature but has some adjustments that make it more suitable for structure feature jigsaws
// size has slightly more variability as well
public class BlockMoundFeature extends Feature<BlockPileConfiguration>
{
	public BlockMoundFeature(Codec<BlockPileConfiguration> codec)
	{
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<BlockPileConfiguration> context)
	{
		BlockPos pos = context.origin();
		if (pos.getY() < 5)
		{
			return false;
		}
		
		RandomSource rand = context.random();
		WorldGenLevel level = context.level();
		BlockPileConfiguration config = context.config();
		
		int xOff = 2 + rand.nextInt(2);
		int zOff = 2 + rand.nextInt(2);
		int ySize = 1 + rand.nextInt(4);

		for (BlockPos nextPos : BlockPos.betweenClosed(pos.offset(-xOff, 0, -zOff), pos.offset(xOff, ySize, zOff)))
		{
			int xDiff = pos.getX() - nextPos.getX();
			int zDiff = pos.getZ() - nextPos.getZ();
			if (xDiff * xDiff + zDiff * zDiff <= rand.nextFloat() * 16.0F - rand.nextFloat() * 12.0F)
			{
				this.setBlock(level, nextPos, rand, config);
			}
			else if (rand.nextFloat() < 0.031D)
			{
				this.setBlock(level, nextPos, rand, config);
			}
		}

		return true;
	}

	private boolean canPlaceOn(WorldGenLevel worldIn, BlockPos pos, RandomSource random)
	{
		BlockPos belowPos = pos.below();
		BlockState belowState = worldIn.getBlockState(belowPos);
		return belowState.is(Blocks.DIRT_PATH) ? random.nextBoolean() : belowState.isFaceSturdy(worldIn, belowPos, Direction.UP);
	}

	private void setBlock(WorldGenLevel world, BlockPos pos, RandomSource rand, BlockPileConfiguration config)
	{
		if (world.isEmptyBlock(pos) && this.canPlaceOn(world, pos, rand))
		{
			// block pile uses flag 4, which doesn't sync the change to the client, stupidly
			world.setBlock(pos, config.stateProvider.getState(rand, pos), 2);
		}

	}

}
