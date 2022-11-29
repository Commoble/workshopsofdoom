package commoble.workshopsofdoom.structure_processors;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.server.level.ServerLevel;

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
		
		// determine direction
//		Direction oldFacing = Direction.byIndex(nbt.getByte("Facing"));
//		Direction newFacing = settings.getRotation().rotate(oldFacing);
//		newFacing = settings.getMirror().mirror(newFacing);
//		nbt.putByte("Facing", (byte)(newFacing.getIndex()));
//		
//		// set position
//		nbt.putInt("TileX", pos.getX());
//		nbt.putInt("TileY", pos.getY());
//		nbt.putInt("TileZ",  pos.getZ());
	}
	
	protected ItemStack generateItemStack(ServerLevel world, BlockPos pos)
	{
		LootContext context = new LootContext.Builder(world)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)) // positional context
			.create(LootContextParamSets.CHEST);	// chest set requires positional context, has no other mandatory parameters
    
		LootTable table = world.getServer()
			.getLootTables()
			.get(this.lootTable);
		List<ItemStack> stacks = table.getRandomItems(context);
		return stacks.size() > 0
			? stacks.get(0)
			: ItemStack.EMPTY;
	}
}
