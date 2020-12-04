package commoble.workshopsofdoom.features;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.features.SpawnEntityFeature.EntityConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;

public class SpawnEntityFeature extends Feature<EntityConfig>
{
	public SpawnEntityFeature(Codec<EntityConfig> codec)
	{
		super(codec);
	}

	@Override
	public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, EntityConfig config)
	{
		Entity entity = config.entityType.create(reader.getWorld());
		if (entity != null)
		{
			entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, MathHelper.wrapDegrees(rand.nextFloat() * 360.0F), 0.0F);
			// this bit comes from EntityType::spawn
			config.getNBT().ifPresent(nbt -> {
				CompoundNBT entityNbt = entity.writeWithoutTypeId(new CompoundNBT());
	            UUID uuid = entity.getUniqueID();
	            entityNbt.merge(nbt);
	            entity.setUniqueId(uuid);
	            entity.read(entityNbt);
			});
			if (entity instanceof MobEntity)
			{
				MobEntity mob = (MobEntity) entity;
				// if we don't enable persistance
				// then the entity will despawn instantly
				// as structures generate well outside of the instant-despawn range
				// so there's no point in making transient entities via structure generation
				mob.enablePersistence();
				mob.onInitialSpawn(reader, reader.getDifficultyForLocation(pos), SpawnReason.STRUCTURE, (ILivingEntityData)null, entity.serializeNBT());
	            
			}
			// add entity and any riders
			reader.func_242417_l(entity);
			return true;
		}

		return false;
	}

	public static class EntityConfig implements IFeatureConfig
	{
		@SuppressWarnings("deprecation")
		public static final Codec<EntityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Registry.ENTITY_TYPE.fieldOf("entity").forGetter(EntityConfig::getEntityType),
				CompoundNBT.CODEC.optionalFieldOf("nbt").forGetter(EntityConfig::getNBT)).
			apply(instance, EntityConfig::new));

		private final EntityType<?> entityType;
		private final @Nullable Optional<CompoundNBT> nbt;

		public EntityConfig(EntityType<?> entityType, Optional<CompoundNBT> nbt)
		{
			this.entityType = entityType;
			this.nbt = nbt;
		}

		public EntityType<?> getEntityType()
		{
			return this.entityType;
		}

		public Optional<CompoundNBT> getNBT()
		{
			return this.nbt;
		}
	}
}
