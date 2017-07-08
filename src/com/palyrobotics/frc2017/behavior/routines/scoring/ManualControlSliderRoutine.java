package com.palyrobotics.frc2017.behavior.routines.scoring;

import com.palyrobotics.frc2017.behavior.Routine;
import com.palyrobotics.frc2017.config.Commands;
import com.palyrobotics.frc2017.subsystems.Slider;
import com.palyrobotics.frc2017.subsystems.Subsystem;
import com.palyrobotics.frc2017.subsystems.Slider.SliderState;

public class ManualControlSliderRoutine extends Routine {	
	@Override
	public void start() {	
//		System.out.println("Manually controlling slider");
	}

	@Override
	public Commands update(Commands commands) {
		return commands;
	}

	@Override
	public Commands cancel(Commands commands) {
		commands.wantedSliderState = SliderState.IDLE;
//		System.out.println("Canceling manual slider control");
		return commands;
	}

	@Override
	public boolean finished() {
		return false;
	}

	@Override
	public Subsystem[] getRequiredSubsystems() {
		return new Subsystem[]{Slider.getInstance()};
	}

	@Override
	public String getName() {
		return "Manual Slider Control Routine";
	}

}