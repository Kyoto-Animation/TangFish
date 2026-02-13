package com.kyotoanimation.tangfish.utils

import org.bukkit.ChatColor

private val HEX_PATTERN = Regex("&#([A-Fa-f0-9]{6})")

fun String.colorize(): String {
    return if (supportsHex()) {
        translateHexColorCodes(this)
    } else {
        ChatColor.translateAlternateColorCodes('&', this)
    }
}

fun List<String>.colorize(): List<String> {
    return this.map { it.colorize() }
}

private fun supportsHex(): Boolean {
    return try {
        val version = org.bukkit.Bukkit.getServer().javaClass.getPackage().name
            .substringAfterLast(".")
        version >= "v1_16"
    } catch (e: Exception) {
        false
    }
}

private fun translateHexColorCodes(input: String): String {
    val sb = StringBuilder(input.length + 32)
    var lastIndex = 0
    
    for (match in HEX_PATTERN.findAll(input)) {
        sb.append(input, lastIndex, match.range.first)
        
        val hex = match.groupValues[1]
        sb.append(ChatColor.COLOR_CHAR)
        sb.append('x')
        for (char in hex) {
            sb.append(ChatColor.COLOR_CHAR)
            sb.append(char)
        }
        
        lastIndex = match.range.last + 1
    }
    
    sb.append(input, lastIndex, input.length)
    
    return ChatColor.translateAlternateColorCodes('&', sb.toString())
}
