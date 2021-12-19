package commoble.workshopsofdoom.loot_modifiers;

import java.util.List;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

public class AddTableModifier extends LootModifier
{
	private final ResourceLocation table;

	protected AddTableModifier(LootItemCondition[] conditionsIn, ResourceLocation table)
	{
		super(conditionsIn);
		this.table = table;
	}

	@Override
	protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context)
	{
		LootTable extraTable = context.getLootTable(this.table);
		extraTable.getRandomItems(context, generatedLoot::add); // don't run loot modifiers for subtables
		return generatedLoot;
	}

	public static class Serializer extends GlobalLootModifierSerializer<AddTableModifier>
	{
		@Override
		public AddTableModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions)
		{
			ResourceLocation table = new ResourceLocation(GsonHelper.getAsString(object, "table"));
			return new AddTableModifier(conditions, table);
		}

		@Override
		public JsonObject write(AddTableModifier instance)
		{
			JsonObject object = this.makeConditions(instance.conditions);
			object.addProperty("table", instance.table.toString());
			return object;
		}
		
	}
}
