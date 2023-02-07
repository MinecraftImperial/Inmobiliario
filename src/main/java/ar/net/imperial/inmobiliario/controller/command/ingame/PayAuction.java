package ar.net.imperial.inmobiliario.controller.command.ingame;

import ar.net.imperial.imperiallangyml.LangSource;
import ar.net.imperial.inmobiliario.Inmobiliario;
import ar.net.imperial.inmobiliario.model.auction.Auction;
import ar.net.imperial.inmobiliario.util.MessagesKey;
import ar.net.imperial.inmobiliario.util.Settings;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("pagarsubasta")
public class PayAuction extends BaseCommand {
    private final Inmobiliario plugin;
    private final Economy economy;
    private final LangSource lang;

    public PayAuction(Inmobiliario plugin) {
        this.plugin = plugin;
        this.economy = Inmobiliario.getEconomy();
        this.lang = plugin.getLang();
    }

    @Default
    @CommandCompletion("@auctionsWon")
    public void onPay(Player player, Auction auction) {
        List<Auction> auctionList = Auction.getAuctionsWon(player);
        if (!auctionList.contains(auction)) {
            player.sendMessage(lang.get(MessagesKey.FAIL_AUCTION_PAYMENT));
            return;
        }
        if (!economy.has(player, auction.getLastBid())) {
            player.sendMessage(lang.get(MessagesKey.AUCTION_PAY_NO_MONEY, true, auction.getLastBid()));
            return;
        }
        economy.withdrawPlayer(player, auction.getLastBid());

        double commission = auction.getLastBid() * plugin.getConfig().getDouble(Settings.AGENT_COMMISSION.name());
        OfflinePlayer agent = auction.getAgent();
        economy.depositPlayer(agent, commission);

        if (agent.getPlayer() != null) agent.getPlayer().sendMessage(lang.get(MessagesKey.AUCTION_PAY_COMMISSION, true, (int) commission));

        auction.setStatus(Auction.AuctionStatus.PAID);
        auction.transferProperties(player);

        player.sendMessage(lang.get(MessagesKey.AUCTION_PAY_SUCCESS));

        auction.delete();
    }
}
