package ar.net.imperial.inmobiliario.model.event;

import ar.net.imperial.inmobiliario.model.auction.Auction;

public class AuctionFinishedEvent extends AuctionEvent {
    public AuctionFinishedEvent(Auction auction) {
        super(auction);
    }
}
