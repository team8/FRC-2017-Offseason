package com.palyrobotics.frc2017.robot;

import com.palyrobotics.frc2017.behavior.Routine;
import com.palyrobotics.frc2017.behavior.RoutineManager;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.robot.team254.lib.util.CrashTrackingRunnable;
import com.palyrobotics.frc2017.subsystems.*;
import com.palyrobotics.frc2017.util.logger.Logger;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.Timer;

import java.util.logging.Level;

/**
 * This code runs all of the robot's loops. Loop objects are stored in a List
 * object. They are started when the robot powers up and stopped after the
 * match.
 * @author Robbie Selwyn
 */
public class RobotEnclosingThread {
	
	// SubsystemLoop update rate
	private final double kPeriod = Constants.kNormalLoopsDt;

	private RoutineManager mRoutineManager = new RoutineManager();
	private OperatorInterface operatorInterface = OperatorInterface.getInstance();
	
    private final Notifier notifier_;

	private static RobotState robotState = new RobotState();
	public static RobotState getRobotState() {
		return robotState;
	}
	
	// Hardware Updater
	private HardwareUpdater mHardwareUpdater;

	// Commands
	private static Commands commands = Commands.getInstance();
	public static Commands getCommands() {return commands;}

	// Is the thread running?
	private boolean running_;

	private final Object taskRunningLock_ = new Object();
	private double timestamp_ = 0;
	private double dt_ = 0;
	// Main method that is run at the update rate
	private final CrashTrackingRunnable runnable_ = new CrashTrackingRunnable() {
		@Override
		public void runCrashTracked() {
			synchronized (taskRunningLock_) {
				if (running_) {
					double now = Timer.getFPGATimestamp();

					mHardwareUpdater.updateSensors(robotState);
					dt_ = now - timestamp_;
					timestamp_ = now;
					
					if (robotState.gamePeriod == RobotState.GamePeriod.TELEOP) {
						commands = mRoutineManager.update(operatorInterface.updateCommands(commands));
					}
					else {
						commands = mRoutineManager.update(getCommands());
					}
					mHardwareUpdater.updateHardware();
					
					Drive.getInstance().update(getCommands(), getRobotState());
					Slider.getInstance().update(getCommands(), getRobotState());
					Spatula.getInstance().update(getCommands(), getRobotState());
					Climber.getInstance().update(getCommands(), getRobotState());

					logPeriodic();
				}
			}
		}
	};

	public RobotEnclosingThread() {
        notifier_ = new Notifier(runnable_);
		running_ = false;
		
		if (Constants.kRobotName == Constants.RobotName.STEIK) {
			try {
				mHardwareUpdater = new HardwareUpdater(this, Drive.getInstance(), Slider.getInstance(), Spatula.getInstance()
						,Intake.getInstance(), Climber.getInstance());
			} catch (Exception e) {
				System.exit(1);
			}

		} else {
			try {
				mHardwareUpdater = new HardwareUpdater(Drive.getInstance());
			} catch (Exception e) {
				System.exit(1);
			}
		}
		mHardwareUpdater.initHardware();

	}

	public synchronized void start() {
		if (!running_) {
			Logger.getInstance().logRobotThread(Level.INFO, "Starting Loops");
			synchronized (taskRunningLock_) {
				timestamp_ = Timer.getFPGATimestamp();
				mRoutineManager.reset(commands);
				Drive.getInstance().start();
				Slider.getInstance().start();
				Spatula.getInstance().start();
				Climber.getInstance().start();
				
				mHardwareUpdater.configureTalons(false);
				mHardwareUpdater.updateSensors(robotState);
				mHardwareUpdater.updateHardware();
				running_ = true;
			}
            notifier_.startPeriodic(kPeriod);
		}
	}

	public synchronized void stop() {
		if (running_) {
			Logger.getInstance().logRobotThread(Level.INFO, "Stopping loops");
            notifier_.stop();

			synchronized (taskRunningLock_) {
				running_ = false;
				Drive.getInstance().stop();
				Slider.getInstance().stop();
				Spatula.getInstance().stop();
				Climber.getInstance().stop();
				mRoutineManager.reset(commands);	
				
				robotState.gamePeriod = RobotState.GamePeriod.DISABLED;
				// Stops updating routines

				// Stop controllers
				Drive.getInstance().setNeutral();
				mHardwareUpdater.configureDriveTalons();
				mHardwareUpdater.disableTalons();
				DashboardManager.getInstance().toggleCANTable(false);
			}
		}
	}
	
	public void autoInit() {
		Logger.getInstance().logRobotThread(Level.INFO, "Start autoInit()");

		// Wait for talons to update
		try {
			Logger.getInstance().logRobotThread(Level.FINER, "Sleeping thread for 200 ms");
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Logger.getInstance().logRobotThread(Level.SEVERE, e);
		}

		DashboardManager.getInstance().toggleCANTable(true);
		robotState.gamePeriod = RobotState.GamePeriod.AUTO;

		Logger.getInstance().logRobotThread(Level.INFO, "End autoInit()");
	}
	
	
	public void teleopInit() {
		Logger.getInstance().logRobotThread(Level.INFO, "Start teleopInit()");

		commands.wantedDriveState = Drive.DriveState.CHEZY;	//switch to chezy after auto ends
		commands = operatorInterface.updateCommands(commands);
		
		robotState.gamePeriod = RobotState.GamePeriod.TELEOP;
		DashboardManager.getInstance().toggleCANTable(true);

		Logger.getInstance().logRobotThread(Level.INFO, "End teleopInit()");
	}
	
	public void addRoutine(Routine r) {
		this.mRoutineManager.addNewRoutine(r);
	}
	
	public void disabledInit() {
		mRoutineManager.reset(commands);
		Commands.reset();
		stop();
		Logger.getInstance().logRobotThread(Level.INFO, "End disabledInit()");
	}

	// Call during tele and auto periodic
	private void logPeriodic() {
		Logger.getInstance().logRobotThread(Level.FINEST, "Match time", DriverStation.getInstance().getMatchTime());
		Logger.getInstance().logRobotThread(Level.FINEST, "DS Connected", DriverStation.getInstance().isDSAttached());
		Logger.getInstance().logRobotThread(Level.FINEST, "DS Voltage", DriverStation.getInstance().getBatteryVoltage());
		Logger.getInstance().logRobotThread(Level.FINEST, "Outputs disabled", DriverStation.getInstance().isSysActive());
		Logger.getInstance().logRobotThread(Level.FINEST, "FMS connected", DriverStation.getInstance().isFMSAttached());
		if (DriverStation.getInstance().isAutonomous()) {
			Logger.getInstance().logRobotThread(Level.FINEST, "Game period: Auto");
		} else if (DriverStation.getInstance().isDisabled()) {
			Logger.getInstance().logRobotThread(Level.FINEST, "Game period: Disabled");
		} else if (DriverStation.getInstance().isOperatorControl()) {
			Logger.getInstance().logRobotThread(Level.FINEST, "Game period: Teleop");
		} else if (DriverStation.getInstance().isTest()) {
			Logger.getInstance().logRobotThread(Level.FINEST, "Game period: Test");
		}
		if (DriverStation.getInstance().isBrownedOut()) Logger.getInstance().logRobotThread(Level.WARNING, "Browned out");
		if (!DriverStation.getInstance().isNewControlData()) Logger.getInstance().logRobotThread(Level.FINE, "Didn't receive new control packet!");
	}
}