package com.acmerobotics.relicrecovery.opmodes.auto2;

import android.annotation.SuppressLint;

import com.acmerobotics.library.util.TimestampedData;
import com.acmerobotics.relicrecovery.configuration.AllianceColor;
import com.acmerobotics.relicrecovery.configuration.BalancingStone;
import com.acmerobotics.relicrecovery.configuration.Cryptobox;
import com.acmerobotics.relicrecovery.opmodes.AutoOpMode;
import com.acmerobotics.relicrecovery.opmodes.AutoPaths;
import com.acmerobotics.relicrecovery.subsystems.JewelSlapper;
import com.acmerobotics.relicrecovery.subsystems.MecanumDrive;
import com.acmerobotics.relicrecovery.vision.JewelPosition;
import com.acmerobotics.splinelib.Pose2d;
import com.acmerobotics.splinelib.Vector2d;
import com.acmerobotics.splinelib.path.ConstantInterpolator;
import com.acmerobotics.splinelib.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;

import java.util.HashMap;
import java.util.Map;

@Autonomous
public class FarThreeGlyphAuto extends AutoOpMode {
    public static final double EPSILON = 1e-6;

    private BalancingStone stone;
    private Cryptobox crypto;

    @Override
    protected void setup() {
        stone = robot.config.getBalancingStone();
        crypto = stone.getCryptobox();

        if (stone == BalancingStone.NEAR_BLUE || stone == BalancingStone.NEAR_RED) {
            telemetry.log().add("Invalid balancing stone: " + stone + "!");
            telemetry.update();

            robot.sleep(1);

            requestOpModeStop();

            return;
        }

        robot.drive.setEstimatedPosition(stone.getPosition());
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void run() {
        double startTime = TimestampedData.getCurrentTime();

        int yMultiplier = crypto.getAllianceColor() == AllianceColor.BLUE ? -1 : 1;

        // jewel logic here
        RelicRecoveryVuMark vuMark = vuMarkTracker.getVuMark();
        JewelPosition jewelPosition = jewelTracker.getJewelPosition();
        jewelTracker.disable();

        final Map<RelicRecoveryVuMark, RelicRecoveryVuMark> COLUMN_TRANSITION = new HashMap<>();
        COLUMN_TRANSITION.put(RelicRecoveryVuMark.LEFT,
                robot.config.getAllianceColor() == AllianceColor.BLUE ? RelicRecoveryVuMark.RIGHT : RelicRecoveryVuMark.CENTER);
        COLUMN_TRANSITION.put(RelicRecoveryVuMark.CENTER,
                robot.config.getAllianceColor() == AllianceColor.BLUE ? RelicRecoveryVuMark.RIGHT : RelicRecoveryVuMark.LEFT);
        COLUMN_TRANSITION.put(RelicRecoveryVuMark.RIGHT,
                robot.config.getAllianceColor() == AllianceColor.BLUE ? RelicRecoveryVuMark.CENTER : RelicRecoveryVuMark.LEFT);

        lowerArmAndSlapper();

        boolean removeLeft = robot.config.getAllianceColor() == jewelPosition.rightColor();

        if (robot.config.getAllianceColor() == AllianceColor.RED) {
            if (!removeLeft) {
                robot.jewelSlapper.setSlapperPosition(JewelSlapper.SlapperPosition.RIGHT);
                robot.sleep(0.75);
                raiseArmAndSlapper();
            } else {
                robot.jewelSlapper.setSlapperPosition(JewelSlapper.SlapperPosition.PARALLEL);
                robot.sleep(0.75);
            }
        }

        RelicRecoveryVuMark firstColumn = vuMark == RelicRecoveryVuMark.UNKNOWN ? RelicRecoveryVuMark.CENTER : vuMark;
        RelicRecoveryVuMark secondColumn = COLUMN_TRANSITION.get(firstColumn);

        Pose2d stonePose = AutoPaths.getAdjustedBalancingStonePose(stone);
        Vector2d firstColumnPosition = AutoPaths.getCryptoboxColumnPosition(crypto, firstColumn);
        Vector2d biasedFirstColumnPosition = firstColumnPosition.plus(new Vector2d(0, -yMultiplier * LATERAL_BIAS));
        Vector2d secondColumnPosition = AutoPaths.getCryptoboxColumnPosition(crypto, secondColumn);
        Vector2d biasedSecondColumnPosition = secondColumnPosition.plus(new Vector2d(0, yMultiplier * LATERAL_BIAS));

        Trajectory stoneToCrypto = robot.drive.trajectoryBuilder(stonePose)
                .reverse()
                .turnTo(removeLeft ? -EPSILON : EPSILON) // fun hack
                .lineTo(new Vector2d(-44, stonePose.y()))
                .lineTo(new Vector2d(-44, biasedFirstColumnPosition.y()), new ConstantInterpolator(0))
                .waitFor(1.0)
                .build();
        robot.drive.setEstimatedPose(stoneToCrypto.start());
        if (robot.config.getAllianceColor() == AllianceColor.BLUE) {
            robot.drive.setVelocityPIDCoefficients(MecanumDrive.SLOW_VELOCITY_PID);
        }
        robot.drive.followTrajectory(stoneToCrypto);

        robot.drive.extendProximitySwivel();
        robot.drive.extendUltrasonicSwivel();

        robot.sleep(0.5);
        raiseArmAndSlapper();
        robot.drive.waitForTrajectoryFollower();
        robot.drive.setVelocityPIDCoefficients(MecanumDrive.NORMAL_VELOCITY_PID);

        robot.drive.getUltrasonicDistance(DistanceUnit.INCH);
        robot.drive.getUltrasonicDistance(DistanceUnit.INCH);
        double distance = (robot.drive.getUltrasonicDistance(DistanceUnit.INCH) + 7) - 71;
        robot.drive.setEstimatedPosition(new Vector2d(distance, robot.drive.getEstimatedPosition().y()));

        Vector2d estimatedPosition = robot.drive.getEstimatedPosition();
        Trajectory cryptoApproach1 = robot.drive.trajectoryBuilder(new Pose2d(estimatedPosition, stoneToCrypto.end().heading()))
                .reverse()
                .lineTo(new Vector2d(-56, biasedFirstColumnPosition.y()))
                .waitFor(0.5)
                .build();

        robot.drive.followTrajectory(cryptoApproach1);
        robot.drive.waitForTrajectoryFollower();

        robot.drive.retractUltrasonicSwivel();

        robot.drive.enableHeadingCorrection(cryptoApproach1.end().heading());
        robot.drive.alignWithColumn();
        robot.drive.waitForColumnAlign();
        robot.drive.disableHeadingCorrection();
        robot.drive.setEstimatedPosition(new Vector2d(-56, firstColumnPosition.y()));

        robot.drive.retractProximitySwivel();
        robot.dumpBed.dump();
        robot.sleep(0.5);

        Trajectory cryptoToPit = robot.drive.trajectoryBuilder(new Pose2d(-56, firstColumnPosition.y(), cryptoApproach1.end().heading()))
                .lineTo(new Vector2d(-44, firstColumnPosition.y()))
                .lineTo(new Vector2d(-44, yMultiplier * 16), new ConstantInterpolator(0))
                .turnTo(-yMultiplier * Math.PI / 4)
                .lineTo(new Vector2d(-12, yMultiplier * 16), new ConstantInterpolator(-yMultiplier * Math.PI / 4))
                .forward(12)
                .reverse()
                .back(12)
                .build();
        robot.drive.followTrajectory(cryptoToPit);
        robot.sleep(0.2 * cryptoToPit.duration());
        robot.dumpBed.retract();
        robot.intake.autoIntake();
        robot.drive.waitForTrajectoryFollower();

        Trajectory pitToCrypto = robot.drive.trajectoryBuilder(cryptoToPit.end())
                .reverse()
                .turnTo(0)
//                .lineTo(new Vector2d(-44, yMultiplier * 16))
                .lineTo(new Vector2d(-40, biasedSecondColumnPosition.y()), new ConstantInterpolator(0))
                .waitFor(1.0)
                .build();
        robot.drive.followTrajectory(pitToCrypto);
        robot.sleep(0.5 * pitToCrypto.duration());
        robot.intake.setIntakePower(1);
        robot.drive.extendUltrasonicSwivel();
        robot.drive.extendProximitySwivel();
        robot.drive.waitForTrajectoryFollower();
        robot.intake.setIntakePower(0);

        robot.drive.getUltrasonicDistance(DistanceUnit.INCH);
        robot.drive.getUltrasonicDistance(DistanceUnit.INCH);
        double distance2 = (robot.drive.getUltrasonicDistance(DistanceUnit.INCH) + 7) - 71;
        robot.drive.setEstimatedPosition(new Vector2d(distance2, robot.drive.getEstimatedPosition().y()));

        estimatedPosition = robot.drive.getEstimatedPosition();
        Trajectory cryptoApproach2 = robot.drive.trajectoryBuilder(new Pose2d(estimatedPosition, pitToCrypto.end().heading()))
                .reverse()
                .lineTo(new Vector2d(-56, biasedSecondColumnPosition.y()))
                .waitFor(0.5)
                .build();

        robot.drive.followTrajectory(cryptoApproach2);
        robot.drive.waitForTrajectoryFollower();
        robot.drive.retractUltrasonicSwivel();

        double elapsedTime = TimestampedData.getCurrentTime() - startTime;
        if (elapsedTime < 28) {
            robot.drive.enableHeadingCorrection(cryptoApproach2.end().heading());
            robot.drive.alignWithColumn();
            robot.drive.waitForColumnAlign();
            robot.drive.disableHeadingCorrection();
            robot.drive.setEstimatedPosition(new Vector2d(-56, secondColumnPosition.y()));

            robot.drive.retractProximitySwivel();
            robot.dumpBed.dump();
            robot.sleep(0.5);

            robot.drive.followTrajectory(robot.drive.trajectoryBuilder(cryptoApproach2.end())
                    .forward(6)
                    .build());
            robot.drive.waitForTrajectoryFollower();
            robot.dumpBed.retract();
        } else {
            robot.drive.retractProximitySwivel();
            robot.drive.followTrajectory(robot.drive.trajectoryBuilder(cryptoApproach2.end())
                    .forward(6)
                    .build());
            robot.drive.waitForTrajectoryFollower();
        }

        robot.waitOneFullCycle();

        telemetry.log().add(String.format("Took %.2fs", TimestampedData.getCurrentTime() - startTime));
        telemetry.update();

        while (opModeIsActive());
    }
}
