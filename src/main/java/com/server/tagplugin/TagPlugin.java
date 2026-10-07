package com.server.tagplugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TagPlugin extends JavaPlugin implements Listener {
    private TagManager tagManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.tagManager = new TagManager(this);
        this.getServer().getPluginManager().registerEvents(this, this);
        TagCommand tagCommand = new TagCommand(this, this.tagManager);
        this.getCommand("tag").setExecutor(tagCommand);
        this.getCommand("tag").setTabCompleter(tagCommand);
        this.getLogger().info("TagPlugin (Çoklu Tag / Suffix Destekli) aktif edildi. " + this.tagManager.getAllTagDefs().size() + " tag tanımı yüklendi.");
    }

    public void onDisable() {
        if (this.tagManager != null) {
            this.tagManager.saveAll();
        }
    }

    public TagManager getTagManager() {
        return this.tagManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        this.tagManager.applyStoredTagOnJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.tagManager.clearScoreboardTag(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        String prefixName = this.tagManager.getPlayerTagName(event.getPlayer().getUniqueId());
        String suffixName = this.tagManager.getPlayerSuffixName(event.getPlayer().getUniqueId());
        
        Component finalPrefix = Component.empty();
        Component finalSuffix = Component.empty();

        // Ek etiket (örn. klan) önekin önüne gelir
        String ekFormat = this.tagManager.getPlayerEk(event.getPlayer().getUniqueId());
        if (ekFormat != null) finalPrefix = LegacyComponentSerializer.legacySection().deserialize(ekFormat);
        
        if (prefixName != null) {
            String rawFormat = this.tagManager.getTagFormat(prefixName);
            if (rawFormat != null) finalPrefix = finalPrefix.append(LegacyComponentSerializer.legacySection().deserialize(rawFormat));
        }
        
        if (suffixName != null) {
            String rawFormat = this.tagManager.getTagFormat(suffixName);
            if (rawFormat != null) finalSuffix = LegacyComponentSerializer.legacySection().deserialize(rawFormat);
        }

        Component playerName = Component.text(event.getPlayer().getName());
        Component separator = Component.text(": ");
        
        final Component p = finalPrefix;
        final Component s = finalSuffix;
        
        event.renderer((source, sourceDisplayName, message, viewer) -> p.append(playerName).append(s).append(separator).append(message));
    }
}