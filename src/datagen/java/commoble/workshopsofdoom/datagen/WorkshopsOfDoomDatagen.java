package commoble.workshopsofdoom.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import commoble.structure_spawn_loader.StructureSpawnEntry;
import commoble.structure_spawn_loader.StructureSpawnEntryDataProvider;
import commoble.workshopsofdoom.WorkshopsOfDoom;
import commoble.workshopsofdoom.util.MultiConsumer;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod(WorkshopsOfDoomDatagen.DATAGEN_MODID)
@EventBusSubscriber(modid=WorkshopsOfDoomDatagen.DATAGEN_MODID, bus=Bus.MOD)
public class WorkshopsOfDoomDatagen
{
	public static final String DATAGEN_MODID = "workshopsofdoom_datagen";
	
	public static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.create();
	
	@SuppressWarnings("unchecked")
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event)
	{
		DataGenerator generator = event.getGenerator();
		
		var structureSpawnEntries = new HashMap<String, StructureSpawnEntry>();
		Consumer<StructureSpawnEntry> spawnPutter = entry ->
		{
			ResourceLocation structureId = entry.structure().get().getRegistryName();
			ResourceLocation entityId = entry.spawner().get().type.getRegistryName();
			String entryId = String.join("/", structureId.getNamespace(), structureId.getPath(), entityId.getNamespace(), entityId.getPath());
			structureSpawnEntries.put(entryId, entry);
		};
		List<SpawnerData> standardMonsters = Lists.newArrayList(
			new SpawnerData(EntityType.SPIDER, 100, 4, 4),
			new SpawnerData(EntityType.CREEPER, 100, 4, 4),
			new SpawnerData(EntityType.SLIME, 100, 4, 4),
			new SpawnerData(EntityType.ENDERMAN, 10, 1, 4),
			new SpawnerData(EntityType.WITCH, 10, 1, 1)
			);
		List<SpawnerData> standardZombies = Lists.newArrayList(
			new SpawnerData(EntityType.ZOMBIE, 95, 4, 4),
			new SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 4, 4)
			);
		List<SpawnerData> desertZombies = Lists.newArrayList(
			new SpawnerData(EntityType.ZOMBIE, 19, 4, 4),
			new SpawnerData(EntityType.ZOMBIE_VILLAGER, 1, 1, 1),
			new SpawnerData(EntityType.HUSK, 80, 4, 4)
			);
		List<SpawnerData> standardSkeletons = Lists.newArrayList(
			new SpawnerData(EntityType.SKELETON, 100, 4, 4)
			);
		List<SpawnerData> coldSkeletons = Lists.newArrayList(
			new SpawnerData(EntityType.SKELETON, 20, 4, 4),
			new SpawnerData(EntityType.STRAY, 80, 4, 4)
			);
		List<SpawnerData> excavationPillagers = Lists.newArrayList(
			new SpawnerData(EntityType.PILLAGER, 1000, 1, 1),
			new SpawnerData(WorkshopsOfDoom.INSTANCE.excavator.get(), 1000, 1, 4)
			);
		List<SpawnerData> workshopPillagers = Lists.newArrayList(
			new SpawnerData(EntityType.PILLAGER, 500, 1, 1),
			new SpawnerData(WorkshopsOfDoom.INSTANCE.excavator.get(), 500, 1, 4),
			new SpawnerData(EntityType.VINDICATOR, 1, 1, 1)
			);
		
		MultiConsumer<StructureFeature<?>, List<SpawnerData>> spawnRegistrar = (structure, spawnLists) ->
		{
			for (List<SpawnerData> spawnList : spawnLists)
			{
				for (SpawnerData spawnData : spawnList)
				{
					spawnPutter.accept(new StructureSpawnEntry(structure, spawnData));
				}
			}
		};
		
		spawnRegistrar.accept(WorkshopsOfDoom.INSTANCE.desertQuarry.get(), standardMonsters, desertZombies, standardSkeletons, excavationPillagers);
		spawnRegistrar.accept(WorkshopsOfDoom.INSTANCE.plainsQuarry.get(), standardMonsters, desertZombies, standardSkeletons, excavationPillagers);
		spawnRegistrar.accept(WorkshopsOfDoom.INSTANCE.mountainsMines.get(), standardMonsters, standardZombies, standardSkeletons, excavationPillagers);
		spawnRegistrar.accept(WorkshopsOfDoom.INSTANCE.badlandsMines.get(), standardMonsters, standardZombies, standardSkeletons, excavationPillagers);
		spawnRegistrar.accept(WorkshopsOfDoom.INSTANCE.workshop.get(), standardMonsters, standardZombies, coldSkeletons, workshopPillagers);
		
		var structureSpawnEntryProvider = new StructureSpawnEntryDataProvider(GSON, generator, WorkshopsOfDoom.MODID, structureSpawnEntries);
		generator.addProvider(structureSpawnEntryProvider);
	}
}
