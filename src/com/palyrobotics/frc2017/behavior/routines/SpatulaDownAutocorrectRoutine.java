package com.palyrobotics.frc2017.behavior.routines;

import com.palyrobotics.frc2017.behavior.Routine;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.robot.Robot;
import com.palyrobotics.frc2017.subsystems.Intake;
import com.palyrobotics.frc2017.subsystems.Slider;
import com.palyrobotics.frc2017.subsystems.Spatula;
import com.palyrobotics.frc2017.subsystems.Subsystem;

public class SpatulaDownAutocorrectRoutine extends Routine {
	private enum AutocorrectState {
		CENTERING,
		FLIPPING
	}
	private AutocorrectState mState = AutocorrectState.CENTERING;
	
	private double mStartTime = 0;
	private boolean mUpdated = false;
	
	@Override
	public void start() {
		mStartTime = System.currentTimeMillis();
		mState = (Math.abs(Robot.getRobotState().sliderEncoder) < 40) ? AutocorrectState.FLIPPING : AutocorrectState.CENTERING;
	}

	@Override
	public Commands update(Commands commands) {
		mUpdated = true;
//		commands.robotSetpoints.sliderSetpoint = Slider.SliderTarget.CENTER;
		switch (mState) {
		case CENTERING:
			commands.robotSetpoints.sliderSetpoint = Slider.SliderTarget.CENTER;
			commands.wantedSliderState = Slider.SliderState.AUTOMATIC_POSITIONING;
			if (System.currentTimeMillis()-mStartTime > 300 && Robot.getRobotState().sliderVelocity == 0) {
				mState = AutocorrectState.FLIPPING;
				break;
			}
			break;
		case FLIPPING:
			commands.wantedSpatulaState = Spatula.SpatulaState.DOWN;
			commands.wantedIntakeState = Intake.IntakeState.EXPEL;
			break;
		}
			return commands;
	}

	@Override
	public Commands cancel(Commands commands) {
		commands.robotSetpoints.sliderSetpoint = Slider.SliderTarget.NONE;
		commands.wantedSliderState = Slider.SliderState.IDLE;
		commands.wantedIntakeState = Intake.IntakeState.IDLE;
		return commands;
	}

	@Override
	public boolean finished() {
		return mUpdated && mState == AutocorrectState.FLIPPING && (System.currentTimeMillis() - mStartTime) > 2000;
	}

	@Override
	public Subsystem[] getRequiredSubsystems() {
		return new Subsystem[]{Slider.getInstance(), Spatula.getInstance(), Intake.getInstance()};
	}

	@Override
	public String getName() {
		return "Spatula Down Autocorrect Routine";
	}

}
