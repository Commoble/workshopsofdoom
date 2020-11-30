package commoble.workshopsofdoom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.workshopsofdoom.util.ConfigHelper;
import commoble.workshopsofdoom.util.ConfigHelper.ConfigObjectListener;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public class ServerConfig
{
	static final Logger LOGGER = LogManager.getLogger();
	public static final Codec<SpawnDataList> SPAWN_LIST_CODEC = SpawnData.CODEC.listOf()
		.xmap(SpawnDataList::new, SpawnDataList::getEntries);
	
	public final ConfigObjectListener<SpawnDataList> desertQuarryMonsters;
	public final ConfigObjectListener<SpawnDataList> plainsQuarryMonsters;
	public final ConfigObjectListener<SpawnDataList> mountainMinesMonsters;
	
	static final SpawnDataList DEFAULT_QUARRY_MONSTERS = new SpawnDataList(
		new SpawnData(EntityType.PILLAGER.getRegistryName(), 100, 1, 4),
		new SpawnData(WorkshopsOfDoom.INSTANCE.excavator.getId(), 100, 1, 4));
	
	
	public ServerConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		builder.push("spawns");
		this.desertQuarryMonsters = subscriber.subscribeObject(builder, Names.DESERT_QUARRY, SPAWN_LIST_CODEC, DEFAULT_QUARRY_MONSTERS);
		this.plainsQuarryMonsters = subscriber.subscribeObject(builder, Names.PLAINS_QUARRY, SPAWN_LIST_CODEC, DEFAULT_QUARRY_MONSTERS);
		this.mountainMinesMonsters = subscriber.subscribeObject(builder, Names.MOUNTAIN_MINES, SPAWN_LIST_CODEC, DEFAULT_QUARRY_MONSTERS);
		builder.pop();
	}
	
	// helper class to initialize spawner lists before entity types are registered
	public static class SpawnData
	{
		public static final Codec<SpawnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("entity").forGetter(SpawnData::getEntityID),
				Codec.INT.fieldOf("weight").forGetter(SpawnData::getWeight),
				Codec.INT.fieldOf("min").forGetter(SpawnData::getMin),
				Codec.INT.fieldOf("max").forGetter(SpawnData::getMax)
			).apply(instance, SpawnData::new));
		
	
		private final ResourceLocation entityID;	public ResourceLocation getEntityID() { return this.entityID; }
		private final int weight; public int getWeight() { return this.weight; }
		private final int min;	public int getMin() { return this.min; }
		private final int max;	public int getMax() { return this.max; }
		
		public SpawnData(ResourceLocation entityID, int weight, int min, int max)
		{
			this.entityID = entityID;
			this.weight = weight;
			this.min = min;
			this.max = max;
		}
		
		public DataResult<EntityType<?>> getEntityType()
		{
			return ForgeRegistries.ENTITIES.containsKey(this.entityID)
				? DataResult.success(ForgeRegistries.ENTITIES.getValue(this.entityID))
				: DataResult.error(String.format("No entity type registered for identifier: %s", this.entityID.toString()));
		}
		
		public DataResult<Spawners> getSpawnEntry()
		{
			return this.getEntityType().map(this::toSpawner);
		}
		
		public Spawners toSpawner(EntityType<?> validatedType)
		{
			return new Spawners(validatedType, this.weight, this.min, this.max);
		}
	}
	
	public static class SpawnDataList implements Supplier<List<Spawners>>
	{
		private final List<SpawnData> entries; public List<SpawnData> getEntries() { return this.entries; }
		private final Supplier<List<Spawners>> validatedSpawns = Suppliers.memoize(this::validateData);
		
		public SpawnDataList(List<SpawnData> entries)
		{
			this.entries = entries;
		}
		
		public SpawnDataList(SpawnData...entries)
		{
			this(Lists.newArrayList(entries));
		}

		@Override
		public List<Spawners> get()
		{
			return this.validatedSpawns.get();
		}
		
		public List<Spawners> validateData()
		{
			List<Spawners> results = new ArrayList<>();
			for(SpawnData data : this.entries)
			{
				data.getSpawnEntry() // returns DataResult<Spawners>
					.resultOrPartial(ServerConfig.LOGGER::warn) // log if bad data
					.ifPresent(results::add); // save if good data
			}
			return results;
		}
		
	}
}
