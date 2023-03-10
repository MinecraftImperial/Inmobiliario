package ar.net.imperial.inmobiliario.controller.command.indiscord;

import ar.net.imperial.inmobiliario.Inmobiliario;
import ar.net.imperial.inmobiliario.model.auction.Auction;
import ar.net.imperial.inmobiliario.util.LangSource;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionMapping;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class Bid implements SlashCommandProvider {
    private final LangSource lang;
    private final Inmobiliario plugin;
    private final String commandName;
    private final String commandOptionName;
    private final String commandDescription;
    private final String commandOptionDescription;

    public Bid(Inmobiliario plugin) {
        this.lang = plugin.getLang();
        this.plugin = plugin;
        this.commandName = lang.getStrNoPrefix("TO_BID_COMMAND");
        this.commandDescription = lang.getStrNoPrefix("TO_BID_COMMAND_DESCRIPTION");
        this.commandOptionName = lang.getStrNoPrefix("TO_BID_COMMAND_OPTION_1");
        this.commandOptionDescription = lang.getStrNoPrefix("TO_BID_COMMAND_OPTION_1_DESCRIPTION");
    }
    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        PluginSlashCommand toBid = new PluginSlashCommand(plugin, new CommandData(commandName, commandDescription)
                .addOption(OptionType.INTEGER, commandOptionName, commandOptionDescription, true));
        return new HashSet<>(Collections.singletonList(toBid));
    }

    /* path should be the name of the command, as it is got from the lang file we use a wildcard and filter the command manually */
    @SlashCommand(path = "*")
    public void onBidCommand(SlashCommandEvent event) {
        if(!event.getName().equals(commandName)) return;

        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (uuid == null) {
            event.reply(lang.getStrNoPrefix("TO_USE_NEED_TO_LINK_ACCOUNT")).queue();
            return;
        }

        String channel = event.getChannel().getName();
        Auction auction = null;
        for(Auction a : Auction.getCurrentAuctions()){
            if(a.getChannel().equals(channel) && a.getStatus() == Auction.AuctionStatus.ACTIVE){
                auction = a;
                break;
            }
        }
        if(auction == null){
            event.reply(lang.getStrNoPrefix("NO_BID_IN_THE_CHANNEL")).queue();
            return;
        }

        long start_date = auction.getStartDate();
        // duration is got from the config in seconds, we need milliseconds
        long duration = auction.getDuration();
        long remaining_time = start_date + duration - System.currentTimeMillis();
        double current_bid = auction.getLastBid();
        int minimum_bid = Auction.getMinimumBid(remaining_time, current_bid);

        OptionMapping option = event.getOption(commandOptionName);
        if (option == null) {
            event.reply(lang.getStrNoPrefix("BID_TOO_LOW", minimum_bid)).queue();
            return;
        }
        long newBid = option.getAsLong();


        if(newBid < minimum_bid){
            event.reply(lang.getStrNoPrefix("BID_TOO_LOW", minimum_bid)).queue();
            return;
        }

        auction.setLastBid(newBid);
        OfflinePlayer lastBidder = Bukkit.getOfflinePlayer(uuid);
        auction.setLastBidder(lastBidder);
        plugin.getAuctionsDatabase().save();
        event.reply(lang.getStrNoPrefix("NEW_BID", lastBidder.getName(), newBid, Auction.getMinimumBid(remaining_time, newBid))).queue();
    }


}
