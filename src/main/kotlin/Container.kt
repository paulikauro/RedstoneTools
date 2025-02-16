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
        val item = container.itemWithPower(power.value)
        player.inventory.addItem(item)
    }

    private fun SignalContainer.itemWithPower(power: Int): ItemStack {
        return if (material == Material.JUKEBOX) {
            createJukeboxWithDisk(power)
        } else {
            createContainerWithItems(power)
        }
    }

    private fun SignalContainer.createJukeboxWithDisk(power: Int): ItemStack {
        val itemStack = ItemStack(material, 1)
        itemStack.modifyMeta<ItemMeta> {
            setDisplayName("Jukebox Power: $power")
            lore = listOf("Power Level: $power")
        }

        val nbtItem = NBTItem(itemStack).apply {
            addFakeEnchant()
            addDisk(power)
        }

        return nbtItem.item
    }

    private fun SignalContainer.createContainerWithItems(power: Int): ItemStack {
        val slots = when (material) {
            Material.FURNACE -> 3
            Material.CHEST -> 27
            Material.BARREL -> 27
            Material.HOPPER -> 5
            else -> 0
        }
        val itemStack = ItemStack(material, 1)
        itemStack.modifyMeta<ItemMeta> {
            setDisplayName("Container Power: $power")
            lore = listOf("Power Level: $power")
        }

        val nbtItem = NBTItem(itemStack).apply {
            addFakeEnchant()
            addItems(power, slots)
        }

        return nbtItem.item
    }

    private fun NBTItem.addItems(power: Int, slots: Int) {
        var itemsNeeded = itemsNeeded(power, slots)
        if (itemsNeeded == 0) return
        addCompound("BlockEntityTag")
        val compound = getCompound("BlockEntityTag")
        val itemList = compound.getCompoundList("Items")

        for (i in 1..(itemsNeeded / 64.toFloat() + 1).toInt()) {
            itemList.addCompound().apply {
                setByte("Count", min(itemsNeeded, 64).toByte())
                setString("id", "minecraft:redstone")
                setByte("Slot", (i - 1).toByte())
            }
            itemsNeeded -= 64
        }
    }

    private fun NBTItem.addDisk(power: Int) {
        if (power == 0) return

        val diskId = when (power) {
            1 -> "minecraft:music_disc_13"
            2 -> "minecraft:music_disc_cat"
            3 -> "minecraft:music_disc_blocks"
            4 -> "minecraft:music_disc_chirp"
            5 -> "minecraft:music_disc_far"
            6 -> "minecraft:music_disc_mall"
            7 -> "minecraft:music_disc_mellohi"
            8 -> "minecraft:music_disc_stal"
            9 -> "minecraft:music_disc_strad"
            10 -> "minecraft:music_disc_ward"
            11 -> "minecraft:music_disc_11"
            12 -> "minecraft:music_disc_wait"
            13 -> "minecraft:music_disc_pigstep"
            14 -> "minecraft:music_disc_otherside"
            15 -> "minecraft:music_disc_5"
            else -> return
        }

        addCompound("BlockEntityTag")
        val compound = getCompound("BlockEntityTag")
        val recordItem = compound.addCompound("RecordItem")

        recordItem.setString("id", diskId)
        recordItem.setByte("Count", 1)
        compound.setBoolean("has_record", true)
    }

    private fun itemsNeeded(power: Int, available: Int): Int {
        if (power == 0) return 0
        if (power == 15) return available * 64
        return ceil((32 * available * power) / 7.toFloat() - 1).toInt()
    }
}
