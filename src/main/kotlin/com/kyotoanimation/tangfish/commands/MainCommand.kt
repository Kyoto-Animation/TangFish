/**
 * 主命令处理器 - 玩家与插件交互的入口
 * 
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 开发者日记                                                   │
 * ├─────────────────────────────────────────────────────────────┤
 * │ 核心职责：处理 /tangfish (别名 /tf) 命令及其子命令            │
 * │                                                               │
 * │ 已实现子命令：                                                │
 * │   - reload: 重载配置和语言文件（需要 tangfish.admin）         │
 * │   - list/pool: 显示奖池列表（需要 tangfish.user）            │
 * │   - help: 显示帮助菜单                                       │
 * │                                                               │
 * │ TODO 可扩展：                                                  │
 * │   - add <id>: 动态添加奖池物品                                │
 * │   - remove <id>: 动态移除奖池物品                             │
 * │   - give <player> <id>: 给玩家指定物品                        │
 * │   - test <times>: 模拟抽奖测试概率分布                        │
 * │                                                               │
 * │ FIXME 别乱动：                                                 │
 * │   - TabExecutor 接口的两个方法签名，参数类型是 Array<out String>│
 * │   - 权限检查必须放在业务逻辑之前，否则会有安全漏洞            │
 * │   - buildString 里的 colorize() 调用位置，提前处理会丢失颜色  │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * @author KyotoAnimation
 */
package com.kyotoanimation.tangfish.commands

import com.kyotoanimation.tangfish.TangFish
import com.kyotoanimation.tangfish.config.ConfigManager
import com.kyotoanimation.tangfish.config.LanguageManager
import com.kyotoanimation.tangfish.utils.colorize
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.util.StringUtil

class MainCommand : TabExecutor {

    // 子命令列表，用于 Tab 补全
    // 坑点：如果要添加新子命令，记得同步更新这个列表
    // 不然 Tab 补全不会显示新命令（但命令本身能执行）
    private val subCommands = listOf("reload", "list", "pool", "help")

    /**
     * 命令执行入口
     * 
     * 返回值含义：
     *   - true: 命令执行成功，不显示 usage
     *   - false: 命令格式错误，会显示 plugin.yml 里的 usage
     * 
     * 坑点：args 可能为空数组（玩家只输入 /tangfish）
     * 这时候应该显示帮助而不是返回 false
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // 无参数时显示帮助
        // 这是命令设计的最佳实践：让玩家知道有哪些功能可用
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        // 分发到具体的子命令处理器
        // 坑点：args[0] 可能是大写或混合大小写，必须 lowercase()
        // 不然 /TF RELOAD 会被当成未知命令
        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "list", "pool" -> handleList(sender)  // 两个别名指向同一个功能
            "help" -> sendHelp(sender)
            else -> sender.sendMessage(LanguageManager.getMessage("error.invalid_command").colorize())
        }

        return true
    }

    /**
     * Tab 补全
     * 
     * 为什么用 StringUtil.copyPartialMatches？
     *   - 这是 Bukkit 提供的工具方法，专门用于命令补全
     *   - 它会过滤出以当前输入开头的候选项
     *   - 比如输入 "re"，会返回 ["reload"]
     * 
     * 坑点：返回值必须是可变集合（MutableList）
     * 不然某些 Bukkit 版本会抛出 UnsupportedOperationException
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        // 只对第一个参数做补全（子命令名）
        // 后续参数的补全需要根据子命令类型来处理，这里暂时返回空
        if (args.size == 1) {
            return StringUtil.copyPartialMatches(args[0], subCommands, mutableListOf())
        }
        // TODO: 根据子命令类型提供更智能的补全
        // 比如 /tf give <player> <id>，第二个参数补全在线玩家，第三个补全奖池 ID
        return emptyList()
    }

    /**
     * 处理重载命令
     * 
     * 设计考量：
     *   - 权限检查必须在 try-catch 之前
     *   - 不然无权限的玩家触发异常时，异常信息可能泄露配置细节
     */
    private fun handleReload(sender: CommandSender) {
        // 权限检查：只有管理员能重载配置
        // 坑点：权限节点字符串必须和 plugin.yml 里定义的一致
        // 建议用常量类统一管理权限节点，避免硬编码
        if (!sender.hasPermission("tangfish.admin")) {
            sender.sendMessage(LanguageManager.getMessage("error.no_permission"))
            return
        }

        try {
            // 重载顺序很重要：先配置，后语言
            // 因为语言文件可能依赖配置中的 language 字段
            ConfigManager.reload()
            LanguageManager.reload()
            sender.sendMessage(LanguageManager.getMessage("reload.success"))
            TangFish.debug("Configuration reloaded by ${sender.name}")
        } catch (e: Exception) {
            // 捕获所有异常，避免把堆栈信息暴露给玩家
            // 坑点：sender 可能是控制台，getName() 可能返回 "CONSOLE"
            sender.sendMessage(LanguageManager.getMessage("reload.failed"))
            // 详细错误只输出到日志，管理员可以去查
            TangFish.instance.logger.warning("Failed to reload configuration: ${e.message}")
        }
    }

    /**
     * 处理奖池列表命令
     * 
     * 性能考量：
     *   - 这里显示的是"假概率"（配置文件里的 chance 值）
     *   - 不做实时权重计算，因为奖池可能很大
     *   - 如果要显示"真实概率"，需要遍历所有物品计算总权重
     * 
     * 为什么用 buildString？
     *   - 比用 + 拼接字符串性能好（减少中间对象创建）
     *   - 比用 StringBuilder 更 Kotlin 风格
     *   - 代码可读性也更高
     */
    private fun handleList(sender: CommandSender) {
        if (!sender.hasPermission("tangfish.user")) {
            sender.sendMessage(LanguageManager.getMessage("error.no_permission"))
            return
        }

        val drops = ConfigManager.getDrops()
        if (drops.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage("fishing.no_drops"))
            return
        }

        // 使用 buildString 构建多行消息
        // 坑点：colorize() 必须在 append 的字符串上调用，不能对整个 buildString 结果调用
        // 因为 buildString 返回的是普通 String，已经没有 & 颜色代码了
        val message = buildString {
            append("&6========== &eTangFish 奖池列表 &6==========".colorize())
            append("\n")
            
            // 遍历奖池，构建列表项
            // 坑点：displayName 可能包含颜色代码，所以要用 ?: fallback 到 id
            // 不然 null 会被显示成 "null"
            drops.forEachIndexed { index, drop ->
                val displayName = drop.displayName ?: drop.id
                // 这里直接显示配置的 chance 值，不做概率计算
                // 原因：1) 性能 2) 用户期望看到的是"我配置的值"
                val chanceStr = String.format("%.1f", drop.chance)
                append(" &7${index + 1}. &f$displayName &8- &a${chanceStr}%".colorize())
                // 换行处理：最后一项后面不加分隔线
                if (index < drops.size - 1) append("\n")
            }
            
            append("\n")
            append("&6=====================================".colorize())
        }

        sender.sendMessage(message)
    }

    /**
     * 发送帮助信息
     * 
     * 为什么单独抽一个方法？
     *   - 复用：无参数时、/tf help 时都要显示
     *   - 可维护：帮助信息的格式统一在这里管理
     */
    private fun sendHelp(sender: CommandSender) {
        // 帮助信息全部走语言文件，方便国际化
        // 坑点：语言文件的换行符需要手动添加，getMessage() 不会自动换行
        val helpMessage = buildString {
            append(LanguageManager.getMessage("help.header"))
            append("\n")
            append(LanguageManager.getMessage("help.reload"))
            append("\n")
            append(LanguageManager.getMessage("help.list"))
            append("\n")
            append(LanguageManager.getMessage("help.info"))
            append("\n")
            append(LanguageManager.getMessage("help.help"))
            append("\n")
            append(LanguageManager.getMessage("help.footer"))
        }
        sender.sendMessage(helpMessage)
    }
}
