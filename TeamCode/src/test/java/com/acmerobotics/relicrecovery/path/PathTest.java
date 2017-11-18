package com.acmerobotics.relicrecovery.path;

import com.acmerobotics.library.localization.Pose2d;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan
 */

public class PathTest {

    @Test
    public void testBasicPathCreation() {
        List<PathSegment> expectedSegments = Arrays.asList(
                new LineSegment(new Pose2d(0, 0, 0), new Pose2d(2, 0, 0)),
                new PointTurn(new Pose2d(2, 0), Math.PI / 2),
                new LineSegment(new Pose2d(2, 0, Math.PI / 2), new Pose2d(2, 2, Math.PI / 2)),
                new PointTurn(new Pose2d(2, 2), Math.PI / 2)
        );

        Path path = Path.createFromPoses(Arrays.asList(
                new Pose2d(0, 0),
                new Pose2d(2, 0),
                new Pose2d(2, 2, Math.PI)
        ));

        assertEquals(expectedSegments, path.getSegments());
    }

    @Test
    public void testEmptyTurnPathCreation() {
        List<PathSegment> expectedSegments = Arrays.asList(
                new LineSegment(new Pose2d(0, 0, 0), new Pose2d(2, 0, 0)),
                new LineSegment(new Pose2d(2, 0, 0), new Pose2d(4, 0, 0))
        );

        Path path = Path.createFromPoses(Arrays.asList(
                new Pose2d(0, 0),
                new Pose2d(2, 0),
                new Pose2d(4, 0)
        ));

        assertEquals(expectedSegments, path.getSegments());
    }
}
