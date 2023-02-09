package ar.net.imperial.inmobiliario.controller;

import ar.net.imperial.inmobiliario.Inmobiliario;
import ar.net.imperial.inmobiliario.model.auction.Auction;
import ar.net.imperial.inmobiliario.model.auction.AuctionTimer;
import ar.net.imperial.inmobiliario.model.event.AuctionFinishedEvent;
import ar.net.imperial.inmobiliario.model.event.AuctionStartedEvent;
import ar.net.imperial.inmobiliario.model.event.NoPaymentReceivedEvent;
import ar.net.imperial.inmobiliario.util.MessagesKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EventListener implements Listener {

    private final Inmobiliario plugin;

    public EventListener(Inmobiliario plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAuctionFinished(AuctionFinishedEvent event) {
        Auction auction = event.getAuction();

        if (auction.getLastBidder() == null) {
            deleteAuctionData(auction);
            return;
        }

        AuctionTimer timer = new AuctionTimer(auction, Bukkit.getPluginManager());
        timer.startTimer(plugin, 0, 20*60*60); // Each hour check if the time to pay was exceeded
        saveAuctionData(event.getAuction());
        Player lastBidder = auction.getLastBidder().isOnline() ? auction.getLastBidder().getPlayer() : null;
        if (lastBidder != null) {
            lastBidder.sendMessage(plugin.getLang().get(MessagesKey.WON_AUCTION));
        }
    }

    @EventHandler
    public void onAuctionStart(AuctionStartedEvent event) {
        saveAuctionData(event.getAuction());
    }

    @EventHandler
    public void onNoPaymentReceived(NoPaymentReceivedEvent event) {
        event.getAuction().delete();
        deleteAuctionData(event.getAuction());
    }

    public void deleteAuctionData(Auction auction) {
        plugin.getAuctionsDatabase().get().set(auction.getUUID().toString(), null);
        plugin.getAuctionsDatabase().save();
    }

    private void saveAuctionData(Auction auction) {
        plugin.getAuctionsDatabase().get().set(auction.getUUID().toString(), auction);
        plugin.getAuctionsDatabase().save();
    }


}
