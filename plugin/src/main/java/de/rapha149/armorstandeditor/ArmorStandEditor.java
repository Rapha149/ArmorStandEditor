package de.rapha149.armorstandeditor;

import de.rapha149.armorstandeditor.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static de.rapha149.armorstandeditor.Messages.getMessage;

public final class ArmorStandEditor extends JavaPlugin {

    private static ArmorStandEditor instance;

    public VersionWrapper wrapper;
    public boolean isPaper;

    private Events events;

    @Override
    public void onEnable() {
        instance = this;

        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
        try {
            wrapper = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to load support for server version \"" + nmsVersion + "\"");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ArmorStandEditor does not support the server version \"" + nmsVersion + "\"");
        }

        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            isPaper = true;
        } catch (ClassNotFoundException ignored) {
        }

        Messages.loadMessages();
        try {
            Config.load();
        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Config.get().checkForUpdates) {
            String version = Updates.getAvailableVersion();
            if (version != null) {
                if (version.isEmpty())
                    getLogger().info(getMessage("plugin.up_to_date"));
                else {
                    for (String line : getMessage("plugin.outdated").split("\n"))
                        getLogger().warning(line.replace("%version%", version).replace("%url%", Updates.SPIGOT_URL));
                }
            }
        }

        new ReloadCommand(getCommand("asereload"));
        getServer().getPluginManager().registerEvents(events = new Events(), this);
        getLogger().info(getMessage("plugin.enable"));
    }

    @Override
    public void onDisable() {
        events.onDisable();
        getLogger().info(getMessage("plugin.disable"));
    }

    public static ArmorStandEditor getInstance() {
        return instance;
    }
}
