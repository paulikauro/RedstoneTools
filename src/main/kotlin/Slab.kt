package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockDataMeta

@CommandAlias("slab")
@Description("Slab fetching command")
@CommandPermission("redstonetools.slab")
class Slab : BaseCommand() {
    @Default
    @CommandCompletion("@slabs")
    fun slab(
            player: Player,
            args: Array<String>
    ) {
        val slab: ItemStack?
        if (args.isNotEmpty()) {
            slab = getSlab(args[0]) ?: throw RedstoneToolsException("Invalid slab type specified.")
            player.inventory.addItem(slab)
        } else {
            slab = getSlab(player.inventory.itemInMainHand.type.name)
            if (slab != null) {
                player.inventory.setItemInMainHand(slab)
            } else {
                player.inventory.addItem(getSlab(Material.SMOOTH_STONE_SLAB.toString()))
            }
        }
    }

    private fun getSlab(type: String) : ItemStack? {
        val material = Material.getMaterial(type.toUpperCase()) ?: return null
        if (!material.isBlock) return null
        val blockData = material.createBlockData() as? Slab ?: return null
        val itemStack = ItemStack(material, 1)
        blockData.type = Slab.Type.TOP
        itemStack.modifyMeta<BlockDataMeta> {
            setBlockData(blockData)
            setDisplayName("Upside Down Slab")
            lore = listOf("UpsiDownORE")
        }
        return NBTItem(itemStack).apply {
            addFakeEnchant()
        }.item
    }
}

class SlabCompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    override fun getCompletions(context: BukkitCommandCompletionContext?): MutableCollection<String> =
        Material.values().filter {
            it.isBlock && (it.createBlockData() is Slab)
        }.map {
            it.toString().toLowerCase()
        }.toMutableList()
}
