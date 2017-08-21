package com.palyrobotics.frc2017.robot;

import com.ctre.CANTalon;
import com.palyrobotics.frc2017.auto.AutoModeBase;
import com.palyrobotics.frc2017.auto.AutoModeSelector;
import com.palyrobotics.frc2017.behavior.RoutineManager;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.Gains;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.config.dashboard.DashboardValue;
import com.palyrobotics.frc2017.subsystems.*;
import com.palyrobotics.frc2017.util.logger.Logger;
import com.palyrobotics.frc2017.vision.AndroidConnectionHelper;
import com.palyrobotics.frc2017.robot.team254.lib.util.Looper;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;

public class Robot extends IterativeRobot {
	// Instantiate singleton classes
	private RobotState robotState = new RobotState();
	public RobotState getRobotState() {
		return robotState;
	}

	// Single instance to be passed around
	private Commands commands = new Commands();
	public Commands getCommands() {return commands;}

	private OperatorInterface operatorInterface = OperatorInterface.getInstance();
	// Instantiate hardware updaters
	private Looper mHardwareEnabledLooper = new Looper();
	private Looper mHardwareSensorLooper = new Looper();
	private RoutineManager mRoutineManager = new RoutineManager();

	// Subsystem controllers
	private Drive mDrive = Drive.getInstance();
	private Flippers mFlippers = Flippers.getInstance();
	private Slider mSlider = Slider.getInstance();
	private Spatula mSpatula = Spatula.getInstance();
	private Intake mIntake = Intake.getInstance();
	private Climber mClimber = Climber.getInstance();
	private Logger mLogger = Logger.getInstance();

	// Hardware Updater
	private HardwareUpdater mHardwareUpdater;
	
	@Override
	public void robotInit() {
		System.out.println("Start robotInit() for "+Constants.kRobotName.toString());
		DashboardManager.getInstance().robotInit();
//		AndroidConnectionHelper.getInstance().start();
		System.out.println("Finished starting");
		mLogger.setFileName("8/20 testing");
		mLogger.start();
		mLogger.logRobotThread("robotInit() start");
		mLogger.logRobotThread("Robot name: "+Constants.kRobotName);
		mLogger.logRobotThread("Alliance: " + DriverStation.getInstance().getAlliance());
		mLogger.logRobotThread("FMS connected: "+DriverStation.getInstance().isFMSAttached());
		mLogger.logRobotThread("Alliance station: "+DriverStation.getInstance().getLocation());
		try {
			DriverStation.reportWarning("Auto is "+AutoModeSelector.getInstance().getAutoMode().toString(), false);
			mLogger.logRobotThread((AndroidConnectionHelper.getInstance().isServerStarted()) ?
					"Nexus streaming": "Nexus not streaming");
			mLogger.logRobotThread("Auto", AutoModeSelector.getInstance().getAutoMode().toString());
			DashboardManager.getInstance().publishKVPair(new DashboardValue("automodestring", AutoModeSelector.getInstance().getAutoMode().toString()));
		} catch (NullPointerException e) {
			mLogger.logRobotThread("Auto: "+e.getMessage());
		}
		if (Constants.kRobotName == Constants.RobotName.STEIK) {
			try {
				mHardwareUpdater = new HardwareUpdater(mDrive, mFlippers, mSlider, mSpatula, mIntake, mClimber);
			} catch (Exception e) {
				System.exit(1);
			}
		} else {
			try {
				mHardwareUpdater = new HardwareUpdater(mDrive);
			} catch (Exception e) {
				System.exit(1);
			}
		}
		mHardwareSensorLooper.register(mHardwareUpdater.getHardwareSensorLoop());
		mHardwareEnabledLooper.register(mHardwareUpdater.getHardwareEnabledLoop());
		mHardwareUpdater.initHardware();
		mHardwareSensorLooper.start();
		System.out.println("Auto: "+AutoModeSelector.getInstance().getAutoMode().toString());
//		AndroidConnectionHelper.getInstance().StartVisionApp();
		System.out.println("End robotInit()");
		mLogger.logRobotThread("End robotInit()");
	}

	@Override
	public void autonomousInit() {
		System.out.println("Start autonomousInit()");

		mLogger.start();
		mLogger.logRobotThread("Start autonomousInit()");
		DashboardManager.getInstance().toggleCANTable(true);
		robotState.gamePeriod = RobotState.GamePeriod.AUTO;
		mHardwareEnabledLooper.start();
		mHardwareUpdater.configureTalons(false);
		// Wait for talons to update
		try {
			System.out.println("Sleeping thread for 200 ms");
			Thread.sleep(200);
		} catch (InterruptedException e) {

		}
		mHardwareUpdater.updateSensors(robotState);
		mRoutineManager.reset(commands);

		// Get the selected auto mode
		AutoModeBase mode = AutoModeSelector.getInstance().getAutoMode();
		// Prestart and run the auto mode
		mode.prestart();
		mRoutineManager.addNewRoutine(mode.getRoutine());
		mLogger.logRobotThread("Auto mode", mode.toString());
		mLogger.logRobotThread("Auto routine", mode.getRoutine().toString());
		System.out.println("End autonomousInit()");
		mLogger.logRobotThread("End autonomousInit()");
	}

	@Override
	public void autonomousPeriodic() {
//		HardwareAdapter.getInstance().getSlider().sliderTalon.changeControlMode(CANTalon.TalonControlMode.Position);
//		HardwareAdapter.getInstance().getSlider().sliderTalon.set(1);
//		System.out.println("Talon stpt:"+HardwareAdapter.getInstance().getSlider().sliderTalon.getSetpoint());
//		System.out.println("Talon mode:"+HardwareAdapter.getInstance().getSlider().sliderTalon.getControlMode());
		//		logPeriodic();
//		System.out.println(robotState.sliderEncoder);
		mLogger.logRobotThread("Nexus xdist: "+AndroidConnectionHelper.getInstance().getXDist());
		commands = mRoutineManager.update(commands);
	}

	@Override
	public void teleopInit() {
		System.out.println("Start teleopInit()");
		mHardwareEnabledLooper.start();
		mLogger.start();
		mLogger.logRobotThread("Start teleopInit()");
		robotState.gamePeriod = RobotState.GamePeriod.TELEOP;
		mHardwareUpdater.configureTalons(false);
		mHardwareUpdater.updateSensors(robotState);
		mRoutineManager.reset(commands);
		DashboardManager.getInstance().toggleCANTable(true);
		commands.wantedDriveState = Drive.DriveState.CHEZY;	//switch to chezy after auto ends
		commands = operatorInterface.updateCommands(commands);
		mLogger.logRobotThread("End teleopInit()");
		System.out.println("End teleopInit()");
	}

	@Override
	public void teleopPeriodic() {
		// Update RobotState
		// Gets joystick commands
		// Updates commands based on routines
//		mLogger.logRobotThread("Teleop Commands: ", commands);
//		logPeriodic();
		commands = mRoutineManager.update(operatorInterface.updateCommands(commands));
		//Update the hardware
	}

	@Override
	public void disabledInit() {
		System.out.println("Start disabledInit()");
		mLogger.logRobotThread("Start disabledInit()");
		System.out.println("Current Auto Mode: " + AutoModeSelector.getInstance().getAutoMode().toString());
		robotState.gamePeriod = RobotState.GamePeriod.DISABLED;
		// Stops updating routines
		mRoutineManager.reset(commands);
		
		commands = new Commands();
		

		// Stop controllers
		mDrive.setNeutral();
		mHardwareEnabledLooper.stop();
		mHardwareUpdater.configureDriveTalons();
		mHardwareUpdater.disableTalons();
		DashboardManager.getInstance().toggleCANTable(false);
		mLogger.logRobotThread("End disabledInit()");
		mLogger.cleanup();
		System.out.println("Log file: "+mLogger.getLogPath());
		// Manually run garbage collector
		System.gc();
		System.out.println("Gyro: "+robotState.drivePose.heading);
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
		mLogger.logRobotThread("Match time", DriverStation.getInstance().getMatchTime());
		mLogger.logRobotThread("DS Connected", DriverStation.getInstance().isDSAttached());
		mLogger.logRobotThread("DS Voltage", DriverStation.getInstance().getBatteryVoltage());
//		mLogger.logRobotThread("Battery current", HardwareAdapter.getInstance().kPDP.getTotalCurrent());
//		mLogger.logRobotThread("Battery watts drawn", HardwareAdapter.getInstance().kPDP.getTotalPower());
		mLogger.logRobotThread("Outputs disabled", DriverStation.getInstance().isSysActive());
		mLogger.logRobotThread("FMS connected: "+DriverStation.getInstance().isFMSAttached());
		if (DriverStation.getInstance().isAutonomous()) {
			mLogger.logRobotThread("Game period: Auto");
		} else if (DriverStation.getInstance().isDisabled()) {
			mLogger.logRobotThread("Game period: Disabled");
		} else if (DriverStation.getInstance().isOperatorControl()) {
			mLogger.logRobotThread("Game period: Teleop");
		} else if (DriverStation.getInstance().isTest()) {
			mLogger.logRobotThread("Game period: Test");
		}
		if (DriverStation.getInstance().isBrownedOut()) mLogger.logRobotThread("Browned out");
		if (!DriverStation.getInstance().isNewControlData()) mLogger.logRobotThread("Didn't receive new control packet!");
	}
}