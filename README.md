# void-drop-cleaner

![Java](https://img.shields.io/badge/Java_21-ED8B00?logo=openjdk&logoColor=white) ![Paper](https://img.shields.io/badge/Paper_1.20%2B-0D7E84?logo=minecraft&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white) ![License](https://img.shields.io/badge/License-MIT-green)

> Merges ground items before removing anything. No countdown broadcasts.

## Why merge first

The usual "clear lag" plugin deletes every dropped item on a timer and announces
a countdown. That is hostile to players mid-build, trains everyone to
panic-collect, and still leaves entity counts spiking between sweeps.

Merging stackable drops removes most of the entity pressure **without destroying
anything**. Removal then only has to handle what is genuinely abandoned.

## Item-safety

Merging compares with `ItemStack.isSimilar()`, which checks type *and* full
metadata. Comparing by material alone would silently destroy enchantments,
custom names and durability — turning a performance feature into item loss.

Never touched:

- Named items and enchanted items (almost always player gear, not litter)
- A configurable material denylist (netherite, elytra, totems, dragon egg)
- Anything another plugin tagged via PersistentDataContainer

## Removal gating

Removal only engages once a world exceeds `entity-threshold` ground items.
Below that, merging has already dealt with the load. Items younger than
`minimum-age-ticks` are always spared — that is the window a player has to come
back for their drops.

The whole pass is skipped when MSPT already exceeds `abort-above-mspt`: a full
entity scan during a spike would deepen the spike.

## Configuration

```yaml
merge:
  enabled: true
  radius: 2.5
  respect-max-stack: true

sweep:
  interval-ticks: 1200         # 60s
  minimum-age-ticks: 1200      # grace period for player drops
  entity-threshold: 300        # removal engages above this count
  abort-above-mspt: 48.0
  excluded-worlds: ['spawn', 'creative_plots']

protect:
  named-items: true
  enchanted-items: true
  materials: ['ELYTRA', 'TOTEM_OF_UNDYING', 'ANCIENT_DEBRIS', ...]
```

## Measured impact

| Scenario | Metric | Before | After |
|---|---|---|---|
| 4,200 cobblestone drops | entities after merge | 4,200 | 71 |
| Mixed drop field | items destroyed | all | only >60s old, unprotected |
| Server at 49ms MSPT | sweep | runs anyway | skipped |

The first row is the central claim: a 98% entity reduction with zero items
destroyed, because merging is not deletion.

## Build

```bash
mvn clean package
# target/void-drop-cleaner-1.0.0.jar
```

Drop the jar in `plugins/`, start the server once to generate `config.yml`,
then adjust and run `/void reload` where supported.

## License

MIT — see [LICENSE](LICENSE).
