package redstonetools

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*;
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Bukkit.createBlockData
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.Repeater
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockDataMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import redstonetools.RedstoneTools

@CommandAlias("rep|repeater")
@Description("Locked Repeater Giving Command")
@CommandPermission("redstonetools.repeater")

class Repeater(val plugin: JavaPlugin)  : BaseCommand() {
    @Default
    @Syntax("[value] - the value of the repeater")
    fun repeater(player: Player, @Default("1") value: Boolean)
    {
        val material = Material.REPEATER
        val itemstack = ItemStack(material, 1)
        val blockdata = material.createBlockData()
        val pdc = itemstack.itemMeta?.persistentDataContainer
        val namespacedkey = NamespacedKey(plugin, "power")
        if(value){
            pdc?.set(namespacedkey, PersistentDataType.INTEGER, 1)
            itemstack.modifyMeta<BlockDataMeta> {
                setBlockData(blockdata)
                setDisplayName("${ChatColor.GREEN} Redstone Repeater")
                lore = listOf("${ChatColor.GRAY} Powered & locked repeater")
            }
        } else {
            pdc?.set(namespacedkey, PersistentDataType.INTEGER, 0)
            itemstack.modifyMeta<BlockDataMeta> {
                setBlockData(blockdata)
                setDisplayName("${ChatColor.RED} Redstone Repeater")
                lore = listOf("${ChatColor.GRAY} Locked repeater")
            }
        }
        player.inventory.addItem(NBTItem(itemstack).apply { addFakeEnchant() }.item)
    }
}

class RepeaterCompletionHandler :
        CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> = listOf("0", "1")
}
