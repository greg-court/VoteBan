# SemiAnarchyMC vote-mute display

This small fork starts from CodingGuru1968/VoteBan commit
`be054cc8237e2d865e458a159bf5142fc8262538` (2.1.1), under the original GPL-3.0
licence. Existing commands, punishment rules, cooldowns and configuration remain.

The MUTE vote task creates a native Bukkit bossbar. Progress is votes received
divided by the minimum count that passes VoteBan's own decision rule, including
its existing percentage rounding and the population captured when voting starts.
The label includes the target's Bukkit display name (the nickname and rank colour on
SemiAnarchyMC),
votes, seconds remaining and `/addvote`. The display name is captured when voting starts; colour resets before the vote count.
Chat announcements and punishment commands retain the actual account name and UUID. Time does
not control the bar's fill. Completion, plugin disable and reload remove the bar.
There is no additional vote timer, command parser, chat scraping or private API.

Build and test with Java 25 and Maven:

```sh
mvn -B -ntp clean verify
```

The shaded artifact is `target/voteban-2.1.1-semianarchy.2.jar`. The homelab
manifest pins its release URL, SHA-256 and source revision. This is a local
enhancement, not a release published or supported by the upstream author.
