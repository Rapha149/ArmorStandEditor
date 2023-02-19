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
                sender.sendMessage(getMessage("reload"));
            } catch (IOException e) {
                e.printStackTrace();
                sender.sendMessage(getMessage("error"));
            }
        } else
            sender.sendMessage(getMessage("no_permission"));
        return false;
    }
}
