package com.palyrobotics.frc2017.util.logger;

import java.util.logging.Level;

/**
 * Created by Joseph on 8/20/17.
 * Replacing TimestamptedString with support for logger levels
 */
public class LeveledString implements Comparable<LeveledString> {
	private String mString;
	private long mTime;
	protected Level mLevel;

	public LeveledString(Level level, String string) {
		mString = string;
		mTime = System.currentTimeMillis();
		mLevel = level;
	}

	public Level getLevel() {
		return mLevel;
	}
	
	public long getTimestamp() {
		return mTime;
	}

	/**
	 * Converts the millisecond timestamp to seconds
	 * @return
	 */
	public String getTimestampedString() {
		return (mTime/1000)+": "+mString+"\n";
	}
	
	public String getLeveledString() {
		return mLevel.toString() + ": " + getTimestampedString();
	}

	@Override
	public String toString() {
		return getLeveledString();
	}

	@Override
	public int compareTo(LeveledString o) {
		return Long.compare(this.getTimestamp(), o.getTimestamp());
	}
}