package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.ceil

@CommandAlias("container")
@Description("Container fetching command")
@CommandPermission("redstonetools.container")
class Container : BaseCommand() {
    @Default
    // ...
    // also missing -parameters in build.gradle.kts which wrecks things
    @Syntax("[type] [power]")
    @CommandCompletion("@containers @range:0-15")
    fun rstack(
            player: Player,
            args: Array<String>
    ) {
        val power = args[1].toIntOrNull()
        if (power == null || power !in 0..15) {
            return
        }

        when (args[0]) {
            "furnace" -> player.inventory.addItem(getContainer(Material.FURNACE, power))
            "chest" -> player.inventory.addItem(getContainer(Material.CHEST, power))
            "barrel" -> player.inventory.addItem(getContainer(Material.BARREL, power))
            "hopper" -> player.inventory.addItem(getContainer(Material.HOPPER, power))
            else -> {}
        }
    }

    private fun getContainer(material: Material, power: Int) : ItemStack {
        val slots = when (material) {
            Material.FURNACE -> 3
            Material.CHEST -> 27
            Material.BARREL -> 27
            Material.HOPPER -> 5
            else -> 0
        }
        val itemStack = ItemStack(material, 1)
        itemStack.itemMeta = getModifiedLore(itemStack.itemMeta, power)
        val nbti = NBTItem(itemStack)
        setEnchantment(nbti)
        addItemsToNbt(power, slots, nbti)
        return nbti.item
    }

    private fun addItemsToNbt(power: Int, slots: Int, nbti: NBTItem) {
        var itemsNeeded = getItemsNeeded(power, slots)
        if (itemsNeeded != 0) {
            nbti.addCompound("BlockEntityTag")
            val compound = nbti.getCompound("BlockEntityTag")
            val itemList = compound.getCompoundList("Items")
            for (i in 1..(itemsNeeded/64.toFloat() + 1).toInt()) {
                val localCompound = itemList.addCompound()
                if (itemsNeeded < 64) {
                    localCompound.setByte("Count", itemsNeeded.toByte())
                } else {
                    localCompound.setByte("Count", 64.toByte())
                }
                localCompound.setString("id", "minecraft:redstone")
                localCompound.setByte("Slot", (i-1).toByte())
                itemsNeeded-=64
            }
        }
    }

    private fun getItemsNeeded(power: Int, available: Int) : Int {
        if (power == 0) return 0
        if (power == 15) return available * 64
        return ceil((32 * available * power ) / 7.toFloat() - 1).toInt()
    }

    private fun getModifiedLore(meta: ItemMeta?, power: Int) : ItemMeta? {
        meta?.setDisplayName(power.toString())
        meta?.lore = listOf("Power $power")
        return meta
    }

    private fun setEnchantment(nbti: NBTItem) {
        nbti.addCompound("Enchantments")
        val enchantments = nbti.getCompoundList("Enchantments")
        val enchantmentCompound = enchantments.addCompound()
        enchantmentCompound.setString("id", "minecraft:knockback")
        enchantmentCompound.setShort("lvl", 1.toShort())
        nbti.setInteger("HideFlags", 1)
    }
}

class ContainerCompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    override fun getCompletions(context: BukkitCommandCompletionContext?): MutableCollection<String> =
        mutableListOf("furnace", "chest", "barrel", "hopper")
}