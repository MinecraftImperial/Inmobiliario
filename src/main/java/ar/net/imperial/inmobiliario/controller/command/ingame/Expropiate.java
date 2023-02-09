package ar.net.imperial.inmobiliario.controller.command.ingame;

import ar.net.imperial.imperiallangyml.LangSource;
import ar.net.imperial.inmobiliario.Inmobiliario;
import ar.net.imperial.inmobiliario.model.auction.Auction;
import ar.net.imperial.inmobiliario.model.auction.AuctionTimer;
import ar.net.imperial.inmobiliario.util.MessagesKey;
import ar.net.imperial.inmobiliario.util.Settings;
import ar.net.imperial.inmobiliario.util.Utils;
import ar.net.imperial.trabajoscore.model.job.Job;
import ar.net.imperial.trabajoscore.model.player.PlayerWrapper;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.espi.protectionstones.PSRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

@CommandAlias("expropiar|expropiate")
public class Expropiate extends BaseCommand {

    private final LangSource lang;
    private final FileConfiguration config;
    private final Inmobiliario plugin;
    private final Job job;
    private final static long DELAY_TO_START = 3000;
    private final static String FORCE_PERMISSION = "inmobiliario.expropiate.force";

    public Expropiate(Inmobiliario plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLang();
        this.job = plugin.getJob();
        this.config = plugin.getConfig();
    }

    @Default
    public void expropriateDefault(Player player) {
        expropriate(player, false);
    }

    @Subcommand("force")
    @CommandPermission(FORCE_PERMISSION)
    public void expropriateForce(Player player){
        expropriate(player, true);
    }

    public void expropriate(Player player, boolean force) {
        PlayerWrapper playerWrapper = PlayerWrapper.of(player);
        if (!force) {
            if (!playerWrapper.hasJob(job)) {
                player.sendMessage(lang.get(MessagesKey.DO_NOT_HAVE_JOB));
                return;
            }
        }

        Location location = player.getLocation();
        PSRegion psRegion = PSRegion.fromLocation(location);
        if (psRegion == null) {
            player.sendMessage(lang.get(MessagesKey.NOT_IN_REGION));
            return;
        }

        if (!force) {
            UUID owner = psRegion.getOwners().get(0);
            long lastLogin = Utils.lastLogin(owner);
            long daysToExpropriate = config.getInt(Settings.DAYS_TO_EXPROPRIATE.name());
            if (lastLogin < daysToExpropriate) {
                player.sendMessage(lang.get(MessagesKey.NOT_EXPROPRIATED));
                return;
            }
        }

        Set<PSRegion> connectedRegions = new HashSet<>();
        World world = location.getWorld();
        RegionManager regionManager = plugin.getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            throw new IllegalStateException("RegionManager is null");
        }

        Utils.getConnectedRegions(psRegion, connectedRegions, regionManager);
        // Check if any connected region is already being auctioned
        if (connectedRegions.stream().anyMatch((Auction::isAuctioned))) {
            player.sendMessage(lang.get(MessagesKey.ALREADY_AUCTIONED));
            return;
        }

        //Check if there is an available auction channel
        List<String> channels = config.getStringList(Settings.AUCTION_CHANNELS.name());
        String channel = Auction.getAvailableChannel(channels);
        if (channel == null) {
            player.sendMessage(lang.get(MessagesKey.NO_CHANNELS_AVAILABLE));
            return;
        }

        long durationInSeconds = config.getLong(Settings.AUCTION_DURATION.name());
        long durationInMilliseconds = durationInSeconds * 1000;
        long timeToPayInSeconds = config.getLong(Settings.TIME_TO_PAY.name());
        long timeToPayInMilliseconds = timeToPayInSeconds * 1000;
        long startDate = System.currentTimeMillis() + DELAY_TO_START;

        Auction auction = new Auction(connectedRegions, player, startDate, durationInMilliseconds, timeToPayInMilliseconds, channel);
        AuctionTimer timer = new AuctionTimer(auction, Bukkit.getPluginManager());
        timer.startTimer(plugin, 0, 20);
        plugin.getAuctionsDatabase().get().set(auction.getUUID().toString(), auction);
        plugin.getAuctionsDatabase().save();
        player.sendMessage(lang.get(MessagesKey.AUCTION_STARTED, true, channel));
    }

}
