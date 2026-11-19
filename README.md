# 铜铁合金 · 群峦版 / Copper-Iron Alloy (TFC)

Minecraft 1.20.1 Forge mod for TerraFirmaCraft 3.2 or newer. Forge 47 or newer.

给群峦传说加铁铜合金、钢金、脆白钢和白钢。含工具、盔甲、镀块、剪刀、渔具、灯、容器和冰箱。金属物品可以回炉。白钢单台是翻盖冰柜，两台叠放变成停止食物腐败的竖式冰箱。脆白钢冰箱是紫色前开门柜子，不能叠放。

## Build / 构建

JDK 17 and Gradle 8.10. Put `TerraFirmaCraft-Forge-1.20.1-3.2.12.jar` in `libs/`. Do not commit that jar.

需要 JDK 17 和 Gradle 8.10。把 `TerraFirmaCraft-Forge-1.20.1-3.2.12.jar` 放到 `libs/`。不要把这个 jar 提交进仓库。

```
gradle build
```

The release jar in this folder is `copper_iron_alloy_tfc-1.0.0.jar`.

成品是本目录里的 `copper_iron_alloy_tfc-1.0.0.jar`。

License: MIT-0 (MIT No Attribution)

## 声明 / Credits

- 创意、数值与美术方向：**U431RT**
- 代码与资源生成：**AI**

## 更新日志 / Changelog

### v1.0.1
- 修复：合金桶、小杯倒液体不再放置水源/岩浆源，改为短时间流动后消失（与群峦"不足一桶"的处理一致）。
  新增配置 `fluids.place_fluid_sources`（默认 false），需要放源方块时打开。
- 新增：白钢薄板可在 tier 6 铁砧上锻造出原版铁桶（原版桶仍能放置水源）。
- 新增配方：`anvil/white_steel_iron_bucket`。

### v1.0.0
- 首个版本。

## 许可证 / License

**MIT-0（MIT No Attribution）** —— 完全放开：可自由使用、修改、再分发、商用，**无需署名**。
详见仓库根目录 `LICENSE`。
