# ArmorStandEditor
A Minecraft plugin to edit armor stands simply by interacting with them.

![armor](https://user-images.githubusercontent.com/49787110/224565054-4ff3f2d8-c782-4e4e-8101-d863e2f641a2.png)
![settings](https://user-images.githubusercontent.com/49787110/224565057-3a5cba3a-95ea-4aac-8069-4b45e3c78fa0.png)  
![advancedcontrols_position](https://user-images.githubusercontent.com/49787110/224565063-b0328b6a-2a52-437e-ac9c-b5932a468c3e.png)
![advancedcontrols_rotation](https://user-images.githubusercontent.com/49787110/224565068-404c35bd-b133-4e51-974b-25a1efaf58a4.png)  
![advancedcontrols_pose_overview](https://user-images.githubusercontent.com/49787110/224565077-5c1dabcf-2fb3-40ad-b4d2-675cfe1fa474.png)
![advancedcontrols_pose_head](https://user-images.githubusercontent.com/49787110/224565076-55dee826-2c77-4f0b-a0d2-1d05c729a639.png)
![advancedcontrols_pose_presets](https://user-images.githubusercontent.com/49787110/224565080-58339510-45e6-4324-9e35-27e07aaedf99.png)


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
<details>
<summary>Expand ↓</summary>

```yaml
# ArmorStandEditor version 1.5.11
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

  # Using advanced controls to change position, rotation and pose.
  # They can be individually enabled/disabled via the options above.
  advancedControls:
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

# Here you can define presets which are shown on the Pose page of the Advanced Controls.
# Maximum of 15 presets.
presets:

- bodyParts:

    head:
      x: 0.0
      y: 0.0
      z: 0.0

    right_arm:
      x: -15.0
      y: 0.0
      z: 10.0

    left_leg:
      x: -1.0
      y: 0.0
      z: -1.0

    left_arm:
      x: -10.0
      y: 0.0
      z: -10.0

    right_leg:
      x: 1.0
      y: 0.0
      z: 1.0

    body:
      x: 0.0
      y: 0.0
      z: 0.0
  name: Default

- bodyParts:

    head:
      x: 0.0
      y: 0.0
      z: 0.0

    right_arm:
      x: 0.0
      y: 0.0
      z: 0.0

    left_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    left_arm:
      x: 0.0
      y: 0.0
      z: 0.0

    right_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: 0.0
      z: 0.0
  name: 0°

- bodyParts:

    head:
      x: 0.8944604
      y: 9.055486
      z: 0.0

    right_arm:
      x: -90.0
      y: 0.0
      z: 0.0

    left_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    left_arm:
      x: 0.0
      y: 0.0
      z: 0.0

    right_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: -2.3101969
      z: 0.0
  name: Item

- bodyParts:

    head:
      x: 2.739634
      y: -5.375096
      z: 0.0

    right_arm:
      x: -15.0
      y: -45.0
      z: 0.0

    left_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    left_arm:
      x: 0.0
      y: 0.0
      z: 0.0

    right_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: -0.29615206
      z: 0.0
  name: Block

- bodyParts:

    head:
      x: 5.883482
      y: -19.316841
      z: 0.0

    right_arm:
      x: 20.0
      y: 0.0
      z: 10.0

    left_leg:
      x: 20.0
      y: 0.0
      z: 0.0

    left_arm:
      x: -20.0
      y: 0.0
      z: -10.0

    right_leg:
      x: -20.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: -1.3423482
      z: 0.0
  name: Walking

- bodyParts:

    head:
      x: 5.747715
      y: 1.8663449
      z: 0.0

    right_arm:
      x: -40.0
      y: 0.0
      z: 10.0

    left_leg:
      x: -40.0
      y: 0.0
      z: 0.0

    left_arm:
      x: 40.0
      y: 0.0
      z: -10.0

    right_leg:
      x: 40.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: 1.5624547
      z: 0.0
  name: Running

- bodyParts:

    head:
      x: 1.7437577
      y: 19.371622
      z: 0.0

    right_arm:
      x: -90.0
      y: 18.0
      z: 0.0

    left_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    left_arm:
      x: 0.0
      y: 0.0
      z: -10.0

    right_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: 2.9660754
      z: 0.0
  name: Pointing

- bodyParts:

    head:
      x: 3.0737185
      y: 6.626904
      z: 0.0

    right_arm:
      x: -124.0
      y: -51.0
      z: -35.0

    left_leg:
      x: 0.0
      y: 4.0
      z: 2.0

    left_arm:
      x: 29.0
      y: 0.0
      z: 25.0

    right_leg:
      x: 0.0
      y: -4.0
      z: -2.0

    body:
      x: 5.0
      y: -2.25304
      z: 0.0
  name: Salute

- bodyParts:

    head:
      x: 1.2775335
      y: -7.32031
      z: 0.0

    right_arm:
      x: -20.0
      y: -20.0
      z: 0.0

    left_leg:
      x: 20.0
      y: 0.0
      z: 0.0

    left_arm:
      x: -50.0
      y: 50.0
      z: 0.0

    right_leg:
      x: -20.0
      y: 0.0
      z: 0.0

    body:
      x: 0.0
      y: -4.3447514
      z: 0.0
  name: Blocking

- bodyParts:

    head:
      x: 3.2998314
      y: -6.680886
      z: 0.0

    right_arm:
      x: -80.0
      y: 20.0
      z: 0.0

    left_leg:
      x: -90.0
      y: -10.0
      z: 0.0

    left_arm:
      x: -80.0
      y: -20.0
      z: 0.0

    right_leg:
      x: -90.0
      y: 10.0
      z: 0.0

    body:
      x: 0.0
      y: 3.2538185
      z: 0.0
  name: Sitting

- bodyParts:

    head:
      x: -83.36189
      y: 3.5046368
      z: 0.0

    right_arm:
      x: -90.0
      y: 10.0
      z: 0.0

    left_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    left_arm:
      x: -90.0
      y: -10.0
      z: 0.0

    right_leg:
      x: 0.0
      y: 0.0
      z: 0.0

    body:
      x: -90.0
      y: -3.1862168
      z: 0.0
  name: Laying

- bodyParts:

    head:
      x: 1.1013848
      y: 38.299202
      z: 0.0

    right_arm:
      x: -22.0
      y: 31.0
      z: 10.0

    left_leg:
      x: -6.0
      y: 0.0
      z: 0.0

    left_arm:
      x: 145.0
      y: 22.0
      z: -49.0

    right_leg:
      x: 6.0
      y: -20.0
      z: 0.0

    body:
      x: 0.0
      y: 12.96918
      z: 0.0
  name: Confused

- bodyParts:

    head:
      x: 47.090084
      y: 3.8555756
      z: 0.0

    right_arm:
      x: 18.0
      y: -14.0
      z: 0.0

    left_leg:
      x: -4.0
      y: -6.0
      z: -2.0

    left_arm:
      x: -72.0
      y: 24.0
      z: 47.0

    right_leg:
      x: 25.0
      y: -2.0
      z: 0.0

    body:
      x: 10.0
      y: -1.2397861
      z: 0.0
  name: Facepalm

- bodyParts:

    head:
      x: 7.1271815
      y: 3.9160357
      z: 0.0

    right_arm:
      x: 30.0
      y: 22.0
      z: -20.0

    left_leg:
      x: 0.0
      y: 0.0
      z: -5.0

    left_arm:
      x: 30.0
      y: -20.0
      z: 21.0

    right_leg:
      x: 0.0
      y: 0.0
      z: 5.0

    body:
      x: 4.0
      y: 3.4252434
      z: 0.0
  name: Formal

- bodyParts:

    head:
      x: 67.50782
      y: 2.741272
      z: 0.0

    right_arm:
      x: -5.0
      y: 0.0
      z: 5.0

    left_leg:
      x: -5.0
      y: 16.0
      z: -5.0

    left_arm:
      x: -5.0
      y: 0.0
      z: -5.0

    right_leg:
      x: -5.0
      y: -10.0
      z: 5.0

    body:
      x: 10.0
      y: 3.969596
      z: 0.0
  name: Sad
```
</details>

## Additional information

This plugin collects anonymous server stats with [bStats](https://bstats.org), an open-source statistics service for Minecraft software. If you don't want this, you can deactivate it in `plugins/bStats/config.yml`.

## Credits

- Credits go to [laGameTV](https://lagametv.de/) for the idea of the plugin.
- This plugin is **inspired** by [Armor Stand Tools](https://www.spigotmc.org/resources/armor-stand-tools.2237/), but many things are different and the code is entirely my own.
- Some of the pose presets are from [Vanilla Tweaks](https://vanillatweaks.net/).
