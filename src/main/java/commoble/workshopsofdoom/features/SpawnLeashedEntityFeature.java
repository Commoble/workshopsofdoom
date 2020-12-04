package commoble.workshopsofdoom.features;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.features.SpawnLeashedEntityFeature.LeashedEntityConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.server.ServerWorld;

public class SpawnLeashedEntityFeature extends Feature<LeashedEntityConfig>
{
	public SpawnLeashedEntityFeature(Codec<LeashedEntityConfig> codec)
	{
		super(codec);
	}

	@Override
	public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, LeashedEntityConfig config)
	{
		ServerWorld serverWorld = reader.getWorld();
		Entity entity = config.entityType.create(serverWorld);
		if (entity instanceof MobEntity) // only mobs can be leashed
		{
			MobEntity mob = (MobEntity) entity;
			mob.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, MathHelper.wrapDegrees(rand.nextFloat() * 360.0F), 0.0F);
			// this bit comes from EntityType::spawn
			CompoundNBT newNBT = config.getNBT().orElseGet(CompoundNBT::new);
			CompoundNBT entityNbt = mob.writeWithoutTypeId(new CompoundNBT());
			
			// mark the mob as leashed
			// we can't just create the leash knot entity
			// because leash knot entities aren't serialized
			// so if we add the leash during chunk generation, it will get yeeted when the world reifies
			// we work around this by adding a leash position to the entity's nbt
			// (which is converted to a leash entity in its first tick)
			BlockPos leashPos = pos.add(config.leashOffset); // leash position in actual worldspace
            UUID uuid = mob.getUniqueID();
            entityNbt.merge(newNBT);
            entityNbt.put("Leash", NBTUtil.writeBlockPos(leashPos));
            mob.setUniqueId(uuid);
            mob.read(entityNbt);
			
			// if we don't enable persistance
			// then the entity will despawn instantly if it isn't normally persistant
			// as structures generate well outside of the instant-despawn range
			// so there's no point in making transient entities via structure generation
			mob.enablePersistence();
			mob.onInitialSpawn(reader, reader.getDifficultyForLocation(pos), SpawnReason.STRUCTURE, (ILivingEntityData)null, entity.serializeNBT());

			// add entity and any riders
			reader.func_242417_l(mob);
			
			return true;
		}

		return false;
	}

	public static class LeashedEntityConfig implements IFeatureConfig
	{
		@SuppressWarnings("deprecation")
		public static final Codec<LeashedEntityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Registry.ENTITY_TYPE.fieldOf("entity").forGetter(LeashedEntityConfig::getEntityType),
				BlockPos.CODEC.fieldOf("leash_offset").forGetter(LeashedEntityConfig::getLeashOffset),
				CompoundNBT.CODEC.optionalFieldOf("nbt").forGetter(LeashedEntityConfig::getNBT)
			).apply(instance, LeashedEntityConfig::new));

		private final EntityType<?> entityType;
		private final BlockPos leashOffset;
		private final @Nullable Optional<CompoundNBT> nbt;

		public LeashedEntityConfig(EntityType<?> entityType, BlockPos leashOffset, Optional<CompoundNBT> nbt)
		{
			this.entityType = entityType;
			this.leashOffset = leashOffset;
			this.nbt = nbt;
		}

		public EntityType<?> getEntityType()
		{
			return this.entityType;
		}
		
		public BlockPos getLeashOffset()
		{
			return this.leashOffset;
		}

		public Optional<CompoundNBT> getNBT()
		{
			return this.nbt;
		}
	}
}
