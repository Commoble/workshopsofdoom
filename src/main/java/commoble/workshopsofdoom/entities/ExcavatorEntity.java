package commoble.workshopsofdoom.entities;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.VindicatorEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.raid.Raid;

public class ExcavatorEntity extends VindicatorEntity
{

	public ExcavatorEntity(EntityType<? extends ExcavatorEntity> type, World worldIn)
	{
		super(type, worldIn);
	}

	public static AttributeModifierMap.MutableAttribute getAttributes()
	{
		return MonsterEntity.func_234295_eP_()
			.createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.35F)
			.createMutableAttribute(Attributes.FOLLOW_RANGE, 12.0D)
			.createMutableAttribute(Attributes.MAX_HEALTH, 24.0D)
			.createMutableAttribute(Attributes.ATTACK_DAMAGE, 4.0D)
			.createMutableAttribute(Attributes.ARMOR, 2.0D);
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

	public ItemStack getStartingWeapon()
	{
		Item item = this.world.rand.nextBoolean() ? Items.IRON_PICKAXE : Items.IRON_SHOVEL;
		return new ItemStack(item);
	}

	@Override
	public void applyWaveBonus(int wave, boolean p_213660_2_)
	{
		ItemStack itemstack = this.getStartingWeapon();
		Raid raid = this.getRaid();
		int i = 1;
		if (wave > raid.getWaves(Difficulty.NORMAL))
		{
			i = 2;
		}

		boolean flag = this.rand.nextFloat() <= raid.getEnchantOdds();
		if (flag)
		{
			Map<Enchantment, Integer> map = Maps.newHashMap();
			map.put(Enchantments.SHARPNESS, i);
			EnchantmentHelper.setEnchantments(map, itemstack);
		}

		this.setItemStackToSlot(EquipmentSlotType.MAINHAND, itemstack);
	}

	/**
	 * Gives armor or weapon for entity based on given DifficultyInstance
	 */
	@Override
	protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty)
	{
		if (this.getRaid() == null)
		{
			this.setItemStackToSlot(EquipmentSlotType.MAINHAND, this.getStartingWeapon());
		}

	}

}
