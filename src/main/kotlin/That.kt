package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.world.World
import org.bukkit.entity.Player

private const val ITERATIONS_LIMIT = 160

@CommandAlias("/that|/t|/hsel")
@Description("Select the build you're looking at")
@CommandPermission("redstonetools.that")
class That(private val worldEdit: WorldEdit) : BaseCommand() {
    @Default
    @Syntax("")
    fun that(
        player: Player,
        args: Array<String>)
    {
        if (args.isNotEmpty()) {
            throw ConditionFailedException("Too many arguments!")
        }

        val weplayer = BukkitAdapter.adapt(player)

        if(selectTargetBlock(weplayer)) {
            growSelection(weplayer)
            weplayer.printInfo(TextComponent.of("Build selected."))
        }
        else weplayer.printError(TextComponent.of("No build in sight!"))
    }

    private fun selectTargetBlock(player: WEPlayer): Boolean {
        val session = worldEdit.sessionManager.get(player)
        val world = player.world
        val regionSelector = session.getRegionSelector(world)
        val targetBlock = player.getBlockTrace(ITERATIONS_LIMIT)?.toVector()?.toBlockPoint() ?: return false

        regionSelector.selectPrimary(targetBlock, null)
        regionSelector.selectSecondary(targetBlock, null)
        regionSelector.explainRegionAdjust(player, session)
        return true
    }

    private fun growSelection(player: WEPlayer) {
        val session = worldEdit.sessionManager.get(player)
        val world = player.world
        val regionSelector = session.getRegionSelector(world)
        val selection = session.getSelection(world)

        var emptyFaces = 0
        var iterations = 0

        while(emptyFaces != 6 && iterations < ITERATIONS_LIMIT) {
            iterations++
            emptyFaces = 0

            if(checkFace(world, selection, 0)) emptyFaces++ // +X
            if(checkFace(world, selection, 1)) emptyFaces++ // -X
            if(checkFace(world, selection, 2)) emptyFaces++ // +Y
            if(checkFace(world, selection, 3)) emptyFaces++ // -Y
            if(checkFace(world, selection, 4)) emptyFaces++ // +Z
            if(checkFace(world, selection, 5)) emptyFaces++ // -Z
        }

        regionSelector.learnChanges()
        regionSelector.explainRegionAdjust(player, session)
    }

    private fun checkFace(world: World, selection: Region, face: Int): Boolean {
        var isAir: Boolean
        val minX: Int
        val minY: Int
        val maxX: Int
        val maxY: Int

        /* MORE READABLE, SLOWER IMPLEMENTATION

        val expandDirection: BlockVector3
        val contractDirection: BlockVector3
        var edge: BlockVector3

        when (face) {
            0 -> {
                minX = selection.minimumPoint.z
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.z
                maxY = selection.maximumPoint.y
                expandDirection = BlockVector3.at(1, 0, 0)
                contractDirection = BlockVector3.at(-1, 0, 0)
            }
            1 -> {
                minX = selection.minimumPoint.z
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.z
                maxY = selection.maximumPoint.y
                expandDirection = BlockVector3.at(-1, 0, 0)
                contractDirection = BlockVector3.at(1, 0, 0)
            }
            2 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.z
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.z
                expandDirection = BlockVector3.at(0, 1, 0)
                contractDirection = BlockVector3.at(0, -1, 0)
            }
            3 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.z
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.z
                expandDirection = BlockVector3.at(0, -1, 0)
                contractDirection = BlockVector3.at(0, 1, 0)
            }
            4 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.y
                expandDirection = BlockVector3.at(0, 0, 1)
                contractDirection = BlockVector3.at(0, 0, -1)
            }
            5 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.y
                expandDirection = BlockVector3.at(0, 0, -1)
                contractDirection = BlockVector3.at(0, 0, 1)
            }
            else -> return false
        }

        selection.expand(expandDirection)
        for(x in minX..maxX) {
            for(y in minY..maxY) {
                edge = when (face) {
                    0 -> BlockVector3.at(selection.maximumPoint.x, y, x)
                    1 -> BlockVector3.at(selection.minimumPoint.x, y, x)
                    2 -> BlockVector3.at(x, selection.maximumPoint.y, y)
                    3 -> BlockVector3.at(x, selection.minimumPoint.y, y)
                    4 -> BlockVector3.at(x, y, selection.maximumPoint.z)
                    5 -> BlockVector3.at(x, y, selection.minimumPoint.z)
                    else -> return false
                }
                isAir = world.getBlock(edge).blockType.material.isAir
                if(!isAir) return false
            }
        }
        selection.contract(contractDirection)
        return true
        */


        // FASTER IMPLEMENTATION

        when (face) {
            0 -> {
                minX = selection.minimumPoint.z
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.z
                maxY = selection.maximumPoint.y

                selection.expand(BlockVector3.at(1, 0, 0))
                for(x in minX..maxX) {
                    for(y in minY..maxY) {
                        isAir = world.getBlock(BlockVector3.at(selection.maximumPoint.x, y, x)).blockType.material.isAir
                        if(!isAir)
                            return false
                    }
                }
                selection.contract(BlockVector3.at(-1, 0, 0))
            }
            1 -> {
                minX = selection.minimumPoint.z
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.z
                maxY = selection.maximumPoint.y

                selection.expand(BlockVector3.at(-1, 0, 0))
                for(x in minX..maxX) {
                    for(y in minY..maxY) {
                        isAir = world.getBlock(BlockVector3.at(selection.minimumPoint.x, y, x)).blockType.material.isAir
                        if(!isAir)
                            return false
                    }
                }
                selection.contract(BlockVector3.at(1, 0, 0))
            }
            2 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.z
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.z

                selection.expand(BlockVector3.at(0, 1, 0))
                for(x in minX..maxX) {
                    for(y in minY..maxY) {
                        isAir = world.getBlock(BlockVector3.at(x, selection.maximumPoint.y, y)).blockType.material.isAir
                        if(!isAir)
                            return false
                    }
                }
                selection.contract(BlockVector3.at(0, -1, 0))
            }
            3 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.z
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.z

                selection.expand(BlockVector3.at(0, -1, 0))
                for(x in minX..maxX) {
                    for(y in minY..maxY) {
                        isAir = world.getBlock(BlockVector3.at(x, selection.minimumPoint.y, y)).blockType.material.isAir
                        if(!isAir)
                            return false
                    }
                }
                selection.contract(BlockVector3.at(0, 1, 0))
            }
            4 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.y

                selection.expand(BlockVector3.at(0, 0, 1))
                for(x in minX..maxX) {
                    for(y in minY..maxY) {
                        isAir = world.getBlock(BlockVector3.at(x, y, selection.maximumPoint.z)).blockType.material.isAir
                        if(!isAir)
                            return false
                    }
                }
                selection.contract(BlockVector3.at(0, 0, -1))
            }
            5 -> {
                minX = selection.minimumPoint.x
                minY = selection.minimumPoint.y
                maxX = selection.maximumPoint.x
                maxY = selection.maximumPoint.y

                selection.expand(BlockVector3.at(0, 0, -1))
                for(x in minX..maxX) {
                    for(y in minY..maxY) {
                        isAir = world.getBlock(BlockVector3.at(x, y, selection.minimumPoint.z)).blockType.material.isAir
                        if(!isAir)
                            return false
                    }
                }
                selection.contract(BlockVector3.at(0, 0, 1))
            }
            else -> return false
        }
        return true
    }
}