package com.gabri.itemeditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RequestLootTableListPacket {
    public RequestLootTableListPacket() {
    }

    public RequestLootTableListPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context == null) {
            return;
        }
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!sender.hasPermissions(2)) {
                ItemEditorNetwork.sendToPlayer(sender, new LootTableListPacket(List.of()));
                return;
            }

            List<String> lootTables = sender.server.getLootData().getKeys(LootDataType.TABLE)
                    .stream()
                    .filter(id -> !isBlockLootTable(id))
                    .map(ResourceLocation::toString)
                    .sorted((left, right) -> left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            ItemEditorNetwork.sendToPlayer(sender, new LootTableListPacket(lootTables));
        });
        context.setPacketHandled(true);
    }

    private static boolean isBlockLootTable(ResourceLocation id) {
        return id != null && id.getPath().startsWith("blocks/");
    }
}
