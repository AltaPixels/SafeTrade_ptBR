package io.github.cjcool06.safetrade.commands;

import com.pixelmonmod.pixelmon.storage.PixelmonStorage;
import io.github.cjcool06.safetrade.SafeTrade;
import io.github.cjcool06.safetrade.obj.Trade;
import io.github.cjcool06.safetrade.utils.Utils;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TradeCommand implements CommandExecutor {
    private static HashMap<User, ArrayList<User>> tradeRequests = new HashMap<>();

    public static CommandSpec getSpec() {
        return CommandSpec.builder()
                .description(Text.of("Trade with another player"))
                .permission("safetrade.common.trade")
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("target"))))
                .executor(new TradeCommand())
                .child(EndTradeCommand.getSpec(), "end")
                .child(ItemsCommand.getSpec(), "items")
                .build();
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!args.<Player>getOne("target").isPresent()) {
            List<Text> contents = new ArrayList<>();

            contents.add(Text.of(TextColors.AQUA, "/safetrade <player>", TextColors.GRAY, " - ", TextColors.GRAY, "Request a safe trade"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade end <player>", TextColors.GRAY, " - ", TextColors.GRAY, "Force end a trade"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade items <add | clear | list> <player>", TextColors.GRAY, " - ", TextColors.GRAY, "Manipulate a player's SafeTrade storage"));

            PaginationList.builder()
                    .title(Text.of(TextColors.DARK_AQUA, " SafeTrade "))
                    .contents(contents)
                    .padding(Text.of(TextColors.AQUA, "-", TextColors.RESET))
                    .sendTo(src);
            return CommandResult.success();
        }
        if (src instanceof Player) {
            Player player = (Player)src;
            Player target = args.<Player>getOne("target").get();

            if (player.equals(target)) {
                player.sendMessage(Text.of(TextColors.RED, "You can't safe trade with yourself, dummy."));
                return CommandResult.success();
            }
            if (SafeTrade.getTrade(player) != null) {
                player.sendMessage(Text.of(TextColors.RED, "You are already involved in a safe trade."));
                return CommandResult.success();
            }
            if (SafeTrade.getTrade(target) != null) {
                player.sendMessage(Text.of(TextColors.RED, "That player is currently safe trading with another player."));
                return CommandResult.success();
            }
            if (tradeRequests.containsKey(player) && tradeRequests.get(player).contains(target)) {
                player.sendMessage(Text.of(TextColors.RED, "There is already a safe trade request pending with that player. Requests expire after 2 minutes."));
                return CommandResult.success();
            }
            // Catches if the requestee uses the command to trade instead of using the executable.
            if (tradeRequests.containsKey(target) && tradeRequests.get(target).contains(player)) {
                acceptInvitation(target, player);
                return CommandResult.success();
            }

            requestTrade(player, target);
        }
        else {
            src.sendMessage(Text.of(TextColors.RED, "You must be a player to trade!"));
        }

        return CommandResult.success();
    }

    public static void requestTrade(Player requester, Player requestee) {
        requestee.sendMessage(Text.of(TextColors.DARK_AQUA, requester.getName(), TextColors.GRAY, " has requested a safe trade. ",
                Text.of(TextColors.GREEN, TextActions.executeCallback(src -> acceptInvitation(requester, requestee)), "[Accept]"),
                " ", Text.of(TextColors.RED, TextActions.executeCallback(src -> rejectInvitation(requester, requestee)), "[Decline]")));

        if (!tradeRequests.containsKey(requester)) {
            tradeRequests.put(requester, new ArrayList<>());
        }
        tradeRequests.get(requester).add(requestee);

        requester.sendMessage(Text.of(TextColors.GRAY, "Safe trade request sent to ", TextColors.DARK_AQUA, requestee.getName(), TextColors.GRAY, "."));

        // Cancels request after 2 minutes
        Sponge.getScheduler().createTaskBuilder()
                .execute(() -> {
                    if (tradeRequests.containsKey(requester) && tradeRequests.get(requester).contains(requestee))
                        tradeRequests.get(requester).remove(requestee);
                })
                .delay(2, TimeUnit.MINUTES)
                .async()
                .submit(SafeTrade.getPlugin());
    }

    public static void rejectInvitation(Player requester, Player requestee) {
        if (tradeRequests.containsKey(requester) && tradeRequests.get(requester).contains(requestee)) {
            tradeRequests.get(requester).remove(requestee);
            requester.sendMessage(Text.of(TextColors.DARK_AQUA, requestee.getName(), TextColors.RED, " rejected your safe trade request."));
            requestee.sendMessage(Text.of(TextColors.GRAY, "Rejected ", TextColors.DARK_AQUA, requester.getName(), TextColors.GRAY, "'s safe trade request."));
        }
    }

    public static void acceptInvitation(Player requester, Player requestee) {
        if (tradeRequests.containsKey(requester) && tradeRequests.get(requester).contains(requestee)) {
            tradeRequests.get(requester).remove(requestee);
            if (Utils.isPlayerOccupied(requester)) {
                requester.sendMessage(Text.of(TextColors.RED, requestee.getName() + " has accepted your safe trade request, but you are otherwise occupied."));
                requestee.sendMessage(Text.of(TextColors.RED, "You have accepted " + requester.getName() + "'s safe trade request, but they are otherwise occupied."));
                return;
            }
            if (Utils.isPlayerOccupied(requestee)) {
                requester.sendMessage(Text.of(TextColors.RED, requestee.getName() + " has accepted your safe trade request, but they are otherwise occupied."));
                requestee.sendMessage(Text.of(TextColors.RED, "You have accepted " + requester.getName() + "'s safe trade request, but you are otherwise occupied."));
                return;
            }

            PixelmonStorage.pokeBallManager.getPlayerStorage((EntityPlayerMP)requester).get().recallAllPokemon();
            PixelmonStorage.pokeBallManager.getPlayerStorage((EntityPlayerMP)requestee).get().recallAllPokemon();
            Trade trade = new Trade(requester, requestee);
            trade.initiateHandshake();
        }
    }
}
