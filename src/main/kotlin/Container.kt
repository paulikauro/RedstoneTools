package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.ceil
import kotlin.math.min

@CommandAlias("container")
@Description("Container fetching command")
@CommandPermission("redstonetools.container")
class Container : BaseCommand() {
    @Default
    @CommandCompletion("@container @signal_strength")
    @Syntax("[type] [power]")
    fun container(
        player: Player,
        container: SignalContainer,
        power: SignalStrength
    ) {
        player.inventory.addItem(container.itemWithPower(power))
    }

    private fun SignalContainer.itemWithPower(power: SignalStrength): ItemStack {
        val slots = when (material) {
            Material.FURNACE -> 3
            Material.CHEST -> 27
            Material.BARREL -> 27
            Material.HOPPER -> 5
            else -> 0
        }
        val itemStack = ItemStack(material, 1)
        // ItemMeta is never null because it's only null if material is air.
        itemStack.modifyMeta<ItemMeta> {
            setDisplayName("Power ${power.originalName}")
            lore = listOf("Power ${power.originalName}")
        }
        return NBTItem(itemStack).apply {
            addFakeEnchant()
            addItems(power.value, slots)
        }.item
    }

    private fun NBTItem.addItems(power: Int, slots: Int) {
        var itemsNeeded = itemsNeeded(power, slots)
        if (itemsNeeded == 0) return
        addCompound("BlockEntityTag")
        val compound = getCompound("BlockEntityTag")
        val itemList = compound.getCompoundList("Items")
        var slot = 0
        while (itemsNeeded > 0) {
            itemList.addCompound().apply {
                setByte("Count", min(itemsNeeded, 64).toByte())
                setString("id", "minecraft:redstone")
                setByte("Slot", (slot).toByte())
            }
            itemsNeeded -= 64
            slot++
        }
    }

    private fun itemsNeeded(power: Int, available: Int): Int {
        if (power == 0) return 0
        if (power == 15) return available * 64
        return ceil((32 * available * power) / 7.toFloat() - 1).toInt()
    }

}
