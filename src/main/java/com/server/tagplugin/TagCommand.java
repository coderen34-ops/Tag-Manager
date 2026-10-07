package com.server.tagplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player; // YENİ: Player objesi tanıtıldı

public class TagCommand implements CommandExecutor, TabCompleter {
    private final TagPlugin plugin;
    private final TagManager tagManager;

    public TagCommand(TagPlugin plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            this.sendUsage(sender);
            return true;
        } else {
            switch (args[0].toLowerCase()) {
                case "set": return this.handleSet(sender, args, false);
                case "setsuffix": return this.handleSet(sender, args, true);
                case "remove": return this.handleRemove(sender, args, false);
                case "removesuffix": return this.handleRemove(sender, args, true);
                case "list": return this.handleList(sender);
                case "create": return this.handleCreate(sender, args);
                case "deletetag": return this.handleDeleteTag(sender, args);
                case "info": return this.handleInfo(sender, args);
                case "cleanup": return this.handleCleanup(sender);
                case "setek": return this.handleSetEk(sender, args);
                case "removeek": return this.handleRemoveEk(sender, args);
                default: this.sendUsage(sender); return true;
            }
        }
    }

    private boolean handleSet(CommandSender sender, String[] args, boolean isSuffix) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag " + (isSuffix ? "setsuffix" : "set") + " <oyuncu> <tagadı>");
            return true;
        } else {
            String targetName = args[1];
            String tagName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Bu oyuncu daha önce sunucuya girmemiş: " + targetName);
                return true;
            } else if (!this.tagManager.tagExists(tagName)) {
                sender.sendMessage(ChatColor.RED + "Böyle bir tag tanımı yok: " + tagName + ". Önce /tag create ile oluştur.");
                return true;
            } else {
                if (isSuffix) {
                    this.tagManager.setPlayerSuffix(target, tagName);
                    sender.sendMessage(ChatColor.GREEN + targetName + " oyuncusuna '" + tagName + "' suffixi atandı.");
                } else {
                    this.tagManager.setPlayerTag(target, tagName);
                    sender.sendMessage(ChatColor.GREEN + targetName + " oyuncusuna '" + tagName + "' tagi atandı.");
                }
                return true;
            }
        }
    }

    private boolean handleRemove(CommandSender sender, String[] args, boolean isSuffix) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag " + (isSuffix ? "removesuffix" : "remove") + " <oyuncu>");
            return true;
        } else {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            boolean removed = isSuffix ? this.tagManager.removePlayerSuffix(target) : this.tagManager.removePlayerTag(target);
            
            if (removed) {
                sender.sendMessage(ChatColor.GREEN + targetName + " oyuncusunun " + (isSuffix ? "suffixi" : "tagi") + " kaldırıldı.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + targetName + " oyuncusunda zaten " + (isSuffix ? "suffix" : "tag") + " yoktu.");
            }
            return true;
        }
    }

    // /tag setek <oyuncu> <format...>  — önekin önüne serbest ek etiket (diğer eklentiler için, örn. Aile)
    private boolean handleSetEk(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag setek <oyuncu> <format>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Bu oyuncu daha önce sunucuya girmemiş: " + args[1]);
            return true;
        }
        String format = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        this.tagManager.setPlayerEk(target, format);
        sender.sendMessage(ChatColor.GREEN + args[1] + " oyuncusuna ek etiket atandı: " + ChatColor.translateAlternateColorCodes('&', format));
        return true;
    }

    private boolean handleRemoveEk(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag removeek <oyuncu>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (this.tagManager.removePlayerEk(target)) {
            sender.sendMessage(ChatColor.GREEN + args[1] + " oyuncusunun ek etiketi kaldırıldı.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + args[1] + " oyuncusunda zaten ek etiket yoktu.");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Map<String, String> defs = this.tagManager.getAllTagDefs();
        if (defs.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Hiç tag tanımı yok.");
            return true;
        } else {
            sender.sendMessage(ChatColor.GOLD + "--- Tanımlı Tagler ---");
            for(Map.Entry<String, String> entry : defs.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "- " + entry.getKey() + ": " + ChatColor.translateAlternateColorCodes('&', entry.getValue()) + ChatColor.RESET + " (önizleme)");
            }
            return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag create <tagadı> <format>");
            sender.sendMessage(ChatColor.GRAY + "Örnek: /tag create Market &a[Market] ");
            return true;
        } else {
            String tagName = args[1];
            String rawFormat = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (this.tagManager.tagExists(tagName)) {
                sender.sendMessage(ChatColor.RED + "Bu isimde tag zaten var: " + tagName + ". Önce /tag deletetag ile sil.");
                return true;
            } else {
                this.tagManager.createTag(tagName, rawFormat);
                sender.sendMessage(ChatColor.GREEN + "Tag oluşturuldu: " + tagName + " -> " + ChatColor.translateAlternateColorCodes('&', rawFormat) + ChatColor.RESET);
                return true;
            }
        }
    }

    private boolean handleDeleteTag(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag deletetag <tagadı>");
            return true;
        } else {
            String tagName = args[1];
            boolean deleted = this.tagManager.deleteTagDefinition(tagName);
            if (deleted) {
                sender.sendMessage(ChatColor.GREEN + "Tag tanımı silindi: " + tagName + " (bu tagi kullanan tüm oyunculardan da kaldırıldı)");
            } else {
                sender.sendMessage(ChatColor.RED + "Böyle bir tag tanımı yok: " + tagName);
            }
            return true;
        }
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /tag info <oyuncu>");
            return true;
        } else {
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            String prefix = this.tagManager.getPlayerTagName(target.getUniqueId());
            String suffix = this.tagManager.getPlayerSuffixName(target.getUniqueId());
            
            sender.sendMessage(ChatColor.GOLD + "--- " + targetName + " Tag Bilgisi ---");
            sender.sendMessage(ChatColor.YELLOW + "Prefix (Tag): " + (prefix == null ? "Yok" : prefix));
            sender.sendMessage(ChatColor.YELLOW + "Suffix (Sonek): " + (suffix == null ? "Yok" : suffix));
            String ek = this.tagManager.getPlayerEk(target.getUniqueId());
            sender.sendMessage(ChatColor.YELLOW + "Ek Etiket: " + (ek == null ? "Yok" : ek + ChatColor.RESET));
            return true;
        }
    }

    private boolean handleCleanup(CommandSender sender) {
        int cleaned = this.tagManager.cleanupInvalidTags();
        if (cleaned == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Temizlenecek geçersiz tag/suffix bulunamadı.");
        } else {
            // HATA 1 BURADAYDI: ChatColor ve int direkt toplanamıyordu, araya "" + eklendi.
            sender.sendMessage(ChatColor.GREEN + "" + cleaned + " oyuncunun geçersiz (silinmiş) tagleri temizlendi.");
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- TagPlugin Komutları ---");
        sender.sendMessage(ChatColor.YELLOW + "/tag set <oyuncu> <tagadı>" + ChatColor.GRAY + " - Oyuncuya prefix (ana tag) ata");
        sender.sendMessage(ChatColor.YELLOW + "/tag setsuffix <oyuncu> <tagadı>" + ChatColor.GRAY + " - Oyuncunun isminin sonuna suffix ata");
        sender.sendMessage(ChatColor.YELLOW + "/tag remove <oyuncu>" + ChatColor.GRAY + " - Oyuncunun prefixini kaldır");
        sender.sendMessage(ChatColor.YELLOW + "/tag removesuffix <oyuncu>" + ChatColor.GRAY + " - Oyuncunun suffixini kaldır");
        sender.sendMessage(ChatColor.YELLOW + "/tag create <tagadı> <format>" + ChatColor.GRAY + " - Yeni tag tanımı oluştur");
        sender.sendMessage(ChatColor.YELLOW + "/tag info <oyuncu>" + ChatColor.GRAY + " - Oyuncunun taglerini göster");
        sender.sendMessage(ChatColor.YELLOW + "/tag setek <oyuncu> <format>" + ChatColor.GRAY + " - Önekin önüne ek etiket (örn. aile)");
        sender.sendMessage(ChatColor.YELLOW + "/tag removeek <oyuncu>" + ChatColor.GRAY + " - Ek etiketi kaldır");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(Arrays.asList("set", "setsuffix", "remove", "removesuffix", "list", "create", "deletetag", "info", "cleanup", "setek", "removeek"));
            return this.filter(options, args[0]);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "set":
                case "setsuffix":
                case "remove":
                case "removesuffix":
                case "info":
                case "setek":
                case "removeek":
                    // HATA 2 BURADAYDI: Player import edilmediği için Player::getName çalışmıyordu. Yukarıda import eklendi.
                    return this.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
                case "deletetag":
                    return this.filter(new ArrayList<>(this.tagManager.getAllTagDefs().keySet()), args[1]);
                default:
                    return options;
            }
        } else {
            return (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("setsuffix"))) ? this.filter(new ArrayList<>(this.tagManager.getAllTagDefs().keySet()), args[2]) : options;
        }
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter((o) -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}