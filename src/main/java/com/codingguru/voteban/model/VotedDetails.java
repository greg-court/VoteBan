package com.codingguru.voteban.model;

import com.codingguru.voteban.scheduler.VoteType;

public class VotedDetails {

	private final long endTime;
	private final VoteType voteType;

	public VotedDetails(long endTime, VoteType voteType) {
		this.endTime = endTime;
		this.voteType = voteType;
	}

	public VoteType getVoteType() {
		return voteType;
	}

	public long getEndTime() {
		return endTime;
	}

	public String getRemainingTime(long now) {
		long seconds = Math.max(0, Math.ceilDiv(endTime - now, 1000));
		return seconds >= 60 ? seconds / 60 + "m " + seconds % 60 + "s" : seconds + "s";
	}

}
