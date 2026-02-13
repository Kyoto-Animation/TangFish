package com.kyotoanimation.tangfish.config

import com.kyotoanimation.tangfish.TangFish
import com.kyotoanimation.tangfish.models.FishingDrop
import org.bukkit.configuration.file.FileConfiguration
import java.util.concurrent.ThreadLocalRandom

object ConfigManager {

    private var drops: List<FishingDrop> = emptyList()
    private var cachedTotalWeight: Double = 0.0

    fun reload() {
        val plugin = TangFish.instance
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        
        drops = loadDrops(plugin.config)
        cachedTotalWeight = drops.sumOf { it.chance }
        FishingDrop.updateTotalWeight(drops)
        
        plugin.logger.info("Loaded ${drops.size} fishing drops with total weight: $cachedTotalWeight")
    }

    fun getDrops(): List<FishingDrop> = drops

    fun getTotalWeight(): Double = cachedTotalWeight

    fun getRandomDrop(): FishingDrop? {
        if (drops.isEmpty()) return null
        
        val random = ThreadLocalRandom.current().nextDouble() * cachedTotalWeight
        var currentWeight = 0.0
        
        for (drop in drops) {
            currentWeight += drop.chance
            if (random < currentWeight) {
                return drop
            }
        }
        
        return drops.lastOrNull()
    }

    private fun loadDrops(config: FileConfiguration): List<FishingDrop> {
        val dropsSection = config.getConfigurationSection("drops") ?: return emptyList()
        
        return dropsSection.getKeys(false).mapNotNull { key ->
            val section = dropsSection.getConfigurationSection(key) ?: return@mapNotNull null
            
            val id = section.getString("id") ?: return@mapNotNull null
            val chance = section.getDouble("chance", 0.0)
            val amount = section.getInt("amount", 1)
            val displayName = section.getString("display-name")
            val lore = section.getStringList("lore").takeIf { it.isNotEmpty() }
            
            FishingDrop(
                id = id,
                chance = chance,
                amount = amount,
                displayName = displayName,
                lore = lore
            )
        }
    }
}
