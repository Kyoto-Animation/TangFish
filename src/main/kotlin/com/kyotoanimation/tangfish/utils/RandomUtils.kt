/**
 * 权重随机工具 - 高性能的奖池抽取算法
 * 
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 开发者日记                                                   │
 * ├─────────────────────────────────────────────────────────────┤
 * │ 核心职责：根据配置的权重值，公平地从奖池中抽取一个物品         │
 * │                                                               │
 * │ 算法原理：                                                    │
 * │   假设有三个物品 A(50), B(30), C(20)，总权重 100              │
 * │   生成随机数 [0, 100)，比如 65                               │
 * │   累加判断：A 区间 [0,50), B 区间 [50,80), C 区间 [80,100)    │
 * │   65 落在 B 区间，所以选中 B                                  │
 * │                                                               │
 * │ TODO 可扩展：                                                  │
 * │   - 添加保底机制（连续 N 次未中稀有物品后提升概率）            │
 * │   - 支持动态权重（根据玩家 VIP 等级调整）                      │
 * │   - 添加奖池预热缓存（避免每次 sumOf 计算总权重）              │
 * │                                                               │
 * │ FIXME 别乱动：                                                 │
 * │   - ThreadLocalRandom 是线程安全的，别换成 Random             │
 * │   - 浮点数比较用 < 而不是 <=，边界情况会出问题                │
 * │   - fallback 到最后一个元素是必要的，防止浮点精度问题         │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * @author KyotoAnimation
 */
package com.kyotoanimation.tangfish.utils

import com.kyotoanimation.tangfish.TangFish
import com.kyotoanimation.tangfish.models.FishingDrop
import java.util.concurrent.ThreadLocalRandom

object RandomUtils {

    /**
     * 根据权重随机选择一个掉落物
     * 
     * @param drops 掉落物列表
     * @return Pair<选中的掉落物, 产生的随机数>，如果列表为空或总权重<=0 则返回 null
     * 
     * 为什么返回随机数？
     *   - Debug 时需要知道"为什么选了这个"
     *   - 可以用于日志分析和概率验证
     * 
     * 坑点警告：
     *   - 每次调用都会重新计算总权重，如果奖池很大（100+物品）会有性能损耗
     *   - 如果需要极致性能，可以在 ConfigManager 里缓存总权重
     */
    fun selectWeightedDrop(drops: List<FishingDrop>): Pair<FishingDrop, Double>? {
        if (drops.isEmpty()) return null
        
        // 计算总权重
        // 坑点：如果配置文件里所有 chance 都是 0，总权重就是 0
        // 这时候 nextDouble(0) 会抛出 IllegalArgumentException
        val totalWeight = drops.sumOf { it.chance }
        if (totalWeight <= 0) return null
        
        // 使用 ThreadLocalRandom 而不是 Random
        // 原因：多线程环境下 Random 性能差（有锁竞争）
        // ThreadLocalRandom 每个线程有自己的随机数生成器，无锁
        val random = ThreadLocalRandom.current().nextDouble(totalWeight)
        
        // 累加权重，找到随机数落入的区间
        // 这里的算法时间复杂度是 O(n)，对于小奖池（<50物品）完全够用
        // 如果奖池超大，可以考虑用 Alias Method 优化到 O(1)
        var currentWeight = 0.0
        for (drop in drops) {
            currentWeight += drop.chance
            // 坑点：这里用 < 而不是 <=
            // 假设 random = 50.0，第一个物品权重也是 50.0
            // 用 < 会选中第一个，用 <= 会选中第二个
            // 两种都对，但必须和你的理解一致
            if (random < currentWeight) {
                TangFish.debug("Weighted selection - Random: ${String.format("%.4f", random)} / Total: ${String.format("%.2f", totalWeight)}, Selected: ${drop.id}")
                return drop to random
            }
        }
        
        // 理论上不应该走到这里，但浮点数运算有精度问题
        // 比如 random = 99.999999999，总权重 = 100.0
        // 累加到最后可能 currentWeight = 99.999999998，比 random 小
        // 这时候就需要 fallback 到最后一个元素
        // 
        // 坑点：如果你删掉这个 fallback，极端情况下会返回 null
        // 然后调用方解构声明会崩溃
        val lastDrop = drops.last()
        TangFish.debug("Weighted selection - Random: ${String.format("%.4f", random)} / Total: ${String.format("%.2f", totalWeight)}, Selected: ${lastDrop.id} (fallback)")
        return lastDrop to random
    }

    /**
     * 根据权重随机选择，返回索引而不是对象
     * 
     * 这个方法存在的意义：
     *   - 有时候你需要知道"选中的是第几个"，而不是"选中的是什么"
     *   - 比如要做"连续抽卡"功能，需要记录每个位置的结果
     * 
     * @return Pair<索引, 随机数>
     */
    fun selectWeightedDropIndex(drops: List<FishingDrop>): Pair<Int, Double>? {
        if (drops.isEmpty()) return null
        
        val totalWeight = drops.sumOf { it.chance }
        if (totalWeight <= 0) return null
        
        val random = ThreadLocalRandom.current().nextDouble(totalWeight)
        
        var currentWeight = 0.0
        drops.forEachIndexed { index, drop ->
            currentWeight += drop.chance
            if (random < currentWeight) {
                TangFish.debug("Weighted selection - Random: ${String.format("%.4f", random)}, Index: $index, Selected: ${drop.id}")
                return index to random
            }
        }
        
        // 同样的 fallback 逻辑
        val lastIndex = drops.lastIndex
        TangFish.debug("Weighted selection - Random: ${String.format("%.4f", random)}, Index: $lastIndex, Selected: ${drops[lastIndex].id} (fallback)")
        return lastIndex to random
    }

    /**
     * 计算单个物品的"真实概率"百分比
     * 
     * 注意：这个方法返回的是理论概率，不是实际统计概率
     * 如果你要做"概率公示"功能，用这个方法
     * 
     * @param drop 单个掉落物配置
     * @param totalWeight 总权重（需要外部传入，避免重复计算）
     * @return 概率百分比，如 25.5 表示 25.5%
     */
    fun calculateDropChance(drop: FishingDrop, totalWeight: Double): Double {
        if (totalWeight <= 0) return 0.0
        // 乘以 100 转成百分比
        // 坑点：别在配置文件里写百分比（如 50%），这里会变成 5000%
        // 配置文件里的 chance 应该是"权重值"而不是"百分比"
        return (drop.chance / totalWeight) * 100
    }
}
