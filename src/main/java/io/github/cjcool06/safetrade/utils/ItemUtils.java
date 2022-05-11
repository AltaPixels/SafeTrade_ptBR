package io.github.cjcool06.safetrade.utils;

import com.google.common.collect.Lists;
import io.github.cjcool06.safetrade.SafeTrade;
import io.github.cjcool06.safetrade.api.enums.TradeState;
import io.github.cjcool06.safetrade.obj.Side;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.RepresentedPlayerData;
import org.spongepowered.api.data.manipulator.mutable.SkullData;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.text.NumberFormat;
import java.util.Locale;

public class ItemUtils {

    public static class Main {

        public static ItemStack getStateStatus(Side side) {
            ItemStack item = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.AQUA, "Status da troca com " + side.getUser().get().getName()));
            if (side.isPaused()) {
                item.offer(Keys.DYE_COLOR, DyeColors.ORANGE);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Estado atual: ", TextColors.GOLD, "Pausada")));
            }
            else if (side.isReady()) {
                item.offer(Keys.DYE_COLOR, DyeColors.LIME);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Estado atual: ", TextColors.GREEN, "Pronto")));
            }
            else {
                item.offer(Keys.DYE_COLOR, DyeColors.RED);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Estado atual: ", TextColors.RED, "Esperando")));
            }
            return item;
        }

        public static ItemStack getQuit() {
            ItemStack item = ItemStack.of(ItemTypes.BARRIER, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RED, "Quit"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Encerre a troca e pegue seus itens e dinheiro de volta.")));
            return item;
        }

        public static ItemStack getHead(Side side) {
            SkullData skullData = Sponge.getDataManager().getManipulatorBuilder(SkullData.class).get().create();
            skullData.set(Keys.SKULL_TYPE, SkullTypes.PLAYER);
            ItemStack itemStack = Sponge.getRegistry().createBuilder(ItemStack.Builder.class).itemType(ItemTypes.SKULL).itemData(skullData).build();
            RepresentedPlayerData skinData = Sponge.getDataManager().getManipulatorBuilder(RepresentedPlayerData.class).get().create();
            skinData.set(Keys.REPRESENTED_PLAYER, GameProfile.of(side.getUser().get().getUniqueId()));
            itemStack.offer(skinData);
            itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.DARK_AQUA, side.getUser().get().getName()));
            itemStack.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Esse lado da troca possui Itens e Dinheiro que " +
                    side.getUser().get().getName() + " quer trocar.")));
            return itemStack;
        }

        public static ItemStack getMoneyStorage(Side side) {
            Currency currency = SafeTrade.getEcoService().getDefaultCurrency();
            ItemStack item = ItemStack.of(ItemTypes.GOLD_BLOCK, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, currency.getSymbol(), side.vault.account.getBalance(currency)));
            if (side.parentTrade.getState().equals(TradeState.TRADING)) {
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Clique para mudar a quantidade de ", currency.getPluralDisplayName(), " para troca"),
                        Text.of(TextColors.GOLD, "Somente " + side.getUser().get().getName() + " pode fazer isso")));
            }
            return item;
        }

        public static ItemStack getItemStorage(Side side) {
            ItemStack item = ItemStack.of(ItemTypes.CHEST, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Itens"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Clique aqui para ver os itens que " + side.getUser().get().getName() + " quer trocar")));
            return item;
        }

        public static ItemStack getReady() {
            ItemStack item = ItemStack.of(ItemTypes.DYE, 1);
            item.offer(Keys.DYE_COLOR, DyeColors.LIME);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, "Pronto"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Estou pronto!")));
            return item;
        }

        public static ItemStack getNotReady() {
            ItemStack item = ItemStack.of(ItemTypes.DYE, 1);
            item.offer(Keys.DYE_COLOR, DyeColors.RED);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RED, "Não estou pronto"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Não estou pronto ainda.")));
            return item;
        }

        public static ItemStack getPause() {
            ItemStack item = ItemStack.of(ItemTypes.DYE, 1);
            item.offer(Keys.DYE_COLOR, DyeColors.ORANGE);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Pausar"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Pausar troca")));
            return item;
        }
    }

    public static class Money {

        public static ItemStack getTotalMoney(Side side) {
            Currency currency = SafeTrade.getEcoService().getDefaultCurrency();
            ItemStack item = ItemStack.of(ItemTypes.GOLD_BLOCK, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, currency.getSymbol(), side.vault.account.getBalance(currency).intValue()));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Esse dinheiro é armazenado com segurança até que a negociação chegue ao fim.")));
            return item;
        }

        public static ItemStack getPlayersMoney(Side side) {
            Currency currency = SafeTrade.getEcoService().getDefaultCurrency();
            ItemStack item = ItemStack.of(ItemTypes.DIAMOND_ORE, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, currency.getSymbol(), SafeTrade.getEcoService().getOrCreateAccount(side.getUser().get().getUniqueId()).get().getBalance(currency).intValue()));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Este é o número total de ", currency.getPluralDisplayName(), " que você tem.")));
            return item;
        }

        public static ItemStack getMoneyBars(int amount) {
            Currency currency = SafeTrade.getEcoService().getDefaultCurrency();
            ItemStack item = ItemStack.of(ItemTypes.GOLD_INGOT, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, currency.getSymbol(), NumberFormat.getNumberInstance(Locale.US).format(amount)));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(
                    Text.of(TextColors.GREEN, "Clique esquerdo: ", TextColors.GRAY, "Adiciona ", currency.getPluralDisplayName()),
                    Text.of(TextColors.RED, "Clique direito: ", TextColors.GRAY, "Remove ", currency.getPluralDisplayName())
            ));
            return item;
        }
    }

    public static class Overview {

        public static ItemStack getConfirmationStatus(Side side) {
            ItemStack item = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.AQUA, "Status de confirmação de " + side.getUser().get().getName() + ""));
            if (side.isConfirmed()) {
                item.offer(Keys.DYE_COLOR, DyeColors.LIME);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Estado atual: ", TextColors.GREEN, "Pronto")));
            }
            else {
                item.offer(Keys.DYE_COLOR, DyeColors.RED);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Estado atual: ", TextColors.RED, "Esperando")));
            }
            return item;
        }

        public static ItemStack getConfirm() {
            ItemStack item = ItemStack.of(ItemTypes.DYE, 1);
            item.offer(Keys.DYE_COLOR, DyeColors.GREEN);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, "Confirmar"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(
                    Text.of(TextColors.GOLD, "Confirme que você está satisfeito com a troca")));
            return item;
        }

        public static ItemStack getCancel() {
            ItemStack item = ItemStack.of(ItemTypes.DYE, 1);
            item.offer(Keys.DYE_COLOR, DyeColors.YELLOW);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Cancelar"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GREEN, "Volte para renegociar a troca.")));
            return item;
        }

        public static ItemStack getOverviewInfo() {
            ItemStack item = ItemStack.of(ItemTypes.PAPER, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Qual é a visão geral da troca?"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GREEN, "A visão geral da troca permite que você navegue pela negociação e certifique-se de "),
                    Text.of(TextColors.GREEN, "você esteja contente."),
                    Text.of(),
                    Text.of(TextColors.DARK_GREEN, "Durante este tempo, você não pode alterar nada sobre a troca."),
                    Text.of(),
                    Text.of(TextColors.GRAY, "A troca só será feita assim que ambos os jogadores confirmarem."),
                    Text.of(TextColors.RED, "Não há como reverter isso!")));
            return item;
        }
    }

    // Yeah yeah, I know this shit is kinda redundant
    public static class Logs {

        public static ItemStack getMoney(User user, int money) {
            Currency currency = SafeTrade.getEcoService().getDefaultCurrency();
            ItemStack item = ItemStack.of(ItemTypes.GOLD_BLOCK, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, currency.getSymbol(), money));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Quantidade de ", currency.getPluralDisplayName(), " " + user.getName() + " trocado")));
            return item;
        }

        public static ItemStack getItems(User user) {
            ItemStack item = ItemStack.of(ItemTypes.CHEST, 1);
            item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Items"));
            item.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Clique para ver os itens de " + user.getName() + " trocado")));
            return item;
        }

        public static ItemStack getHead(User user) {
            SkullData skullData = Sponge.getDataManager().getManipulatorBuilder(SkullData.class).get().create();
            skullData.set(Keys.SKULL_TYPE, SkullTypes.PLAYER);
            ItemStack itemStack = Sponge.getRegistry().createBuilder(ItemStack.Builder.class).itemType(ItemTypes.SKULL).itemData(skullData).build();
            RepresentedPlayerData skinData = Sponge.getDataManager().getManipulatorBuilder(RepresentedPlayerData.class).get().create();
            skinData.set(Keys.REPRESENTED_PLAYER, GameProfile.of(user.getUniqueId()));
            itemStack.offer(skinData);
            itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.DARK_AQUA, user.getName()));
            itemStack.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Este lado da troca contém os itens e dinheiro que " +
                    user.getName() + " trocou")));
            return itemStack;
        }
    }

    public static class Other {

        public static ItemStack getBackButton() {
            ItemStack itemStack = ItemStack.of(ItemTypes.DYE, 1);
            itemStack.offer(Keys.DYE_COLOR, DyeColors.RED);
            itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RED, "Voltar"));
            itemStack.offer(Keys.ITEM_LORE, Lists.newArrayList(Text.of(TextColors.GRAY, "Retorne ao inventário principal da troca")));
            return itemStack;
        }

        public static ItemStack getFiller(DyeColor color) {
            ItemStack background = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
            background.offer(Keys.DYE_COLOR, color);
            return background;
        }
    }
}
