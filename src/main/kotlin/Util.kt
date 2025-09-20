package redstonetools

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import de.tr7zw.nbtapi.NBT
import de.tr7zw.nbtapi.iface.ReadWriteNBT
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.ComponentBuilderApplicable
import net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE
import net.kyori.adventure.text.format.NamedTextColor.RED
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

operator fun String.get(s: ComponentBuilderApplicable): Component =
    text().content(this).applicableApply(s).build()

operator fun ComponentBuilderApplicable.times(other: ComponentBuilderApplicable) = ComponentBuilderApplicable {
    it.applicableApply(this)
    it.applicableApply(other)
}

// overloads for WEPlayer until WE migrates to Adventure
fun Audience.info(msg: String): Unit = sendMessage(msg[LIGHT_PURPLE])
fun WEPlayer.info(msg: String): Unit = sendMessage(msg[LIGHT_PURPLE])
fun Audience.err(msg: String): Unit = sendMessage(msg[RED])
fun WEPlayer.err(msg: String): Unit = sendMessage(msg[RED])

typealias WEPlayer = com.sk89q.worldedit.entity.Player

fun Player.we(): WEPlayer = BukkitAdapter.adapt(this)
fun WEPlayer.bukkit(): Player = BukkitAdapter.adapt(this)

fun WEPlayer.sendMessage(message: Component): Unit = bukkit().sendMessage(message)

fun Location.toBlockVector3(): BlockVector3 = BlockVector3.at(x, y, z)

// 😎
@Suppress("UNCHECKED_CAST")
inline fun <T : ItemMeta> ItemStack.modifyMeta(action: T.() -> Unit) {
    itemMeta = (itemMeta as T).apply(action)
}

fun ItemStack.modifyComponents(action: ReadWriteNBT.() -> Unit) = this.also { NBT.modifyComponents(this, action) }

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
    selection
} catch (_: IncompleteRegionException) {
    null
}

const val MAKE_SELECTION_FIRST = "Make a region selection first."

fun LocalSession.requireSelection(): Region =
    getSelectionOrNull() ?: throw RedstoneToolsException(MAKE_SELECTION_FIRST)
