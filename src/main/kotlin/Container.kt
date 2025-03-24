package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.NBT
import de.tr7zw.nbtapi.NBTItem
import de.tr7zw.nbtapi.iface.ReadWriteItemNBT
import net.kyori.adventure.text.Component
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
        val itemStack = ItemStack(material, 1)
        // ItemMeta is never null because it's only null if material is air.
        itemStack.modifyMeta<ItemMeta> {
            val text = Component.text("Power ${power.originalName}")
            displayName(text)
            lore(listOf(text))
        }

        return itemStack.modifyNBT {
            addFakeEnchant()
            if (material == Material.JUKEBOX) {
                addDisk(power.value)
            } else {
                val slots = when (material) {
                    Material.FURNACE -> 3
                    Material.CHEST -> 27
                    Material.BARREL -> 27
                    Material.HOPPER -> 5
                    else -> throw RedstoneToolsException("Unknown material, this is a bug")
                }
                addItems(power.value, slots)
            }
        }
    }

    private fun ReadWriteItemNBT.addItems(power: Int, slots: Int) {
        var itemsNeeded = itemsNeeded(power, slots)
        if (itemsNeeded == 0) return
        val itemList = getOrCreateCompound("BlockEntityTag").getCompoundList("Items")
        for (i in 0..(itemsNeeded / 64.toFloat()).toInt()) {
            itemList.addCompound().apply {
                setByte("Count", min(itemsNeeded, 64).toByte())
                setString("id", "minecraft:redstone")
                setByte("Slot", i.toByte())
            }
            itemsNeeded -= 64
        }
    }

    private fun ReadWriteItemNBT.addDisk(power: Int) {
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

        getOrCreateCompound("BlockEntityTag").apply {
            getOrCreateCompound("RecordItem").apply {
                setString("id", diskId)
                setByte("Count", 1)
            }
            setBoolean("has_record", true)
        }
    }

    private fun itemsNeeded(power: Int, slots: Int): Int {
        if (power == 0) return 0
        if (power == 15) return slots * 64
        return ceil((32 * slots * power) / 7.toFloat() - 1).toInt()
    }
}
