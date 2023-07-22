package commoble.workshopsofdoom.features;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.features.SpawnLeashedEntityFeature.LeashedEntityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraftforge.event.ForgeEventFactory;

public class SpawnLeashedEntityFeature extends Feature<LeashedEntityConfig>
{
	public SpawnLeashedEntityFeature(Codec<LeashedEntityConfig> codec)
	{
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<LeashedEntityConfig> context)
	{
		WorldGenLevel level = context.level();
		ServerLevel serverLevel = level.getLevel();
		LeashedEntityConfig config = context.config();
		Entity entity = config.entityType.create(serverLevel);
		if (!(entity instanceof Mob)) // only mobs can be leashed
			return false;

		Mob mob = (Mob) entity;
		BlockPos pos = context.origin();
		RandomSource rand = context.random();
		
		mob.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, Mth.wrapDegrees(rand.nextFloat() * 360.0F), 0.0F);
		// this bit comes from EntityType::spawn
		CompoundTag newNBT = config.getNBT().orElseGet(CompoundTag::new);
		CompoundTag entityNbt = mob.saveWithoutId(new CompoundTag());
		
		// mark the mob as leashed
		// we can't just create the leash knot entity
		// because leash knot entities aren't serialized
		// so if we add the leash during chunk generation, it will get yeeted when the world reifies
		// we work around this by adding a leash position to the entity's nbt
		// (which is converted to a leash entity in its first tick)
		BlockPos leashPos = pos.offset(config.leashOffset); // leash position in actual worldspace
        UUID uuid = mob.getUUID();
        entityNbt.merge(newNBT);
        entityNbt.put("Leash", NbtUtils.writeBlockPos(leashPos));
        mob.setUUID(uuid);
        mob.load(entityNbt);
		
		// if we don't enable persistance
		// then the entity will despawn instantly if it isn't normally persistant
		// as structures generate well outside of the instant-despawn range
		// so there's no point in making transient entities via structure generation
		mob.setPersistenceRequired();
		ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, (SpawnGroupData)null, entity.serializeNBT());

		// add entity and any riders
		level.addFreshEntityWithPassengers(mob);
		
		return true;
	}

	public static class LeashedEntityConfig implements FeatureConfiguration
	{
		@SuppressWarnings("deprecation")
		public static final Codec<LeashedEntityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(LeashedEntityConfig::getEntityType),
				BlockPos.CODEC.fieldOf("leash_offset").forGetter(LeashedEntityConfig::getLeashOffset),
				CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(LeashedEntityConfig::getNBT)
			).apply(instance, LeashedEntityConfig::new));

		private final EntityType<?> entityType;
		private final BlockPos leashOffset;
		private final @Nullable Optional<CompoundTag> nbt;

		public LeashedEntityConfig(EntityType<?> entityType, BlockPos leashOffset, Optional<CompoundTag> nbt)
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

		public Optional<CompoundTag> getNBT()
		{
			return this.nbt;
		}
	}
}
