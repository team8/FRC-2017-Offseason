package com.palyrobotics.frc2017.config;

public class DriverProfile {
	/**
	 * Class for configuring the control constants for the robot
	 * Has one static method which configures the constants based off the driver
	 * @author Justin
	 */
	public static void configConstants() {
		switch(Constants.kDriverName) {
		case ERIC:
			Constants.kLowGearDriveSensitivity = .70;
			Constants.kHighGearDriveSensitivity = 0.85;
			
			Constants.kLowGearQuickTurnSensitivity = 0.8;
			Constants.kLowGearPreciseQuickTurnSensitivity = 0.35;
			Constants.kHighGearQuickTurnSensitivity = 0.8;
			Constants.kHighGearPreciseQuickTurnSensitivity = 0.35;
			
			Constants.kQuickTurnSensitivityThreshold = 0.90;

			Constants.kQuickStopAccumulatorDecreaseRate = 0.8;

			Constants.kQuickStopAccumulatorDecreaseThreshold = 1.2;
			Constants.kNegativeInertiaScalar = 5.0;
			
			Constants.kAlpha = 0.45;

			Constants.kCyclesUntilStop = 50;

			Constants.kManualIntakeSpeed = -1.0;
			Constants.kManualExhaustSpeed = 1.0;
			
			Constants.kManualIntakeSpeed = -1.0;
			Constants.kManualExhaustSpeed = 1.0;
			break;
		}
	}
}
