package com.palyrobotics.frc2017.robot;

import java.util.logging.Level;

import com.ctre.CANTalon;
import com.palyrobotics.frc2017.auto.AutoModeBase;
import com.palyrobotics.frc2017.auto.AutoModeSelector;
import com.palyrobotics.frc2017.behavior.RoutineManager;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.config.dashboard.DashboardValue;
import com.palyrobotics.frc2017.subsystems.*;
import com.palyrobotics.frc2017.util.logger.Logger;
import com.palyrobotics.frc2017.vision.AndroidConnectionHelper;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;

public class Robot extends IterativeRobot {
	// Instantiate singleton classes


	// Single instance to be passed around

	

	// Instantiate separate thread controls
	//private SubsystemLooper mSubsystemLooper = new SubsystemLooper();
	// Instantiate hardware updaters

	// Subsystem controllers

	public static RobotState getRobotState() {
		return RobotEnclosingThread.getRobotState();
	}
	
	private RobotEnclosingThread robotThread;
	
	@Override
	public void robotInit() {
//		mLogger.setFileName("LogTest");
//		mLogger.start();
		DashboardManager.getInstance().robotInit();
		AndroidConnectionHelper.getInstance().start();
//		mLogger.logRobotThread(Level.FINE, "Startup sucessful");
//		mLogger.logRobotThread(Level.INFO, "Start robotInit() for "+Constants.kRobotName.toString());
//		mLogger.logRobotThread(Level.INFO, "Robot name: "+Constants.kRobotName);
//		mLogger.logRobotThread(Level.INFO, "Alliance: " + DriverStation.getInstance().getAlliance());
//		mLogger.logRobotThread(Level.INFO, "FMS connected: "+DriverStation.getInstance().isFMSAttached());
//		mLogger.logRobotThread(Level.INFO, "Alliance station: "+DriverStation.getInstance().getLocation());
//		try {
//			DriverStation.reportWarning("Auto is "+AutoModeSelector.getInstance().getAutoMode().toString(), false);
//			mLogger.logRobotThread((AndroidConnectionHelper.getInstance().isServerStarted()) ? Level.INFO : Level.WARNING,
//					(AndroidConnectionHelper.getInstance().isServerStarted()) ? "Nexus streaming": "Nexus not streaming");
//			mLogger.logRobotThread(Level.INFO, "Auto", AutoModeSelector.getInstance().getAutoMode().toString());
//			DashboardManager.getInstance().publishKVPair(new DashboardValue("automodestring", AutoModeSelector.getInstance().getAutoMode().toString()));
//		} catch (NullPointerException e) {
//			mLogger.logRobotThread(Level.SEVERE, "Auto: "+e.getMessage());
//		}
		
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
//		mLogger.logRobotThread(Level.FINE, "Auto mode", mode.toString());
//		mLogger.logRobotThread(Level.FINER, "Auto routine", mode.getRoutine().toString());
//		mLogger.logRobotThread(Level.INFO, "End autonomousInit()");
	}

	@Override
	public void autonomousPeriodic() {
//		HardwareAdapter.getInstance().getSlider().sliderTalon.changeControlMode(CANTalon.TalonControlMode.Position);
//		HardwareAdapter.getInstance().getSlider().sliderTalon.set(1);
//		System.out.println("Talon stpt:"+HardwareAdapter.getInstance().getSlider().sliderTalon.getSetpoint());
//		System.out.println("Talon mode:"+HardwareAdapter.getInstance().getSlider().sliderTalon.getControlMode());
		//		logPeriodic();
//		System.out.println(robotState.sliderEncoder);
//		mLogger.logRobotThread(Level.FINEST, "Nexus xdist", AndroidConnectionHelper.getInstance().getXDist());

	}

	@Override
	public void teleopInit() {
//		mLogger.start();
//		mLogger.logRobotThread(Level.INFO, "Start teleopInit()");

		
		this.robotThread.start();
//		mLogger.logRobotThread(Level.INFO, "End teleopInit()");
		}

	@Override
	public void teleopPeriodic() {
		// Update RobotState
		// Gets joystick commands
		// Updates commands based on routines
//		mLogger.logRobotThread(Level.FINEST, "Teleop Commands", commands);
		logPeriodic();


		
		//Update the hardware
	}

	@Override
	
	public void disabledInit() {
//		mLogger.logRobotThread(Level.INFO, "Start disabledInit()");
//		mLogger.logRobotThread(Level.FINE, "Current Auto Mode", AutoModeSelector.getInstance().getAutoMode().toString());

		this.robotThread.stop();
		// Manually run garbage collector
		System.gc();
		System.out.println("End disabledInit()");
	}

	@Override
	public void disabledPeriodic() {
//		System.out.println("Gyro: "+robotState.drivePose.heading);
//		System.out.println("Left enc: " + robotState.drivePose.leftEnc +"\n"
//				+"Right enc: "+robotState.drivePose.rightEnc);
//		Gains.updateNetworkTableGains();
//		System.out.println("Gyro: "+robotState.drivePose.heading);
//		if (robotState.sliderClosedLoopError.isPresent()) {
//			System.out.println("Slider closed" + robotState.sliderClosedLoopError.get());
//		}
	}

	// Call during tele and auto periodic
	private void logPeriodic() {
//		mLogger.logRobotThread(Level.FINEST, "Match time", DriverStation.getInstance().getMatchTime());
//		mLogger.logRobotThread(Level.FINEST, "DS Connected", DriverStation.getInstance().isDSAttached());
//		mLogger.logRobotThread(Level.FINEST,"DS Voltage", DriverStation.getInstance().getBatteryVoltage());
////		mLogger.logRobotThread("Battery current", HardwareAdapter.getInstance().kPDP.getTotalCurrent());
////		mLogger.logRobotThread("Battery watts drawn", HardwareAdapter.getInstance().kPDP.getTotalPower());
//		mLogger.logRobotThread(Level.FINEST, "Outputs disabled", DriverStation.getInstance().isSysActive());
//		mLogger.logRobotThread(Level.FINEST, "FMS connected"+DriverStation.getInstance().isFMSAttached());
//		if (DriverStation.getInstance().isAutonomous()) {
//			mLogger.logRobotThread(Level.FINEST, "Game period: Auto");
//		} else if (DriverStation.getInstance().isDisabled()) {
//			mLogger.logRobotThread(Level.FINEST,"Game period: Disabled");
//		} else if (DriverStation.getInstance().isOperatorControl()) {
//			mLogger.logRobotThread(Level.FINEST,"Game period: Teleop");
//		} else if (DriverStation.getInstance().isTest()) {
//			mLogger.logRobotThread(Level.FINEST,"Game period: Test");
//		}
//		if (DriverStation.getInstance().isBrownedOut()) mLogger.logRobotThread(Level.WARNING, "Browned out");
//		if (!DriverStation.getInstance().isNewControlData()) mLogger.logRobotThread(Level.FINER, "Didn't receive new control packet!");
	}

}