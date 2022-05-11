package io.github.cjcool06.safetrade.commands;

import io.github.cjcool06.safetrade.SafeTrade;
import io.github.cjcool06.safetrade.api.enums.InventoryType;
import io.github.cjcool06.safetrade.api.events.trade.ConnectionEvent;
import io.github.cjcool06.safetrade.obj.Side;
import io.github.cjcool06.safetrade.obj.Trade;
import io.github.cjcool06.safetrade.trackers.Tracker;
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
                .description(Text.of("Trocar com outro jogador"))
                .permission("safetrade.common.trade")
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("target"))))
                .executor(new TradeCommand())
                .child(OpenCommand.getSpec(), "open")
                .child(EndTradeCommand.getSpec(), "end")
                .child(StorageCommand.getSpec(), "storage")
                .child(LogsCommand.getSpec(), "logs")
                .child(ViewCommand.getSpec(), "view")
                //.child(TestCommand.getSpec(), "test")
                .child(ReloadCommand.getSpec(), "reload")
                .child(WikiCommand.getSpec(), "wiki")
                .build();
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!args.<Player>getOne("target").isPresent()) {
            List<Text> contents = new ArrayList<>();

            contents.add(Text.of(TextColors.AQUA, "/safetrade <player>", TextColors.GRAY, " - ", TextColors.GRAY, "Pedir/Aceitar uma troca"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade open", TextColors.GRAY, " - ", TextColors.GRAY, "Abre a troca atual"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade wiki", TextColors.GRAY, " - ", TextColors.GRAY, "Envia o link da wiki"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade end <player>", TextColors.GRAY, " - ", TextColors.GRAY, "Força o encerramento de uma troca"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade view <player>", TextColors.GRAY, " - ", TextColors.GRAY, "Ver a troca de um jogador"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade logs <user> [other user]", TextColors.GRAY, " - ", TextColors.GRAY, "Ver as logs de troca de um jogador"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade storage <user> <add | clear | list>", TextColors.GRAY, " - ", TextColors.GRAY, "Abrir o armazém de troca do jogador"));
            contents.add(Text.of(TextColors.AQUA, "/safetrade reload", TextColors.GRAY, " - ", TextColors.GRAY, "Recarregar as configurações"));

            PaginationList.builder()
                    .title(Text.of(TextColors.GREEN, " SafeTrade "))
                    .contents(contents)
                    .padding(Text.of(TextColors.AQUA, "-", TextColors.RESET))
                    .sendTo(src);
            return CommandResult.success();
        }

        if (src instanceof Player) {
            Player player = (Player)src;
            Player target = args.<Player>getOne("target").get();

            if (player.equals(target)) {
                player.sendMessage(Text.of(Text.of(TextColors.RED, "Você não pode trocar com si próprio, bocô.")));
            }
            else if (Tracker.getActiveTrade(player) != null) {
                player.sendMessage(Text.of(TextColors.RED, "Você já está numa troca."));
            }
            else if (Tracker.getActiveTrade(target) != null) {
                player.sendMessage(Text.of(TextColors.RED, "Esse jogador já está em uma troca com outro jogador."));
            }
            else if (tradeRequests.containsKey(player) && tradeRequests.get(player).contains(target)) {
                player.sendMessage(Text.of(TextColors.RED, "Já existe um pedidod e troca para esse jogador. Pedidos expiram em 2 minutos."));
            }
            // Catches if the requestee uses the command to trade instead of using the executable.
            else if (tradeRequests.containsKey(target) && tradeRequests.get(target).contains(player)) {
                acceptInvitation(target, player);
            }
            else {
                requestTrade(player, target);
            }
        }
        else {
            src.sendMessage(Text.of(TextColors.RED, "Você deve ser um jogador para fazer isso."));
        }

        return CommandResult.success();
    }

    public static void requestTrade(Player requester, Player requestee) {
        requestee.sendMessage(Text.of(TextColors.DARK_AQUA, requester.getName(), TextColors.GRAY, " pediu para trocar com você. ",
                Text.of(TextColors.GREEN, TextActions.executeCallback(dummySrc -> acceptInvitation(requester, requestee)), "[Aceitar]"),
                " ",
                Text.of(TextColors.RED, TextActions.executeCallback(dummySrc -> rejectInvitation(requester, requestee)), "[Recusar]")));

        if (!tradeRequests.containsKey(requester)) {
            tradeRequests.put(requester, new ArrayList<>());
        }
        tradeRequests.get(requester).add(requestee);

        requester.sendMessage(Text.of(TextColors.GRAY, "Troca enviada para ", TextColors.DARK_AQUA, requestee.getName(), TextColors.GRAY, "."));

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
            requester.sendMessage(Text.of(TextColors.DARK_AQUA, requestee.getName(), TextColors.RED, " rejeitou o seu pedido de troca."));
            requestee.sendMessage(Text.of(TextColors.GRAY, "Troca de ", TextColors.DARK_AQUA, requester.getName(), TextColors.GRAY, " foi rejeitada."));
        }
    }

    public static void acceptInvitation(Player requester, Player requestee) {
        if (tradeRequests.containsKey(requester) && tradeRequests.get(requester).contains(requestee)) {
            tradeRequests.get(requester).remove(requestee);

            // The initial open needs to be like this, otherwise players will be flagged as paused unless they pause or close inv and resume.
            // This is because no player cause is given to the InteractInventoryEvent.Open event. Not sure why.
            Trade trade = new Trade(requester, requestee);
            Side side0 = trade.getSides()[0];
            Side side1 = trade.getSides()[1];
            side0.getPlayer().ifPresent(player -> {
                side0.setPaused(false);
                trade.reformatInventory();
                side0.changeInventory(InventoryType.MAIN);
                Sponge.getEventManager().post(new ConnectionEvent.Join.Post(side0));
            });
            side1.getPlayer().ifPresent(player -> {
                side1.setPaused(false);
                trade.reformatInventory();
                side1.changeInventory(InventoryType.MAIN);
                Sponge.getEventManager().post(new ConnectionEvent.Join.Post(side1));
            });
        }
    }
}
