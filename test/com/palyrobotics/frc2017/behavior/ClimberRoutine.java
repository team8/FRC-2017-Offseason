package com.palyrobotics.frc2017.behavior;

import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.subsystems.Climber;
import com.palyrobotics.frc2017.subsystems.Subsystem;


/**
 * Created by Nihar on 1/22/17.
 * Used for testing {@link RoutineManager} in {@link com.palyrobotics.frc2017.behavior.RoutineManagerTest}
 */
public class ClimberRoutine extends Routine {
	
	private boolean isFinished;
	
	@Override
	public void start() {
		isFinished = false;
	}

	@Override
	public Commands update(Commands commands) {
		return null;
	}

	@Override
	public Commands cancel(Commands commands) {
		isFinished = true;
		return null;
	}

	@Override
	public boolean finished() {
		return isFinished;
	}

	@Override
	public Subsystem[] getRequiredSubsystems() {
		Subsystem[] required = {Climber.getInstance()};
		return required;
	}

	@Override
	public String getName() {
		return null;
	}
}