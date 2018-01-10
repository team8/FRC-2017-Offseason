package com.palyrobotics.frc2017.subsystems.controllers;

import com.ctre.CANTalon;
import com.palyrobotics.frc2017.config.Constants;
import com.palyrobotics.frc2017.config.Gains;
import com.palyrobotics.frc2017.config.RobotState;
import com.palyrobotics.frc2017.robot.RobotPosition;
import com.palyrobotics.frc2017.subsystems.Drive;
import com.palyrobotics.frc2017.util.CANTalonOutput;
import com.palyrobotics.frc2017.util.Pose;
import com.palyrobotics.frc2017.util.archive.DriveSignal;
import com.team254.lib.trajectory.*;
import edu.wpi.first.wpilibj.Timer;

import java.util.Optional;
import java.util.Set;

/**
 * Implements an adaptive pure pursuit controller. See:
 * https://www.ri.cmu.edu/pub_files/pub1/kelly_alonzo_1994_4/kelly_alonzo_1994_4
 * .pdf
 * 
 * Basically, we find a spot on the path we'd like to follow and calculate the
 * wheel speeds necessary to make us land on that spot. The target spot is a
 * specified distance ahead of us, and we look further ahead the greater our
 * tracking error.
 */
public class AdaptivePurePursuitController implements Drive.DriveController {

    private static final double kEpsilon = 1E-9;

    double mFixedLookahead;
    Path mPath;
    RigidTransform2d.Delta mLastCommand;
    double mLastTime;
    double mMaxAccel;
    double mDt;
    boolean mReversed;
    double mPathCompletionTolerance;

    public AdaptivePurePursuitController(double fixed_lookahead, double max_accel, double nominal_dt, Path path,
                                         boolean reversed, double path_completion_tolerance) {
    	//System.out.println("AAAAAAAAH " +  RobotPosition.getInstance().generateOdometryFromSensors(0, 0, Rotation2d.fromRadians(0)));
        mFixedLookahead = fixed_lookahead;
        mMaxAccel = max_accel;
        mPath = path;
        mDt = nominal_dt;
        mLastCommand = null;
        mReversed = reversed;
        mPathCompletionTolerance = path_completion_tolerance;
    }

    /**
     * Calculate the robot's required Delta (movement along an arc) based on the
     * current robot pose and lookahead point.
     * 
     * @param robot_pose - 	the current position of the robot denoted by its
     * 						translation and rotation from the original position
     * @param now - the current timestamp
     * @return the target x translation dx, y translation dy, and rotation dtheta
     */
    public RigidTransform2d.Delta update(RigidTransform2d robot_pose, double now) {
        RigidTransform2d pose = robot_pose;
        if (mReversed) {
            pose = new RigidTransform2d(robot_pose.getTranslation(),
                    robot_pose.getRotation().rotateBy(Rotation2d.fromRadians(Math.PI)));
        }
        System.out.println("Current point: " + robot_pose.getTranslation());
        double distance_from_path = mPath.update(robot_pose.getTranslation());

        PathSegment.Sample lookahead_point = mPath.getLookaheadPoint(robot_pose.getTranslation(),
                distance_from_path + mFixedLookahead);
        System.out.println("Lookahead point: " + lookahead_point.translation.toString());
        Optional<Circle> circle = joinPath(pose, lookahead_point.translation);

        double speed = lookahead_point.speed;
        if (mReversed) {
            speed *= -1;
        }
        // Ensure we don't accelerate too fast from the previous command
        double dt = now - mLastTime;
        if (mLastCommand == null) {
            mLastCommand = new RigidTransform2d.Delta(0, 0, 0);
            dt = mDt;
        }
        double accel = (speed - mLastCommand.dx) / dt;
        if (accel < -mMaxAccel) {
            speed = mLastCommand.dx - mMaxAccel * dt;
        } else if (accel > mMaxAccel) {
            speed = mLastCommand.dx + mMaxAccel * dt;
        }

        // Ensure we slow down in time to stop
        // vf^2 = v^2 + 2*a*d
        // 0 = v^2 + 2*a*d
        double remaining_distance = mPath.getRemainingLength();
        double max_allowed_speed = Math.sqrt(2 * mMaxAccel * remaining_distance);
        if (Math.abs(speed) > max_allowed_speed) {
            speed = max_allowed_speed * Math.signum(speed);
        }
        final double kMinSpeed = 4.0;
        if (Math.abs(speed) < kMinSpeed) {
            // Hack for dealing with problems tracking very low speeds with
            // Talons
            speed = kMinSpeed * Math.signum(speed);
        }

        RigidTransform2d.Delta rv;
        if (circle.isPresent()) {
            rv = new RigidTransform2d.Delta(speed, 0,
                    (circle.get().turn_right ? -1 : 1) * Math.abs(speed) / circle.get().radius);
        } else {
            rv = new RigidTransform2d.Delta(speed, 0, 0);
        }
        mLastTime = now;
        mLastCommand = rv;
        //System.out.println("dx: " + rv.dx + "	dy: " + rv.dy + "	dtheta: " + rv.dtheta);
        return rv;
    }

    /**
     * Returns the list of labeled waypoints (markers) that the robot has passed
     * @return a set of Strings representing each of the crossed markers
     */
    public Set<String> getMarkersCrossed() {
        return mPath.getMarkersCrossed();
    }

	// An abstraction of a circular arc used for turning motion
    public static class Circle {
        public final Translation2d center;
        public final double radius;
        public final boolean turn_right;

        public Circle(Translation2d center, double radius, boolean turn_right) {
            this.center = center;
            this.radius = radius;
            this.turn_right = turn_right;
        }
    }

    /**
     * Connect the current position and lookahead point with a circular arc
     * @param robot_pose - the current translation and rotation of the robot
     * @param lookahead_point - the coordinates of the lookahead point
     * @return - A circular arc representing the path, null if the arc is degenerate
     */
    public static Optional<Circle> joinPath(RigidTransform2d robot_pose, Translation2d lookahead_point) {
        double x1 = robot_pose.getTranslation().getX();
        double y1 = robot_pose.getTranslation().getY();
        double x2 = lookahead_point.getX();
        double y2 = lookahead_point.getY();

        Translation2d pose_to_lookahead = robot_pose.getTranslation().inverse().translateBy(lookahead_point);
        double cross_product = pose_to_lookahead.getX() * robot_pose.getRotation().sin()
                - pose_to_lookahead.getY() * robot_pose.getRotation().cos();
        if (Math.abs(cross_product) < kEpsilon) {
            return Optional.empty();
        }

        double dx = x1 - x2;
        double dy = y1 - y2;
        double my = (cross_product > 0 ? -1 : 1) * robot_pose.getRotation().cos();
        double mx = (cross_product > 0 ? 1 : -1) * robot_pose.getRotation().sin();

        double cross_term = mx * dx + my * dy;

        if (Math.abs(cross_term) < kEpsilon) {
            // Points are collinear
            return Optional.empty();
        }

        return Optional.of(new Circle(
                new Translation2d((mx * (x1 * x1 - x2 * x2 - dy * dy) + 2 * my * x1 * dy) / (2 * cross_term),
                        (-my * (-y1 * y1 + y2 * y2 + dx * dx) + 2 * mx * y1 * dx) / (2 * cross_term)),
                .5 * Math.abs((dx * dx + dy * dy) / cross_term), cross_product > 0));
    }

    /**
     * Calculate the next Delta and convert it to a drive signal
     * @param state - the current robot state, currently unused
     * @return the left and right drive voltage outputs needed to move in the
     * calculated Delta
     */
    @Override
    public DriveSignal update(RobotState state) {
    	//System.out.println("Number of observations during update: " + RobotPosition.getInstance().getNumObservations());
    		RigidTransform2d robot_pose = RobotPosition.getInstance().getLatestFieldToVehicle().getValue();
    		//System.out.println(robot_pose.toString());
        RigidTransform2d.Delta command = this.update(robot_pose, Timer.getFPGATimestamp());
        Kinematics.DriveVelocity setpoint = Kinematics.inverseKinematics(command);

        // Scale the command to respect the max velocity limits
        double max_vel = 0.0;
        max_vel = Math.max(max_vel, Math.abs(setpoint.left));
        max_vel = Math.max(max_vel, Math.abs(setpoint.right));
        if (max_vel > Constants.kPathFollowingMaxVel) {
            double scaling = Constants.kPathFollowingMaxVel / max_vel;
            setpoint = new Kinematics.DriveVelocity(setpoint.left * scaling, setpoint.right * scaling);
        }

        final CANTalonOutput
            left  = new CANTalonOutput(CANTalon.TalonControlMode.Voltage, new Gains(0, 0, 0, 0, 0, 0), setpoint.left),
            right = new CANTalonOutput(CANTalon.TalonControlMode.Voltage, new Gains(0, 0, 0, 0, 0, 0), setpoint.right);

        //System.out.println(new DriveSignal(left, right).toString());
        
        return new DriveSignal(left, right);
    }

    // HOPING THIS METHOD NEVER GETS CALLED
    @Override
    public Pose getSetpoint() {
    		Pose setpoint = new Pose(0,0,0,0,0,0,0,0);
    		return setpoint;
    }

    @Override
	public boolean onTarget() {
    	/*RigidTransform2d pose = RobotPosition.getInstance().getLatestFieldToVehicle().getValue();
    	if (pose.getTranslation().getX() > 40) return true;*/
    		double remainingLength = mPath.getRemainingLength();
        return remainingLength <= mPathCompletionTolerance;
	}

}