package commoble.workshopsofdoom.structure_pieces;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.gen.feature.template.IStructureProcessorType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.StructureProcessor;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.Template.EntityInfo;
import net.minecraft.world.server.ServerWorld;

public class ItemFrameLootProcessor extends StructureProcessor
{
	public static final Codec<ItemFrameLootProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("loot_table").forGetter(ItemFrameLootProcessor::getLootTable)
		).apply(instance, ItemFrameLootProcessor::new));
	
	public static final IStructureProcessorType<ItemFrameLootProcessor> DESERIALIZER = () -> CODEC;

	private final ResourceLocation lootTable;	public ResourceLocation getLootTable() { return this.lootTable; }
	
	public ItemFrameLootProcessor(ResourceLocation lootTable)
	{
		this.lootTable = lootTable;
	}


	@Override
	protected IStructureProcessorType<?> getType()
	{
		return DESERIALIZER;
	}
	
	@Override
	public EntityInfo processEntity(IWorldReader world, BlockPos seedPos, EntityInfo rawEntityInfo, EntityInfo entityInfo, PlacementSettings placementSettings, Template template)
	{
		EntityInfo currentInfo = super.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
		
		CompoundNBT entityNBT = currentInfo.nbt;
		
		String id = entityNBT.getString("id"); // entity type ID
		if (world instanceof IServerWorld && EntityType.ITEM_FRAME.getRegistryName().toString().equals(id))
		{
			ServerWorld serverWorld = ((IServerWorld)world).getWorld();
			this.writeEntityNBT(serverWorld, currentInfo.blockPos, entityNBT, placementSettings);
		}
		
		return currentInfo;
	}
	
	protected void writeEntityNBT(ServerWorld world, BlockPos pos, CompoundNBT nbt, PlacementSettings settings)
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
	
	protected ItemStack generateItemStack(ServerWorld world, BlockPos pos)
	{
		LootContext context = new LootContext.Builder(world)
			.withParameter(LootParameters.field_237457_g_, Vector3d.copyCentered(pos)) // positional context
			.build(LootParameterSets.CHEST);	// chest set requires positional context, has no other mandatory parameters
    
		LootTable table = world.getServer()
			.getLootTableManager()
			.getLootTableFromLocation(this.lootTable);
		List<ItemStack> stacks = table.generate(context);
		return stacks.size() > 0
			? stacks.get(0)
			: ItemStack.EMPTY;
	}
}
