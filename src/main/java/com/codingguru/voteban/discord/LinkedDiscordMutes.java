package com.codingguru.voteban.discord;

import com.codingguru.voteban.VoteBan;
import com.codingguru.voteban.util.MuteStatus;
import com.codingguru.voteban.util.ServerTypeUtil;
import com.earth2me.essentials.Essentials;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.utils.MarkdownSanitizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** DiscordSRV owns identity links; Essentials owns mute state and absolute expiry. */
public final class LinkedDiscordMutes implements AutoCloseable, Listener {
    private final VoteBan plugin;
    private final Essentials essentials;
    private final AtomicInteger revision = new AtomicInteger();
    private volatile Map<String, UUID> linkedPlayers = Map.of();

    public LinkedDiscordMutes(VoteBan plugin) {
        this.plugin = plugin;
        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        DiscordSRV.api.subscribe(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (DiscordSRV.isReady) refresh();
    }

    public Map<String, UUID> linkedPlayers() {
        return linkedPlayers;
    }

    public String displayName(UUID uuid, String accountName) {
        var user = essentials.getUser(uuid);
        return user == null ? accountName : user.getDisplayName();
    }

    /** Mirror the existing public vote notices using the same channel as linked mutes. */
    public void announce(String message) {
        if (!DiscordSRV.isReady) {
            plugin.getLogger().warning("Could not announce vote-mute: DiscordSRV is not ready");
            return;
        }
        TextChannel channel = DiscordSRV.getPlugin().getMainTextChannel();
        if (channel == null) {
            plugin.getLogger().warning("Could not announce vote-mute: DiscordSRV has no main chat channel");
            return;
        }
        sendAnnouncement(channel, message, plugin.getLogger());
    }

    static void sendAnnouncement(TextChannel channel, String message, Logger logger) {
        String content = MarkdownSanitizer.escape(ChatColor.stripColor(message)
                .replace("\\\\n", "\n").replace("\\n", "\n"));
        if (content.isBlank()) return;
        try {
            // Reasons are player text: display them literally and never notify mentions.
            channel.sendMessage("**" + content + "**").allowedMentions(Set.of()).queue(
                    ignored -> {},
                    error -> logger.warning("Could not announce vote-mute in Discord: " + error.getMessage()));
        } catch (RuntimeException error) {
            // Discord permissions/connection failures must not interrupt the game vote.
            logger.warning("Could not announce vote-mute in Discord: " + error.getMessage());
        }
    }

    @Subscribe
    public void onReady(DiscordReadyEvent event) { refresh(); }

    @Subscribe
    public void onLink(AccountLinkedEvent event) { refresh(); }

    @Subscribe
    public void onUnlink(AccountUnlinkedEvent event) { refresh(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // A Minecraft rename is recorded by Essentials when the player joins.
        refresh();
    }

    private void refresh() {
        int requested = revision.incrementAndGet();
        // Account links may use JDBC. Keep storage access off the server thread.
        Runnable load = () -> {
            Map<String, UUID> links = DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts();
            Set<UUID> uuids;
            // The file-backed API returns its live map; copy before loading users.
            synchronized (links) {
                uuids = Set.copyOf(links.values());
            }
            Map<String, UUID> players = new HashMap<>();
            for (UUID uuid : uuids) {
                var user = essentials.getUser(uuid);
                if (user != null && user.getName() != null) players.put(user.getName(), uuid);
            }
            if (requested == revision.get()) linkedPlayers = Map.copyOf(players);
        };
        if (plugin.getServerType() == ServerTypeUtil.FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> load.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, load);
        }
    }

    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePreProcessEvent event) {
        // Only the existing main server-chat bridge is moderated, never other channels.
        if (!event.getChannel().equals(DiscordSRV.getPlugin().getMainTextChannel())) return;
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getAuthor().getId());
        if (uuid == null) return;
        var user = essentials.getUser(uuid);
        if (user == null || !MuteStatus.isActive(user.isMuted(), user.getMuteTimeout(), System.currentTimeMillis())) return;
        event.setCancelled(true);
        event.getMessage().delete().reason("Minecraft community mute").queue(
                ignored -> {},
                error -> plugin.getLogger().warning("Could not delete muted Discord message "
                        + event.getMessage().getId() + ": " + error.getMessage()));
        plugin.getLogger().info("Blocked server-chat message from muted Minecraft account " + user.getName());
    }

    @Override
    public void close() {
        revision.incrementAndGet();
        DiscordSRV.api.unsubscribe(this);
    }
}
