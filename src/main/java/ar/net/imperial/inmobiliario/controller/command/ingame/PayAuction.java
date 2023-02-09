package ar.net.imperial.inmobiliario.controller.command.ingame;

import ar.net.imperial.inmobiliario.Inmobiliario;
import ar.net.imperial.inmobiliario.model.auction.Auction;
import ar.net.imperial.inmobiliario.util.Settings;
import ar.net.imperial.inmobiliario.util.LangSource;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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
    public void onCommand(Player player) {
        Collection<Auction> auctions = Auction.getAuctionsWon(player);
        if (auctions.isEmpty()) {
            player.sendRichMessage(lang.getStr("PAYMENT_MENU_NO_AUCTIONS"));
            return;
        }
        ScrollingGui gui = getGUI();
        gui.setDefaultClickAction(event -> event.setCancelled(true));
        for (Auction auction : auctions) {
            GuiItem guiItem = getGuiItem(player, auction);
            gui.addItem(guiItem);
        }
        gui.open(player);
    }

    @NotNull
    private GuiItem getGuiItem(Player player, Auction auction) {
        Block block = auction.getPSRegions().iterator().next().getProtectBlock();
        int x = block.getX();
        int z = block.getZ();
        boolean playerHasMoney = economy.has(player, auction.getLastBid());
        Component actionComponent = playerHasMoney ?
                lang.getForItem("PAYMENT_MENU_LORE_ACTION_PAY")
                : lang.getForItem("PAYMENT_MENU_LORE_NOT_ENOUGH_MONEY");

        @NotNull ItemBuilder itemBuilder = ItemBuilder.from(Material.BOOK)
                .name(lang.getForItem("PAYMENT_MENU_ITEM_NAME", x, z))
                .lore(
                        lang.getForItem("PAYMENT_MENU_LORE_AUCTION_DEBT", auction.getLastBid()),
                        lang.getForItem("PAYMENT_MENU_LORE_TIME_TO_PAY", getRemainingTimeToPay(auction)),
                        Component.text(" "),
                        actionComponent
                );

        GuiItem guiItem;
        if (playerHasMoney) guiItem = itemBuilder.asGuiItem(e -> {
            payAuction(player, auction);
            e.getWhoClicked().closeInventory();
        });
        else guiItem = itemBuilder.asGuiItem();
        return guiItem;
    }

    private ScrollingGui getGUI() {
        Component title = lang.getForItem("PAYMENT_MENU_TITLE");
        Component previousText = lang.getForItem("PAYMENT_MENU_PREVIOUS");
        Component nextText = lang.getForItem("PAYMENT_MENU_NEXT");
        ScrollingGui gui = Gui.scrolling()
                .title(title)
                .rows(1)
                .scrollType(ScrollType.HORIZONTAL)
                .create();
        gui.setItem(1, 1, ItemBuilder.from(Material.ARROW).name(previousText).asGuiItem(event -> {gui.previous();}));
        gui.setItem(1, 9, ItemBuilder.from(Material.ARROW).name(nextText).asGuiItem(event -> {gui.next();}));
        return gui;
    }

    public void payAuction(Player player, Auction auction) {
        if (auction.getStatus().equals(Auction.AuctionStatus.PAID)) {
            player.sendRichMessage(lang.getStr("AUCTION_ALREADY_PAID"));
            return;
        }
        if (auction.getStatus().equals(Auction.AuctionStatus.CANCELLED)) {
            player.sendRichMessage(lang.getStr("AUCTION_CANCELLED"));
            return;
        }
        makePayments(player, auction);
        
        auction.setStatus(Auction.AuctionStatus.PAID);
        auction.transferProperties(player);
        
        player.sendRichMessage(lang.getStr("AUCTION_PAY_SUCCESS"));
        
        auction.delete();
        plugin.getAuctionsDatabase().get().set(auction.getUUID().toString(), null);
        plugin.getAuctionsDatabase().save();
    }

    private void makePayments(Player player, Auction auction) {
        economy.withdrawPlayer(player, auction.getLastBid());

        double commission = auction.getLastBid() * plugin.getConfig().getDouble(Settings.AGENT_COMMISSION.name());
        OfflinePlayer agent = auction.getAgent();
        economy.depositPlayer(agent, commission);

        if (agent.getPlayer() != null) agent.getPlayer().sendRichMessage(lang.getStr("AUCTION_PAY_COMMISSION", (int) commission));
    }

    public String getRemainingTimeToPay(Auction auction) {
        long remainingTime = (auction.getStartDate() + auction.getDuration() + auction.getTimeToPay()) - System.currentTimeMillis();
        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        else sb.append(lang.getStrNoPrefix("AUCTION_PAY_TIME_LEFT_LESS_THAN_HOUR"));
        return sb.toString();
    }
}
