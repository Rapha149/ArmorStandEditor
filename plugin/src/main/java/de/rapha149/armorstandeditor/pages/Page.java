package de.rapha149.armorstandeditor.pages;

import de.rapha149.armorstandeditor.ArmorStandEditor;
import de.rapha149.armorstandeditor.Util.ArmorStandStatus;
import de.rapha149.armorstandeditor.version.VersionWrapper;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public abstract class Page {

    final VersionWrapper wrapper = ArmorStandEditor.getInstance().wrapper;

    public abstract GuiResult getGui(Player player, ArmorStand armorStand, boolean adminBypass);

    public record GuiResult(Gui gui, ArmorStandStatus status, Runnable closeAction) {

        public GuiResult(Gui gui, ArmorStandStatus status) {
            this(gui, status, () -> {});
        }
    }
}