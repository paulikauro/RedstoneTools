package redstonetools

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import de.tr7zw.nbtapi.NBT
import de.tr7zw.nbtapi.iface.ReadWriteItemNBT
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

// 😎
@Suppress("UNCHECKED_CAST")
inline fun <T : ItemMeta> ItemStack.modifyMeta(action: T.() -> Unit) {
    itemMeta = (itemMeta as T).apply(action)
}

fun ItemStack.modifyNBT(action: ReadWriteItemNBT.() -> Unit) = this.also { NBT.modify(this, action) }

fun ReadWriteItemNBT.addFakeEnchant() {
    getCompoundList("Enchantments").addCompound().apply {
        setString("id", "minecraft:knockback")
        setShort("lvl", 1.toShort())
    }
    setInteger("HideFlags", 1)
}

fun parseMaskOrThrow(arg: String, worldEdit: WorldEdit, localSession: LocalSession?, player: WEPlayer?): Mask {
    val parserContext = ParserContext().apply {
        actor = player
        world = player?.world
        session = localSession
        extent = player?.world
        isRestricted = true
    }
    return worldEdit.maskFactory.parseFromInput(arg, parserContext)
}

fun LocalSession.getSelectionOrNull(): Region? = try {
    getSelection(selectionWorld ?: throw IncompleteRegionException())
} catch (exception: IncompleteRegionException) {
    null
}

const val MAKE_SELECTION_FIRST = "Make a region selection first."

fun LocalSession.requireSelection(): Region =
    getSelectionOrNull() ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
