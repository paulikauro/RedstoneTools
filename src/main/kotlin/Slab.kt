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
import org.bukkit.inventory.meta.ItemMeta

@CommandAlias("slab")
@Description("Slab fetching command")
@CommandPermission("redstonetools.slab")
class Slab : BaseCommand() {
    @Default
    @Syntax("[type]")
    @CommandCompletion("@slabs")
    fun slab(
            player: Player,
            args: Array<String>
    ) {
        val slab: ItemStack
        if (args.isEmpty()) {
            slab = getSlab(player.inventory.itemInMainHand.type.name) ?: return
            player.inventory.setItemInMainHand(slab)
        } else {
            slab = getSlab(args[0]) ?: return
            player.inventory.addItem(slab)
        }
    }

    private fun getSlab(type: String) : ItemStack? {
        val material = Material.getMaterial(type.toUpperCase()) ?: return null
        val itemStack = ItemStack(material, 1)
        val itemMeta = itemStack.itemMeta as BlockDataMeta
        val blockData = material.createBlockData() as? Slab ?: return null
        blockData.type = Slab.Type.TOP
        itemMeta.setBlockData(blockData)
        itemMeta.modifyLore()
        itemStack.itemMeta = itemMeta
        val nbti = NBTItem(itemStack)
        setFakeEnchant(nbti)
        return nbti.item
    }

    private fun ItemMeta.modifyLore(): ItemMeta {
        this.setDisplayName("Upside Down Slab")
        this.lore = listOf("UpsiDownORE")
        return this
    }

}

class SlabCompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    override fun getCompletions(context: BukkitCommandCompletionContext?): MutableCollection<String> =
        Material.values().filter { it.isBlock && (it.createBlockData() is Slab) }.map { it.toString().toLowerCase() }.toMutableList()
}