package io.github.cjcool06.safetrade.commands;

import io.github.cjcool06.safetrade.obj.Trade;
import io.github.cjcool06.safetrade.trackers.Tracker;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class EndTradeCommand implements CommandExecutor {
    public static CommandSpec getSpec() {
        return CommandSpec.builder()
                .description(Text.of("Encerrar a troca"))
                .permission("safetrade.admin.end")
                .arguments(GenericArguments.player(Text.of("target")))
                .executor(new EndTradeCommand())
                .build();
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        Player target = args.<Player>getOne("target").get();
        Trade trade = Tracker.getActiveTrade(target);

        if (trade == null) {
            src.sendMessage(Text.of(TextColors.RED, "Esse jogador não está atualmente em uma troca."));
            return CommandResult.success();
        }
        trade.sendMessage(Text.of(TextColors.GRAY, "A troca foi encerrada por: " + src.getName() + "."));
        trade.forceEnd();
        src.sendMessage(Text.of(TextColors.GREEN, "A troca de " + target.getName() + " foi encerrada."));

        return CommandResult.success();
    }
}
