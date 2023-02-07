package ar.net.imperial.inmobiliario.model.event;

import ar.net.imperial.inmobiliario.model.auction.Auction;

public class AuctionStartedEvent extends AuctionEvent {
    public AuctionStartedEvent(Auction auction) {
        super(auction);
    }
}
