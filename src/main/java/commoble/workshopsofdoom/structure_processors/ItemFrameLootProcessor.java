package commoble.workshopsofdoom.structure_processors;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class ItemFrameLootProcessor extends StructureProcessor
{
	public static final Codec<ItemFrameLootProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("loot_table").forGetter(ItemFrameLootProcessor::getLootTable)
		).apply(instance, ItemFrameLootProcessor::new));

	private final ResourceLocation lootTable;	public ResourceLocation getLootTable() { return this.lootTable; }
	
	public ItemFrameLootProcessor(ResourceLocation lootTable)
	{
		this.lootTable = lootTable;
	}


	@Override
	protected StructureProcessorType<?> getType()
	{
		return WorkshopsOfDoom.INSTANCE.itemFrameLootProcessor.get();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos, StructureEntityInfo rawEntityInfo, StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
	{
		StructureEntityInfo currentInfo = super.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
		
		CompoundTag entityNBT = currentInfo.nbt;
		
		String id = entityNBT.getString("id"); // entity type ID
		if (world instanceof ServerLevelAccessor && EntityType.ITEM_FRAME.builtInRegistryHolder().key().location().toString().equals(id))
		{
			ServerLevel serverWorld = ((ServerLevelAccessor)world).getLevel();
			this.writeEntityNBT(serverWorld, currentInfo.blockPos, entityNBT, placementSettings);
		}
		
		return currentInfo;
	}
	
	protected void writeEntityNBT(ServerLevel world, BlockPos pos, CompoundTag nbt, StructurePlaceSettings settings)
	{
		// generate and set itemstack
		ItemStack stack = this.generateItemStack(world, pos);
		nbt.put("Item", stack.serializeNBT());
	}
	
	protected ItemStack generateItemStack(ServerLevel world, BlockPos pos)
	{
		// Loot tables aren't threadsafe.
		// If the root loot table doesn't have a random sequence, the ServerLevel's random is used, which is not threadsafe.
		// If the root table DOES have a random sequence, the loot table's random is computeIfAbsent-ed, which is not threadsafe.
		// Only way to make them threadsafe seems to be to sneak our own random into the context builder,
		// which causes the other two things to not happen.
		long hashedSeed = world.getSeed() + world.dimension().location().hashCode() + pos.hashCode();
		RandomSource random = new XoroshiroRandomSource(hashedSeed);
		LootParams params = new LootParams.Builder(world)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)) // positional context
			.create(LootContextParamSets.CHEST);	// chest set requires positional context, has no other mandatory parameters
		LootTable table = world.getServer()
			.getLootData()
			.getLootTable(this.lootTable);
		var contextBuilder = new LootContext.Builder(params);
		// public net.minecraft.world.level.storage.loot.LootContext$Builder f_78958_ # random
		contextBuilder.random = random;
		LootContext context = contextBuilder.create(null); // we already set the random so it will ignore the id param here
		// public net.minecraft.world.level.storage.loot.LootTable m_230922_(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList; # getRandomItems
		List<ItemStack> stacks = table.getRandomItems(context);
		return stacks.size() > 0
			? stacks.get(0)
			: ItemStack.EMPTY;
	}
}
