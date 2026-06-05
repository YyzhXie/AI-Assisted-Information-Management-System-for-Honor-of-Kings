# 综合实力排名公式说明

本文说明玩家排行榜和装备排名中的“综合实力”指标。

## 设计原则

本项目没有直接采用简单加权相加，而是先将各指标归一化，并使用贝叶斯修正处理小样本胜率，避免少量比赛或少量估算使用导致玩家、装备异常靠前。

## 玩家综合实力

公式：

```text
PlayerScore =
100 * (
  0.40 * BayesianWinRate
+ 0.25 * LevelScore
+ 0.25 * MatchVolumeScore
+ 0.10 * HeroDiversityScore
)
```

各项定义：

```text
BayesianWinRate = (wins + 5 * globalWinRate) / (matches + 5)
LevelScore = level / maxLevel
MatchVolumeScore = log(1 + totalMatches) / log(1 + maxTotalMatches)
HeroDiversityScore = ownedHeroCount / maxOwnedHeroCount
```

权重说明：

- 贝叶斯胜率 40%：胜率仍是玩家实力核心，但小样本会被拉回全局平均胜率。
- 等级 25%：等级代表长期成长。
- 对战量 25%：使用对数归一化，避免高对战数无限放大。
- 英雄多样性 10%：拥有英雄越多，说明可选策略更丰富。

平局处理：

```text
综合分高者优先；
若相同，贝叶斯胜率高者优先；
再相同，对战次数多者优先；
再相同，等级高者优先；
最后按玩家 ID 升序，保证排序稳定。
```

## 装备综合实力

公式：

```text
EquipmentScore =
100 * (
  0.35 * BayesianWinRate
+ 0.25 * PopularityScore
+ 0.25 * RatingScore
+ 0.15 * HeroCoverageScore
)
```

各项定义：

```text
RatingScore = rating / 10.0
HeroCoverageScore = compatibleHeroCount / maxCompatibleHeroCount
PopularityScore = log(1 + estimatedUsage) / log(1 + maxEstimatedUsage)
BayesianWinRate = (wins + 5 * globalWinRate) / (matches + 5)
```

权重说明：

- 贝叶斯胜率 35%：体现装备可能带来的胜率贡献，同时抑制小样本偏差。
- 热度/使用估计 25%：用对数压缩估算使用量，避免热门装备直接碾压。
- 装备评分 25%：体现基础评分。
- 适配英雄数量 15%：体现装备泛用性。

## 装备估算口径

本项目没有真实记录“某场比赛玩家实际购买了什么装备”。因此装备胜率贡献是估算值：

```text
若某玩家在比赛中选择的英雄兼容某装备，
则认为该装备可能参与该英雄的推荐出装统计。
```

系统不会声称这是游戏内真实购买数据，而是作为课程项目中的推荐与排名参考指标。
