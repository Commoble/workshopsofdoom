package commoble.workshopsofdoom.client;

import commoble.workshopsofdoom.WorkshopsOfDoom;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientInit
{
	// called from mod constructor
	public static void doClientInit(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientInit::onRegisterEntityRenderers);
	}
	
	public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerEntityRenderer(WorkshopsOfDoom.INSTANCE.excavator.get(), ExcavatorRenderer::new);
	}
}
