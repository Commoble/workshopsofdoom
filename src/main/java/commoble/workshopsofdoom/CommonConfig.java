package commoble.workshopsofdoom;

import commoble.workshopsofdoom.structures.LoadableJigsawStructure.LoadableJigsawConfig;
import commoble.workshopsofdoom.util.ConfigHelper;
import commoble.workshopsofdoom.util.ConfigHelper.ConfigObjectListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig
{
	public final ConfigObjectListener<LoadableJigsawConfig> desertQuarry;
	public final ConfigObjectListener<LoadableJigsawConfig> plainsQuarry;
	public final ConfigObjectListener<LoadableJigsawConfig> mountainsMines;
	public final ConfigObjectListener<LoadableJigsawConfig> badlandsMines;
	public final ConfigObjectListener<LoadableJigsawConfig> workshop;
	
	public CommonConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		builder.push("structures");
		
		builder.comment(
			"Ideally this configuration would be done via worldgen datapacks,",
			"but due to the way we're working around the hardcodedness inherent to structures,",
			"that doesn't seem to be working, so we're using forge configs for this for now.",
			"The values for each of these are as follows:",
			"* size -- the maximum iteration depth of the jigsaw placer for this structure.",
			"       -- Total structure size can increase exponentially with this",
			"       -- increasing this too high may cause dedicated servers to timeout and shut down",
			"       -- (this is safe to increase to 50 on singleplayer worlds, though it still takes a while to generate)",
			"* min_separation -- Minimum farapartness for generating instances of this structure",
			"* max_separation -- Maximum farapartness for generating instances of this structure",
			"       -- must be greater than min_separation or minecraft will crash",
			"",
			" The rest of these values should only be changed if worldgen datapacks are being used to significantly alter the structure:",
			"* start_pool -- which structure template pool json the first piece in the structure uses",
			"* snap_to_height_map -- if true, the first structure piece will be placed on the world's surface.",
			"                     -- if false, start_y is used instead. Defaults to true",
			"* start_y -- If snap_to_height_map is enabled, the first structure piece generates at this height.",
			"          -- Otherwise, this is ignored. Defaults to 0",
			"* enable_legacy_piece_intersactions -- if true, subtly alters the way the jigsaw placer determines",
			"          -- whether two jigsaw pieces overlap. Defaults to false. Not recommended to turn on",
			"",
			"Changing anything in this config file requires a total shutdown and reboot of the minecraft client or server to take effect."
			);
		
		this.desertQuarry = subscriber.subscribeObject(builder, "desert_quarry", LoadableJigsawConfig.CODEC, new LoadableJigsawConfig(new ResourceLocation(WorkshopsOfDoom.MODID, Names.DESERT_QUARRY_START), 7, 12, 32, 0, false, true));
		this.plainsQuarry = subscriber.subscribeObject(builder, "plains_quarry", LoadableJigsawConfig.CODEC, new LoadableJigsawConfig(new ResourceLocation(WorkshopsOfDoom.MODID, Names.PLAINS_QUARRY_START), 7, 12, 32, 0, false, true));	
		this.mountainsMines = subscriber.subscribeObject(builder, "mountain_mines", LoadableJigsawConfig.CODEC, new LoadableJigsawConfig(new ResourceLocation(WorkshopsOfDoom.MODID, Names.MOUNTAIN_MINES_START), 20, 12, 32, 0, false, true));	
		this.badlandsMines = subscriber.subscribeObject(builder, "badlands_mines", LoadableJigsawConfig.CODEC, new LoadableJigsawConfig(new ResourceLocation(WorkshopsOfDoom.MODID, Names.BADLANDS_MINES_START), 20, 12, 32, 0, false, true));	
		this.workshop = subscriber.subscribeObject(builder, "workshop", LoadableJigsawConfig.CODEC, new LoadableJigsawConfig(new ResourceLocation(WorkshopsOfDoom.MODID, Names.WORKSHOP_START), 20, 12, 32, 0, false, true));	
		
		
		builder.pop();
	}
}
