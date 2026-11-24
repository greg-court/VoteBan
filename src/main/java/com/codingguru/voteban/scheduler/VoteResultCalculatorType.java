package com.codingguru.voteban.scheduler;

public enum VoteResultCalculatorType {

	MIN_VOTES,
	SERVER_PERCENTAGE;

	public static VoteResultCalculatorType getVoteCalculatorType(String name) {
		for (VoteResultCalculatorType type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return MIN_VOTES;
	}

	public boolean isSuccessful(VoteType voteType, int votes, int playersOnline) {
		return isSuccessful(voteType.getMinVotes(), votes, playersOnline);
	}

	boolean isSuccessful(int minimum, int votes, int playersOnline) {
		switch (this) {
		case MIN_VOTES:
			return votes >= minimum;
		case SERVER_PERCENTAGE:
			return playersOnline > 0 && calculatePercentage(votes, playersOnline) >= minimum;
		}
		return false;
	}

	// Match the final decision rule, including its percentage rounding.
	int requiredVotes(int minimum, int playersOnline) {
		if (this == MIN_VOTES) return minimum;
		for (int votes = 0; votes <= playersOnline; votes++) {
			if (isSuccessful(minimum, votes, playersOnline)) return votes;
		}
		return playersOnline + 1;
	}

	public int calculatePercentage(double obtained, double total) {
		return (int) Math.round(obtained * 100 / total);
	}
	
	@Override
	public String toString() {
		return name().toLowerCase().replaceAll("_", "-");
	}

}
