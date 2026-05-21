package com.gabri.itemeditor;

import com.gabri.itemeditor.network.ItemEditorNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ItemEditor.MODID)
public class ItemEditor {
    public static final String MODID = "itemeditor";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemEditor() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ItemEditorNetwork::registerPackets);
        LOGGER.info("ItemEditor initialized");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ItemEditorCommands.register(event.getDispatcher());
    }
}
