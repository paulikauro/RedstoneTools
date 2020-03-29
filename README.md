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

