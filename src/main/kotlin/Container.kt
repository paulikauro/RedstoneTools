package io.github.paulikauro.redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.iface.ReadWriteNBT
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList
import io.github.paulikauro.redstonetools.adventure.c
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.ceil
import kotlin.math.max

fun PluginScope.createContainer() {
    commandManager.apply {
        registerThing(SignalStrength)
        registerThing(SignalContainer)
        registerCommand(Container())
    }
}

@CommandAlias("container")
@Description("Container fetching command")
@CommandPermission("redstonetools.container")
private class Container : BaseCommand() {
    @Default
    @Syntax("[type] [power]")
    fun container(
        player: Player,
        container: SignalContainer,
        power: SignalStrength,
    ) {
        player.inventory.addItem(container.itemWithPower(power))
    }

    private fun SignalContainer.itemWithPower(power: SignalStrength): ItemStack {
        val itemStack = ItemStack(material, 1)
        // ItemMeta is never null because it's only null if material is air.
        itemStack.modifyMeta<ItemMeta> {
            val text = "Power ${power.originalName}".c()
            displayName(text)
            lore(listOf(text))
            @Suppress("UsePropertyAccessSyntax") // cannot reassign val
            setEnchantmentGlintOverride(true)
        }

        return itemStack.modifyComponents {
            if (material == Material.JUKEBOX) {
                getOrCreateCompound("minecraft:block_entity_data").apply {
                    addDisk(power.value)
                    setString("id", "minecraft:jukebox")
                }
            } else {
                val slots = when (material) {
                    Material.FURNACE -> 3
                    Material.CHEST -> 27
                    Material.BARREL -> 27
                    Material.HOPPER -> 5
                    else -> throw RedstoneToolsException("Unknown material, this is a bug")
                }
                getCompoundList("minecraft:container").addItems(power.value, slots)
            }
        }
    }

    private fun ReadWriteNBTCompoundList.addSlot(slot: Int, itemCount: Int) {
        addCompound().apply {
            getOrCreateCompound("item").apply {
                setByte("count", itemCount.toByte())
                setString("id", "minecraft:redstone")
            }
            setByte("slot", slot.toByte())
        }
    }

    private fun ReadWriteNBTCompoundList.addItems(power: Int, slots: Int) {
        val itemsNeeded = max(power.toDouble(), ceil(32.0 * (power - 1.0) * slots / 7.0)).toInt()
        val stacks = itemsNeeded / 64
        val rest = itemsNeeded % 64
        repeat(stacks) { i ->
            addSlot(i, 64)
        }
        if (rest == 0) return
        addSlot(stacks, rest)
    }

    private fun ReadWriteNBT.addDisk(power: Int) {
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

        getOrCreateCompound("RecordItem").apply {
            setString("id", diskId)
            setByte("count", 1)
        }
        // 6 minutes, longer than the longest song (blocks, 5:45)
        setInteger("ticks_since_song_started", 6 * 60 * 20)
    }
}

private class SignalStrength(val value: Int, val originalName: String) {
    companion object : Thing<SignalStrength> {
        override fun of(arg: String): SignalStrength? = when (arg.lowercase()) {
            in hexValues -> SignalStrength(arg.toInt(16), arg)
            in intValues -> SignalStrength(arg.toInt(), arg)
            else -> null
        }

        private val intValues = (0..15).map(Int::toString)
        private val hexValues = ('a'..'f').map(Char::toString)
        override val values = intValues + hexValues
        override val readableName = "Signal strength"
        override val valueClass = SignalStrength::class.java
    }
}

private class SignalContainer(val material: Material) {
    companion object : Thing<SignalContainer> {
        // Not a map [yet] cuz we want shortcuts
        // maybe possible to just check the first letter (like WorldEdit does with directions)
        // depending on what other containers we want to support
        private val materials = listOf(
            "furnace" to Material.FURNACE,
            "chest" to Material.CHEST,
            "barrel" to Material.BARREL,
            "hopper" to Material.HOPPER,
            "jukebox" to Material.JUKEBOX,
        )
        override val values = materials.map { it.first }.sorted()
        override fun of(arg: String): SignalContainer? = materials
            .firstOrNull { (name, _) -> name.startsWith(arg) }
            ?.let { (_, material) -> SignalContainer(material) }

        override val readableName = "Container"
        override val valueClass = SignalContainer::class.java
    }
}
