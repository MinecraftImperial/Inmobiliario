package ar.net.imperial.inmobiliario.model.auction;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AuctionTimer extends BukkitRunnable {

    private final Auction auction;
    private final long duration;
    private final long scheduledStartDate;
    private final long timeToPay;
    private final PluginManager pluginManager;

    public AuctionTimer(Auction auction, PluginManager pluginManager) {
        this.auction = auction;
        this.scheduledStartDate = auction.getScheduledStartDate();
        this.duration = auction.getDuration();
        this.timeToPay = auction.getTimeToPay();
        this.pluginManager = pluginManager;
    }

    @Override
    public void run() {
        switch (auction.getStatus()) {
            case WAITING_TO_START -> {
                if (scheduledStartDate <= System.currentTimeMillis()) {
                    auction.start(pluginManager);
                }
            }
            case ACTIVE -> {
                if (auction.getStartDate() + duration <= System.currentTimeMillis()) {
                    auction.finish(pluginManager);
                    cancel();
                }
            }
            case WAITING_FOR_PAYMENT -> {
                if (auction.getStartDate() + duration + timeToPay <= System.currentTimeMillis()) {
                    auction.cancelBecauseNoPayment(pluginManager);
                    cancel();
                }
            }
            case PAID -> cancel();
        }
    }

    public void startTimer(JavaPlugin plugin, long delay, long period) {
        runTaskTimer(plugin, delay, period);
    }

}
