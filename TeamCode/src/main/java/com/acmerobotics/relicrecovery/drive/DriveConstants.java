package com.acmerobotics.relicrecovery.drive;

import com.acmerobotics.library.dashboard.config.Config;
import com.acmerobotics.relicrecovery.motion.MotionConstraints;
import com.acmerobotics.relicrecovery.motion.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.PIDCoefficients;

/**
 * Created by ryanbrott on 10/28/17.
 */

@Config
public class DriveConstants {
    public static MotionConstraints AXIAL_CONSTRAINTS = new MotionConstraints(16.0, 32.0, 64.0, MotionConstraints.EndBehavior.OVERSHOOT);
    public static MotionConstraints POINT_TURN_CONSTRAINTS = new MotionConstraints(1.5, 3.0, 6.0, MotionConstraints.EndBehavior.VIOLATE_MAX_ABS_V);

    public static PIDFCoefficients HEADING_COEFFS = new PIDFCoefficients(-0.05, 0, 0, 0.1, 0);
    public static PIDFCoefficients AXIAL_COEFFS = new PIDFCoefficients(0, 0, 0, 0.055, 0);
    public static PIDCoefficients LATERAL_COEFFS = new PIDCoefficients(-0.15, 0, 0);

    public static PIDCoefficients BALANCE_AXIAL_COEFFS = new PIDCoefficients(0, 0, 0);
    public static PIDCoefficients BALANCE_LATERAL_COEFFS = new PIDCoefficients(0, 0, 0);
    
    public static PIDCoefficients MAINTAIN_HEADING_COEFFS = new PIDCoefficients(-0.5, 0, 0);

    public static double RAMP_MAX_ACCEL = 25;
}
