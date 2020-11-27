package commoble.workshopsofdoom.features;

import java.util.Optional;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.looot.data.DataCodecs;
import commoble.workshopsofdoom.features.SpawnEntityFeature.EntityConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnEntityFeature extends Feature<EntityConfig>
{
	public SpawnEntityFeature(Codec<EntityConfig> codec)
	{
		super(codec);
	}

	@Override
	public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, EntityConfig config)
	{
		ServerWorld world = reader.getWorld();
		CompoundNBT nbt = config.getSpawnNBT();
		Entity entity = config.entityType.spawn(world, nbt, null, null, pos, SpawnReason.CHUNK_GENERATION, false, false);
		if (entity != null)
		{
			return world.addEntity(entity);
		}

		return false;
	}

	public static class EntityConfig implements IFeatureConfig
	{
		public static final Codec<EntityConfig> CODEC = RecordCodecBuilder
			.create(instance -> instance.group(DataCodecs.makeRegistryEntryCodec(ForgeRegistries.ENTITIES).fieldOf("entity").forGetter(EntityConfig::getEntityType),
				CompoundNBT.CODEC.optionalFieldOf("nbt").forGetter(EntityConfig::getNBT)).apply(instance, EntityConfig::new));

		private final EntityType<?> entityType;

		public EntityType<?> getEntityType()
		{
			return this.entityType;
		}

		private final @Nullable Optional<CompoundNBT> nbt;

		public Optional<CompoundNBT> getNBT()
		{
			return this.nbt;
		}

		public EntityConfig(EntityType<?> entityType, Optional<CompoundNBT> nbt)
		{
			this.entityType = entityType;
			this.nbt = nbt;
		}

		// get a CompoundNBT that can be used as the nbt parameter for EntityType::spawn
		public CompoundNBT getSpawnNBT()
		{
			return this.getNBT().map(EntityConfig::getNBTWithData).orElse(null);
		}

		static CompoundNBT getNBTWithData(@Nonnull CompoundNBT configNBT)
		{
			CompoundNBT out = new CompoundNBT();
			out.put("EntityTag", configNBT.copy());
			return out;
		}
	}
}
