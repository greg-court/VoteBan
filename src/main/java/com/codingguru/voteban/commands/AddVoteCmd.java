package com.codingguru.voteban.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.codingguru.voteban.VoteBan;
import com.codingguru.voteban.handlers.VoteHandler;
import com.codingguru.voteban.scheduler.Schedule;
import com.codingguru.voteban.scheduler.StartVoteTask;
import com.codingguru.voteban.util.LangDefaults;
import com.codingguru.voteban.util.MessageBuilder;

public class AddVoteCmd implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
		if (!(sender instanceof Player player)) {
			new MessageBuilder.Builder("in-game-only", LangDefaults.IN_GAME_ONLY).send(sender);
			return false;
		}

		if (!sender.hasPermission("voteban.*") && !sender.hasPermission("voteban.vote")) {
			new MessageBuilder.Builder("no-permission", LangDefaults.NO_PERMISSION).send(sender);
			return false;
		}

		VoteHandler handler = VoteHandler.getInstance();
		StartVoteTask vote = handler.getActiveVote();
		if (vote == null) {
			new MessageBuilder.Builder("not-active-vote", LangDefaults.NOT_ACTIVE_VOTE).send(sender);
			return false;
		}

		new Schedule(VoteBan.getInstance()) {
			@Override
			public void run() {
				if (!player.isOnline()) return;
				if (handler.getActiveVote() == vote) {
					vote.addVote(player);
				} else {
					new MessageBuilder.Builder("not-active-vote", LangDefaults.NOT_ACTIVE_VOTE).send(player);
				}
			}
		}.runTask();
		return false;
	}
}
