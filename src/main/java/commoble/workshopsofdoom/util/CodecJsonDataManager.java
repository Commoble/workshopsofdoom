/*

The MIT License (MIT)

Copyright (c) 2020 Joseph Bettendorff a.k.a. "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */

package commoble.workshopsofdoom.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.Runnables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

public class CodecJsonDataManager<T> extends SimpleJsonResourceReloadListener
{
	// default gson if unspecified
	private static final Gson STANDARD_GSON = new Gson();
	
	/** The codec we use to convert jsonelements to Ts **/
	private final Codec<T> codec;
	
	/** Logger that will log data parsing errors **/
	private final Logger logger;
	
	private final String folderName;
	
	/** The raw data that we parsed from json last time resources were reloaded **/
	protected Map<ResourceLocation, T> data = new HashMap<>();
	private Runnable syncOnReloadCallback = Runnables.doNothing();
	
	/**
	 * Creates a data manager with a standard gson parser
	 * @param folderName The name of the data folder that we will load from, vanilla folderNames are "recipes", "loot_tables", etc<br>
	 * Jsons will be read from data/all_modids/folderName/all_jsons<br>
	 * folderName can include subfolders, e.g. "some_mod_that_adds_lots_of_data_loaders/cheeses"
	 * @param codec A codec to deserialize the json into your T, see javadocs above class
	 * @param logger A logger that will log json parsing problems when they are caught.
	 */
	public CodecJsonDataManager(String folderName, Codec<T> codec, Logger logger)
	{
		this(folderName, codec, logger, STANDARD_GSON);
	}
	
	/**
	 * As above but with a custom GSON
	 * @param folderName The name of the data folder that we will load from, vanilla folderNames are "recipes", "loot_tables", etc<br>
	 * Jsons will be read from data/all_modids/folderName/all_jsons<br>
	 * folderName can include subfolders, e.g. "some_mod_that_adds_lots_of_data_loaders/cheeses"
	 * @param codec A codec to deserialize the json into your T, see javadocs above class
	 * @param logger A logger that will log json parsing problems when they are caught.
	 * @param gson A gson for parsing the raw json data into JsonElements. JsonElement-to-T conversion will be done by the codec,
	 * so gson type adapters shouldn't be necessary here
	 */
	public CodecJsonDataManager(String folderName, Codec<T> codec, Logger logger, Gson gson)
	{
		super(gson, folderName);
		this.folderName = folderName; // superclass has this but it's a private field
		this.codec = codec;
		this.logger = logger;
	}
	
	/**
	 * Get the data object for the given key
	 * @param id A resourcelocation identifying a json; e.g. a json at data/some_modid/folderName/some_json.json has id "some_modid:some_json"
	 * @return The java object that was deserializd from the json with the given ID, or null if no such object is associated with that ID
	 */
	@Nullable
	public T getData(ResourceLocation id)
	{
		return this.data.get(id);
	}
	
	public Set<Entry<ResourceLocation, T>> entrySet()
	{
		return this.data.entrySet();
	}
	
	public Collection<T> values()
	{
		return this.data.values();
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler)
	{
		this.logger.info("Beginning loading of data for data loader: {}", this.folderName);
		this.data = this.mapValues(jsons);
		this.logger.info("Data loader for {} loaded {} jsons", this.folderName, this.data.size());
		if (ServerLifecycleHooks.getCurrentServer() != null)
		{
			// if we're on the server and we are configured to send syncing packets, send syncing packets
			this.syncOnReloadCallback.run();
		}
	}

	private Map<ResourceLocation, T> mapValues(Map<ResourceLocation, JsonElement> inputs)
	{
		Map<ResourceLocation, T> newMap = new HashMap<>();

		for (Entry<ResourceLocation, JsonElement> entry : inputs.entrySet())
		{
			ResourceLocation key = entry.getKey();
			JsonElement element = entry.getValue();
			// if we fail to parse json, log an error and continue
			// if we succeeded, add the resulting T to the map
			this.codec.decode(JsonOps.INSTANCE, element)
				.get()
				.ifLeft(result -> newMap.put(key, result.getFirst()))
				.ifRight(partial -> this.logger.error("Failed to parse data json for {} due to: {}", key.toString(), partial.message()));
		}

		return newMap;
	}

	/**
	 * This should be called at most once, in a mod constructor (FMLCommonSetupEvent *may* work as well)
	 * Calling this method in static init may cause it to be called later than it should be.
	 * Calling this method A) causes the data manager to send a data-syncing packet to all players when a server /reloads data,
	 * and B) subscribes the data manager to the PlayerLoggedIn event to allow it to sync itself to players when they log in.
	 * Be aware that the invoker must still manually subscribe the relevant packet type to the channel themselves.
	 * @param <PACKET> the packet type that will be sent on the given channel
	 * @param channel The networking channel of your mod
	 * @param packetFactory  A packet constructor or factory method that converts the given map to a packet object to send on the given channel
	 * @return this manager object
	 */
	public <PACKET> CodecJsonDataManager<T> subscribeAsSyncable(final SimpleChannel channel,
		final Function<Map<ResourceLocation, T>, PACKET> packetFactory)
	{
		MinecraftForge.EVENT_BUS.addListener(this.getLoginListener(channel, packetFactory));
		this.syncOnReloadCallback = () -> channel.send(PacketDistributor.ALL.noArg(), packetFactory.apply(this.data));
		return this;
	}
	
	/** Generate an event listener function for the player-login-event **/
	private <PACKET> Consumer<PlayerEvent.PlayerLoggedInEvent> getLoginListener(final SimpleChannel channel,
		final Function<Map<ResourceLocation, T>, PACKET> packetFactory)
	{
		return event -> {
			Player player = event.getPlayer();
			if (player instanceof ServerPlayer serverPlayer)
			{
				channel.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packetFactory.apply(this.data));
			}
		};
	}
}