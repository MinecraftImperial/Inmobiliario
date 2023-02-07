package ar.net.imperial.inmobiliario.model.event;

import ar.net.imperial.inmobiliario.model.auction.Auction;

public class NoPaymentReceivedEvent extends AuctionEvent {
    public NoPaymentReceivedEvent(Auction auction) {
        super(auction);
    }
}
