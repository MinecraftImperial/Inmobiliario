package ar.net.imperial.inmobiliario.model.auction;

import ar.net.imperial.inmobiliario.model.event.AuctionFinishedEvent;
import ar.net.imperial.inmobiliario.model.event.AuctionStartedEvent;
import ar.net.imperial.inmobiliario.model.event.NoPaymentReceivedEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.PSProtectBlock;
import dev.espi.protectionstones.PSRegion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SerializableAs("Auction")
public class Auction implements ConfigurationSerializable {

    private static final List<Auction> currentAuctions = new ArrayList<>();
    private final UUID uuid;
    private final Set<PSRegion> psRegions;
    private final OfflinePlayer agent;
    private final long scheduledStartDate; // The date when the auction is supposed to start in milliseconds
    private final long duration; // The duration of the auction in milliseconds
    private final long timeToPay;
    private final String channel;
    private long startDate; // The date when the auction actually started in milliseconds

    private long lastBid;
    private OfflinePlayer lastBidder;
    private AuctionStatus status;

    public Auction(Set<PSRegion> psregions, OfflinePlayer agent, long scheduledStartDate, long duration, long timeToPay, String channel) {
        this(UUID.randomUUID(), AuctionStatus.WAITING_TO_START, null, 0, agent, psregions, scheduledStartDate, duration, timeToPay, channel);
    }

    public Auction(UUID uuid, AuctionStatus status, OfflinePlayer lastBidder, int lastBid, OfflinePlayer agent,
                   Set<PSRegion> psregions, long scheduledStartDate, long duration, long timeToPay, String channel) {
        this.psRegions = psregions;
        this.agent = agent;
        this.scheduledStartDate = scheduledStartDate;
        this.startDate = scheduledStartDate;
        this.channel = channel;
        this.duration = duration;
        this.timeToPay = timeToPay;
        this.uuid = uuid;
        this.status = status;
        this.lastBidder = lastBidder;
        this.lastBid = lastBid;
        currentAuctions.add(this);
    }

    public static boolean isAuctioned(PSRegion connectedRegion) {
        for (Auction auction : currentAuctions) {
            if (auction.getPSRegions().contains(connectedRegion)) return true;
        }
        return false;
    }

    public static String getAvailableChannel(List<String> channels) {
        for (String channel : channels) {
            if (!isBusy(channel)) return channel;
        }
        return null;
    }

    private static boolean isBusy(String channel) {
        for (Auction auction : currentAuctions) {
            boolean auctionIsAssignedToChannel = auction.getChannel().equals(channel);
            boolean auctionIsActive = auction.getStatus() == AuctionStatus.ACTIVE;
            boolean auctionIsWaitingToStart = auction.getStatus() == AuctionStatus.WAITING_TO_START;
            if (auctionIsAssignedToChannel && (auctionIsActive || auctionIsWaitingToStart)) return true;
        }
        return false;
    }

    public static List<Auction> getCurrentAuctions() {
        return currentAuctions;
    }

    public static int getMinimumBid(long remaining_time, double current_bid) {
        // If remaining time is more than 20 minutes, minimum bid is 110% of the current bid
        // If remaining time is less than 20 minutes, minimum bid is 150% of the current bid
        // If remaining time is less than 1 minute, minimum bid is 500% of the current bid
        double minimum_bid_multiplier = 1.1;
        if (remaining_time < 1200000) minimum_bid_multiplier = 1.5;
        if (remaining_time < 60000) minimum_bid_multiplier = 5;
        return (int) (current_bid * minimum_bid_multiplier);
    }

/* To be used when implementing Notifications API
   public static ItemStack getPaymentBillIcon(Auction auction, LangSource lang) {
        ItemStack payItem = new ItemStack(Material.BOOK);
        ItemMeta im = payItem.getItemMeta();
        im.displayName(lang.get(MessagesKey.WON_AUCTION));
        List<Component> lore = new ArrayList<>();
        Set<PSRegion> regions = auction.getPSRegions();
        Optional<PSRegion> firstRegion = regions.stream().findFirst();
        if (!firstRegion.isPresent()) throw new IllegalStateException("Auction has no regions");
        PSRegion region = firstRegion.get();
        int x = region.getProtectBlock().getX();
        int y = region.getProtectBlock().getY();
        int z = region.getProtectBlock().getZ();
        lore.add(lang.get(MessagesKey.LORE_AUCTION_COORDS,false, x, y, z));
        lore.add(lang.get(MessagesKey.LORE_AUCTION_DEBT,false, auction.getLastBid()));
        lore.add(Component.text(" "));
        lore.add(lang.get(MessagesKey.LORE_AUCTION_PAY,false));
        im.lore(lore);
        payItem.setItemMeta(im);
        return payItem;
    }*/

    @SuppressWarnings("unused")
    public static Auction deserialize(@NotNull Map<String, Object> map) {
        UUID uuid = UUID.fromString((String) map.get("uuid"));
        AuctionStatus status = AuctionStatus.valueOf((String) map.get("status"));
        String channel = (String) map.get("channel");
        long duration = (int) map.get("duration");
        long timeToPay = (int) map.get("timeToPay");
        long scheduledStartDate = (long) map.get("scheduledStartDate");
        long startDate = (long) map.get("startDate");
        int lastBid = (int) map.get("lastBid");
        OfflinePlayer lastBidder = null;
        if (map.containsKey("lastBidder"))
            lastBidder = Bukkit.getOfflinePlayer(UUID.fromString((String) map.get("lastBidder")));
        OfflinePlayer agent = Bukkit.getOfflinePlayer(UUID.fromString((String) map.get("agent")));
        @SuppressWarnings("unchecked") List<String> regionNameList = (List<String>) map.get("psRegions");
        Set<PSRegion> psRegions = new HashSet<>();

        for (String regionName : regionNameList) {
            String[] region = regionName.split("::");
            World world = Bukkit.getWorld(region[1]);
            if (world == null) continue;
            RegionManager rgManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (rgManager == null) continue;
            ProtectedRegion wgRegion = rgManager.getRegion(region[0]);
            if (wgRegion == null) continue;
            PSRegion psRegion = PSRegion.fromWGRegion(Bukkit.getWorld(region[1]), wgRegion);
            psRegions.add(psRegion);
        }

        Auction auction = new Auction(uuid, status, lastBidder, lastBid, agent, psRegions, scheduledStartDate, duration, timeToPay, channel);
        auction.setStartDate(startDate);
        return auction;
    }

    public static List<Auction> getAuctionsWon(Player player) {
        List<Auction> auctions = new ArrayList<>();
        for (Auction auction : currentAuctions) {
            if (auction.getStatus() != AuctionStatus.WAITING_FOR_PAYMENT) continue;
            if (!auction.getLastBidder().equals(player)) continue;
            auctions.add(auction);
        }
        return auctions;
    }

    public static Auction getAuctionByID(String auctionID) {
        if (auctionID.length() < 8) return null;
        for (Auction auction : currentAuctions) {
            if (auction.getUUID().toString().startsWith(auctionID)) return auction;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public String getFormattedPSBlockList() {
        // Create a string with the form "{alias type 1} ({quantity}), {alias type 2} ({quantity}) ... {alias type 3} ({quantity})"
        // from the list psRegions
        // to get the alias of a psRegion use ProtectionStones.getBlockOptions(psRegion.getProtectBlock()).alias
        Map<String, Integer> aliasQuantities = new HashMap<>();
        for (PSRegion psRegion : psRegions) {
            PSProtectBlock psProtectBlock = psRegion.getTypeOptions();
            String alias = "Unknown block";
            if (psProtectBlock != null) alias = psProtectBlock.alias;
            if (aliasQuantities.containsKey(alias)) aliasQuantities.put(alias, aliasQuantities.get(alias) + 1);
            else aliasQuantities.put(alias, 1);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : aliasQuantities.entrySet()) {
            sb.append("`").append(entry.getKey()).append(" (x").append(entry.getValue()).append(")`, ");
        }
        return sb.toString();
    }

    public void start(PluginManager pluginManager) {
        setStartDate(System.currentTimeMillis());
        setStatus(Auction.AuctionStatus.ACTIVE);
        pluginManager.callEvent(new AuctionStartedEvent(this));
    }

    public void finish(PluginManager pluginManager) {
        if (lastBidder == null) {
            delete();
        } else {
            setStatus(Auction.AuctionStatus.WAITING_FOR_PAYMENT);
        }
        pluginManager.callEvent(new AuctionFinishedEvent(this));
    }

    public void delete() {
        currentAuctions.remove(this);
    }

    public void cancelBecauseNoPayment(PluginManager pluginManager) {
        pluginManager.callEvent(new NoPaymentReceivedEvent(this));
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getChannel() {
        return channel;
    }

    public Set<PSRegion> getPSRegions() {
        return psRegions;
    }

    public long getScheduledStartDate() {
        return scheduledStartDate;
    }

    public long getDuration() {
        return duration;
    }

    public long getTimeToPay() {
        return timeToPay;
    }

    public long getStartDate() {
        return startDate;
    }

    private void setStartDate(long time) {
        this.startDate = time;
    }

    public long getLastBid() {
        return lastBid;
    }

    public void setLastBid(long newBid) {
        lastBid = newBid;
    }

    public OfflinePlayer getLastBidder() {
        return lastBidder;
    }

    public void setLastBidder(OfflinePlayer offlinePlayer) {
        lastBidder = offlinePlayer;
    }

    public OfflinePlayer getAgent() {
        return agent;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid.toString());
        map.put("status", status.toString());
        map.put("channel", channel);
        map.put("duration", duration);
        map.put("timeToPay", timeToPay);
        map.put("scheduledStartDate", scheduledStartDate);
        map.put("startDate", startDate);
        map.put("lastBid", lastBid);
        if (lastBidder != null) map.put("lastBidder", lastBidder.getUniqueId().toString());
        map.put("agent", agent.getUniqueId().toString());
        List<String> regionNameList = psRegions.stream()
                .map(psRegion -> psRegion.getId() + "::" + psRegion.getWorld().getName())
                .collect(Collectors.toList());
        map.put("psRegions", regionNameList);
        return map;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void transferProperties() {
        for (PSRegion psRegion : psRegions) {
            for(UUID uuid : psRegion.getOwners()){
                psRegion.removeOwner(uuid);
            }
            psRegion.addOwner(lastBidder.getUniqueId());
        }
    }



    public enum AuctionStatus {
        WAITING_TO_START, // Is waiting to be started
        ACTIVE, // Is running on a discord channel
        WAITING_FOR_PAYMENT, // Has finished and is waiting for payment
        PAID, // Has been paid and is waiting for the agent to dismiss the notification (Useful for the Notifications plugin)
        CANCELLED, // The user has not paid in time
    }
}
