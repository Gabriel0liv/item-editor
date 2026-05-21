package com.gabri.itemeditor;

import com.gabri.itemeditor.network.ItemEditorNetwork;
import com.gabri.itemeditor.network.OpenItemEditorPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ItemEditorCommands {
    private ItemEditorCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("itemeditor")
                .requires(source -> source.hasPermission(2))
                .executes(context -> open(context.getSource())));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("itemeditor.command.only_player"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("itemeditor.command.hold_item"));
            return 0;
        }

        ItemEditorNetwork.sendToPlayer(player, new OpenItemEditorPacket(stack, -1, -1));
        source.sendSuccess(() -> Component.translatable("itemeditor.command.opening"), false);
        return 1;
    }
}
