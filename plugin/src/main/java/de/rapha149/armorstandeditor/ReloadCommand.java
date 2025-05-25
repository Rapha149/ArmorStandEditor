package de.rapha149.armorstandeditor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.io.IOException;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public class ReloadCommand implements CommandExecutor {

    public ReloadCommand(PluginCommand command) {
        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("armorstandeditor.reload")) {
            try {
                Messages.loadMessages();
                Config.load();
                sender.spigot().sendMessage(getMessage("reload").spigot());
            } catch (IOException e) {
                e.printStackTrace();
                sender.spigot().sendMessage(getMessage("error").spigot());
            }
        } else
            sender.spigot().sendMessage(getMessage("no_permission").spigot());
        return false;
    }
}
