# ArmorStandEditor
A Minecraft plugin to edit armor stands simply by interacting with them.

![inventory page 1](https://user-images.githubusercontent.com/49787110/222918545-5b2e58b4-7420-40dc-80d7-9c0c1cc8f54a.png)
![inventory page 2](https://user-images.githubusercontent.com/49787110/220210919-40dfba04-afd5-4002-89d1-726121ab6058.png)

## Features

- Survival friendly (no commands needed)
- Replace the armor stand's equipment in an inventory
- Move the armor stand's body parts and position with your head movement
- Make your armor stand private (other players can't modify it with this plugin)
- Lock the armor stand's slots so that the equipment can't be removed directly
- Change various settings of the armor stand
- Receive the armor stand as an item - equipment, settings is saved!
- Copy the armor stand's settings by combining the item with another armor stand in the crafting table
- Rename the armor stand (color codes and hex codes supported!)
- Use the armor stand as a passenger/vehicle
- You can deactivate or set permissions for any feature!
- All messages are customizable!

## URLs

- [Spigot](https://www.spigotmc.org/resources/armorstandeditor.108120/)
- [bStats](https://bstats.org/plugin/bukkit/Armor%20Stand%20Editor/17771)

## How does it work?

You can access the ArmorStandEditor inventory simply by sneaking and right clicking the armor stand you want to edit!

## Commands

The only command of the plugin is `/asereload`.  
You can use to reload the config and messages of the plugin without having to restart/reload your server.

## Permissions

The only permission that cannot be changed is `armorstandeditor.reload`. With this permission you can use the command `/asereload`.  
You can set other permissions in the config:
- Permission to be able to use the plugin (Default: none)
- Permission to be able to access any private armor stand (Default: `armorstandeditor.ignoreprivate`)
- Each feature can have its own permission (Default: none)

## Default config
```yaml
# ArmorStandEditor version 1.5
# Github: https://github.com/Rapha149/ArmorStandEditor
# Spigot: https://www.spigotmc.org/resources/armorstandeditor.108120/

# Whether to check for updates on enabling.
checkForUpdates: true

# The advancement to grant the player when he first accesses an armor stand. Set to "null" to disable.
advancement: null

permissions:
  # The permission that is needed to interact with armor stands and open the menu.
  # Set to "null" to disable the permission and allow everybody to use the plugin.
  general: null

  # With this permission, players can open private armor stands even if they wouldn't have access to them.
  # You can set this permission to "null" to disable it, but it's not recommended as private armor stands would be available to everyone.
  ignorePrivate: armorstandeditor.ignoreprivate

# The item that is displayed when a feature is disabled.
# Set to "null" to show the actual item of the feature even though it's disabled.
deactivatedItem: minecraft:gray_dye

# A list of features. You can enable/disable each feature or set a permission to use a certain feature.
# If you want a feature to be enabled and everybody to be able to use it, set the permission to "null".
features:
  # Replacing the armor stand's equipment (armor and hand items) in the ASE inventory.
  replaceEquipment:
    # Whether or not to replace the armor and hand items of the armor stand with the disabled item when this feature is disabled.
    useDeactivatedItem: false
    enabled: true
    permission: null

  # Moving the armor stand's body parts.
  moveBodyParts:
    enabled: true
    permission: null

  # Moving the armor stand's position.
  movePosition:
    enabled: true
    permission: null

  # Changing the armor stand's rotation.
  rotate:
    enabled: true
    permission: null

  # Making your armor stand private so that only you can open its ASE inventory.
  privateArmorstand:
    enabled: true
    permission: null

  # Locking the equipment slots of your armor stand so that players can't take items directly.
  disabledSlots:
    enabled: true
    permission: null

  # Making your armor stand invisible.
  invisibility:
    enabled: true
    permission: null

  # Making your armor stand invulnerable.
  invulnerability:
    enabled: true
    permission: null

  # Making your armor stand's arms visible.
  showArms:
    enabled: true
    permission: null

  # Making your armor stand not affected by gravity.
  gravity:
    enabled: true
    permission: null

  # Making your armor stand's base plate invisible.
  basePlate:
    enabled: true
    permission: null

  # Making your armor stand small.
  small:
    enabled: true
    permission: null

  # Making your armor stand glow.
  glowing:
    enabled: true
    permission: null

  movable:
    enabled: true
    permission: null

  # Making your armor stand seeming to be on fire.
  fire:
    enabled: true
    permission: null

  # Set your armor stand as a passenger on a vehicle.
  passenger:
    # Whether or not players can be selected as vehicles.
    players: false
    enabled: true
    permission: null

  # Set another entity as a passenger on your armor stand.
  vehicle:
    # Whether or not players can be selected as passengers.
    players: false
    enabled: true
    permission: null

  # Renaming your armor stand.
  rename:
    enabled: true
    permission: null

  # Receiving your armor stand as an item.
  giveItem:
    enabled: true
    permission: null

  # Copying your armor stand settings by combining a modified armor stand item with a normal armor stand item in the crafting table.
  # This behavior is similar to the copying behavior of written books.
  copy:
    enabled: true
    permission: null
```

## Additional information

This plugin collects anonymous server stats with [bStats](https://bstats.org), an open-source statistics service for Minecraft software. If you don't want this, you can deactivate it in `plugins/bStats/config.yml`.

## Credits

- Credits go to [laGameTV](https://lagametv.de/) for the idea of the plugin.
- This plugin is **inspired** by [Armor Stand Tools](https://www.spigotmc.org/resources/armor-stand-tools.2237/), but many things are different and the code is entirely my own.
