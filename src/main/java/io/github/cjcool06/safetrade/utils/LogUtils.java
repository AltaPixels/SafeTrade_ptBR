package io.github.cjcool06.safetrade.utils;

import io.github.cjcool06.safetrade.SafeTrade;
import io.github.cjcool06.safetrade.managers.DataManager;
import io.github.cjcool06.safetrade.obj.Log;
import io.github.cjcool06.safetrade.obj.Side;
import io.github.cjcool06.safetrade.obj.Trade;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LogUtils {

    public static void logTrade(Trade trade) {
        Log log = new Log(trade);
        Sponge.getScheduler().createTaskBuilder().execute(() -> {
            DataManager.addLog(log.getParticipant(), log);
            DataManager.addLog(log.getOtherParticipant(), log);
        }).async().submit(SafeTrade.getPlugin());

    }

    /**
     * This method loops through one of the participants, as both participants will have the logs of their trades.
     * If for some reason one of the users has had their logs removed, you can swap the parameters around.
     *
     * Won't get legacy logs due to an oversight by me in the {@link Log} class. Fuck me.
     *
     * @param participant0 - The first participant of the trade
     * @param participant1 - The second participant of the trade
     * @return - List of logs that had both participants
     */
    public static ArrayList<Log> getLogsOf(User participant0, User participant1) {
        ArrayList<Log> logs = new ArrayList<>();
        ArrayList<Log> logsParticipant0 = DataManager.getLogs(participant0);
        for (Log log : logsParticipant0) {
            // Legacy parcer - Parces improved logs from legacy logs. Legacy logs will cause NPE.
            if (log.getParticipantUUID() != null) {
                if (log.getParticipantUUID().equals(participant1.getUniqueId()) || log.getOtherParticipantUUID().equals(participant1.getUniqueId())) {
                    logs.add(log);
                }
            }
        }

        return logs;
    }

    @Deprecated
    public static List<String> createContents(Trade trade) {
        List<String> contents = new ArrayList<>();
        Text[] extentedLogs = getExtendedLogs(trade);
        contents.add(TextSerializers.JSON.serialize(
                Text.builder().append(Text.of(TextColors.LIGHT_PURPLE, "[" + Log.getFormatter().format(Utils.convertToUTC(LocalDateTime.now())) + " UTC] "))
                        .onHover(TextActions.showText(Text.of(TextColors.GRAY, "Day/Month/Year Hour:Minute"))).build()));

        // Participant 0
        User p0 = trade.getSides()[0].getUser().get();
        contents.add(TextSerializers.JSON.serialize(
                Text.builder().append(Text.of(TextColors.AQUA, p0.getName()))
                        .onHover(TextActions.showText(Text.of(TextColors.GRAY, "Clique aqui para ver o registro extendido da troca com o " + p0.getName()))).build()));
        contents.add(TextSerializers.JSON.serialize(
                Text.of(TextColors.GREEN, "Registro extendido de " + p0.getName())));
        contents.add(TextSerializers.JSON.serialize(extentedLogs[0]));

        contents.add(TextSerializers.JSON.serialize(Text.of(TextColors.DARK_AQUA, " & ")));

        // Participant 1
        User p1 = trade.getSides()[1].getUser().get();
        contents.add(TextSerializers.JSON.serialize(
                Text.builder().append(Text.of(TextColors.AQUA, p1.getName()))
                        .onHover(TextActions.showText(Text.of(TextColors.GRAY, "Clique aqui para ver o registro extendido da troca com o " + p1.getName()))).build()));
        contents.add(TextSerializers.JSON.serialize(
                Text.of(TextColors.GREEN, "Registro extendido de " + p1.getName())));
        contents.add(TextSerializers.JSON.serialize(extentedLogs[1]));


        return contents;
    }

    /**
     * Creates the in-depth log text.
     *
     * @param trade - Trade to log
     * @return - Text array corresponding to trade participant indexes. For example, texts[0] is for trade.participants[0]
     */
    @Deprecated
    private static Text[] getExtendedLogs(Trade trade) {
        Currency currency = SafeTrade.getEcoService().getDefaultCurrency();

        Text.Builder builder1 =  Text.builder();
        Text.Builder builder2 = Text.builder();

        Side side0 = trade.getSides()[0];
        Side side1 = trade.getSides()[1];

        // TODO: Hover over the money to see their balance: before -> after
        builder1.append(Text.of("Dinheiro: "))
                .color(TextColors.DARK_AQUA)
                .append(Text.builder().append(Text.of(TextColors.AQUA, side0.vault.account.getBalance(currency).intValue()))
                        .onHover(TextActions.showText(Text.of()))
                        .build())
                .build();
        builder2.append(Text.of("Dinheiro: "))
                .color(TextColors.DARK_AQUA)
                .append(Text.builder().append(Text.of(TextColors.AQUA, side1.vault.account.getBalance(currency).intValue())).build())
                .build();

        // TODO: Show items stats: durability
        builder1.append(Text.of("\n" + "Itens:"))
                .color(TextColors.DARK_AQUA)
                .build();
        builder2.append(Text.of("\n" + "Itens:"))
                .color(TextColors.DARK_AQUA)
                .build();
        for (ItemStackSnapshot snapshot : side0.vault.getAllItems()) {
            Text.Builder builder = Text.builder();
            snapshot.get(Keys.ITEM_ENCHANTMENTS).ifPresent(enchantments -> {
                enchantments.forEach(enchantment -> {
                    builder.append(Text.of(TextColors.DARK_AQUA, "Encantamentos: "));
                    builder.append(Text.of(TextColors.AQUA, "\n", enchantment.getType(), " ", enchantment.getLevel()));
                });
            });
            builder1.append(Text.builder().append(Text.of("\n", TextColors.GREEN, snapshot.getQuantity() + "x ", TextColors.AQUA, snapshot.getTranslation().get()))
                    .onHover(TextActions.showText(builder.build()))
                    .build()).build();
            if (snapshot.get(Keys.DISPLAY_NAME).isPresent()) {
                builder1.append(Text.builder().append(Text.of("  ", TextColors.GOLD, "[", snapshot.get(Keys.DISPLAY_NAME).get(), TextColors.GOLD, "]")).build()).build();
            }
        }
        for (ItemStackSnapshot snapshot : side1.vault.getAllItems()) {
            Text.Builder builder = Text.builder();
            snapshot.get(Keys.ITEM_ENCHANTMENTS).ifPresent(enchantments -> {
                builder.append(Text.of(TextColors.DARK_AQUA, "Encantamentos: "));
                enchantments.forEach(enchantment -> {
                    builder.append(Text.of(TextColors.AQUA, "\n", enchantment.getType(), " ", enchantment.getLevel()));
                });
            });
            builder2.append(Text.builder().append(Text.of("\n", TextColors.GREEN, snapshot.getQuantity() + "x ", TextColors.AQUA, snapshot.getTranslation().get()))
                    .onHover(TextActions.showText(builder.build()))
                    .build()).build();
            if (snapshot.get(Keys.DISPLAY_NAME).isPresent()) {
                builder2.append(Text.builder().append(Text.of("  ", TextColors.GOLD, "[", snapshot.get(Keys.DISPLAY_NAME).get(), TextColors.GOLD, "]")).build()).build();
            }
        }

        return new Text[]{builder1.build(), builder2.build()};
    }

}
