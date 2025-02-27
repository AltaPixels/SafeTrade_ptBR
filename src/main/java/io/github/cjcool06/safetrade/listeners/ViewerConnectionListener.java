package io.github.cjcool06.safetrade.listeners;

import io.github.cjcool06.safetrade.api.events.trade.ViewerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class ViewerConnectionListener {

    @Listener
    public void onViewer(ViewerEvent event) {
        if (event instanceof ViewerEvent.Add) {
            event.trade.sendMessage(Text.of(TextColors.GOLD, event.viewer.getName(), TextColors.GREEN, " está visualizando a troca."));
        }
        else if (event instanceof ViewerEvent.Remove) {
            event.trade.sendMessage(Text.of(TextColors.GOLD, event.viewer.getName(), TextColors.RED, " não está mais visualizando a troca."));
        }
    }
}