package com.github.xaeroblock.command;

import com.github.xaeroblock.MapAccessManager;
import com.github.xaeroblock.XaeroPacketSender;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the /xaeromap command for administrators.
 *
 * Usage:
 *   /xaeromap allow <player>   – grant map access to a non-OP player
 *   /xaeromap deny  <player>   – revoke map access from a player
 *   /xaeromap status <player>  – check current access status
 *   /xaeromap reload <player>  – re-send restriction packet to player
 */
public final class XaeroBlockCommand {

    private XaeroBlockCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("xaeromap")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("allow")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(XaeroBlockCommand::allow)))
                .then(Commands.literal("deny")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(XaeroBlockCommand::deny)))
                .then(Commands.literal("status")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(XaeroBlockCommand::status)))
                .then(Commands.literal("reload")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(XaeroBlockCommand::reload)))
        );
    }

    private static int allow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        MapAccessManager.allow(player.getUUID());
        XaeroPacketSender.updateRestrictions(player);
        ctx.getSource().sendSuccess(
            () -> Component.literal("[XaeroBlock] Enabled Xaero maps for "
                    + player.getName().getString() + "."),
            true);
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        MapAccessManager.deny(player.getUUID());
        XaeroPacketSender.updateRestrictions(player);
        ctx.getSource().sendSuccess(
            () -> Component.literal("[XaeroBlock] Disabled Xaero maps for "
                    + player.getName().getString() + "."),
            true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        boolean op       = player.hasPermissions(2);
        boolean explicit = MapAccessManager.isExplicitlyAllowed(player.getUUID());
        boolean canUse   = MapAccessManager.canUseMap(player);

        ctx.getSource().sendSuccess(() -> {
            String reason = op ? " (OP)" : explicit ? " (explicitly allowed)" : " (blocked)";
            return Component.literal("[XaeroBlock] " + player.getName().getString()
                    + " → maps " + (canUse ? "ENABLED" : "DISABLED") + reason);
        }, false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        XaeroPacketSender.updateRestrictions(player);
        ctx.getSource().sendSuccess(
            () -> Component.literal("[XaeroBlock] Restriction packet re-sent to "
                    + player.getName().getString() + "."),
            false);
        return 1;
    }
}
