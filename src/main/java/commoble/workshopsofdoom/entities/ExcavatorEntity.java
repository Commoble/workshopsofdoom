package commoble.workshopsofdoom.entities;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class ExcavatorEntity extends Vindicator
{

	public ExcavatorEntity(EntityType<? extends ExcavatorEntity> type, Level worldIn)
	{
		super(type, worldIn);
	}

	/**
	 * Returns new PathNavigateGround instance
	 */
	@Override
	protected PathNavigation createNavigation(Level worldIn)
	{
		return new ExcavatorEntity.Navigator(this, worldIn);
	}

	@Override
	public boolean canJoinRaid()
	{
		return false;
	}
	
	@Override
	public boolean canBeLeader()
	{
		return false;
	}

	public ItemStack getStartingWeapon(RandomSource rand)
	{
		Item item = rand.nextBoolean() ? Items.IRON_PICKAXE : Items.IRON_SHOVEL;
		return new ItemStack(item);
	}

	@Override
	public void applyRaidBuffs(int wave, boolean p_213660_2_)
	{
		@SuppressWarnings("resource")
		ItemStack itemstack = this.getStartingWeapon(this.level().random);
		Raid raid = this.getCurrentRaid();
		int i = 1;
		if (wave > raid.getNumGroups(Difficulty.NORMAL))
		{
			i = 2;
		}

		boolean flag = this.random.nextFloat() <= raid.getEnchantOdds();
		if (flag)
		{
			Map<Enchantment, Integer> map = Maps.newHashMap();
			map.put(Enchantments.SHARPNESS, i);
			EnchantmentHelper.setEnchantments(map, itemstack);
		}

		this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
	}

	/**
	 * Gives armor or weapon for entity based on given DifficultyInstance
	 */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource rand, DifficultyInstance difficulty)
	{
		if (this.getCurrentRaid() == null)
		{
			this.setItemSlot(EquipmentSlot.MAINHAND, this.getStartingWeapon(rand));
		}

	}

	// make sure we can path over rails
	// just setting the path weight of UNPASSABLE_RAIL to 0 doesn't work as there are a few checks that assume
	// that UNPASSABLE_RAILs can't be pathed through
	// so we need to ensure that rails aren't classified as UNPASSABLE_RAILS at all
	// overriding evaluateBlockPathType (process path node type) in the WalkNodeProcessor to ensure that UNPASSABLE_RAIL
	// is never returned should be sufficient
	static class Navigator extends GroundPathNavigation
	{
		public Navigator(Mob mob, Level world)
		{
			super(mob, world);
		}

		@Override
		protected PathFinder createPathFinder(int followRangeTimesSixteen)
		{
			this.nodeEvaluator = new ExcavatorEntity.Processor();
			return new PathFinder(this.nodeEvaluator, followRangeTimesSixteen);
		}
	}

	static class Processor extends WalkNodeEvaluator
	{
		private Processor()
		{
		}

		@Override
		protected BlockPathTypes evaluateBlockPathType(BlockGetter world, BlockPos pos, BlockPathTypes pathNodeType)
		{
			BlockPathTypes base = super.evaluateBlockPathType(world, pos, pathNodeType);
			return base == BlockPathTypes.RAIL || base == BlockPathTypes.UNPASSABLE_RAIL
				? BlockPathTypes.WALKABLE
				: base;
		}
	}

}
