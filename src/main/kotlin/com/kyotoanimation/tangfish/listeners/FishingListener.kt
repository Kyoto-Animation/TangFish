/**
 * 钓鱼监听器 - 插件的核心业务逻辑入口
 * 
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 开发者日记                                                   │
 * ├─────────────────────────────────────────────────────────────┤
 * │ 核心职责：拦截原版钓鱼事件，用我们配置的奖池替换钓上来的物品    │
 * │                                                               │
 * │ TODO 可扩展：                                                  │
 * │   - 添加钓鱼经验值加成逻辑                                     │
 * │   - 支持按 biome/世界过滤不同奖池                             │
 * │   - 钓鱼成就系统                                              │
 * │                                                               │
 * │ FIXME 别乱动：                                                 │
 * │   - State.CAUGHT_FISH 的判断顺序，改了可能拦截到其他状态       │
 * │   - ignoreCancelled = true，去掉会导致被其他插件取消的事件     │
 * │     仍然进入我们的处理流程，产生诡异 bug                        │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * @author KyotoAnimation
 */
package com.kyotoanimation.tangfish.listeners

import com.kyotoanimation.tangfish.TangFish
import com.kyotoanimation.tangfish.config.ConfigManager
import com.kyotoanimation.tangfish.config.LanguageManager
import com.kyotoanimation.tangfish.utils.RandomUtils
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class FishingListener : Listener {

    /**
     * 监听玩家钓鱼事件
     * 
     * 为什么用 HIGH 优先级？
     *   - 我们要让其他插件（如保护插件、反作弊）先处理事件
     *   - 如果它们取消了事件，我们就不需要浪费时间处理了
     *   - 但也不能用 LOWEST，否则我们的物品替换可能被其他插件覆盖
     * 
     * 为什么 ignoreCancelled = true？
     *   - 性能优化：已被取消的事件直接跳过，不进入方法体
     *   - 坑点警告：如果这里改成 false，记得在方法开头检查 event.isCancelled
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        // 只处理成功钓到鱼的情况
        // 坑点：PlayerFishEvent 有很多状态（FISHING, CAUGHT_FISH, CAUGHT_ENTITY, IN_GROUND 等）
        // 如果你只判断 caught != null，会把钓到实体（如鱼竿钩住玩家）也算进去
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        
        // 安全取值：caught 理论上不会为 null，但 API 文档说"可能"
        // 这里的 ?: return 是 Kotlin 式的防御性编程
        val caught = event.caught ?: return
        
        // 只处理物品实体，钓到鱼竿钩住其他实体的情况直接放行
        // 坑点：1.12 版本中，caught 可能是 Item 或其他实体类型
        // 别用 caught as? Item，因为我们需要明确拒绝非 Item 的情况
        if (caught !is Item) return
        
        val drops = ConfigManager.getDrops()
        if (drops.isEmpty()) {
            // 配置为空时走原版逻辑，不做任何干扰
            // 这样即使配置文件出问题，玩家至少还能正常钓鱼
            TangFish.debug("No drops configured, using vanilla fishing")
            return
        }
        
        // 权重随机抽取，返回选中的掉落物和随机数值（用于 debug）
        // 坑点：解构声明时如果返回 null，会抛出 NoSuchElementException
        // 所以这里用 ?: run { return } 来安全处理
        val (selectedDrop, randomValue) = RandomUtils.selectWeightedDrop(drops) ?: run {
            TangFish.debug("Weighted selection returned null")
            return
        }
        
        // 将配置转换为实际的物品堆
        // 坑点：XMaterial.parseMaterial() 在某些版本可能返回 null
        // 比如 "BEDROCK" 在创造模式拿不到，或者 "UNKNOWN_MATERIAL"
        // FishingDrop.toItemStack() 内部已经做了缓存和空值处理
        val newItem = selectedDrop.toItemStack()
        if (newItem == null) {
            TangFish.debug("Failed to create ItemStack for: ${selectedDrop.id}")
            return
        }
        
        // 核心操作：替换钓上来的物品
        // 这里直接修改 caught 实体的 itemStack，不需要 spawn 新实体
        // 好处：保留原版的拾取动画和音效，玩家体验更自然
        caught.itemStack = newItem
        
        // Debug 模式下给玩家发送调试信息
        // 为什么用 displayName 而不是 id？因为 displayName 更友好
        // 但如果没配置 displayName，就 fallback 到 id
        val displayName = selectedDrop.displayName ?: selectedDrop.id
        TangFish.debug("Player ${event.player.name} caught: $displayName (random: ${String.format("%.4f", randomValue)})")
        
        // 如果开启了 debug 模式，给玩家也发一条消息
        // 这样服主测试时能实时看到权重算法是否正常工作
        if (TangFish.isDebugEnabled()) {
            event.player.sendMessage(
                LanguageManager.getMessage("debug.caught", 
                    "random" to String.format("%.4f", randomValue),
                    "item" to displayName
                )
            )
        }
    }
}
