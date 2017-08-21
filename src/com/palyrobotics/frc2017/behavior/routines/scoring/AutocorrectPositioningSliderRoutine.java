package com.palyrobotics.frc2017.behavior.routines.scoring;

import com.palyrobotics.frc2017.behavior.Routine;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.subsystems.Slider;
import com.palyrobotics.frc2017.subsystems.Spatula;
import com.palyrobotics.frc2017.subsystems.Subsystem;
import com.palyrobotics.frc2017.subsystems.Slider.SliderState;
import com.palyrobotics.frc2017.subsystems.Spatula.SpatulaState;

/** 
 * Autocorrects -> only tells the slider to move once safe (spatula up)
 * @author Prashanti, Nihar, Ailyn
 *
 */
public class AutocorrectPositioningSliderRoutine extends Routine {	
	private enum DistancePositioningState {
		RAISING,
		MOVING
	}
	private DistancePositioningState mState = DistancePositioningState.RAISING;
	// Use to make sure routine ran at least once before "finished"
	private boolean updated = false;
	
	private Slider.SliderTarget mTarget;
	
	private double startTime;
	private static final double raiseTime = 1000;
	
	public AutocorrectPositioningSliderRoutine(Slider.SliderTarget target) {
		mTarget = target;
	}
	
	@Override
	public void start() {
		if (spatula.getState() == SpatulaState.DOWN || slider.getSliderState() == Slider.SliderState.WAITING) {
			System.out.println("Autocorrecting spatula!");
			mState = DistancePositioningState.RAISING;
		}
		else {
			mState = DistancePositioningState.MOVING;
		}
		startTime = System.currentTimeMillis();
	}

	@Override
	public Commands update(Commands commands) {
		commands.robotSetpoints.sliderSetpoint = mTarget;
		updated = true;
		switch(mState) {
		case MOVING:
			commands.wantedSliderState = Slider.SliderState.AUTOMATIC_POSITIONING;
			break;
		case RAISING:
			if(System.currentTimeMillis() > (raiseTime+startTime)) {
				System.out.println("Time up");
				mState = DistancePositioningState.MOVING;
				break;
			}
			commands.wantedSpatulaState = Spatula.SpatulaState.UP;
			commands.wantedSliderState = Slider.SliderState.WAITING;
			break;
		}
		return commands;
	}

	@Override
	public Commands cancel(Commands commands) {
		commands.wantedSliderState = SliderState.IDLE;
		return commands;
	}

	@Override
	public boolean finished() {
		return updated && mState==DistancePositioningState.MOVING && slider.onTarget();
	}

	@Override
	public Subsystem[] getRequiredSubsystems() {
		return new Subsystem[]{Slider.getInstance(), Spatula.getInstance()};
	}

	@Override
	public String getName() {
		return "Slider Distance Positioning Autocorrect Routine";
	}

}
