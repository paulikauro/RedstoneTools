package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.ceil
import kotlin.math.max

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
        player.inventory.addItem(container.itemWithPower(power.value))
    }

    private fun SignalContainer.itemWithPower(power: Int): ItemStack {
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
            setDisplayName(power.toString())
            lore = listOf("Power $power")
        }
        return NBTItem(itemStack).apply {
            addFakeEnchant()
            addItems(power, slots)
        }.item
    }

    private fun NBTItem.addItems(power: Int, slots: Int) {
        var itemsNeeded = itemsNeeded(power, slots)
        if (itemsNeeded == 0) return
        addCompound("BlockEntityTag")
        val compound = getCompound("BlockEntityTag")
        val itemList = compound.getCompoundList("Items")
        for (i in 1..(itemsNeeded / 64.toFloat() + 1).toInt()) {
            itemList.addCompound().apply {
                setByte("Count", max(itemsNeeded, 64).toByte())
                setString("id", "minecraft:redstone")
                setByte("Slot", (i - 1).toByte())
            }
            itemsNeeded -= 64
        }
    }

    private fun itemsNeeded(power: Int, available: Int): Int {
        if (power == 0) return 0
        if (power == 15) return available * 64
        return ceil((32 * available * power) / 7.toFloat() - 1).toInt()
    }

}
