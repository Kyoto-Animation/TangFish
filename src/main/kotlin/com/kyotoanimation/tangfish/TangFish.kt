package com.kyotoanimation.tangfish

import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import com.kyotoanimation.tangfish.commands.MainCommand
import com.kyotoanimation.tangfish.config.ConfigManager
import com.kyotoanimation.tangfish.config.LanguageManager
import com.kyotoanimation.tangfish.listeners.FishingListener

class TangFish : JavaPlugin() {

    companion object {
        lateinit var instance: TangFish
            private set

        fun isDebugEnabled(): Boolean {
            return instance.config.getBoolean("debug", false)
        }

        fun debug(message: String) {
            if (!isDebugEnabled()) return
            instance.logger.info("[Debug] $message")
        }
    }

    override fun onEnable() {
        instance = this

        ConfigManager.reload()
        LanguageManager.reload()

        registerListeners()
        registerCommands()
        printWelcomeMessage()

        logger.info("TangFish has been enabled successfully!")
    }

    override fun onDisable() {
        logger.info("TangFish has been disabled.")
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(FishingListener(), this)
        debug("FishingListener registered")
    }

    private fun registerCommands() {
        val mainCommand = MainCommand()
        
        getCommand("tangfish")?.let { cmd ->
            cmd.setExecutor(mainCommand)
            cmd.tabCompleter = mainCommand
            debug("MainCommand registered")
        } ?: run {
            logger.warning("Failed to register tangfish command - command not found in plugin.yml")
        }
    }

    private fun printWelcomeMessage() {
        val banner = """
            |${ChatColor.GOLD}╔══════════════════════════════════════╗
            |${ChatColor.GOLD}║${ChatColor.AQUA}    _____ ___  _   _ _____ ___ ${ChatColor.GOLD}   ║
            |${ChatColor.GOLD}║${ChatColor.AQUA}   |_   _/ _ \| \ | |  __ |__ \${ChatColor.GOLD}   ║
            |${ChatColor.GOLD}║${ChatColor.AQUA}     | || | | |  \| | |  | | ) |${ChatColor.GOLD}  ║
            |${ChatColor.GOLD}║${ChatColor.AQUA}     | || |_| | |\  | |  | |/ / ${ChatColor.GOLD}  ║
            |${ChatColor.GOLD}║${ChatColor.AQUA}     |_| \___/|_| \_|_|  |_|___|${ChatColor.GOLD} ║
            |${ChatColor.GOLD}║${ChatColor.GREEN}       Fishing Plugin v${description.version}${ChatColor.GOLD}      ║
            |${ChatColor.GOLD}║${ChatColor.YELLOW}       Author: KyotoAnimation${ChatColor.GOLD}        ║
            |${ChatColor.GOLD}╚══════════════════════════════════════╝${ChatColor.RESET}
        """.trimMargin()

        server.consoleSender.sendMessage(banner)
    }
}
