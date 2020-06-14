package redstonetools

import de.tr7zw.nbtapi.NBTItem
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

fun NBTItem.addFakeEnchant() {
    addCompound("Enchantments")
    val enchantments = getCompoundList("Enchantments")
    enchantments.addCompound().apply {
        setString("id", "minecraft:knockback")
        setShort("lvl", 1.toShort())
    }
    setInteger("HideFlags", 1)
}

// 😎
@Suppress("UNCHECKED_CAST")
inline fun <T: ItemMeta> ItemStack.modifyMeta(action: T.() -> Unit) {
    itemMeta = (itemMeta as T).apply(action)
}
