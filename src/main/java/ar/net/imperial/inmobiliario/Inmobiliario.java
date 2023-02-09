package ar.net.imperial.inmobiliario;

//import ar.net.imperial.imperiallangyml.LangAPI;
//import ar.net.imperial.imperiallangyml.LangSource;
import ar.net.imperial.inmobiliario.controller.EventListener;
import ar.net.imperial.inmobiliario.controller.PlaceholderManager;
import ar.net.imperial.inmobiliario.controller.command.ingame.PayAuction;
import ar.net.imperial.inmobiliario.controller.command.indiscord.Bid;
import ar.net.imperial.inmobiliario.controller.command.ingame.Expropiate;
import ar.net.imperial.inmobiliario.model.auction.Auction;
import ar.net.imperial.inmobiliario.model.auction.AuctionTimer;
import ar.net.imperial.inmobiliario.util.Settings;
import ar.net.imperial.inmobiliario.util.ConfigAccessor;
import ar.net.imperial.inmobiliario.util.LangSource;
import ar.net.imperial.trabajoscore.model.job.Job;
import co.aikar.commands.PaperCommandManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import github.scarsz.discordsrv.DiscordSRV;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

public class Inmobiliario extends JavaPlugin {


    private static final long TICKS_PER_SECOND = 20;

//    private LangSource lang;
    private Job job;
    private RegionContainer regionContainer; // WorldGuard region container
    private ConfigAccessor auctionsDatabase;
    private static Economy econ = null;
    private LangSource lang;

    @Override
    public void onEnable() {
        if (!setupEconomy() ) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        setupJob();
        setupLanguageSource();
        setupEvents();
        setupPlaceholders();
        setupWorldGuardRegionContainer();
        setupCommands();
        setupDatabase();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public void onDisable() {
        auctionsDatabase.save();
    }

    private void setupDatabase() {
        ConfigurationSerialization.registerClass(Auction.class, "Auction");
        auctionsDatabase = new ConfigAccessor(this, "auctions_database");
        auctionsDatabase.saveDefault();
        // We need to wait for the DiscordSRV plugin to start the bot
        new BukkitRunnable() {
            @Override
            public void run() {
                loadAuctionsFromFile();
            }
        }.runTaskLater(this, 20 * 10);
    }

    private void loadAuctionsFromFile() {
        Set<String> auctionsID = auctionsDatabase.get().getKeys(true);
        for (String auctionID : auctionsID) {
            if (auctionID.equalsIgnoreCase("version")) continue;
            Auction auction = (Auction) auctionsDatabase.get().get(auctionID);
            if (auction == null) return;
            AuctionTimer auctionTimer = new AuctionTimer(auction, Bukkit.getPluginManager());
            long period = switch (auction.getStatus()) {
                case WAITING_TO_START -> TICKS_PER_SECOND * 5;
                case WAITING_FOR_PAYMENT -> TICKS_PER_SECOND * 60 * 60;
                default -> TICKS_PER_SECOND * 60;
            };
            auctionTimer.startTimer(this, 0, period);
        }
        System.out.println("Loaded " + Auction.getCurrentAuctions().size() + " auctions from file");
    }

    private void setupCommands() {
        PaperCommandManager cmdManager = new PaperCommandManager(this);
        registerCommandContexts(cmdManager);
        registerCommands(cmdManager);
    }

    private void registerCommands(PaperCommandManager cmdManager) {
        cmdManager.registerCommand(new Expropiate(this));
        cmdManager.registerCommand(new PayAuction(this));

        DiscordSRV.api.addSlashCommandProvider(new Bid(this));
        new BukkitRunnable() {
            @Override
            public void run() {
                DiscordSRV.api.updateSlashCommands();
            }
        }.runTaskLater(this, 20);
    }

    private void registerCommandContexts(PaperCommandManager cmdManager) {
        cmdManager.getCommandContexts().registerContext(Auction.class, c -> {
            String auctionID = c.popFirstArg();
            return Auction.getAuctionByID(auctionID);
        });
    }

    private void setupWorldGuardRegionContainer() {
        regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    private void setupPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderManager().register();
        }
    }

    private void setupEvents() {
        EventListener eventListener = new EventListener(this);
        Bukkit.getPluginManager().registerEvents(eventListener, this);
    }

    private void setupLanguageSource() {
        String locale = getConfig().getString(Settings.LOCALE.name(), "es");
        Locale.setDefault(new Locale(locale));
        lang = new LangSource(ResourceBundle.getBundle("messages"));
//        lang = LangAPI.getLangSource(this);
//        lang.saveDefault();
    }

    private void setupJob() {
        String name = getConfig().getString(Settings.JOB_NAME.name());
        String displayname = getConfig().getString(Settings.JOB_DISPLAYNAME.name());
        Location location = getConfig().getLocation(Settings.JOB_LOCATION.name());
        job = Job.createJob(name, displayname, location);
        Job.setupDefaultRequirements(job);
    }

    public LangSource getLang() {
        return lang;
    }

    public Job getJob() {
        return job;
    }

    public RegionContainer getRegionContainer() {
        return regionContainer;
    }

    public ConfigAccessor getAuctionsDatabase() {
        return auctionsDatabase;
    }

    public static Economy getEconomy() {
        return econ;
    }

}
