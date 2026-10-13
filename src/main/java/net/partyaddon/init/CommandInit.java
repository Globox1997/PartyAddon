package net.partyaddon.init;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.partyaddon.group.PartyManager;

public class CommandInit {

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            dispatcher.register(CommandManager.literal("party")
                    .then(CommandManager.literal("invite").then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> execute(ctx, player -> PartyManager.invite(player, EntityArgumentType.getPlayer(ctx, "target"))))))
                    .then(CommandManager.literal("accept").executes(ctx -> execute(ctx, player -> PartyManager.acceptInvitation(player, null))))
                    // Kept for backwards compatibility
                    .then(CommandManager.literal("join").executes(ctx -> execute(ctx, player -> PartyManager.acceptInvitation(player, null))))
                    .then(CommandManager.literal("decline").executes(ctx -> execute(ctx, player -> PartyManager.declineInvitation(player, null))))
                    .then(CommandManager.literal("leave").executes(ctx -> execute(ctx, PartyManager::leave)))
                    .then(CommandManager.literal("kick").then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> execute(ctx, player -> PartyManager.kick(player, EntityArgumentType.getPlayer(ctx, "target").getUuid())))))
                    .then(CommandManager.literal("promote").then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> execute(ctx, player -> PartyManager.promote(player, EntityArgumentType.getPlayer(ctx, "target").getUuid())))))
                    .then(CommandManager.literal("list").executes(ctx -> execute(ctx, PartyManager::list))));
        });
    }

    private static int execute(CommandContext<ServerCommandSource> ctx, PartyAction action) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.translatable("text.partyaddon.player_only_command"));
            return 0;
        }
        action.run(player);
        return 1;
    }

    @FunctionalInterface
    private interface PartyAction {
        void run(ServerPlayerEntity player) throws CommandSyntaxException;
    }
}
