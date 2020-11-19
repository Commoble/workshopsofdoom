package commoble.workshopsofdoom;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WorkshopsOfDoom.MODID)
public class WorkshopsOfDoom
{
  public static final String MODID = "workshopsofdoom";
  
  public WorkshopsOfDoom() // invoked by forge due to @Mod
  {
    // mod bus has modloading init events and registry events
    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    // forge bus is for server starting events and in-game events
    IEventBus forgeBus = MinecraftForge.EVENT_BUS;

    // add listeners to mod bus and forge bus, register deferred registers to mod bus

    // add listeners to clientjar events separately
//    if (FMLEnvironment.dist == Dist.CLIENT)
//    {
//      ClientEvents.subscribeClientEvents(modBus, forgeBus);
//    }
  }
}