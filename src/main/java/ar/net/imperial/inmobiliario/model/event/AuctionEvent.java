package ar.net.imperial.inmobiliario.model.event;

import ar.net.imperial.inmobiliario.model.auction.Auction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class AuctionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Auction auction;

    public AuctionEvent(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
