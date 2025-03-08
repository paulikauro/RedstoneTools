# RedstoneTools
A collection of tools (mostly WorldEdit-related) for working with redstone

Depends on these plugins: [Item-NBT-API](https://github.com/tr7zw/Item-NBT-API) and [WorldEdit](https://github.com/EngineHub/WorldEdit)

# Commands
## `//rstack`
Like WorldEdit's `//stack`, but 
- allows the stacked copies to overlap
- supports more directions, in particular ones used in diagonal building orientations
- has a flag for expanding the selection to cover the whole stacked area
    
Usage: `//rstack [-e] [-w] [direction] [count] [spacing]`
- `-e`: when specified, the selection is expanded
- `-w` ("with air"): when specified, **stacked copies may not overlap** (corresponds to omitting `-a` from WorldEdit commands)
- `direction`: the usual WorldEdit directions. Additionally, `u` or `d` may be appended to
most directions to get a diagonal (in the y-axis) direction. Defaults to `me`
(whatever direction you're looking at)
- `count`: how many copies to stack. Defaults to 1.
- `spacing`: how far apart copies should be stacked. Defaults to 2 due to that being
common in redstoning.

NOTE: The order of arguments does not matter apart from `count` and `spacing`.

## `//that [mask]`
Selects the build you're looking at. More specifically, it tries to select the smallest cuboid containing all connected blocks (according to mask, which defaults to `#existing`) starting from whatever block you're looking at.

## `//find [mask]`
Search a selected region for a specific WorldEdit material mask.

## `//signsearch|//ss [regex]`
Search a selected region for signs that match a provided regex.

## `/autowire|/aw`
When enabled, places redstone wire ontop of solid blocks the user is placing, provided that air is above said block.

## `/autorotate|/ar`
When enabled, rotates redstone components 180 degrees upon placement.

## `/container`
Gives the player a chest, barrel, hopper, furnace or jukebox with the proper amount of items to provide a signal strength of specified power.

Usage: `/container [type] [power]`
- `type`: a container of type `chest`, `barrel`, `hopper`, `furnace` or `jukebox`.
- `power`: an integer of range `0` through `15`. 

## `/livestack`
Automatically stacks any block you place

## `//selstack`
- `//selstack push` pushes your selection points onto a stack (and clears your current selection)
- `//selstack pop` clears your selection and restores a previous one from the stack
- `//selstack clear` clears the stack itself
- `//selstack show` shows you what's on the stack
To be clear, `//selstack` never changes any blocks, it only operates on your selection just like the selection wand or `//pos1` and `//pos2` do.

## `/pin`
- `/pin add <name>` lets you click a lever to add it as an input pin that you can toggle with commands
- `/pin remove <name>` removes the pin but does not remove the lever
- `/pin list` lists your pins
- `/pin turn on|off <name>` turns the pin's lever on or off
- `/pin pulse on|off <name> <ticks>` pulses a pin, ie changes its state to the wanted one for `<ticks>` amount of redstone ticks and after that flips the state back
- `/pin toggle <name>` toggles a pin's state

## `/slab`
Gives the player an upside-down slab of specified type

Usage: `/slab [type]`
- `type`: a type of slab
If type is not specified and the player is holding a slab, the upside-down magic is applied to that instead.

## `/cauldron`
Toggles cauldron adjustment mode, which lets you right-click a cauldron with an empty hand to cycle the water level.
NOTE: The name is likely to change in the future to encompass composters (and possibly other signal strength containers)

# Other features
## WorldEdit helper
This gives you a scoreboard containing your selection's corners, volume and dimensions.

# Building
`gradle build` or `./gradlew build`
