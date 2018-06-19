package io.github.cjcool06.safetrade.obj;

import com.pixelmonmod.pixelmon.PixelmonMethods;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.storage.PixelmonStorage;
import com.pixelmonmod.pixelmon.storage.PlayerStorage;
import io.github.cjcool06.safetrade.SafeTrade;
import io.github.cjcool06.safetrade.api.events.item.ItemAddEvent;
import io.github.cjcool06.safetrade.api.events.item.ItemRemoveEvent;
import io.github.cjcool06.safetrade.api.events.trade.HandleTradeEvent;
import io.github.cjcool06.safetrade.api.events.trade.TradeEndEvent;
import io.github.cjcool06.safetrade.api.events.trade.TradeStartEvent;
import io.github.cjcool06.safetrade.utils.ItemUtils;
import io.github.cjcool06.safetrade.utils.Utils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Trade {
    public final Player[] participants;
    private final Inventory inventory;
    // TODO: Make methods for this shit
    public final HashMap<Integer, ItemStackSnapshot> view = new HashMap<>();
    public final HashMap<Player, HashMap<ItemStack, EntityPixelmon>> listedPokemon = new HashMap<>();
    public final HashMap<Player, Integer> money = new HashMap<>();
    public boolean isExecuting = false;
    public boolean participant0Ready = false;
    public boolean participant1Ready = false;
    private Player forceCloser = null;

    public Trade(Player participant1, Player participant2) {
        participants = new Player[]{participant1, participant2};
        listedPokemon.put(participant1, new HashMap<>());
        listedPokemon.put(participant2, new HashMap<>());
        money.put(participant1, 0);
        money.put(participant2, 0);
        inventory = Inventory.builder()
                .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(TextColors.DARK_AQUA, "SafeTrade: ", participants[0].getName(), " & ", participants[1].getName())))
                .property(InventoryDimension.PROPERTY_NAME, new InventoryDimension(9,6))
                .of(InventoryArchetypes.MENU_GRID)
                .listener(ClickInventoryEvent.class, this::handleClick)
                .listener(InteractInventoryEvent.Close.class, this::handleClose)
                .listener(ClickInventoryEvent.Shift.class, this::handleShiftClick)
                .build(SafeTrade.getPlugin());
    }

    public void initiateHandshake() {
        if (SafeTrade.EVENT_BUS.post(new TradeStartEvent(this))) {
            return;
        }
        if (participants[0].isViewingInventory()) {
            participants[0].closeInventory();
        }
        if (participants[1].isViewingInventory()) {
            participants[1].closeInventory();
        }
        drawDesign();
        update();
        participants[0].openInventory(inventory);
        participants[1].openInventory(inventory);
        SafeTrade.activeTrades.add(this);
    }

    public boolean hasPlayer(Player player) {
        if (player.equals(participants[0]) || player.equals(participants[1])) {
            return true;
        }

        return false;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isPokemonItem(ItemStack item) {
        for (HashMap<ItemStack, EntityPixelmon> map : listedPokemon.values()) {
            for (ItemStack itemStack : map.keySet()) {
                if (item.equalTo(itemStack)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Untested
    public EntityPixelmon getPokemon(ItemStack item) {
        for (HashMap<ItemStack, EntityPixelmon> map : listedPokemon.values()) {
            for (ItemStack itemStack : map.keySet()) {
                if (item.equalTo(itemStack)) {
                    return map.get(item);
                }
            }
        }

        return null;
    }

    public boolean isInSide(Player player, int index) {
        if (player.equals(participants[0]) && ((index >= 0 && index <= 3) || (index >= 9 && index <= 12) || (index >= 18 && index <= 21) || (index >= 27 && index <= 30))) {
            return true;
        }
        else if (player.equals(participants[1]) && ((index >= 5 && index <= 8) || (index >= 14 && index <= 17) || (index >= 23 && index <= 26) || (index >= 32 && index <= 35))) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean hasSideGotSpace(Player player) {
        for (int i = 0; i < 36; i++) {
            if (player.equals(participants[0]) && ((i <= 3) || (i >= 9 && i <= 12) || (i >= 18 && i <= 21) || (i >= 27 && i <= 30))) {
                if (!view.containsKey(i)) {
                    return true;
                }
            }
            else if (player.equals(participants[1]) && ((i >= 5 && i <= 8) || (i >= 14 && i <= 17) || (i >= 23 && i <= 26) || (i >= 32))) {
                if (!view.containsKey(i)) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getFreeSlotOfSide(Player player) {
        for (int i = 0; i < 36; i++) {
            if (player.equals(participants[0]) && ((i <= 3) || (i >= 9 && i <= 12) || (i >= 18 && i <= 21) || (i >= 27 && i <= 30))) {
                if (!view.containsKey(i)) {
                    return i;
                }
            }
            else if (player.equals(participants[1]) && ((i >= 5 && i <= 8) || (i >= 14 && i <= 17) || (i >= 23 && i <= 26) || (i >= 32))) {
                if (!view.containsKey(i)) {
                    return i;
                }
            }
        }

        return 0;
    }

    /**
     * Adds an item to the respected player's side of the trade UI.
     *
     * @param player - UI side
     * @param snapshot - Item snapshot
     */
    public boolean addItem(Player player, ItemStackSnapshot snapshot) {
        if (hasSideGotSpace(player)) {
            if (SafeTrade.EVENT_BUS.post(new ItemAddEvent(this, snapshot))) {
                return false;
            }
            view.put(getFreeSlotOfSide(player), snapshot);
            return true;
        }

        return false;
    }

    public boolean addPokemon(Player player, EntityPixelmon pokemon) {
        if (hasSideGotSpace(player)) {
            ItemStackSnapshot pokemonItemSnapshot = ItemUtils.getPokemonIcon(pokemon).createSnapshot();
            if (addItem(player, pokemonItemSnapshot)) {
                for (ItemStack item : listedPokemon.get(player).keySet()) {
                    if (item.equalTo(pokemonItemSnapshot.createStack())) {
                        return false;
                    }
                }
                listedPokemon.get(player).put(pokemonItemSnapshot.createStack(), pokemon);
                return true;
            }
        }

        return false;
    }

    public void removePokemon(Player player, ItemStack item, int index) {
        Iterator<ItemStack> iter = listedPokemon.get(player).keySet().iterator();

        while (iter.hasNext()) {
            ItemStack itemStack = iter.next();
            if (item.equalTo(itemStack)) {
                if (removeItem(index)) {
                    iter.remove();
                }
            }
        }
    }

    public void clearSide(Player player) {
        view.keySet().removeIf(index -> isInSide(player, index));
    }

    // Only used for gui items
    private void addItem(int index, ItemStackSnapshot snapshot) {
        view.put(index, snapshot);
    }

    public boolean removeItem(int index) {
        if (view.containsKey(index) && SafeTrade.EVENT_BUS.post(new ItemRemoveEvent(this, view.get(index)))) {
            return false;
        }
        view.remove(index);
        return true;
    }

    public ArrayList<ItemStackSnapshot> getItems(Player player) {
        ArrayList<ItemStackSnapshot> items = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (player.equals(participants[0]) && ((i <= 3) || (i >= 9 && i <= 12) || (i >= 18 && i <= 21) || (i >= 27 && i <= 30))) {
                if (view.containsKey(i) && !isPokemonItem(view.get(i).createStack())) {
                    items.add(view.get(i));
                }
            }
            else if (player.equals(participants[1]) && ((i >= 5 && i <= 8) || (i >= 14 && i <= 17) || (i >= 23 && i <= 26) || (i >= 32))) {
                if (view.containsKey(i) && !isPokemonItem(view.get(i).createStack())) {
                    items.add(view.get(i));
                }
            }
        }

        return items;
    }

    public void executeTrade() {
        isExecuting = true;
        Sponge.getScheduler().createTaskBuilder()
                .execute(new Consumer<Task>() {
                    int count = 5;
                    @Override
                    public void accept(Task task) {
                        if (!isExecuting) {
                            task.cancel();
                            drawStatusDesign(DyeColors.RED, 5);
                            update();
                            return;
                        }
                        if (count == 0) {
                            handleTrade();
                            task.cancel();
                            isExecuting = false;
                            return;
                        }
                        if (isExecuting) {
                            drawStatusDesign(DyeColors.GREEN, count);
                        }
                        update();
                        count--;
                    }
                })
                .interval(1, TimeUnit.SECONDS)
                .submit(SafeTrade.getPlugin());

    }

    public void update() {
        inventory.clear();
        drawInfoDesign();
        shuffleItems(participants[0], 0);
        shuffleItems(participants[1], 0);


        inventory.slots().forEach(slot -> {
            int index = slot.getProperty(SlotIndex.class, "slotindex").get().getValue();
            if (view.containsKey(index)) {
                slot.set(view.get(index).createStack());
            }
        });
    }

    // Shuffles items across if their is a free slot in a lower index
    public void shuffleItems(Player player, int startingIndex) {
        boolean hasPrevFreeSpace = false;
        for (int i = startingIndex; i < 36; i++) {
            if (isInSide(player, i)) {
                if (view.containsKey(i)) {
                    if (hasPrevFreeSpace) {
                        ItemStackSnapshot snapshot = view.get(i);
                        removeItem(i);
                        addItem(player, snapshot);
                    }
                }
                else {
                    hasPrevFreeSpace = true;
                }
            }
        }
    }

    // Forces an inventory to close (if none present, will call #end), which will force #handleClose, which will call #end.
    // Use when a you want to force a trade to close.
    public void forceEnd() {
        if (participants[0].isOnline()) {
            forceCloser = participants[0];
            participants[0].closeInventory();
        }
        else if (participants[1].isOnline()) {
            forceCloser = participants[1];
            participants[1].closeInventory();
        }
        else {
            end();
        }
    }

    // Only used when a player has closed their inv, either on their own (ESC) or forced (#forceEnd)
    private void end() {
        SafeTrade.EVENT_BUS.post(new TradeEndEvent(this));
        isExecuting = false;
        participant0Ready = false;
        participant1Ready = false;
        SafeTrade.activeTrades.remove(this);

        // Needs scheduler to work, otherwise the player who didn't close the inventory will not receive their items unless they refresh their inventory. Don't know why it happens
        Sponge.getScheduler().createTaskBuilder()
                .execute(() -> {
                    if (participants[0].isOnline()) {
                        Utils.giveItems(participants[0], getItems(participants[0]), true);
                    }
                    if (participants[1].isOnline()) {
                        Utils.giveItems(participants[1], getItems(participants[1]), true);
                    }
                    view.clear();
                    listedPokemon.clear();
                    money.clear();
                    inventory.clear();
                })
                .delayTicks(1)
                .submit(SafeTrade.getPlugin());
    }

    public void handleTrade() {
        if (SafeTrade.EVENT_BUS.post(new HandleTradeEvent(this))) {
            drawStatusDesign(DyeColors.RED, 5);
            update();
            return;
        }
        ArrayList<Player> players = new ArrayList<>();
        players.add(participants[0]);
        players.add(participants[1]);

        // Ensures each player still has the pokemon they are trading with in their parties
        for (Player participant : players) {
            int count = 0;
            outerloop:
            for (int i = 1; i < 7; i++) {
                EntityPixelmon slotPokemon = Utils.getPokemonInSlot(participant, i);
                if (slotPokemon != null) {
                    for (EntityPixelmon pixelmon : listedPokemon.get(participant).values()) {
                        if (PixelmonMethods.isIDSame(slotPokemon.getPokemonId(), pixelmon.getPokemonId())) {
                            count++;
                            continue outerloop;
                        }
                    }
                }
            }
            if (count != listedPokemon.get(participant).size()) {
                sendMessage(Text.of(TextColors.RED, "A discrepency was found between ", participant.getName(), "'s trading pokemon and their party. " +
                        "To prevent a possible scam, the trade has been cancelled."));
                forceEnd();
                return;
            }
        }

        // Ensures each player has the money to pay for the trade
        for (Player participant : players) {
            if (SafeTrade.getEcoService().getOrCreateAccount(participant.getUniqueId()).get().getBalance(SafeTrade.getEcoService().getDefaultCurrency()).intValue()
                    < money.get(participant)) {
                sendMessage(Text.of(TextColors.RED, participant.getName(), " has insufficient funds for the trade."));
                forceEnd();
                return;
            }
        }

        ArrayList<EntityPixelmon> p0Pokemon = new ArrayList<>(listedPokemon.get(participants[0]).values());
        ArrayList<EntityPixelmon> p1Pokemon = new ArrayList<>(listedPokemon.get(participants[1]).values());
        PlayerStorage p0Storage = PixelmonStorage.pokeBallManager.getPlayerStorage((EntityPlayerMP)participants[0]).get();
        PlayerStorage p1Storage = PixelmonStorage.pokeBallManager.getPlayerStorage((EntityPlayerMP)participants[1]).get();

        // Prevents either player from ending up with 0 pokemon in their party
        if ((p0Storage.getTeam().size() - p0Pokemon.size() + p1Pokemon.size()) == 0) {
            sendMessage(Text.of(TextColors.RED, participants[0].getName(), " will end up with no pokemon in their party. " +
                    "Please adjust your trade."));
            forceEnd();
            return;
        }
        else if ((p1Storage.getTeam().size() - p1Pokemon.size() + p0Pokemon.size()) == 0) {
            sendMessage(Text.of(TextColors.RED, participants[1].getName(), " will end up with no pokemon in their party. " +
                    "Please adjust your trade."));
            forceEnd();
            return;
        }

        // Processes trade. This is all done last so that if the last check (above) fails I do not have to revert stuff already traded.
        // Pokemon
        // Removes the pokemon. Allows there to be party space when the pokemon is added to the respected party
        for (EntityPixelmon pixelmon : p0Pokemon) {
            for (int i = 1; i < 7; i++) {
                EntityPixelmon slotPokemon = Utils.getPokemonInSlot(participants[0], i);
                if (slotPokemon != null) {
                    if (PixelmonMethods.isIDSame(pixelmon.getPokemonId(), slotPokemon.getPokemonId())) {
                        p0Storage.removeFromPartyPlayer(i - 1);
                    }
                }
            }
        }
        for (EntityPixelmon pixelmon : p1Pokemon) {
            for (int i = 1; i < 7; i++) {
                EntityPixelmon slotPokemon = Utils.getPokemonInSlot(participants[1], i);
                if (slotPokemon != null) {
                    if (PixelmonMethods.isIDSame(pixelmon.getPokemonId(), slotPokemon.getPokemonId())) {
                        p1Storage.removeFromPartyPlayer(i - 1);
                    }
                }
            }
        }

        // Gives the pokemon to the respected players
        for (EntityPixelmon pixelmon : p0Pokemon) {
            boolean hasSpace = false;
            for (int i = 0; i < 6; i++) {
                NBTTagCompound nbt = p1Storage.partyPokemon[i];
                if (nbt == null) {
                    hasSpace = true;
                    break;
                }
            }
            if (hasSpace) {
                p1Storage.addToParty(pixelmon);
            }
            else {
                p1Storage.addToPC(pixelmon);
            }
        }
        for (EntityPixelmon pixelmon : p1Pokemon) {
            boolean hasSpace = false;
            for (int i = 0; i < 6; i++) {
                NBTTagCompound nbt = p0Storage.partyPokemon[i];
                if (nbt == null) {
                    hasSpace = true;
                    break;
                }
            }
            if (hasSpace) {
                p0Storage.addToParty(pixelmon);
            }
            else {
                p0Storage.addToPC(pixelmon);
            }
        }

        // Items
        Utils.giveItems(participants[0], getItems(participants[1]), true);
        Utils.giveItems(participants[1], getItems(participants[0]), true);

        // Money
        if (money.get(participants[0]) > 0) {
            Account accountP0 = SafeTrade.getEcoService().getOrCreateAccount(participants[0].getUniqueId()).get();
            Account accountP1 = SafeTrade.getEcoService().getOrCreateAccount(participants[1].getUniqueId()).get();

            TransactionResult result = accountP0.transfer(accountP1, SafeTrade.getEcoService().getDefaultCurrency(), BigDecimal.valueOf(money.get(participants[0])),
                    Cause.of(EventContext.empty(), this));

            if (result.getResult() != ResultType.SUCCESS) {
                sendMessage(Text.of(TextColors.RED, "Error processing ", participants[0].getName(), "'s money transfer. " +
                        "To prevent a possible scam, the trade has been cancelled."));
                forceEnd();
                return;
            }
        }
        if (money.get(participants[1]) > 0) {
            Account accountP0 = SafeTrade.getEcoService().getOrCreateAccount(participants[0].getUniqueId()).get();
            Account accountP1 = SafeTrade.getEcoService().getOrCreateAccount(participants[1].getUniqueId()).get();

            TransactionResult result = accountP1.transfer(accountP0, SafeTrade.getEcoService().getDefaultCurrency(), BigDecimal.valueOf(money.get(participants[1])),
                    Cause.of(EventContext.empty(), this));

            if (result.getResult() != ResultType.SUCCESS) {
                sendMessage(Text.of(TextColors.RED, "Error processing ", participants[1].getName(), "'s money transfer. " +
                        "To prevent a possible scam, the trade has been cancelled."));
                forceEnd();
                return;
            }
        }

        sendMessage(Utils.getSuccessMessage(this));
        clearSide(participants[0]);
        clearSide(participants[1]);
        forceEnd();
    }

    public void sendMessage(Text text) {
        participants[0].sendMessage(text);
        participants[1].sendMessage(text);
    }

    private void drawInfoDesign() {
        ArrayList<Text> lore = new ArrayList<>();
        lore.add(Text.of(TextColors.GOLD, "Money: ", TextColors.AQUA, money.get(participants[0])));
        lore.add(Text.of(TextColors.GOLD, "Pokemon: ", TextColors.AQUA, listedPokemon.get(participants[0]).size()));
        lore.add(Text.of(TextColors.GOLD, "Items: ", TextColors.AQUA, getItems(participants[0]).size()));

        addItem(39, ItemUtils.getInfoItem(Text.of(TextColors.DARK_AQUA, TextStyles.UNDERLINE, participants[0].getName(), "'s Trade Overview:", TextStyles.RESET),
                lore, participant0Ready ? DyeColors.GREEN : DyeColors.RED).createSnapshot());

        lore.clear();
        lore.add(Text.of(TextColors.GOLD, "Money: ", TextColors.AQUA, money.get(participants[1])));
        lore.add(Text.of(TextColors.GOLD, "Pokemon: ", TextColors.AQUA, listedPokemon.get(participants[1]).values().size()));
        lore.add(Text.of(TextColors.GOLD, "Items: ", TextColors.AQUA, getItems(participants[1]).size()));

        addItem(41, ItemUtils.getInfoItem(Text.of(TextColors.DARK_AQUA, TextStyles.UNDERLINE, participants[1].getName(), "'s Trade Overview:", TextStyles.RESET),
                lore, participant1Ready ? DyeColors.GREEN : DyeColors.RED).createSnapshot());
    }

    private void drawStatusDesign(DyeColor color, int amount) {
        for (int index = 0; index < 32; index++) {
            if (index == 4 || index == 13 || index == 22 || index == 31) {
                addItem(index, ItemUtils.getStatusItem(color, amount).createSnapshot());
            }
        }
    }

    private void drawDesign() {
        ItemStack borderItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        borderItem.offer(Keys.DYE_COLOR, DyeColors.MAGENTA);

        ItemStack greenCounter = ItemStack.of(ItemTypes.DYE, 1);
        greenCounter.offer(Keys.DYE_COLOR, DyeColors.GREEN);

        ItemStack redCounter = ItemStack.of(ItemTypes.DYE, 1);
        redCounter.offer(Keys.DYE_COLOR, DyeColors.RED);

        drawStatusDesign(DyeColors.RED, 5);
        drawInfoDesign();

        int slotCount = 1;
        int moneyCount = 1;
        for (int index = 36; index < 54; index++) {
            // Pokemon
            if (index <= 38 || (index >= 45 && index <= 47)) {
                addItem(index, ItemUtils.getSlotButton(slotCount).createSnapshot());
                slotCount++;
            }
            // Money
            else if ((index >= 42 && index <= 44) || index >= 51) {
                addItem(index, ItemUtils.getMoneyButton(moneyCount).createSnapshot());
                moneyCount *= 10;
            }
            // Exit
            else if (index == 40) {
                addItem(index, ItemUtils.getExitButton().createSnapshot());
            }
            // Accept
            else if (index == 48) {
                addItem(index, ItemUtils.getAcceptButton().createSnapshot());
            }
            // Update
            else if (index == 49) {
                addItem(index, ItemUtils.getResetMoneyButton().createSnapshot());
            }
            // Not Ready
            else if (index == 50) {
                addItem(index, ItemUtils.getCancelButton().createSnapshot());
            }
        }
    }

    private void handleClick(ClickInventoryEvent event) {
        event.setCancelled(true);
        // Shift clicking puts an empty air item in the view.
        if (event instanceof ClickInventoryEvent.Shift) {
            return;
        }
        event.getCause().first(Player.class).ifPresent(player -> {
            event.getTransactions().forEach(transaction -> {
                transaction.getSlot().getProperty(SlotIndex.class, "slotindex").ifPresent(slot -> {
                    ItemStackSnapshot snapshot = transaction.getOriginal();
                    // This is the player's inventory
                    if (slot.getValue() >= 54) {
                        if (isExecuting) {
                            return;
                        }
                        if (addItem(player, snapshot)) {
                            // Needs to wait for the click to cancel as the slot is momentarily empty when clicked
                            Sponge.getScheduler().createTaskBuilder().execute(() -> transaction.getSlot().clear()).delayTicks(1).submit(SafeTrade.getPlugin());
                        }
                    }
                    else {
                        if (snapshot.createStack().equalTo(ItemUtils.getCancelButton())) {
                            isExecuting = false;
                            if (player.equals(participants[0])) {
                                participant0Ready = false;
                            }
                            else {
                                participant1Ready = false;
                            }
                        }
                        else if (snapshot.createStack().equalTo(ItemUtils.getExitButton())) {
                            forceEnd();
                        }
                        else if (!isExecuting) {
                            if (isInSide(player, slot.getValue())) {
                                // Handles items inside the player's respected trade side
                                if (isPokemonItem(snapshot.createStack())) {
                                    removePokemon(player, snapshot.createStack(), slot.getValue());
                                }
                                else {
                                    if (removeItem(slot.getValue())) {
                                        Utils.giveItem(player, snapshot.createStack(), true);
                                    }
                                }
                            }
                            else if (snapshot.createStack().equalTo(ItemUtils.getAcceptButton())) {
                                if (player.equals(participants[0])) {
                                    participant0Ready = true;
                                }
                                else {
                                    participant1Ready = true;
                                }
                                if (participant0Ready && participant1Ready) {
                                    executeTrade();
                                }
                            }
                            else if (snapshot.createStack().equalTo(ItemUtils.getResetMoneyButton())) {
                                money.put(player, 0);
                            }
                            // Uses for loop to check money items
                            for (int i = 1; i <= 100000; i *= 10) {
                                if (snapshot.createStack().equalTo(ItemUtils.getMoneyButton(i))) {
                                    if (player.equals(participants[0])) {
                                        int newMoney = money.get(participants[0]);
                                        newMoney += i;
                                        money.put(participants[0], newMoney);
                                    }
                                    else {
                                        int newMoney = money.get(participants[1]);
                                        newMoney += i;
                                        money.put(participants[1], newMoney);
                                    }
                                }
                            }
                            // Uses for loop to check slot items
                            outerloop:
                            for (int i = 1; i <= 6; i++) {
                                if (snapshot.createStack().equalTo(ItemUtils.getSlotButton(i))) {
                                    EntityPixelmon slotPokemon = Utils.getPokemonInSlot(player, i);
                                    for (EntityPixelmon pixelmon : listedPokemon.get(player).values()) {
                                        if (PixelmonMethods.isIDSame(slotPokemon.getPokemonId(), pixelmon.getPokemonId())) {
                                            break outerloop;
                                        }
                                    }
                                    if (player.equals(participants[0])) {
                                        if (slotPokemon != null) {
                                            addPokemon(player, slotPokemon);
                                        }
                                    } else {
                                        if (slotPokemon != null) {
                                            addPokemon(player, slotPokemon);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Don't know why it needs a delay to work
                    Sponge.getScheduler().createTaskBuilder().execute(this::update).delayTicks(1).submit(SafeTrade.getPlugin());
                });
            });
        });
    }

    // Handles giving back items
    private void handleClose(InteractInventoryEvent.Close event) {
        isExecuting = false;
        if (forceCloser != null) {
            // Schedular prevents a disgusting sponge phase error that I'm guessing is due to attempting to closing the inventory during the close inventory event.
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                if (forceCloser.equals(participants[0])) {
                    // Needs to be null before trying to close another inventory, but also needs to be checked.
                    forceCloser = null;
                    participants[1].closeInventory();
                }
                else {
                    forceCloser = null;
                    participants[0].closeInventory();
                }
                end();
            }).delayTicks(1).submit(SafeTrade.getPlugin());
        }
        // Checks if the player is the cause of the inventory closing (ESC)
        if (event.getCause().first(Player.class).isPresent()) {
            Player player = event.getCause().first(Player.class).get();
            // Schedular prevents a disgusting sponge phase error that I'm guessing is due to attempting to closing the inventory during the close inventory event.
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                if (player.equals(participants[0])) {
                    participants[1].closeInventory();
                }
                else {
                    participants[0].closeInventory();
                }
                end();
            }).delayTicks(1).submit(SafeTrade.getPlugin());
        }
    }

    private void handleShiftClick(ClickInventoryEvent.Shift event) {
        event.setCancelled(true);
    }
}
