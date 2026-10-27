# SemiAnarchyMC community vote-mutes

This small fork starts from CodingGuru1968/VoteBan commit
`be054cc8237e2d865e458a159bf5142fc8262538` (2.1.1), under the original GPL-3.0
licence. Existing commands, punishment rules, cooldowns and configuration remain.

The MUTE vote task creates a native Bukkit bossbar. Progress is votes received
divided by the minimum count that passes VoteBan's own decision rule, including
its existing percentage rounding and the population captured when voting starts.
The label includes the target's Bukkit display name (the nickname and rank colour
on SemiAnarchyMC), votes, seconds remaining and `/addvote`. The display name is
captured when voting starts; colour resets before the vote count. Chat
announcements and punishment commands retain the actual account name and UUID.
The three vote-mute announcements and added-vote confirmation use
`%player_colour%` from that same display name while `%player%` remains the real
account name. This uses Bukkit colour codes; punishment arguments never receive
presentation formatting.
Time does not control the bar's fill. Completion, plugin disable and reload remove the bar.
There is no additional vote timer, command parser, chat scraping or private API.

Blocked repeat vote-mutes show the time remaining on the existing target cooldown,
rounded up to a whole second (for example, `3m 42s`). The new language key is added
through the normal message loader; existing language overrides remain intact.

With Essentials present, an already-muted target cannot have another mute vote
started against them. Online and linked offline targets use the same existing
Essentials flag and absolute expiry as the Discord bridge. Expired offline flags
do not block votes, and the check never changes or extends a mute.

Build and test with Java 25 and Maven:

```sh
mvn -B -ntp clean verify
```

The shaded artifact is `target/voteban-2.1.1-semianarchy.10.jar`. The homelab
manifest pins its release URL, SHA-256 and source revision. This is a local
enhancement, not a release published or supported by the upstream author.


With EssentialsX, DiscordSRV and LuckPerms present, exact linked Minecraft account
names can be voted on while offline and appear in `/votemute` completion. Unknown
names are rejected. Offline bypass permissions are loaded through LuckPerms'
public API before a vote begins; explicitly denied OP bypasses remain denied.
Minecraft account names and UUIDs remain the punishment identity. Discord display
names are never used to guess which player to punish.

Essentials remains the only mute store. The normal configured mute command uses
the exact account name (this Essentials build does not accept offline UUID command
arguments), and the shared absolute expiry is checked for Discord messages even
if an offline Essentials record still has its expired mute flag. Reasons are
literal text, including dollar signs and backslashes.

The supported DiscordSRV pre-process event blocks relay into Minecraft and uses
the existing bot's Manage Messages permission to delete attempted posts only in
its main bridged server-chat channel. A failed deletion is reported. Other Discord
channels remain usable. No native Discord timeout, permission overwrite, muted
role, extra database, scheduled expiry or second bot is introduced. Posts can be
briefly visible before deletion. If Minecraft/the bridge is offline, Discord
messages are not policed; no persistent Discord restriction can outlive a crash.
When the bridge returns it reads the existing Essentials mute/expiry again. An
intentional permanent staff mute is still honoured, but community votes use the
configured temporary mute. Separately linked alternate identities require
separate votes; there is no attempt to identify a human across accounts.

Public vote-mute reminders and the successful/failed result are mirrored once per
game broadcast into DiscordSRV's existing main server-chat channel. Private vote
confirmations are not relayed. The same language messages supply both outputs;
Minecraft colour codes are stripped, line breaks are retained and Discord
formatting is escaped. Mention notifications are disabled for these notices.
Sending uses the existing bot asynchronously; disconnected/misconfigured Discord
or a failed send is logged without interrupting the in-game vote. There is no new
plugin, credential, channel setting, listener or scheduler.
