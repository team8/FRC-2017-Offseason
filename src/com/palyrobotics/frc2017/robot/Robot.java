package com.palyrobotics.frc2017.robot;

import com.palyrobotics.frc2017.auto.AutoModeBase;
import com.palyrobotics.frc2017.auto.AutoModeSelector;
import com.palyrobotics.frc2017.behavior.routines.AutomaticClimberRoutine;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.util.logger.Logger;
import com.palyrobotics.frc2017.vision.VisionManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;

import java.util.logging.Level;

public class Robot extends IterativeRobot {

	public static RobotState getRobotState() {
		return RobotEnclosingThread.getRobotState();
	}

	public static Commands getCommands() {
		return RobotEnclosingThread.getCommands();
	}

	private RobotEnclosingThread robotThread;
	
	private double mStartTime;
	private boolean startedClimberRoutine = false;

	@Override
	public void robotInit() {
		Logger.getInstance().setFileName("Offseason");
		Logger.getInstance().start();

		Logger.getInstance().logRobotThread(Level.INFO, "Start robotInit() for "+Constants.kRobotName.toString());
		DashboardManager.getInstance().robotInit();
		VisionManager.getInstance().start(Constants.kAndroidConnectionUpdateRate, false);
		Logger.getInstance().logRobotThread(Level.CONFIG, "Startup sucessful");
		Logger.getInstance().logRobotThread(Level.CONFIG, "Robot name: "+Constants.kRobotName);
		Logger.getInstance().logRobotThread(Level.CONFIG, "Alliance: " + DriverStation.getInstance().getAlliance());
		Logger.getInstance().logRobotThread(Level.CONFIG, "FMS connected: "+DriverStation.getInstance().isFMSAttached());
		Logger.getInstance().logRobotThread(Level.CONFIG, "Alliance station: "+DriverStation.getInstance().getLocation());
		try {
			DriverStation.reportWarning("Auto is "+AutoModeSelector.getInstance().getAutoMode().toString(), false);
			Logger.getInstance().logRobotThread((VisionManager.getInstance().isServerStarted()) ? Level.CONFIG : Level.WARNING,
					(VisionManager.getInstance().isServerStarted()) ? "Nexus streaming": "Nexus not streaming");
			Logger.getInstance().logRobotThread(Level.INFO, "Auto", AutoModeSelector.getInstance().getAutoMode().toString());
		} catch (NullPointerException e) {
			Logger.getInstance().logRobotThread(Level.SEVERE, "Auto", e);
		}
		
		robotThread = new RobotEnclosingThread();
		Logger.getInstance().logRobotThread(Level.INFO, "End robotInit()");
	}

	@Override
	public void autonomousInit() {
		Logger.getInstance().start();
		this.robotThread.start();
		this.robotThread.autoInit();
		
		// Get the selected auto mode
		AutoModeBase mode = AutoModeSelector.getInstance().getAutoMode();

		// Prestart and run the auto mode
		mode.prestart();
		this.robotThread.addRoutine(mode.getRoutine());
	}

	@Override
	public void autonomousPeriodic() {}

	@Override
	public void teleopInit() {
		Logger.getInstance().start();
		mStartTime = System.currentTimeMillis();
		this.robotThread.start();
		this.robotThread.teleopInit();
	}

	@Override
	public void teleopPeriodic() {
		//Auto climber
		if(System.currentTimeMillis() - mStartTime >= 105000 && !startedClimberRoutine) {
			this.robotThread.addRoutine(new AutomaticClimberRoutine());
			startedClimberRoutine = true;
		}
	}

	@Override
	public void disabledInit() {
		Logger.getInstance().start();
		this.robotThread.disabledInit();
		// Manually run garbage collector
		System.gc();
	}

	@Override
	public void disabledPeriodic() {
	}

}