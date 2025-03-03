package redstonetools

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

fun String.withHighlightedReplacement(replacement: String): TextComponent =
    TextComponent.of(this.substringBefore(replacement))
        .color(TextColor.WHITE)
        .append(
            TextComponent.of(replacement)
                .color(TextColor.YELLOW)
        )
        .append(
            TextComponent.of(this.substringAfter(replacement))
                .color(TextColor.WHITE)
        )

fun Location.toBlockVector3(): BlockVector3 = BlockVector3.at(x, y, z)

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
inline fun <T : ItemMeta> ItemStack.modifyMeta(action: T.() -> Unit) {
    itemMeta = (itemMeta as T).apply(action)
}

fun LocalSession.getSelectionOrNull(): Region? = try {
    getSelection(selectionWorld ?: throw IncompleteRegionException())
} catch (exception: IncompleteRegionException) {
    null
}

const val MAKE_SELECTION_FIRST = "Make a region selection first."

fun LocalSession.requireSelection(): Region =
    getSelectionOrNull() ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
