package com.server.tagplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TagManager {
    private final TagPlugin plugin;
    private final Map<String, String> tagDefinitions = new HashMap<>();
    private final Map<UUID, String> playerTags = new HashMap<>();
    private final Map<UUID, String> playerSuffixes = new HashMap<>(); // YENİ: Suffixleri tutan harita
    // EK ETİKET: Önekin de önünde görünen, diğer eklentilerin (örn. Klan) yönettiği serbest etiket.
    // Önceden tanımlı tag gerektirmez; doğrudan renkli format saklanır. Önek ve sonekten bağımsızdır.
    private final Map<UUID, String> playerEkler = new HashMap<>();
    // KURUCU ETİKETİ: en başta görünen, elle (komutla) verilen ayrı etiket: [Kurucu] [Ek] [Önek] İsim [Sonek] [Lig]
    private final Map<UUID, String> playerKuruculer = new HashMap<>();
    // LİG ETİKETİ: sonekin de arkasında görünen, Arena Ligi'nin yönettiği serbest etiket: [Ek] [Önek] İsim [Sonek] [Lig]
    private final Map<UUID, String> playerLigler = new HashMap<>();
    private static final String TEAM_PREFIX = "tagplg_";

    /** Tag adı karşılaştırması: büyük/küçük harf ve Türkçe karakterler (ı, ş, ğ, ü, ö, ç) önemsiz. */
    public static String norm(String ad) {
        if (ad == null) return "";
        String k = ad.toLowerCase(java.util.Locale.ROOT).replace("ı", "i").replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o").replace("ç", "c").replace("i̇", "i");
        return k.trim();
    }

    public TagManager(TagPlugin plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        FileConfiguration cfg = this.plugin.getConfig();
        this.tagDefinitions.clear();
        this.playerTags.clear();
        this.playerSuffixes.clear();
        this.playerEkler.clear();
        this.playerLigler.clear();
        this.playerKuruculer.clear();

        if (cfg.isConfigurationSection("tagdefs")) {
            for(String key : cfg.getConfigurationSection("tagdefs").getKeys(false)) {
                String raw = cfg.getString("tagdefs." + key);
                String colored = raw != null ? ChatColor.translateAlternateColorCodes('&', raw) : null;
                this.tagDefinitions.put(norm(key), colored);
            }
        }

        if (cfg.isConfigurationSection("player-tags")) {
            for(String key : cfg.getConfigurationSection("player-tags").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String tag = cfg.getString("player-tags." + key);
                    this.playerTags.put(uuid, tag);
                } catch (IllegalArgumentException var6) {
                    this.plugin.getLogger().warning("Geçersiz UUID config'de atlandı (Prefix): " + key);
                }
            }
        }

        this.loadEkler(cfg);
        if (cfg.isConfigurationSection("player-kurucu-tags")) {
            for (String key : cfg.getConfigurationSection("player-kurucu-tags").getKeys(false)) {
                try {
                    this.playerKuruculer.put(UUID.fromString(key), cfg.getString("player-kurucu-tags." + key));
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Geçersiz UUID config'de atlandı (Kurucu etiketi): " + key);
                }
            }
        }
        if (cfg.isConfigurationSection("player-lig-tags")) {
            for (String key : cfg.getConfigurationSection("player-lig-tags").getKeys(false)) {
                try {
                    this.playerLigler.put(UUID.fromString(key), cfg.getString("player-lig-tags." + key));
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Geçersiz UUID config'de atlandı (Lig etiketi): " + key);
                }
            }
        }

        // YENİ: Suffix yükleme
        if (cfg.isConfigurationSection("player-suffixes")) {
            for(String key : cfg.getConfigurationSection("player-suffixes").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String suffix = cfg.getString("player-suffixes." + key);
                    this.playerSuffixes.put(uuid, suffix);
                } catch (IllegalArgumentException var6) {
                    this.plugin.getLogger().warning("Geçersiz UUID config'de atlandı (Suffix): " + key);
                }
            }
        }
    }

    private void loadEkler(FileConfiguration cfg) {
        if (!cfg.isConfigurationSection("player-ek-tags")) return;
        for (String key : cfg.getConfigurationSection("player-ek-tags").getKeys(false)) {
            try {
                this.playerEkler.put(UUID.fromString(key), cfg.getString("player-ek-tags." + key));
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().warning("Geçersiz UUID config'de atlandı (Ek etiket): " + key);
            }
        }
    }

    public void saveAll() {
        FileConfiguration cfg = this.plugin.getConfig();
        
        cfg.set("tagdefs", null);
        for(Map.Entry<String, String> entry : this.tagDefinitions.entrySet()) {
            cfg.set("tagdefs." + entry.getKey(), entry.getValue());
        }
        
        cfg.set("player-tags", null);
        for(Map.Entry<UUID, String> entry : this.playerTags.entrySet()) {
            cfg.set("player-tags." + entry.getKey().toString(), entry.getValue());
        }

        // YENİ: Suffix kaydetme
        cfg.set("player-suffixes", null);
        for(Map.Entry<UUID, String> entry : this.playerSuffixes.entrySet()) {
            cfg.set("player-suffixes." + entry.getKey().toString(), entry.getValue());
        }

        cfg.set("player-ek-tags", null);
        for (Map.Entry<UUID, String> entry : this.playerEkler.entrySet()) {
            cfg.set("player-ek-tags." + entry.getKey().toString(), entry.getValue());
        }

        cfg.set("player-kurucu-tags", null);
        for (Map.Entry<UUID, String> entry : this.playerKuruculer.entrySet()) {
            cfg.set("player-kurucu-tags." + entry.getKey().toString(), entry.getValue());
        }

        cfg.set("player-lig-tags", null);
        for (Map.Entry<UUID, String> entry : this.playerLigler.entrySet()) {
            cfg.set("player-lig-tags." + entry.getKey().toString(), entry.getValue());
        }
        
        this.plugin.saveConfig();
    }

    public boolean tagExists(String tagName) {
        return this.tagDefinitions.containsKey(norm(tagName));
    }

    public void createTag(String tagName, String rawFormat) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawFormat);
        this.tagDefinitions.put(norm(tagName), colored);
        this.saveAll();
    }

    public boolean deleteTagDefinition(String tagName) {
        String key = norm(tagName);
        if (!this.tagDefinitions.containsKey(key)) {
            return false;
        } else {
            this.tagDefinitions.remove(key);
            this.playerTags.entrySet().removeIf((e) -> norm(e.getValue()).equals(key));
            this.playerSuffixes.entrySet().removeIf((e) -> norm(e.getValue()).equals(key)); // Suffixi de sil
            this.saveAll();
            for(Player online : Bukkit.getOnlinePlayers()) {
                this.updateScoreboardTeam(online); // Direkt güncelle, silme
            }
            return true;
        }
    }

    public String getTagFormat(String tagName) {
        return this.tagDefinitions.get(norm(tagName));
    }

    public Map<String, String> getAllTagDefs() {
        return this.tagDefinitions;
    }

    public String getPlayerTagName(UUID uuid) {
        return this.playerTags.get(uuid);
    }

    public String getPlayerSuffixName(UUID uuid) {
        return this.playerSuffixes.get(uuid);
    }

    /** Oyuncunun ek etiketi (renkli, sonunda boşluk ile) ya da yoksa null. */
    public String getPlayerEk(UUID uuid) {
        return this.playerEkler.get(uuid);
    }

    /** Ek etiketi atar. Format & renk kodlarıyla yazılır; isimle arasında boşluk yoksa otomatik eklenir. */
    public void setPlayerEk(OfflinePlayer target, String rawFormat) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawFormat);
        if (!colored.endsWith(" ")) colored = colored + " ";
        this.playerEkler.put(target.getUniqueId(), colored);
        this.saveAll();
        if (target.isOnline()) {
            this.updateScoreboardTeam(target.getPlayer());
        }
    }

    /** Oyuncunun kurucu etiketi (renkli, sonunda boşluk ile) ya da yoksa null. */
    public String getPlayerKurucu(UUID uuid) {
        return this.playerKuruculer.get(uuid);
    }

    public void setPlayerKurucu(OfflinePlayer target, String rawFormat) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawFormat);
        if (!colored.endsWith(" ")) colored = colored + " ";
        this.playerKuruculer.put(target.getUniqueId(), colored);
        this.saveAll();
        if (target.isOnline()) {
            this.updateScoreboardTeam(target.getPlayer());
        }
    }

    public boolean removePlayerKurucu(OfflinePlayer target) {
        if (this.playerKuruculer.remove(target.getUniqueId()) == null) return false;
        this.saveAll();
        if (target.isOnline()) {
            this.updateScoreboardTeam(target.getPlayer());
        }
        return true;
    }

    /** Oyuncunun lig etiketi (renkli, başında boşluk ile) ya da yoksa null. */
    public String getPlayerLig(UUID uuid) {
        return this.playerLigler.get(uuid);
    }

    /** Lig etiketini atar (sonekin arkasında). Format & renk kodlarıyla; isimle arasında boşluk yoksa otomatik eklenir. */
    public void setPlayerLig(OfflinePlayer target, String rawFormat) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawFormat);
        if (!colored.startsWith(" ")) colored = " " + colored;
        this.playerLigler.put(target.getUniqueId(), colored);
        this.saveAll();
        if (target.isOnline()) {
            this.updateScoreboardTeam(target.getPlayer());
        }
    }

    public boolean removePlayerLig(OfflinePlayer target) {
        if (this.playerLigler.remove(target.getUniqueId()) == null) return false;
        this.saveAll();
        if (target.isOnline()) {
            this.updateScoreboardTeam(target.getPlayer());
        }
        return true;
    }

    public boolean removePlayerEk(OfflinePlayer target) {
        if (this.playerEkler.remove(target.getUniqueId()) == null) return false;
        this.saveAll();
        if (target.isOnline()) {
            this.updateScoreboardTeam(target.getPlayer());
        }
        return true;
    }

    public boolean setPlayerTag(OfflinePlayer target, String tagName) {
        if (!this.tagExists(tagName)) {
            return false;
        } else {
            if (this.playerTags.containsKey(target.getUniqueId())) {
                String oldTag = this.playerTags.get(target.getUniqueId());
                String safeOldTag = norm(oldTag).replaceAll("[^a-z0-9]", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent remove " + safeOldTag);
            }

            this.playerTags.put(target.getUniqueId(), tagName);
            this.saveAll();
            if (target.isOnline()) {
                this.updateScoreboardTeam(target.getPlayer());
            }
            
            String safeGroupName = norm(tagName).replaceAll("[^a-z0-9]", "");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp creategroup " + safeGroupName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent add " + safeGroupName);

            return true;
        }
    }

    // YENİ: Suffix Atama Metodu
    public boolean setPlayerSuffix(OfflinePlayer target, String suffixName) {
        if (!this.tagExists(suffixName)) {
            return false;
        } else {
            this.playerSuffixes.put(target.getUniqueId(), suffixName);
            this.saveAll();
            if (target.isOnline()) {
                this.updateScoreboardTeam(target.getPlayer());
            }
            return true;
        }
    }

    public boolean removePlayerTag(OfflinePlayer target) {
        if (!this.playerTags.containsKey(target.getUniqueId())) {
            return false;
        } else {
            String oldTag = this.playerTags.get(target.getUniqueId());
            this.playerTags.remove(target.getUniqueId());
            this.saveAll();
            if (target.isOnline()) {
                this.updateScoreboardTeam(target.getPlayer());
            }

            if (oldTag != null) {
                String safeGroupName = norm(oldTag).replaceAll("[^a-z0-9]", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent remove " + safeGroupName);
            }
            return true;
        }
    }

    // YENİ: Suffix Silme Metodu
    public boolean removePlayerSuffix(OfflinePlayer target) {
        if (!this.playerSuffixes.containsKey(target.getUniqueId())) {
            return false;
        } else {
            this.playerSuffixes.remove(target.getUniqueId());
            this.saveAll();
            if (target.isOnline()) {
                this.updateScoreboardTeam(target.getPlayer());
            }
            return true;
        }
    }

    // GÜNCELLENDİ: Prefix ve Suffix'i birleştirip takıma atayan dev motor
    public void updateScoreboardTeam(Player player) {
        String prefixName = this.playerTags.get(player.getUniqueId());
        String suffixName = this.playerSuffixes.get(player.getUniqueId());

        String prefixFormat = prefixName != null && this.tagExists(prefixName) ? this.getTagFormat(prefixName) : "";
        String suffixFormat = suffixName != null && this.tagExists(suffixName) ? this.getTagFormat(suffixName) : "";
        // Ek etiket önekin önüne gelir: [Ek] [Önek] İsim [Sonek]
        String ekFormat = this.playerEkler.getOrDefault(player.getUniqueId(), "");
        prefixFormat = this.playerKuruculer.getOrDefault(player.getUniqueId(), "") + ekFormat + prefixFormat;
        // Lig etiketi sonekin arkasına gelir: ... İsim [Sonek] [Lig]
        suffixFormat = suffixFormat + this.playerLigler.getOrDefault(player.getUniqueId(), "");

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "tagplg_" + player.getUniqueId().toString().substring(0, 12);
        
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            team.unregister();
        }

        if (prefixFormat.isEmpty() && suffixFormat.isEmpty()) {
            player.playerListName(Component.text(player.getName()));
            player.setDisplayName(player.getName());
            return;
        }

        team = board.registerNewTeam(teamName);
        team.setPrefix(this.trimTo(prefixFormat, 64));
        team.setSuffix(this.trimTo(suffixFormat, 64)); // Suffix eklendi!
        team.setColor(ChatColor.WHITE);
        team.addEntry(player.getName());

        Component finalListName = Component.empty();
        if (!prefixFormat.isEmpty()) {
            finalListName = finalListName.append(LegacyComponentSerializer.legacySection().deserialize(prefixFormat));
        }
        finalListName = finalListName.append(Component.text(player.getName(), NamedTextColor.WHITE));
        if (!suffixFormat.isEmpty()) {
            finalListName = finalListName.append(LegacyComponentSerializer.legacySection().deserialize(suffixFormat));
        }
        
        player.playerListName(finalListName);
        player.setDisplayName(prefixFormat + ChatColor.WHITE + player.getName() + ChatColor.RESET + suffixFormat);
    }

    public void clearScoreboardTag(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "tagplg_" + player.getUniqueId().toString().substring(0, 12);
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            team.unregister();
        }
        player.playerListName(Component.text(player.getName()));
        player.setDisplayName(player.getName());
    }

    private String trimTo(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    public void applyStoredTagOnJoin(Player player) {
        this.updateScoreboardTeam(player);
    }

    public int cleanupInvalidTags() {
        int cleaned = 0;
        for(Player online : Bukkit.getOnlinePlayers()) {
            boolean changed = false;
            String prefix = this.playerTags.get(online.getUniqueId());
            String suffix = this.playerSuffixes.get(online.getUniqueId());

            if (prefix != null && !this.tagExists(prefix)) {
                this.playerTags.remove(online.getUniqueId());
                changed = true;
            }
            if (suffix != null && !this.tagExists(suffix)) {
                this.playerSuffixes.remove(online.getUniqueId());
                changed = true;
            }

            if (changed) {
                this.updateScoreboardTeam(online);
                ++cleaned;
            }
        }
        if (cleaned > 0) {
            this.saveAll();
        }
        return cleaned;
    }
}