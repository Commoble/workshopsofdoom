package commoble.workshopsofdoom.features;

import java.util.Random;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.BlockStateProvidingFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

// similar to BlockPileFeature but has some adjustments that make it more suitable for structure feature jigsaws
// size has slightly more variability as well
public class BlockMoundFeature extends Feature<BlockStateProvidingFeatureConfig>
{
	public BlockMoundFeature(Codec<BlockStateProvidingFeatureConfig> codec)
	{
		super(codec);
	}

	@Override
	public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, BlockStateProvidingFeatureConfig config)
	{
		if (pos.getY() < 5)
		{
			return false;
		}
		else
		{
			int xOff = 2 + rand.nextInt(2);
			int zOff = 2 + rand.nextInt(2);
			int ySize = 1 + rand.nextInt(4);

			for (BlockPos nextPos : BlockPos.getAllInBoxMutable(pos.add(-xOff, 0, -zOff), pos.add(xOff, ySize, zOff)))
			{
				int xDiff = pos.getX() - nextPos.getX();
				int zDiff = pos.getZ() - nextPos.getZ();
				if (xDiff * xDiff + zDiff * zDiff <= rand.nextFloat() * 16.0F - rand.nextFloat() * 12.0F)
				{
					this.setBlock(reader, nextPos, rand, config);
				}
				else if (rand.nextFloat() < 0.031D)
				{
					this.setBlock(reader, nextPos, rand, config);
				}
			}

			return true;
		}
	}

	private boolean canPlaceOn(IWorld worldIn, BlockPos pos, Random random)
	{
		BlockPos belowPos = pos.down();
		BlockState belowState = worldIn.getBlockState(belowPos);
		return belowState.isIn(Blocks.GRASS_PATH) ? random.nextBoolean() : belowState.isSolidSide(worldIn, belowPos, Direction.UP);
	}

	private void setBlock(IWorld world, BlockPos pos, Random rand, BlockStateProvidingFeatureConfig config)
	{
		if (world.isAirBlock(pos) && this.canPlaceOn(world, pos, rand))
		{
			// block pile uses flag 4, which doesn't sync the change to the client, stupidly
			world.setBlockState(pos, config.field_227268_a_.getBlockState(rand, pos), 2);
		}

	}

}
