package redstonetools

import de.tr7zw.nbtapi.NBTItem

fun setFakeEnchant(nbti: NBTItem) {
    nbti.addCompound("Enchantments")
    val enchantments = nbti.getCompoundList("Enchantments")
    val enchantmentCompound = enchantments.addCompound()
    enchantmentCompound.setString("id", "minecraft:knockback")
    enchantmentCompound.setShort("lvl", 1.toShort())
    nbti.setInteger("HideFlags", 1)
}