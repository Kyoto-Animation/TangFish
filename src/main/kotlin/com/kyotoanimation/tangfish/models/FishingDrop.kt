package com.kyotoanimation.tangfish.models

import com.cryptomorin.xseries.XMaterial
import com.kyotoanimation.tangfish.utils.colorize
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

data class FishingDrop(
    val id: String,
    val chance: Double,
    val amount: Int,
    val displayName: String?,
    val lore: List<String>?
) {

    private val cachedMaterial: Material? by lazy {
        XMaterial.matchXMaterial(id)
            .orElse(null)
            ?.parseMaterial()
    }

    fun toItemStack(): ItemStack? {
        val material = cachedMaterial ?: return null
        
        val item = ItemStack(material, amount)
        val meta: ItemMeta = item.itemMeta ?: return item
        
        displayName?.let {
            meta.setDisplayName(it.colorize())
        }
        
        lore?.let { loreList ->
            meta.lore = loreList.map { line -> line.colorize() }
        }
        
        item.itemMeta = meta
        return item
    }

    companion object {
        @Volatile
        private var totalWeight: Double = 0.0

        internal fun updateTotalWeight(drops: List<FishingDrop>) {
            totalWeight = drops.sumOf { it.chance }
        }

        fun getTotalWeight(): Double = totalWeight
    }
}
