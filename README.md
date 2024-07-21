# BlueMap Filtered Entities

[![GitHub Total Downloads](https://img.shields.io/github/downloads/TechnicJelle/BlueMapFilteredEntities/total?label=Downloads&color=success "Click here to download the plugin")](https://github.com/TechnicJelle/BlueMapFilteredEntities/releases/latest)
[![Servers using this plugin](https://img.shields.io/bstats/servers/21976?label=Servers)](https://bstats.org/plugin/bukkit/BlueMap%20Filtered%20Entities/21976)

A Minecraft Paper BlueMap addon that allows you to track entities on your map, with lots of filtering options.

![A screenshot of a BlueMap, with a bunch of entity heads visible on it](.github/readme_assets/demo.png)

Compatible with Paper 1.13+, just like BlueMap itself.

To reload this plugin, just reload BlueMap itself with `/bluemap reload`.

## [Click here to download!](../../releases/latest)

## Configuration
The configuration for this plugin uses [HOCON](https://github.com/lightbend/config/blob/main/HOCON.md) files.

In the `plugins/BlueMapFilteredEntities` folder you should make a `.conf` file for each BlueMap map you want to control,
with the map-id as the name.\
When you install this plugin for the first time, it will generate a template config for each registered BlueMap map.
You should delete the ones for the maps you don't want to show entities on.

The general format of this config is very similar to BlueMap's own built-in marker configuration,
but instead of configuring specific markers, you have to configure filters.

The general idea is that you have one or more "filter-sets", which are kind of like categories.\
(These translate basically directly to BlueMap's own marker-sets.)\
You then define all your filter-sets inside a big list at the root of your configuration file(s).

Inside each filter-set, you define one or more filters,
which are responsible for adding the specific markers for each entity.

### Filter-Sets List
This is the base of the file.
Every .conf file *must* have a single `filter-sets` section at the root.

Inside this section, you can define your actual filter-sets.

```hocon
filter-sets: {
  # Filter sets go in here
}
```

### Filter-Sets
A filter-set looks like this:
```hocon
my-filter-set: {
  label: "My Filter Set"
  toggleable: true  # Optional, default: true
  default-hidden: true  # Optional, default: true
  filters: [
    # Filters go in here
  ]
}
```
You might recognise this format from BlueMap's own marker configuration.

### Filters
A single filter contains one or multiple properties, between a pair of curly brackes: ` { } `

The different filters inside a filter-set are combined with OR logic,
meaning that if any of the filters match, the entity will be matched.\
_Inside_ each filter, the different conditions are combined with AND logic,
meaning that all conditions must be met for the filter to match.

#### Entity data you can filter on
- `type: <string>`: The entity type.
  - Any of these: https://jd.papermc.io/paper/1.20.6/org/bukkit/entity/EntityType.html
  - This string is case-insensitive.
- `name: <string>`: The name of the entity.
  - This is a regex pattern, so it checks if the name of the entity _contains_ this string.
    - If you want it to be an exact match, use `^` and `$` to match the start and end of the string.
    - **Example:** `name: "Villager"` will match any entity with "Villager" in its name,
      while `name: "^Villager$"` will only match entities named exactly "Villager".
  - This string is case-sensitive, but you can use `(?i)` at the start to make it case-insensitive.
    - **Example:** `name: "(?i)villager"` will match entities named "villager", "Villager", "VILLAGER", etc.
  - For normal mobs, this is basically the same as the `type` property.
  - For nametagged entities, this is their new name.
  - For dropped items, this is the actual item name.
    This does not change even if you have renamed it in an anvil.
- `custom-name: <string>`: The custom name of the entity.
  - This is also a case-sensitive regex pattern, just like the `name` property.
  - Most entities don't have one of these.
  - For nametagged entities, this is their new name.
  - For dropped items, this is the item name.
    This does change if you have renamed it in an anvil.
- `uuid: <string>`: The UUID of the entity.
- `spawn-reason: <string>`: The reason the entity was spawned.
  - Any of these: https://jd.papermc.io/paper/1.20.6/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html
  - This string is case-insensitive.
- `instance-of: <string>`: The class name of the entity.
  - Any of these: https://jd.papermc.io/paper/1.20.6/org/bukkit/entity/package-summary.html
  - This string is case-sensitive.
  - This is useful for filtering entities on "categories", like all animals, all monsters, all flying, etc.
- `min-x: <number>`: The minimum X coordinate of the entity.
  - Use these to filter entities in a specific area.
  - You don't have to specify all of them, you can specify only the ones you need.
- `max-x: <number>`: The maximum X coordinate of the entity.
- `min-z: <number>`: The minimum Z coordinate of the entity.
- `max-z: <number>`: The maximum Z coordinate of the entity.
- `min-y: <number>`: The minimum Y coordinate of the entity.
- `max-y: <number>`: The maximum Y coordinate of the entity.
- `scoreboard-tags: <string[]>`: The scoreboard tags of the entity.
  - This is a list of case-sensitive strings.
  - Read more about these here: https://minecraft.wiki/w/Commands/tag
  - This is useful for filtering entities that have been tagged by a command, datapack or other plugin.
  - This matches if the entity has _all_ the tags in the list.
- `exclude: <filter[]>`: A list of filters that should _not_ match.
  - This is useful for excluding certain entities from a filter.
  - In here, you can put another list of filters
    in the exact same way as the ones you put in the main filter.

All of these are optional, but you must have at least _something_ in the filter.

#### Appearance (Extra filter options)
Per filter, there are some other options that don't affect the filtering,
but do affect the way matched entities are displayed on the map:
- `icon: <string>`: The icon to use for all the markers on the map that match this filter.
  - If you don't specify this, the default BlueMap POI icon will be used. 📍
  - Icons should be placed in the `plugins/BlueMapFilteredEntities/icons` folder.
    - You can use subfolders in this folder to organise your icons.
      Just make sure to include the subfolder name in the icon path.
- `anchor: <vector2>`: The anchor point of the icon
  - Same as the anchor property in BlueMap's own [POI marker configuration](https://bluemap.bluecolored.de/wiki/customization/Markers.html#poi-markers).
  - **Example:** `anchor: {x:24, y:24}`
- `max-distance: <number>`: The maximum distance from the camera to show the markers.
  - If the camera is further away from the entity marker than this, the marker will not be shown.
  - This is useful for performance reasons, and to prevent the map from getting cluttered.
- `popup-info-template: <string>`: The template for the popup info of the markers.
  - When you click an entity marker on the map, a popup will appear with some information about the entity.
  - This is a string that contains a template for the content of this popup.
  - If you don't specify this, a default template will be used that includes all available information.
  - Available placeholders (the `{}` around these are required):
    - `{type}`: The entity type.
    - `{name}`: The name of the entity.
    - `{uuid}`: The UUID of the entity.
    - `{spawn-reason}`: The reason the entity was spawned.
    - `{custom-name}`: The custom name of the entity.
    - `{x}`: The X coordinate of the entity.
    - `{y}`: The Y coordinate of the entity.
    - `{z}`: The Z coordinate of the entity.
    - `{world}`: The world the entity is in.
    - `{scoreboard-tags}`: The scoreboard tags of the entity.
  - You can use these placeholders in any order and as many times as you want.
  - You can also add any other text you want.
  - To add a newline, use `<br>`, `\n`,
    or a [HOCON multiline string](https://github.com/lightbend/config/blob/main/HOCON.md#multi-line-strings) (triple quotes).
  - **Example:** `popup-info-template: "{name} ({type})"`
  - **Example:**
    ```hocon
    popup-info-template: """
    {name}
    Type: {type}
    Location: {x}, {y}, {z}
    """
    ```

You should put the more specific filters first, and the more general filters last,
because entities will only get caught by the first filter that matches them.\
This is especially important if you're using custom icons and popup info templates.\
You could also get around this by creating multiple filter-sets.

### Some tips and tricks
- A full example config can be found [here](https://github.com/TechnicJelle/BlueMapFilteredEntities/blob/main/example.conf).
- If the filter is not valid for some reason, a warning will be printed to the console telling you what's wrong.
- If you find yourself repeating something a lot, you can make it into a variable.
  - This is a HOCON feature, and you can read more about it [here](https://github.com/lightbend/config/blob/main/HOCON.md#substitutions).

## Support
To get support with this plugin, join the [BlueMap Discord server](https://bluecolo.red/map-discord)
and ask your questions in [#3rd-party-support](https://discord.com/channels/665868367416131594/863844716047106068).
You're welcome to ping me, @TechnicJelle.

## Community Setups
If you have a setup that you think is useful for others,
please upload it somewhere and create a PR to add it to this list.

Make sure you include any icons as well, and credit the original creator if you didn't make them yourself.\
Make sure to have permission to use and distribute them!
You could also link to where to get them.

I recommend creating a new repository for this.

- List currently empty.
