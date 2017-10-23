package com.palyrobotics.frc2017.subsystems.controllers;

import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.config.dashboard.DashboardManager;
import com.palyrobotics.frc2017.subsystems.Drive.DriveController;
import com.palyrobotics.frc2017.util.CANTalonOutput;
import com.palyrobotics.frc2017.util.Pose;
import com.palyrobotics.frc2017.util.archive.DriveSignal;

public class TimedDriveController implements DriveController {

	private double voltage;
	private double time;
	private double startTime;
		
	public TimedDriveController(double voltage, double time) {
		this.voltage = voltage;
		this.time = time;
		this.startTime = System.currentTimeMillis();
		
	}

	@Override
	public boolean onTarget() {
		return System.currentTimeMillis() > startTime+time*1000;
	}
	
	@Override
	public DriveSignal update(RobotState state) {
		
		CANTalonOutput leftOutput = new CANTalonOutput();
		CANTalonOutput rightOutput = new CANTalonOutput();
		
		leftOutput.setVoltage(voltage);
		rightOutput.setVoltage(voltage);
		
		DashboardManager.getInstance().updateCANTable(voltage + "," + state.drivePose.leftSpeed/(12.0*Constants.kDriveSpeedUnitConversion) + "," + state.drivePose.rightSpeed/(12.0*Constants.kDriveSpeedUnitConversion));
		
		return new DriveSignal(leftOutput, rightOutput);
		
	}
	
	@Override
	public Pose getSetpoint() {
		return new Pose(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

}
