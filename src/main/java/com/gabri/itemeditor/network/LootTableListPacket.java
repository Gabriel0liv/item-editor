package com.gabri.itemeditor.network;

import com.gabri.itemeditor.client.screen.LootTableSelectionModal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LootTableListPacket {
    private static final int MAX_ID_LENGTH = 512;
    private final List<String> lootTables;

    public LootTableListPacket(List<String> lootTables) {
        this.lootTables = lootTables == null ? List.of() : lootTables;
    }

    public LootTableListPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> entries = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            entries.add(buf.readUtf(MAX_ID_LENGTH));
        }
        this.lootTables = entries;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.lootTables.size());
        for (String id : this.lootTables) {
            buf.writeUtf(id, MAX_ID_LENGTH);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context == null) {
            return;
        }
        context.enqueueWork(() -> {
            List<ResourceLocation> parsed = new ArrayList<>();
            for (String id : this.lootTables) {
                ResourceLocation key = ResourceLocation.tryParse(id);
                if (key != null) {
                    parsed.add(key);
                }
            }
            LootTableSelectionModal.applyServerLootTables(parsed);
        });
        context.setPacketHandled(true);
    }
}
