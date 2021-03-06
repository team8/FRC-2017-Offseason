package com.palyrobotics.frc2017.config;

import org.junit.Test;

public class CalibrationCalculator {	
	public boolean blue = false;
	@Test
	public void calculateLoadingSide() {
		// Devin values lol
		/*
		 * 55 in forward
		 * final values
		 * 3333 left
		 * 4098 right
		 */
		double kSidePegDistanceLoadingStationInches = (blue) ? 
				AutoDistances.kBlueLoadingStationForwardDistanceInches : AutoDistances.kRedLoadingStationForwardDistanceInches;
		double kSidePegDistanceToAirshipLoadingStationInches = (blue) ?
				AutoDistances.kBlueLoadingStationAirshipDistanceInches : AutoDistances.kRedLoadingStationAirshipDistanceInches;
		
		System.out.println("Loading side");
		double left = kSidePegDistanceLoadingStationInches * Constants.kDriveTicksPerInch;
		double right = left;
		System.out.println("Forward: "+left+","+right);
		left += (60 * Constants.kDriveInchesPerDegree * Constants.kDriveTicksPerInch);
		right -= (60 * Constants.kDriveInchesPerDegree * Constants.kDriveTicksPerInch);
		System.out.println("Turn: "+left+","+right);
		left += kSidePegDistanceToAirshipLoadingStationInches * Constants.kDriveTicksPerInch;
		right += kSidePegDistanceToAirshipLoadingStationInches * Constants.kDriveTicksPerInch;
		System.out.println("Airship: "+left+","+right);
		System.out.println("");
	}
	
	@Test
	public void calculateBoilerSide() {
		
		// Final values - 4192, 3438.
		double kSidePegDistanceBoilerInches = (blue) ?
				AutoDistances.kBlueBoilerForwardDistanceInches : AutoDistances.kRedBoilerForwardDistanceInches;
		double kSidePegDistanceToAirshipBoilerInches = (blue) ?
				AutoDistances.kBlueBoilerAirshipDistanceInches : AutoDistances.kBlueBoilerAirshipDistanceInches;
		System.out.println("Boiler side");
		double left = kSidePegDistanceBoilerInches * Constants.kDriveTicksPerInch;
		double right = left;
		System.out.println("Forward: "+left+","+right);
		left += (60 * Constants.kDriveInchesPerDegree * Constants.kDriveTicksPerInch);
		right -= (60 * Constants.kDriveInchesPerDegree * Constants.kDriveTicksPerInch);
		System.out.println("Turn: "+left+","+right);
		left += kSidePegDistanceToAirshipBoilerInches * Constants.kDriveTicksPerInch;
		right += kSidePegDistanceToAirshipBoilerInches * Constants.kDriveTicksPerInch;
		System.out.println("Airship: "+left+","+right);
		System.out.println("");
	}
	
	@Test
	public void calculateBaseline() {
		double kBaseLineDistanceInches = (blue) ? AutoDistances.kBlueBaseLineDistanceInches : AutoDistances.kRedBaseLineDistanceInches;
		System.out.println("Base line");
		System.out.println(kBaseLineDistanceInches * Constants.kDriveTicksPerInch);
		System.out.println("");
	}
	
	@Test
	public void calculateCenterPeg() {
		/*
		 * 114.375 inches red
		 * 108.5 inches blue
		 */
		double kCenterPegDistanceInches = (blue) ? AutoDistances.kBlueCenterPegDistanceInches : AutoDistances.kRedCenterPegDistanceInches;
		System.out.println("Center peg");
		System.out.println(kCenterPegDistanceInches * Constants.kDriveTicksPerInch);
		System.out.println("");
	}
	
	@Test
	public void generalCalibration() {
		System.out.println("Inches to ticks: " + Constants.kDriveTicksPerInch);
		System.out.println("Inches per degree: " + Constants.kDriveInchesPerDegree);
		System.out.println("60 degrees: " + Constants.kDriveInchesPerDegree*60);
		System.out.println("61.8 degrees: "+(21.5/90)*60);
		System.out.println("93 degrees: "+(21.5/90)*90);
		System.out.println("");
	}
}
