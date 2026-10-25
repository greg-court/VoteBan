package com.codingguru.voteban.scheduler;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import com.codingguru.voteban.VoteBan;
import com.codingguru.voteban.handlers.VoteHandler;
import com.codingguru.voteban.util.LangDefaults;
import com.codingguru.voteban.util.MessageBuilder;
import com.google.common.collect.Lists;

public class StartVoteTask extends Schedule {

	private final UUID playerUUID;
	private final String playerName;
	private final String playerDisplayName;
	private final String playerNameColour;
	private final String reason;
	private final VoteType voteType;
	private List<UUID> playersVoted;
	private int playersOnline;
	private int countdown;
	private final int requiredVotes;
	private final BossBar bossBar;

	public StartVoteTask(VoteBan plugin, Player target, Player sender, String reason, VoteType voteType,
			boolean addVote) {
		this(plugin, target.getUniqueId(), target.getName(), target.getDisplayName(), sender, reason, voteType, addVote);
	}

	public StartVoteTask(VoteBan plugin, UUID targetId, String accountName, String displayName, Player sender,
			String reason, VoteType voteType, boolean addVote) {
		super(plugin);
		VoteHandler.getInstance().setActiveVote(this);
		this.reason = reason == null
				? new MessageBuilder.Builder("no-ban-reason", LangDefaults.NO_BAN_REASON).buildString()
				: reason;
		this.playerUUID = targetId;
		this.playerName = accountName;
		// Show the nickname in the bar only; account name/UUID still own voting and punishment.
		this.playerDisplayName = displayName;
		this.playerNameColour = ChatColor.getLastColors(displayName);
		this.voteType = voteType;
		this.playersVoted = Lists.newArrayList();
		this.countdown = voteType.getCountdown();
		this.playersOnline = Bukkit.getOnlinePlayers().size();
		this.requiredVotes = voteType.getVoteCalculatorType()
				.requiredVotes(voteType.getMinVotes(), playersOnline);
		// Native API bars work independently of the vanilla /bossbar command.
		this.bossBar = voteType == VoteType.MUTE
				? Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID) : null;

		if (voteType.isStoppingChat()) {
			if (!voteType.stopChatRequiresPermission()
					|| (sender.hasPermission("voteban.*") || sender.hasPermission("voteban.stopchat"))) {
				VoteHandler.getInstance().setChatDisabled(true);
			}
		}

		if (addVote) {
			addVote(sender);
		}
		if (VoteHandler.getInstance().getActiveVote() == this) updateBossBar();
	}

	@Override
	public void run() {
		updateBossBar();
		if (isBroadcastingTime(countdown)) {
			new MessageBuilder.Builder(voteType.getBroadcastMessagePath(), getDefaultBroadcastMessage())
					.set("%player_colour%", playerNameColour).set("%player%", playerName)
					.set("%timeleft%", String.valueOf(countdown)).set("%reason%", reason)
					.broadcast();
		}

		if (countdown == 0) {
			completeTask();
			return;
		}

		countdown--;
	}

	private void completeTask() {
		VoteHandler.getInstance().addAlreadyVotedPlayer(playerUUID, voteType);
		VoteHandler.getInstance().setActiveVote(null);
		VoteHandler.getInstance().setChatDisabled(false);

		boolean isSuccessful = voteType.getVoteCalculatorType().isSuccessful(voteType, getVotes(), playersOnline);

		if (isSuccessful) {
			voteType.execute(playerUUID, playerName, reason);
			new MessageBuilder.Builder(voteType.getSuccessfulMessagePath(), getDefaultSuccessfulMessage())
					.set("%player_colour%", playerNameColour).set("%player%", playerName)
					.set("%timeleft%", String.valueOf(countdown)).set("%reason%", reason)
					.set("%votes%", String.valueOf(getVotes())).broadcast();
		} else {
			new MessageBuilder.Builder(voteType.getFailedMessagePath(), getDefaultFailedMessage())
					.set("%player_colour%", playerNameColour).set("%player%", playerName)
					.set("%timeleft%", String.valueOf(countdown)).set("%reason%", reason)
					.set("%votes%", String.valueOf(getVotes())).broadcast();
		}

		cancel();
	}

	private boolean isBroadcastingTime(int time) {
		return voteType.getAnnouncementTimes().contains(time);
	}

	private boolean isInstant() {
		return voteType.isInstant();
	}

	private String getDefaultBroadcastMessage() {
		switch (voteType) {
		case BAN:
			return LangDefaults.VOTE_BAN_BROADCAST;
		case MUTE:
			return LangDefaults.VOTE_MUTE_BROADCAST;
		case KICK:
		default:
			return LangDefaults.VOTE_KICK_BROADCAST;
		}
	}

	private String getDefaultSuccessfulMessage() {
		switch (voteType) {
		case BAN:
			return LangDefaults.SUCCESSFUL_VOTE_BAN_BROADCAST;
		case MUTE:
			return LangDefaults.SUCCESSFUL_VOTE_MUTE_BROADCAST;
		case KICK:
		default:
			return LangDefaults.SUCCESSFUL_VOTE_KICK_BROADCAST;
		}
	}

	private String getDefaultFailedMessage() {
		switch (voteType) {
		case BAN:
			return LangDefaults.FAILED_VOTE_BAN_BROADCAST;
		case MUTE:
			return LangDefaults.FAILED_VOTE_MUTE_BROADCAST;
		case KICK:
		default:
			return LangDefaults.FAILED_VOTE_KICK_BROADCAST;
		}
	}

	private int getMinimumRequiredVotes() {
		return voteType.getMinVotes();
	}

	private int getVotes() {
		return playersVoted.size();
	}

	public boolean canVote(Player sender) {
		return !playersVoted.contains(sender.getUniqueId());
	}

	public void addVote(Player sender) {
		playersVoted.add(sender.getUniqueId());
		updateBossBar();

		new MessageBuilder.Builder("successful-vote", LangDefaults.SUCCESSFUL_VOTE)
				.set("%player_colour%", playerNameColour).set("%player%", playerName)
				.send(sender);

		if (voteType.isAnnouncingVotes()) {
			new MessageBuilder.Builder("vote-added", LangDefaults.VOTE_ADDED).set("%votedplayer%", sender.getName())
					.set("%target%", playerName).set("%type%", voteType.toString()).broadcast();
		}

		if (getVotes() >= getMinimumRequiredVotes() && isInstant()) {
			completeTask();
		}
	}

	private void updateBossBar() {
		if (bossBar == null) return;
		bossBar.setTitle("Mute " + playerDisplayName + ChatColor.RESET + " · " + getVotes() + "/" + requiredVotes
				+ " votes · " + countdown + "s left · /addvote");
		bossBar.setProgress(Math.min(1.0, getVotes() / (double) Math.max(1, requiredVotes)));
		for (Player viewer : bossBar.getPlayers()) {
			if (!viewer.isOnline()) bossBar.removePlayer(viewer);
		}
		for (Player viewer : Bukkit.getOnlinePlayers()) {
			if (!bossBar.getPlayers().contains(viewer)) bossBar.addPlayer(viewer);
		}
	}

	@Override
	public void cancel() {
		if (bossBar != null) bossBar.removeAll();
		super.cancel();
	}
}
