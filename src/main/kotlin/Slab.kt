package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.NBTItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
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
        @Optional
        type: String?,
    ) {
        val slab: ItemStack?
        if (type != null) {
            slab = getSlab(type) ?: throw RedstoneToolsException("Invalid slab type specified.")
            player.inventory.addItem(slab)
        } else {
            slab = getSlab(player.inventory.itemInMainHand.type.name)
            if (slab != null) {
                player.inventory.setItemInMainHand(slab)
            } else {
                // kinda bad but it shouldn't be null
                player.inventory.addItem(getSlab(Material.SMOOTH_STONE_SLAB.toString())!!)
            }
        }
    }

    private fun getSlab(type: String): ItemStack? {
        val material = Material.getMaterial(type.uppercase()) ?: return null
        if (!material.isBlock) return null
        val blockData = material.createBlockData() as? Slab ?: return null
        val itemStack = ItemStack(material, 1)
        blockData.type = Slab.Type.TOP
        itemStack.modifyMeta<BlockDataMeta> {
            setBlockData(blockData)
            displayName(Component.text("Upside Down Slab"))
            lore(listOf(Component.text("UpsiDownORE")))
        }
        return itemStack.modifyNBT { addFakeEnchant() }
    }
}

class SlabListener : Listener {
    @EventHandler
    fun onSlabPlace(event: BlockPlaceEvent) {
        val slabData = event.blockPlaced.blockData as? Slab ?: return
        if (slabData.type != Slab.Type.TOP) return
        val existingSlabData = event.blockReplacedState.blockData as? Slab ?: return
        if (existingSlabData.type != Slab.Type.TOP) return
        if (event.blockPlaced.location != event.blockReplacedState.location) return
        event.isCancelled = true
        val slabLocation = event.blockPlaced.location.add(0.0, -1.0, 0.0)
        if (slabLocation.block.type != Material.AIR) return
        slabLocation.block.type = event.blockPlaced.type
        slabLocation.block.blockData = event.blockPlaced.blockData
    }
}

class SlabCompletionHandler :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> = Material.entries
        .filter { it.isBlock && it.createBlockData() is Slab }
        .map { it.toString().lowercase() }
}
