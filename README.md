# RedstoneTools
A collection of tools (mostly WorldEdit-related) for working with redstone

# Commands
## `//rstack`
Like WorldEdit's `//stack`, but 
- allows the stacked copies to overlap
- supports more directions, in particular ones used in diagonal building orientations
- has a flag for expanding the selection to cover the whole stacked area
    
Usage: `//rstack [-e] [direction] [count] [spacing]`
- `-e`: when specified, the selection is expanded
- `direction`: the usual WorldEdit directions. Additionally, `u` or `d` may be appended to
most directions to get a diagonal (in the y-axis) direction. Defaults to `me`
(whatever direction you're looking at)
- `count`: how many copies to stack. Defaults to 1.
- `spacing`: how far apart copies should be stacked. Defaults to 2 due to that being
common in redstoning.

NOTE: The order of arguments does not matter apart from `count` and `spacing`.

## `//find`
Search a selected region for a specific WorldEdit material mask.

## `/container`
Gives the player a chest, barrel, hopper, or furnace with the proper amount of items to provide a signal strength of specified power.

Usage: `/container [type] [power]`
- `type`: a container of type `chest`, `barrel`, `hopper`, or `furnace`.
- `power`: an integer of range `0` through `15`. 

## `/slab`
Gives the player an upside-down slab of specified type

Usage: `/slab [type]`
- `type`: a type of slab

# Building
`gradle build` or `./gradlew build`
