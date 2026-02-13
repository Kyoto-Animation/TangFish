package com.kyotoanimation.tangfish.config

import com.kyotoanimation.tangfish.TangFish
import com.kyotoanimation.tangfish.utils.colorize
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object LanguageManager {

    private var langConfig: FileConfiguration? = null
    private var currentLanguage: String = "zh_CN"

    fun reload() {
        val plugin = TangFish.instance
        currentLanguage = plugin.config.getString("language") ?: "zh_CN"
        
        val langFile = File(plugin.dataFolder, "languages/$currentLanguage.yml")
        
        if (!langFile.exists()) {
            saveDefaultLanguage(langFile)
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile)
        
        val defaultStream: InputStream? = plugin.getResource("languages/$currentLanguage.yml")
        if (defaultStream != null) {
            val defaultConfig = YamlConfiguration.loadConfiguration(
                InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            )
            langConfig?.setDefaults(defaultConfig)
        }
        
        plugin.logger.info("Loaded language file: $currentLanguage")
    }

    fun getMessage(key: String): String {
        val message = langConfig?.getString(key) ?: "§cMissing message: $key"
        return message.colorize()
    }

    fun getMessage(key: String, vararg placeholders: Pair<String, String>): String {
        var message = getMessage(key)
        placeholders.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }
        return message
    }

    private fun saveDefaultLanguage(targetFile: File) {
        val plugin = TangFish.instance
        val parentDir = targetFile.parentFile
        
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        
        val resourcePath = "languages/$currentLanguage.yml"
        val resource: InputStream? = plugin.getResource(resourcePath)
        
        if (resource != null) {
            plugin.saveResource(resourcePath, false)
        } else {
            val fallbackPath = "languages/zh_CN.yml"
            val fallbackResource: InputStream? = plugin.getResource(fallbackPath)
            
            if (fallbackResource != null) {
                plugin.saveResource(fallbackPath, false)
                val fallbackFile = File(plugin.dataFolder, fallbackPath)
                if (fallbackFile.exists() && !targetFile.exists()) {
                    fallbackFile.copyTo(targetFile)
                }
            } else {
                createDefaultLanguageFile(targetFile)
            }
        }
    }

    private fun createDefaultLanguageFile(targetFile: File) {
        targetFile.parentFile?.mkdirs()
        targetFile.createNewFile()
        
        val defaultConfig = YamlConfiguration()
        defaultConfig.set("prefix", "&6[TangFish] &r")
        defaultConfig.set("debug.caught", "&7[Debug] 随机数: {random}, 选中物品: {item}")
        defaultConfig.set("fishing.caught", "&a你钓到了: &f{item}")
        defaultConfig.set("fishing.no_drops", "&c没有配置任何钓鱼掉落物")
        defaultConfig.set("reload.success", "&a配置已重新加载")
        defaultConfig.set("error.no_permission", "&c你没有权限执行此操作")
        defaultConfig.set("error.player_only", "&c此命令只能由玩家执行")
        
        defaultConfig.save(targetFile)
    }
}
