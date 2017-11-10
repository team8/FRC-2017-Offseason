package com.palyrobotics.frc2017.robot;

import java.util.logging.Level;

import com.palyrobotics.frc2017.auto.AutoModeBase;
import com.palyrobotics.frc2017.auto.AutoModeSelector;
import com.palyrobotics.frc2017.behavior.RoutineManager;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.config.dashboard.DashboardValue;
import com.palyrobotics.frc2017.robot.team254.lib.util.Loop;
import com.palyrobotics.frc2017.robot.team254.lib.util.Looper;
import com.palyrobotics.frc2017.subsystems.*;
import com.palyrobotics.frc2017.util.logger.Logger;
import com.palyrobotics.frc2017.vision.VisionManager;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;

public class Robot extends IterativeRobot {
	// Instantiate singleton classes


	

	// Instantiate separate thread controls
	//private SubsystemLooper mSubsystemLooper = new SubsystemLooper();
	// Instantiate hardware updaters

	// Subsystem controllers
	
	public static RobotState getRobotState() {
		return RobotEnclosingThread.getRobotState();
	}


	private RobotEnclosingThread robotThread;
	
	private double mStartTime;
	private boolean startedClimberRoutine = false;

	@Override
	public void robotInit() {

		Logger.getInstance().logRobotThread("Start robotInit() for "+Constants.kRobotName.toString());
		DashboardManager.getInstance().robotInit();
		VisionManager.getInstance().start(Constants.kAndroidConnectionUpdateRate, false);
		Logger.getInstance().logRobotThread("Finished starting");
		Logger.getInstance().setFileName("Offseason");
		Logger.getInstance().start();
		Logger.getInstance().logRobotThread("robotInit() start");
		Logger.getInstance().logRobotThread("Robot name: "+Constants.kRobotName);
		Logger.getInstance().logRobotThread("Alliance: " + DriverStation.getInstance().getAlliance());
		Logger.getInstance().logRobotThread("FMS connected: "+DriverStation.getInstance().isFMSAttached());
		Logger.getInstance().logRobotThread("Alliance station: "+DriverStation.getInstance().getLocation());
		try {
			DriverStation.reportWarning("Auto is "+AutoModeSelector.getInstance().getAutoMode().toString(), false);
			Logger.getInstance().logRobotThread((VisionManager.getInstance().isServerStarted()) ?
					"Nexus streaming": "Nexus not streaming");
			Logger.getInstance().logRobotThread("Auto", AutoModeSelector.getInstance().getAutoMode().toString());
			DashboardManager.getInstance().publishKVPair(new DashboardValue("automodestring", AutoModeSelector.getInstance().getAutoMode().toString()));
		} catch (NullPointerException e) {
			Logger.getInstance().logRobotThread("Auto: "+e.getMessage());
		}
		

		System.out.println("Auto: "+AutoModeSelector.getInstance().getAutoMode().toString());
		System.out.println("End robotInit()");
		Logger.getInstance().logRobotThread("End robotInit()");
		Logger.getInstance().logRobotThread(Level.FINE, "Startup sucessful");
		Logger.getInstance().logRobotThread(Level.INFO, "Start robotInit() for "+Constants.kRobotName.toString());
		Logger.getInstance().logRobotThread(Level.INFO, "Robot name: "+Constants.kRobotName);
		Logger.getInstance().logRobotThread(Level.INFO, "Alliance: " + DriverStation.getInstance().getAlliance());
		Logger.getInstance().logRobotThread(Level.INFO, "FMS connected: "+DriverStation.getInstance().isFMSAttached());
		Logger.getInstance().logRobotThread(Level.INFO, "Alliance station: "+DriverStation.getInstance().getLocation());
		try {
			DriverStation.reportWarning("Auto is "+AutoModeSelector.getInstance().getAutoMode().toString(), false);
			Logger.getInstance().logRobotThread((VisionManager.getInstance().isServerStarted()) ? Level.INFO : Level.WARNING,
					(VisionManager.getInstance().isServerStarted()) ? "Nexus streaming": "Nexus not streaming");
			Logger.getInstance().logRobotThread(Level.INFO, "Auto", AutoModeSelector.getInstance().getAutoMode().toString());
			DashboardManager.getInstance().publishKVPair(new DashboardValue("automodestring", AutoModeSelector.getInstance().getAutoMode().toString()));
		} catch (NullPointerException e) {
			Logger.getInstance().logRobotThread(Level.SEVERE, "Auto: "+e.getMessage());
		}
		
//		mLogger.logRobotThread(Level.INFO, "Auto", AutoModeSelector.getInstance().getAutoMode().toString());
//		AndroidConnectionHelper.getInstance().StartVisionApp();
//		mLogger.logRobotThread(Level.INFO, "End robotInit()");
		robotThread = new RobotEnclosingThread();
	}

	@Override
	public void autonomousInit() {
////		mLogger.start();
//		mLogger.logRobotThread(Level.INFO, "Start autonomousInit()");
		this.robotThread.autoInit();
		// Wait for talons to update
		try {
//			mLogger.logRobotThread(Level.FINEST, "Sleeping thread for 200 ms");
			Thread.sleep(200);
		} catch (InterruptedException e) {

		}


		robotThread.start();
		
		// Get the selected auto mode
		AutoModeBase mode = AutoModeSelector.getInstance().getAutoMode();

		// Prestart and run the auto mode
		mode.prestart();
		this.robotThread.addRoutine(mode.getRoutine());
	}

	@Override
	public void autonomousPeriodic() {
		
	}

	@Override
	public void teleopInit() {
//		mLogger.start();
//		mLogger.logRobotThread(Level.INFO, "Start teleopInit()");

		
		this.robotThread.start();
		this.robotThread.teleopInit();
//		mLogger.logRobotThread(Level.INFO, "End teleopInit()");
	}

	@Override
	public void teleopPeriodic() {
		// Update RobotState
		// Gets joystick commands
		// Updates commands based on routines
		logPeriodic();
	
		//Update the hardware
	}

	@Override
	public void disabledInit() {
		this.robotThread.stop();
		// Manually run garbage collector
		System.gc();
		System.out.println("Gyro: "+RobotEnclosingThread.getRobotState().drivePose.heading);
		System.out.println("End disabledInit()");
	}

	@Override
	public void disabledPeriodic() {
	}

	// Call during tele and auto periodic
	private void logPeriodic() {
		Logger.getInstance().logRobotThread("Match time", DriverStation.getInstance().getMatchTime());
		Logger.getInstance().logRobotThread("DS Connected", DriverStation.getInstance().isDSAttached());
		Logger.getInstance().logRobotThread("DS Voltage", DriverStation.getInstance().getBatteryVoltage());
		Logger.getInstance().logRobotThread("Outputs disabled", DriverStation.getInstance().isSysActive());
		Logger.getInstance().logRobotThread("FMS connected: "+DriverStation.getInstance().isFMSAttached());
		if (DriverStation.getInstance().isAutonomous()) {
			Logger.getInstance().logRobotThread("Game period: Auto");
		} else if (DriverStation.getInstance().isDisabled()) {
			Logger.getInstance().logRobotThread("Game period: Disabled");
		} else if (DriverStation.getInstance().isOperatorControl()) {
			Logger.getInstance().logRobotThread("Game period: Teleop");
		} else if (DriverStation.getInstance().isTest()) {
			Logger.getInstance().logRobotThread("Game period: Test");
		}
		if (DriverStation.getInstance().isBrownedOut()) Logger.getInstance().logRobotThread("Browned out");
		if (!DriverStation.getInstance().isNewControlData()) Logger.getInstance().logRobotThread("Didn't receive new control packet!");
	}

}