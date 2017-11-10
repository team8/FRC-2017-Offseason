package com.palyrobotics.frc2017.robot;

import java.util.ArrayList;
import java.util.List;

import com.palyrobotics.frc2017.behavior.Routine;
import com.palyrobotics.frc2017.behavior.RoutineManager;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.robot.team254.lib.util.CrashTrackingRunnable;
import com.palyrobotics.frc2017.robot.team254.lib.util.Loop;
import com.palyrobotics.frc2017.subsystems.Climber;
import com.palyrobotics.frc2017.subsystems.Drive;
import com.palyrobotics.frc2017.subsystems.Intake;
import com.palyrobotics.frc2017.subsystems.Slider;
import com.palyrobotics.frc2017.subsystems.Spatula;

import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This code runs all of the robot's loops. Loop objects are stored in a List
 * object. They are started when the robot powers up and stopped after the
 * match.
 * @author Team 254
 */
public class RobotEnclosingThread {
	// SubsystemLoop update rate
	private final double kPeriod = Constants.kNormalLoopsDt;

	private RoutineManager mRoutineManager = new RoutineManager();
	private OperatorInterface operatorInterface = OperatorInterface.getInstance();
	

	private static RobotState robotState = new RobotState();
	public static RobotState getRobotState() {
		return robotState;
	}
	
	// Hardware Updater
	private HardwareUpdater mHardwareUpdater;

	
	private static Commands commands = new Commands();
	public static Commands getCommands() {return commands;}
	public static void updateCommands(Commands cmds) {
		commands = cmds;
	}

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
					Drive.getInstance().update(RobotEnclosingThread.getCommands(), getRobotState());
					Slider.getInstance().update(getCommands(), getRobotState());
					Spatula.getInstance().update(getCommands(), getRobotState());
					Climber.getInstance().update(getCommands(), getRobotState());
					
					mHardwareUpdater.updateSensors(robotState);
					mHardwareUpdater.updateHardware();
					Commands commands = mRoutineManager.update(getCommands());
					dt_ = now - timestamp_;
					timestamp_ = now;
				}
			}
		}
	};

	public RobotEnclosingThread() {
		running_ = false;
		
		if (Constants.kRobotName == Constants.RobotName.STEIK) {
			try {
				mHardwareUpdater = new HardwareUpdater(this, Drive.getInstance(), Slider.getInstance(), Spatula.getInstance()
						, Intake.getInstance(), Climber.getInstance());
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
			System.out.println("Starting loops");
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
		}
	}

	public synchronized void stop() {
		if (running_) {
			System.out.println("Stopping loops");
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
		DashboardManager.getInstance().toggleCANTable(true);
		robotState.gamePeriod = RobotState.GamePeriod.AUTO;
	}
	
	
	public void teleopInit() {
		commands.wantedDriveState = Drive.DriveState.CHEZY;	//switch to chezy after auto ends
		commands = operatorInterface.updateCommands(commands);
		
		robotState.gamePeriod = RobotState.GamePeriod.TELEOP;
		DashboardManager.getInstance().toggleCANTable(true);
		
		
	}

	public void outputToSmartDashboard() {
		SmartDashboard.putNumber("looper_dt", dt_);
	}
	
	public void  addRoutine(Routine r) {
		this.mRoutineManager.addNewRoutine(r);
	}
	
	public void disabledInit() {
		mRoutineManager.reset(commands);
		
		commands = new Commands();
		
		stop();
	}
}
