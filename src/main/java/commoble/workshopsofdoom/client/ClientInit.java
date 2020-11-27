package commoble.workshopsofdoom.client;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInit
{
	// called from mod constructor
	public static void doClientInit(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientInit::onClientSetup);
	}
	
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		RenderingRegistry.registerEntityRenderingHandler(WorkshopsOfDoom.INSTANCE.excavator.get(), ExcavatorRenderer::new);
	}
}
