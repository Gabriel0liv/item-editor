package com.gabri.itemeditor.network;

import com.gabri.itemeditor.ItemEditor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ItemEditorNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ItemEditor.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private ItemEditorNetwork() {
    }

    public static void registerPackets() {
        CHANNEL.messageBuilder(OpenItemEditorPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenItemEditorPacket::new)
                .encoder(OpenItemEditorPacket::toBytes)
                .consumerMainThread(OpenItemEditorPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestOpenItemEditorPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestOpenItemEditorPacket::new)
                .encoder(RequestOpenItemEditorPacket::toBytes)
                .consumerMainThread(RequestOpenItemEditorPacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveItemEditorPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SaveItemEditorPacket::new)
                .encoder(SaveItemEditorPacket::toBytes)
                .consumerMainThread(SaveItemEditorPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestLootTableListPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestLootTableListPacket::new)
                .encoder(RequestLootTableListPacket::toBytes)
                .consumerMainThread(RequestLootTableListPacket::handle)
                .add();

        CHANNEL.messageBuilder(LootTableListPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(LootTableListPacket::new)
                .encoder(LootTableListPacket::toBytes)
                .consumerMainThread(LootTableListPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
