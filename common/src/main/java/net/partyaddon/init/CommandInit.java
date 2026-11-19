package net.partyaddon.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.partyaddon.group.PartyManager;

public class CommandInit {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .then(Commands.literal("invite").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> execute(ctx, player -> PartyManager.invite(player, EntityArgument.getPlayer(ctx, "target"))))))
                .then(Commands.literal("accept").executes(ctx -> execute(ctx, player -> PartyManager.acceptInvitation(player, null))))
                // Kept for backwards compatibility
                .then(Commands.literal("join").executes(ctx -> execute(ctx, player -> PartyManager.acceptInvitation(player, null))))
                .then(Commands.literal("decline").executes(ctx -> execute(ctx, player -> PartyManager.declineInvitation(player, null))))
                .then(Commands.literal("leave").executes(ctx -> execute(ctx, PartyManager::leave)))
                .then(Commands.literal("kick").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> execute(ctx, player -> PartyManager.kick(player, EntityArgument.getPlayer(ctx, "target").getUUID())))))
                .then(Commands.literal("promote").then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> execute(ctx, player -> PartyManager.promote(player, EntityArgument.getPlayer(ctx, "target").getUUID())))))
                .then(Commands.literal("list").executes(ctx -> execute(ctx, PartyManager::list))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, PartyAction action) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.translatable("text.partyaddon.player_only_command"));
            return 0;
        }
        action.run(player);
        return 1;
    }

    @FunctionalInterface
    private interface PartyAction {
        void run(ServerPlayer player) throws CommandSyntaxException;
    }
}
