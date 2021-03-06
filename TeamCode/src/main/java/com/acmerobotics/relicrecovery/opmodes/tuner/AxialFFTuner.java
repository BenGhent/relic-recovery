package com.acmerobotics.relicrecovery.opmodes.tuner;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.library.localization.Pose2d;
import com.acmerobotics.library.localization.Vector2d;
import com.acmerobotics.library.path.TrajectoryFollower;
import com.acmerobotics.relicrecovery.subsystems.MecanumDrive;
import com.acmerobotics.relicrecovery.subsystems.Robot;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@Disabled
@Config
@TeleOp
public class AxialFFTuner extends LinearOpMode {
    public static double LOWER_BOUND = 0, UPPER_BOUND = 0.005;
    public static double DISTANCE = 60; // in

    /**
     * The search confidence codifies how fast the search window homes in on the solution. This
     * value should be in the range (0, 1] where 1 indicates a true binary search.
     */
    public static double SEARCH_CONFIDENCE = 0.7;

    public static int TRIALS = 5;

    private Robot robot;
    private TrajectoryFollower trajectoryFollower;
    private boolean shouldTravelForward = true;

    @Override
    public void runOpMode() throws InterruptedException {
        robot = new Robot(this);
        robot.drive.enablePositionEstimation();
        robot.start();

        trajectoryFollower = robot.drive.getTrajectoryFollower();

        telemetry.log().add("Ready. Make sure to clear PID gains.");
        telemetry.update();

        waitForStart();

        telemetry.log().clear();
        telemetry.update();

        double lowerBound = LOWER_BOUND, upperBound = UPPER_BOUND;

        while (opModeIsActive()) {
            double value = (lowerBound + upperBound) / 2;

            double meanError = 0;
            for (int i = 0; i < TRIALS && opModeIsActive(); i++) {
                meanError += testCoefficient(value);

                sleep(500);
            }
            meanError /= TRIALS;

            if (meanError > 0) {
                // the coefficient is too high; lower the upper bound
                upperBound = (upperBound - value) * (1 - SEARCH_CONFIDENCE) + value;
            } else {
                // the coefficient is too low; raise the lower bound
                lowerBound = (value - lowerBound) * SEARCH_CONFIDENCE + lowerBound;
            }

            telemetry.addData("value", value);
            telemetry.addData("lastError", meanError);
            telemetry.update();
        }
    }

    /**
     * Complete a single motion with the provided coefficient and measure its accuracy
     * @param coefficient axial K_a coefficient to test
     * @return the mean error for the movement, positive indicates the coefficient is too low
     */
    public double testCoefficient(double coefficient) {
        MecanumDrive.AXIAL_PIDF.a = coefficient;

        robot.drive.setEstimatedPose(new Pose2d(0, 0, 0));

        if (shouldTravelForward) {
            robot.drive.followTrajectory(robot.drive.trajectoryBuilder(new Pose2d(0, 0, 0))
                    .lineTo(new Vector2d(DISTANCE, 0))
                    .build());
        } else {
            robot.drive.followTrajectory(robot.drive.trajectoryBuilder(new Pose2d(0, 0, 0))
                    .lineTo(new Vector2d(-DISTANCE, 0))
                    .build());
        }

//        double errorSum = 0;
//        int numErrors = 0;

        while (opModeIsActive() && robot.drive.isFollowingTrajectory()) {
//            double rawAxialError = pathFollower.getAxialError();
//            // If axial K_a is too high, then the error will be positive in when acceleration is
//            // positive and negative when acceleration is negative. Thus we use the sign of the
//            // acceleration to make sure all the errors have matching signs.
//            if (pathFollower.getPoseAcceleration() == null) continue;
//            errorSum += -Math.signum(pathFollower.getPoseAcceleration().x()) * rawAxialError;
//            numErrors++;
//            sleep(25);
        }

//        errorSum *= shouldTravelForward ? 1 : -1;

        shouldTravelForward = !shouldTravelForward;

//        return errorSum / numErrors;

        return -(shouldTravelForward ? 1 : -1) * trajectoryFollower.getAxialError();
    }
}
