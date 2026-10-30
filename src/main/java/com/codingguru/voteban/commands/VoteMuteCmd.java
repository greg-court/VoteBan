package com.codingguru.voteban.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.util.Tristate;

import com.codingguru.voteban.VoteBan;
import com.earth2me.essentials.Essentials;
import com.codingguru.voteban.util.MuteStatus;
import com.codingguru.voteban.handlers.VoteHandler;
import com.codingguru.voteban.scheduler.StartVoteTask;
import com.codingguru.voteban.scheduler.Schedule;
import com.codingguru.voteban.scheduler.VoteType;
import com.codingguru.voteban.util.LangDefaults;
import com.codingguru.voteban.util.MessageBuilder;

public class VoteMuteCmd implements TabExecutor {

	private final VoteBan plugin;

	public VoteMuteCmd(VoteBan plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			new MessageBuilder.Builder("in-game-only", LangDefaults.IN_GAME_ONLY).send(sender);
			return true;
		}
		if (plugin.getConfig().getBoolean("vote-mute.requires-permission") && !sender.hasPermission("voteban.*")
				&& !sender.hasPermission("voteban.startmute")) {
			new MessageBuilder.Builder("no-permission", LangDefaults.NO_PERMISSION).send(sender);
			return false;
		}

		if (!plugin.getConfig().getBoolean("vote-mute.enabled")) {
			new MessageBuilder.Builder("not-enabled", LangDefaults.NOT_ENABLED).send(sender);
			return false;
		}

		if (VoteHandler.getInstance().hasActiveVote()) {
			new MessageBuilder.Builder("active-vote", LangDefaults.ACTIVE_VOTE).send(sender);
			return false;
		}

		if (args.length == 0) {
			new MessageBuilder.Builder("incorrect-usage", LangDefaults.INCORRECT_USAGE)
					.set("%command%", "/votemute <player> [reason]").send(sender);
			return false;
		}

		OfflinePlayer target = Bukkit.getPlayerExact(args[0]);
		if (target == null && plugin.getDiscordMutes() != null) {
			UUID uuid = plugin.getDiscordMutes().linkedPlayers().entrySet().stream()
					.filter(entry -> entry.getKey().equalsIgnoreCase(args[0]))
					.map(java.util.Map.Entry::getValue).findFirst().orElse(null);
			if (uuid != null) target = Bukkit.getOfflinePlayer(uuid);
		}

		if (target == null) {
			new MessageBuilder.Builder("player-not-found", LangDefaults.PLAYER_NOT_FOUND).set("%player%", args[0])
					.send(sender);
			return false;
		}

		if (((Player) sender).getUniqueId().equals(target.getUniqueId())) {
			new MessageBuilder.Builder("cannot-execute-yourself", LangDefaults.CANNOT_EXECUTE_YOURSELF).send(sender);
			return false;
		}

		String reason = args.length == 1 ? null : String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		if (target instanceof Player online) {
			startVote((Player) sender, target, reason,
					online.hasPermission("voteban.*") || online.hasPermission("voteban.bypass")
					|| online.hasPermission("essentials.mute.exempt"));
		} else {
			OfflinePlayer offline = target;
			boolean operator = target.isOp();
			var permissions = LuckPermsProvider.get();
			permissions.getUserManager().loadUser(target.getUniqueId()).whenComplete((user, error) -> {
				if (!plugin.isEnabled()) return;
				new Schedule(plugin) {
					@Override
					public void run() {
						if (error != null) {
							sender.sendMessage("Could not check the target's permissions; vote not started.");
							plugin.getLogger().warning("Offline vote-mute permission lookup failed: " + error);
							return;
						}
						var data = user.getCachedData().getPermissionData(permissions.getContextManager().getStaticQueryOptions());
						var wildcard = data.checkPermission("voteban.*");
						var bypass = data.checkPermission("voteban.bypass");
						boolean exempt = wildcard.asBoolean() || bypass.asBoolean()
								|| data.checkPermission("essentials.mute.exempt").asBoolean()
								|| (operator && (wildcard != Tristate.FALSE || bypass != Tristate.FALSE));
						startVote((Player) sender, offline, reason, exempt);
					}
				}.runTask();
			});
		}
		return true;
	}

	private void startVote(Player sender, OfflinePlayer target, String reason, boolean exempt) {
		// A second command may have started a vote while an offline permission lookup ran.
		if (VoteHandler.getInstance().hasActiveVote()) {
			new MessageBuilder.Builder("active-vote", LangDefaults.ACTIVE_VOTE).send(sender);
			return;
		}
		if (exempt) {
			new MessageBuilder.Builder("cannot-execute-this-player", LangDefaults.CANNOT_EXECUTE_THIS_PLAYER)
					.set("%player%", target.getName()).send(sender);
			return;
		}

		if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
			Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
			var user = essentials.getUser(target.getUniqueId());
			if (user != null && MuteStatus.isActive(user.isMuted(), user.getMuteTimeout(), System.currentTimeMillis())) {
				new MessageBuilder.Builder("already-muted", LangDefaults.ALREADY_MUTED)
						.set("%player%", target.getName()).send(sender);
				return;
			}
		}

		if (!VoteHandler.getInstance().isVoteAllowed(VoteType.MUTE, target.getUniqueId())) {
			new MessageBuilder.Builder("vote-mute-cooldown", LangDefaults.VOTE_MUTE_COOLDOWN)
					.set("%player%", target.getName())
					.set("%time%", VoteHandler.getInstance().getRemainingCooldown(target.getUniqueId())).send(sender);
			return;
		}

		boolean addVote = plugin.getConfig().getBoolean("vote-mute.automatically-add-vote");

		String display = target instanceof Player online ? online.getDisplayName()
				: plugin.getDiscordMutes().displayName(target.getUniqueId(), target.getName());
		StartVoteTask muteTask = new StartVoteTask(plugin, target.getUniqueId(), target.getName(), display,
				sender, reason, VoteType.MUTE, addVote);
		muteTask.runTaskAtFixedRate(20);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length != 1 || (!sender.hasPermission("voteban.*") && !sender.hasPermission("voteban.startmute")))
			return List.of();
		List<String> names = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!(sender instanceof Player viewer) || viewer.canSee(player)) names.add(player.getName());
		}
		if (plugin.getDiscordMutes() != null) names.addAll(plugin.getDiscordMutes().linkedPlayers().keySet());
		return names.stream().filter(name -> name.regionMatches(true, 0, args[0], 0, args[0].length()))
				.distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
	}
}
