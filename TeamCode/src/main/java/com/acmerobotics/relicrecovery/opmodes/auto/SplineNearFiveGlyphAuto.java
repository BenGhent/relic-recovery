package com.acmerobotics.relicrecovery.opmodes.auto;

import android.annotation.SuppressLint;
import android.util.TimingLogger;

import com.acmerobotics.library.util.TimestampedData;
import com.acmerobotics.relicrecovery.configuration.AllianceColor;
import com.acmerobotics.relicrecovery.configuration.BalancingStone;
import com.acmerobotics.relicrecovery.configuration.Cryptobox;
import com.acmerobotics.relicrecovery.localization.UltrasonicLocalizer;
import com.acmerobotics.relicrecovery.opmodes.AutoOpMode;
import com.acmerobotics.relicrecovery.opmodes.AutoPaths;
import com.acmerobotics.relicrecovery.subsystems.JewelSlapper;
import com.acmerobotics.relicrecovery.vision.JewelPosition;
import com.acmerobotics.splinelib.Pose2d;
import com.acmerobotics.splinelib.Vector2d;
import com.acmerobotics.splinelib.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;

import java.util.HashMap;
import java.util.Map;

@Autonomous
public class SplineNearFiveGlyphAuto extends AutoOpMode {
    public static final Map<RelicRecoveryVuMark, RelicRecoveryVuMark> COLUMN_TRANSITION1 = new HashMap<>();
    static {
        COLUMN_TRANSITION1.put(RelicRecoveryVuMark.LEFT, RelicRecoveryVuMark.RIGHT);
        COLUMN_TRANSITION1.put(RelicRecoveryVuMark.CENTER, RelicRecoveryVuMark.RIGHT);
        COLUMN_TRANSITION1.put(RelicRecoveryVuMark.RIGHT, RelicRecoveryVuMark.LEFT);
    }
    public static final Map<RelicRecoveryVuMark, RelicRecoveryVuMark> COLUMN_TRANSITION2 = new HashMap<>();
    static {
        COLUMN_TRANSITION2.put(RelicRecoveryVuMark.LEFT, RelicRecoveryVuMark.CENTER);
        COLUMN_TRANSITION2.put(RelicRecoveryVuMark.CENTER, RelicRecoveryVuMark.LEFT);
        COLUMN_TRANSITION2.put(RelicRecoveryVuMark.RIGHT, RelicRecoveryVuMark.CENTER);
    }

    private UltrasonicLocalizer ultrasonicLocalizer;
    private BalancingStone stone;
    private Cryptobox crypto;

    private static boolean isNearColumn(RelicRecoveryVuMark column, AllianceColor color) {
        return (column == RelicRecoveryVuMark.RIGHT && color == AllianceColor.RED)
                || (column == RelicRecoveryVuMark.LEFT && color == AllianceColor.BLUE);
    }

    @Override
    protected void setup() {
        stone = robot.config.getBalancingStone();
        crypto = stone.getCryptobox();

        if (stone == BalancingStone.FAR_BLUE || stone == BalancingStone.FAR_RED) {
            telemetry.log().add("Invalid balancing stone: " + stone + "!");
            telemetry.update();

            robot.sleep(1);

            requestOpModeStop();

            return;
        }

        ultrasonicLocalizer = new UltrasonicLocalizer(robot.drive);
        robot.addListener(() -> {
            robot.dashboard.getTelemetry().addData("ultrasonicDistance", ultrasonicLocalizer.getUltrasonicDistance(DistanceUnit.INCH));
            telemetry.addData("ultrasonicDistance", ultrasonicLocalizer.getUltrasonicDistance(DistanceUnit.INCH));
        });
        robot.drive.setLocalizer(ultrasonicLocalizer);
        robot.drive.setEstimatedPosition(stone.getPosition());
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void run() {
        TimingLogger timings = new TimingLogger("Auto", "splineNearFiveGlyph");
        double startTime = TimestampedData.getCurrentTime();

        int yMultiplier = (crypto.getAllianceColor() == AllianceColor.BLUE) ? -1 : 1;

        RelicRecoveryVuMark vuMark = vuMarkTracker.getVuMark();
        JewelPosition jewelPosition = jewelTracker.getJewelPosition();
        jewelTracker.disable();

        timings.addSplit("vuMark");

        lowerArmAndSlapper();

        boolean removeLeft = robot.config.getAllianceColor() == jewelPosition.rightColor();

        if (removeLeft && robot.config.getAllianceColor() == AllianceColor.BLUE) {
            robot.jewelSlapper.setSlapperPosition(JewelSlapper.SlapperPosition.LEFT);
            robot.sleep(0.4);
            raiseArmAndSlapper();
        } else if (!removeLeft && robot.config.getAllianceColor() == AllianceColor.RED) {
            robot.jewelSlapper.setSlapperPosition(JewelSlapper.SlapperPosition.RIGHT);
            robot.sleep(0.4);
            raiseArmAndSlapper();
        } else {
            robot.jewelSlapper.setSlapperPosition(JewelSlapper.SlapperPosition.PARALLEL);
            robot.sleep(0.4);
        }

        timings.addSplit("jewel");

        RelicRecoveryVuMark firstColumn = (vuMark == RelicRecoveryVuMark.UNKNOWN) ? RelicRecoveryVuMark.LEFT : vuMark;
        RelicRecoveryVuMark secondColumn = COLUMN_TRANSITION1.get(firstColumn);
        RelicRecoveryVuMark thirdColumn = COLUMN_TRANSITION2.get(firstColumn);

        Pose2d stonePose = AutoPaths.getAdjustedBalancingStonePose(stone);
        Vector2d firstColumnPosition = AutoPaths.getCryptoboxColumnPosition(crypto, firstColumn);
        Vector2d secondColumnPosition = AutoPaths.getCryptoboxColumnPosition(crypto, secondColumn);
        Vector2d thirdColumnPosition = AutoPaths.getCryptoboxColumnPosition(crypto, thirdColumn);

        Trajectory stoneToCrypto1 = robot.drive.trajectoryBuilder(stonePose)
                .lineTo(new Vector2d(firstColumnPosition.x(), stonePose.y()))
                .turnTo(-yMultiplier * Math.PI / 2)
                .lineTo(new Vector2d(firstColumnPosition.x(), yMultiplier * 56))
                .waitFor(0.5)
                .build();

        timings.addSplit("stoneToCrypto1 gen");

        robot.drive.setEstimatedPose(stoneToCrypto1.start());
        robot.drive.followTrajectory(stoneToCrypto1);
        robot.sleep(0.5);
        robot.drive.extendProximitySwivel();
        raiseArmAndSlapper();
        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("stoneToCrypto1");

        robot.drive.retractUltrasonicSwivel();
        robot.drive.enableHeadingCorrection(-yMultiplier * Math.PI / 2);

        robot.drive.alignWithColumn(robot.config.getAllianceColor());
        robot.drive.waitForColumnAlign();

        robot.drive.disableHeadingCorrection();
        robot.drive.setEstimatedPosition(stoneToCrypto1.end().pos());

        timings.addSplit("columnAlign1");

        robot.drive.retractProximitySwivel();
        robot.dumpBed.dump();
        robot.sleep(0.5);

        timings.addSplit("dump1");

        Trajectory cryptoToPit2;
        if (firstColumn == RelicRecoveryVuMark.CENTER || isNearColumn(firstColumn, robot.config.getAllianceColor())) {
            cryptoToPit2 = robot.drive.trajectoryBuilder(stoneToCrypto1.end())
                    .beginComposite()
                    .splineTo(new Pose2d(12, yMultiplier * 24, -yMultiplier * 3 * Math.PI / 4))
                    .lineTo(new Vector2d(0, yMultiplier * 12))
                    .closeComposite()
                    .build();
        } else {
            cryptoToPit2 = robot.drive.trajectoryBuilder(stoneToCrypto1.end())
                    .beginComposite()
                    .splineTo(new Pose2d(12, yMultiplier * 24, -yMultiplier * Math.PI / 4))
                    .lineTo(new Vector2d(24, yMultiplier * 12))
                    .closeComposite()
                    .build();
        }

        timings.addSplit("cryptoToPit2 gen");

        robot.drive.followTrajectory(cryptoToPit2);

        robot.sleep(0.25 * cryptoToPit2.duration());

        robot.dumpBed.retract();
        robot.intake.autoIntake();

        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("cryptoToPit2");

        Trajectory pitToCrypto2 = robot.drive.trajectoryBuilder(cryptoToPit2.end())
                .splineTo(new Pose2d(secondColumnPosition.x(), yMultiplier * 40, -yMultiplier * Math.PI / 2))
                .waitFor(0.25)
                .build();

        timings.addSplit("pitToCrypto2 gen");

        robot.drive.followTrajectory(pitToCrypto2);

        robot.drive.extendUltrasonicSwivel();
        robot.drive.extendProximitySwivel();

        robot.intake.setIntakePower(1);
        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("pitToCrypto2");

        ultrasonicLocalizer.enableUltrasonicFeedback();
        robot.waitOneFullCycle();
        ultrasonicLocalizer.disableUltrasonicFeedback();

        Trajectory cryptoApproach2 = robot.drive.trajectoryBuilder(new Pose2d(robot.drive.getEstimatedPosition(), pitToCrypto2.end().heading()))
                .lineTo(new Vector2d(secondColumnPosition.x(), yMultiplier * 56))
                .waitFor(0.5)
                .build();

        timings.addSplit("cryptoApproach2 gen");

        robot.drive.followTrajectory(cryptoApproach2);
        robot.sleep(0.5 * cryptoApproach2.duration());
        robot.intake.setIntakePower(0);
        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("cryptoApproach2");

        robot.drive.retractUltrasonicSwivel();
        robot.drive.enableHeadingCorrection(-yMultiplier * Math.PI / 2);

        robot.drive.alignWithColumn(robot.config.getAllianceColor());
        robot.drive.waitForColumnAlign();

        robot.drive.disableHeadingCorrection();
        robot.drive.setEstimatedPosition(cryptoApproach2.end().pos());

        timings.addSplit("columnAlign2");

        robot.drive.retractProximitySwivel();
        robot.dumpBed.dump();
        robot.sleep(0.5);

        timings.addSplit("dump2");

        Trajectory cryptoToPit3;
        if (isNearColumn(secondColumn, robot.config.getAllianceColor())) {
            cryptoToPit3 = robot.drive.trajectoryBuilder(cryptoApproach2.end())
                    .beginComposite()
                    .splineTo(new Pose2d(12, yMultiplier * 24, -yMultiplier * 3 * Math.PI / 4))
                    .lineTo(new Vector2d(0, yMultiplier * 12))
                    .closeComposite()
                    .build();
        } else {
            cryptoToPit3 = robot.drive.trajectoryBuilder(cryptoApproach2.end())
                    .beginComposite()
                    .splineTo(new Pose2d(12, yMultiplier * 24, -yMultiplier * Math.PI / 4))
                    .lineTo(new Vector2d(24, yMultiplier * 12))
                    .closeComposite()
                    .build();
        }

        timings.addSplit("cryptoToPit3 gen");

        robot.drive.followTrajectory(cryptoToPit3);

        robot.sleep(0.25 * cryptoToPit3.duration());

        robot.dumpBed.retract();
        robot.intake.autoIntake();

        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("cryptoToPit3");

        Trajectory pitToCrypto3 = robot.drive.trajectoryBuilder(cryptoToPit3.end())
                .splineTo(new Pose2d(thirdColumnPosition.x(), yMultiplier * 40, -yMultiplier * Math.PI / 2))
                .waitFor(0.25)
                .build();

        timings.addSplit("pitToCrypto3 gen");

        robot.drive.followTrajectory(pitToCrypto3);

        robot.drive.extendUltrasonicSwivel();
        robot.drive.extendProximitySwivel();

        robot.intake.setIntakePower(1);
        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("pitToCrypto3");

        ultrasonicLocalizer.enableUltrasonicFeedback();
        robot.waitOneFullCycle();
        ultrasonicLocalizer.disableUltrasonicFeedback();

        Trajectory cryptoApproach3 = robot.drive.trajectoryBuilder(new Pose2d(robot.drive.getEstimatedPosition(), pitToCrypto3.end().heading()))
                .lineTo(new Vector2d(thirdColumnPosition.x(), yMultiplier * 56))
                .waitFor(0.5)
                .build();

        timings.addSplit("cryptoApproach3 gen");

        robot.drive.followTrajectory(cryptoApproach3);
        robot.sleep(0.5 * cryptoApproach3.duration());
        robot.intake.setIntakePower(0);
        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("cryptoApproach3");

        robot.drive.retractUltrasonicSwivel();
        robot.drive.enableHeadingCorrection(-yMultiplier * Math.PI / 2);

        robot.drive.alignWithColumn(robot.config.getAllianceColor());
        robot.drive.waitForColumnAlign();

        robot.drive.disableHeadingCorrection();
        robot.drive.setEstimatedPosition(cryptoApproach3.end().pos());

        timings.addSplit("columnAlign3");

        robot.dumpBed.dump();
        robot.sleep(0.5);

        timings.addSplit("dump3");

        robot.drive.followTrajectory(robot.drive.trajectoryBuilder(cryptoApproach3.end())
                .forward(8)
                .build());

        timings.addSplit("return gen");

        robot.drive.waitForTrajectoryFollower();

        timings.addSplit("return");

        robot.dumpBed.retract();
        robot.drive.retractProximitySwivel();

        robot.waitOneFullCycle();

        timings.dumpToLog();

        telemetry.log().add(String.format("Took %.2fs", TimestampedData.getCurrentTime() - startTime));
        telemetry.update();

        while (opModeIsActive());
    }
}
