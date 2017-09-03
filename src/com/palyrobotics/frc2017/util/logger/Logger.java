package com.palyrobotics.frc2017.util.logger;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.palyrobotics.frc2017.config.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Log is at /home/lvuser/logs/fileName directory
 * Unit test safe, creates diff file directory for Mac/Windows/Linux
 * fileName defaults to ex: "Mar13 13-29" using 24-hr-time
 * Can set desired filename manually
 * If log file exists on first start, automatically creates new file
 *
 * If run on roboRIO, will attempt to copy the driverstation console log to this directory to save it
 * No longer uses bufferedwriter, uses Guava Files to append to file
 *
 * FYI, buffered writer closes the underlying filewriter and flushes the buffer
 */
public class Logger {
	private static Logger instance = new Logger();
	public static Logger getInstance() {
		return instance;
	}

	private String fileName = null;

	private boolean isEnabled = false;
	
	private ArrayList<LeveledString> mData;
	// Separates to prevent concurrent modification exception
	private ArrayList<LeveledString> mSubsystemThreadLogs = new ArrayList<>();
	private ArrayList<LeveledString> mRobotThreadLogs = new ArrayList<>();
	private ConcurrentLinkedQueue<LeveledString> mUnknownThreadLogs = new ConcurrentLinkedQueue<LeveledString>();

	// synchronized lock for writing out the latest data
	private final Object writingLock = new Object();
	private Thread mWritingThread = null;
	// Stores the runnable for the thread to be restarted
	private Runnable mRunnable;

	private int duplicatePrevent = 0;
	private File mainLog;

	private String os = null;
//	// Finds the driver station console output
//	private File rioLog;

	public boolean setFileName(String fileName) {
		if (mainLog != null) {
			System.err.println("Already created log file");
			return false;
		}
		this.fileName = fileName;
		return true;
	}

	/**
	 * Creates the file at desired filepath, avoids file collision, will not recreate if logger already there
	 */
	public void start() {
		// If initialized before, then recreate the buffered writer and re-enable
		if (mWritingThread != null) {
			isEnabled = true;
			mWritingThread = new Thread(mRunnable);
			mWritingThread.start();
			return;
		}
		Date date = new Date();
//		if (fileName == null) {
//			fileName = new SimpleDateFormat("MMMdd HH-mm").format(date);
//		}
		fileName = fileName + new SimpleDateFormat("MMMdd HH-mm").format(date);
		os = System.getProperty("os.name");
		String filePath;
		if (os.startsWith("Mac")) {
			filePath = "logs" + File.separatorChar + fileName;
		}
		else if (os.startsWith("Windows")) {
			filePath = "C:" + File.separatorChar + "logs" + File.separatorChar + fileName;
		} 
		else if (os.startsWith("Unix")){
			// Pray that this is a roborio
			// TODO: Maybe find the exact OS name
			filePath = "/home/lvuser/logs/" + fileName;
//			// TODO:
//			rioLog = new File("/var/local/natinst/log/FRC_UserProgram.log");
		}
		else {
			filePath = "/home/lvuser/logs/" + fileName;
			System.err.println("Unrecognized OS; defaulting to Unix system");
		}
		mainLog = new File(filePath+File.separatorChar+"log.log");
		while (mainLog.exists()) {
			duplicatePrevent++;
			mainLog = new File(filePath+File.separatorChar+"log"+duplicatePrevent+".log");
		}
		try {
			// File header
			Files.createParentDirs(mainLog);
			Files.append("Robot log:"+ "\n", mainLog, Charsets.UTF_8);
			Files.append(date.toString()+ "\n", mainLog, Charsets.UTF_8);
			System.out.println("Created new log at " + filePath);
		} catch (IOException e) {
			System.err.println("Failed to create log at "+filePath);
			e.printStackTrace();
		}

		// Create thread to write out logger
		mWritingThread = new Thread(mRunnable);
		isEnabled = true;
		mWritingThread.start();
	}

	/**
	 * Called on subsystem thread
	 * @param value
	 */
	@Deprecated
	public void logSubsystemThread(Object value) {
		try {
			mSubsystemThreadLogs.add(new LeveledString(Level.INFO, value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on subsystem logger");
		}
	}

	public void logSubsystemThread(Level level, Object value) {
		try {
			mSubsystemThreadLogs.add(new LeveledString(level, value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on subsystem logger");
		}
	}
	
	/**
	 * Called on subsystem thread
	 * @param key
	 * @param value will call .toString()
	 */
	@Deprecated
	public void logSubsystemThread(String key, Object value) {
		try {
			mSubsystemThreadLogs.add(new LeveledString(Level.INFO, key + ": " + value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on subsystem logger");
		}
	}
	
	public void logSubsystemThread(Level level, String key, Object value) {
		try {
			mSubsystemThreadLogs.add(new LeveledString(level, key + ": " + value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on subsystem logger");
		}
	}
	
	/**
	 * Called on robot thread
	 * @param value
	 */
	@Deprecated
	public void logRobotThread(Object value) {
		try {
			mRobotThreadLogs.add(new LeveledString(Level.INFO, value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on robot logger");
		}
	}

	public void logRobotThread(Level level, Object value) {
		try {
			mRobotThreadLogs.add(new LeveledString(level, value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on robot logger");
		}
	}
	
	/**
	 * Called on robot thread
	 * @param key will be paired with the object
	 * @param value will call .toString()
	 */
	@Deprecated
	public void logRobotThread(String key, Object value) {
		try {
			mRobotThreadLogs.add(new LeveledString(Level.INFO, key + ": " + value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on robot logger");
		}
	}
	
	public void logRobotThread(Level level, String key, Object value) {
		try {
			mRobotThreadLogs.add(new LeveledString(level, key + ": " + value.toString()));
		} catch (ConcurrentModificationException e) {
			System.err.println("Attempted concurrent modification on robot logger");
		}
	}
	
	public synchronized void cleanup() {
		mWritingThread.interrupt();
	}
	private Logger() {
		mData = new ArrayList<>();
		mRunnable = () -> {
			while (true) {
					synchronized (writingLock) {
						if (isEnabled) {
							mData = new ArrayList<>(mRobotThreadLogs);
							mData.addAll(mSubsystemThreadLogs);
							mData.sort(LeveledString::compareTo);
							mSubsystemThreadLogs.clear();
							mRobotThreadLogs.clear();
							mData.forEach((LeveledString c) -> {
								if(Constants.writeLevel.intValue() <= c.getLevel().intValue()) {
									if(Constants.displayLevel.intValue() <= c.getLevel().intValue()){
										System.out.println(c.toString());
									}
									try {
										Files.append(c.getLeveledString(), mainLog, Charsets.UTF_8);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							});
							mData.clear();
						}
					}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e){
					shutdown();
					return;
				}
				// If thread is interrupted, cleanup
				if (Thread.currentThread().isInterrupted()) {
					shutdown();
					return;
				}
			}
		};
	}

	public String getLogPath() {
		if (mainLog != null) {
			return mainLog.getAbsolutePath();
		} else {
			return "NoLogYet";
		}
	}
	
	/**
	 * Sends local file to driver station
	 * @param localFile the log file
	 * @return successful
	 */
	private boolean sendLog(File localFile) {
		return localFile != null;
	}

	// Used to cleanup internally, write out last words, etc
	private synchronized void shutdown() {
		System.out.println("Shutting down");
		synchronized (writingLock) {
			mData = new ArrayList<>(mRobotThreadLogs);
			mData.addAll(mSubsystemThreadLogs);
			mData.sort(LeveledString::compareTo);
			mData.forEach((LeveledString c) -> {
				if(Constants.writeLevel.intValue() <= c.getLevel().intValue()) {
					if(Constants.displayLevel.intValue() <= c.getLevel().intValue()){
						System.out.println(c.toString());
					}
					try {
						Files.append(c.getLeveledString(), mainLog, Charsets.UTF_8);
					} catch (IOException e) {
						System.out.println("Unable to write last strings");
						e.printStackTrace();
					}
				}
			});
			mRobotThreadLogs.clear();
			mSubsystemThreadLogs.clear();
			mData.clear();
			try {
				Files.append("Logger stopped \n", mainLog, Charsets.UTF_8);
			} catch (IOException e) {
				System.out.println("Unable to write, logger stopped");
				e.printStackTrace();
			}
			isEnabled = false;
		}
		if(os.startsWith("Unix")) {
			sendLog(mainLog);
		}
//		// Try to copy riolog to logging directory if it exists
//		if (rioLog != null) {
//			try {
//				Files.copy(rioLog, new File("/home/lvuser/logs/" + fileName+"/riolog"+duplicatePrevent+".log"));
//			} catch (IOException e) {
//				System.out.println("Unable to copy riolog");
//				e.printStackTrace();
//			}
//		}
	}
}