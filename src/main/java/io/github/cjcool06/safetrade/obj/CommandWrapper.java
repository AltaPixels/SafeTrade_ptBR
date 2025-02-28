package io.github.cjcool06.safetrade.obj;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.cjcool06.safetrade.SafeTrade;
import io.github.cjcool06.safetrade.api.enums.CommandType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

/**
 * A CommandWrapper encapsulates the functionality of Sponge's command execution.
 */
public class CommandWrapper {
    public final String cmd;
    public final CommandType commandType;

    public CommandWrapper(String cmd, CommandType commandType) {
        this.cmd = cmd;
        this.commandType = commandType;
    }

    /**
     * Execute the command as {@link org.spongepowered.api.command.source.ConsoleSource}.
     */
    public void consoleExecute() {
        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd);
    }

    /**
     * Execute the command as the {@link Player}.
     *
     * @param player The player
     */
    public void sudoExecute(Player player) {
        Sponge.getCommandManager().process(player, cmd);
    }

    public void toContainer(JsonObject jsonObject) {
        jsonObject.add("Command", new JsonPrimitive(cmd));
        jsonObject.add("CommandType", new JsonPrimitive(commandType.name()));
    }

    public static CommandWrapper fromContainer(JsonObject jsonObject) {
        try {
            String cmd = jsonObject.get("Command").getAsString();
            CommandType commandType = jsonObject.get("CommandType").getAsString().equals("CONSOLE") ? CommandType.CONSOLE : CommandType.SUDO;

            return new CommandWrapper(cmd, commandType);
        } catch (Exception e) {
            SafeTrade.getLogger().warn("There was a problem deserialising a CommandWrapper from a container.");
            e.printStackTrace();
            return null;
        }
    }
}