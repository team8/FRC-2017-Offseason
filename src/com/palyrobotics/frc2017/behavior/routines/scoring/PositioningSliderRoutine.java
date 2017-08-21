package com.palyrobotics.frc2017.behavior.routines.scoring;

import com.palyrobotics.frc2017.behavior.Routine;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.subsystems.Slider;
import com.palyrobotics.frc2017.subsystems.Spatula;
import com.palyrobotics.frc2017.subsystems.Subsystem;
import com.palyrobotics.frc2017.subsystems.Spatula.SpatulaState;

/**
 * Moves the slider to a setpoint
 * Slider target needs to be set from elsewhere
 * @deprecated, use AutocorrectPositioning
 * NOTE: When unit testing, set Robot.RobotState appropriately
 * @author Prashanti
 */
public class PositioningSliderRoutine extends Routine {
	// Whether this routine is allowed to run or not
	private boolean mAllowed;
	
	@Override
	public void start() {
		if (spatula.getState() == SpatulaState.DOWN) {
			mAllowed = false;
		} else {
			mAllowed = true;
		}
	}

	@Override
	public Commands update(Commands commands) {
		if (mAllowed) {
			commands.wantedSliderState = Slider.SliderState.AUTOMATIC_POSITIONING;

		} else {
			commands.wantedSliderState = Slider.SliderState.IDLE;
		}
		return commands;
	}

	@Override
	public Commands cancel(Commands commands) {
		commands.wantedSliderState = Slider.SliderState.IDLE;
		return commands;
	}

	@Override
	public boolean finished() {
		return !mAllowed || slider.onTarget();
	}

	@Override
	public Subsystem[] getRequiredSubsystems() {
		return new Subsystem[]{Slider.getInstance(), Spatula.getInstance()};
	}

	@Override
	public String getName() {
		return "Slider Distance Positioning Routine";
	}

}
