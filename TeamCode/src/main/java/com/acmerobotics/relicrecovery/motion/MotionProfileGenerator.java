package com.acmerobotics.relicrecovery.motion;

import com.acmerobotics.relicrecovery.util.SuperArrayList;

import java.util.ArrayList;

/**
 * Created by kelly on 10/16/2017.
 *        1   2   3      4        5   6   7
 *       ___                             ___
 * J:   |   |___     ___________     ___|   |
 *              |___|           |___|
 *
 */

public class MotionProfileGenerator {

    //static class
    private MotionProfileGenerator() {}

    public static MotionProfile generateProfile(MotionState start, MotionGoal goal, MotionConstraints constraints) {

        //for simplicity, assume that the profile requires positive movement
        if (goal.pos < start.x) {
            return generateFlippedProfile(start, goal, constraints);
        }

        MotionProfile profile = new MotionProfile(start);

        //if we are headed away from the goal, first come to a stop
        if (start.v < 0) {
            double[] stoppingTimes = getDeltaVTimes(-start.v, constraints);
            profile.appendControl(constraints.maxJ, stoppingTimes[0]);
            profile.appendControl(0, stoppingTimes[1]);
            profile.appendControl(-constraints.maxJ, stoppingTimes[2]);
        }

        //neat lets go to the goal now

        boolean canStop = true;
        double dv = profile.end().v - goal.maxAbsV;
        if (dv > 0) {
            MotionProfile stoppingProfile = new MotionProfile(profile.end());
            double[] stoppingTimes = getDeltaVTimes(dv, constraints);
            stoppingProfile.appendControl(-constraints.maxJ, stoppingTimes[0]);
            stoppingProfile.appendControl(0, stoppingTimes[1]);
            stoppingProfile.appendControl(constraints.maxJ, stoppingTimes[2]);
            double stoppingDistance = stoppingProfile.end().x;

            if (stoppingDistance > goal.pos) {
                //well, we can't stop now
                if (constraints.endBehavior == MotionConstraints.END_BEHAVIOR.OVERSHOOT) {
                    System.out.print("overshoot");
                    stoppingTimes = getDeltaVTimes(start.v, constraints);
                    profile.appendControl(-constraints.maxJ, stoppingTimes[0]);
                    profile.appendControl(0, stoppingTimes[1]);
                    profile.appendControl(constraints.maxJ, stoppingTimes[2]);
                    profile.appendProfile(generateProfile(profile.end(), goal, constraints));
                    return profile;
                }
                else if(constraints.endBehavior == MotionConstraints.END_BEHAVIOR.VIOLATE_MAX_ABS_A) {
                    System.out.println("violate a");
                    // well if we are already slamming on the brakes then we might as well do infinite jerk
                    double stoppingA = (Math.pow(goal.maxAbsV, 2) - Math.pow(profile.end().v, 2)) / (2 * (goal.pos - profile.end().x));
                    double stoppingTime = (goal.maxAbsV - profile.end().v) / stoppingA;
                    profile.appendInfiniteJerkControl(stoppingA, stoppingTime);
                    return profile;
                }
                else if(constraints.endBehavior == MotionConstraints.END_BEHAVIOR.VIOLATE_MAX_ABS_V) {
                    System.out.println("violate v");
                    //we will just work with what we got - we will slow down as much as possible
                    profile.appendControl(-constraints.maxJ, stoppingTimes[0]);
                    profile.appendControl(0, stoppingTimes[1]);
                    profile.appendControl(constraints.maxJ, stoppingTimes[2]);
                    double timeAtGoal = profile.timeAtPos(goal.pos);
                    return profile.trimAfter(timeAtGoal);
                }
            }
        }

        //ok cool we should be able to do it now

        double dX = goal.pos - profile.end().x;
        double j = constraints.maxJ;

        double [] timesToAccel = getDeltaVTimes(constraints.maxV - profile.end().v, constraints);
        double [] timesToDecel = getDeltaVTimes(goal.maxAbsV - constraints.maxV, constraints);

        MotionProfile accelProfile = new MotionProfile(new MotionState (0,0,0,0,0));
        accelProfile.appendControl(j, timesToAccel[0]);
        accelProfile.appendControl(0, timesToAccel[1]);
        accelProfile.appendControl(-j, timesToAccel[2] + timesToDecel[0]);
        accelProfile.appendControl(0, timesToDecel[1]);
        accelProfile.appendControl(j, timesToDecel[2]);
        double dxWithoutCoast = accelProfile.end().x;

        double dxCoast = dX - dxWithoutCoast;
        double timeCoast = Math.max(0, dxCoast / constraints.maxV);

        //spend hours trying to figure out the correct way to find max reachable velocity
        //sculpt velocity graphs out of mashed potatoes
        //loose sleep over it
        //give up and decide a binary search is not that bad

        double maxV = constraints.maxV;
        double epsilon = 1E-10;
        double maxVmax = constraints.maxV;
        double maxVmin = 0;
        if (dxCoast < 0) {

            //yeah its a hack but its so much easier than the alternative, and on the plus side it converges fairly fast and could definitely be optimized
            int iterations = 0;
            while (Math.abs(dxCoast) > epsilon && iterations < 1000) {
                iterations ++;
                System.out.println(iterations + ", " + dxCoast + ", " + maxV);
                if (dxCoast < 0) {
                    maxVmax = maxV;
                    maxV -= (maxV - maxVmin)/2;
                } else {
                    maxVmin = maxV;
                    maxV += (maxVmax - maxV)/2;
                }
                //this could probably be done the correct way, but this does not slow it down that much and improves readability
                accelProfile = new MotionProfile(new MotionState(0, 0, 0, 0, 0));
                timesToAccel = getDeltaVTimes(maxV - profile.end().v, constraints);
                timesToDecel = getDeltaVTimes(goal.maxAbsV - maxV, constraints);
                accelProfile.appendControl(j, timesToAccel[0]);
                accelProfile.appendControl(0, timesToAccel[1]);
                accelProfile.appendControl(-j, timesToAccel[2] + timesToDecel[0]);
                accelProfile.appendControl(0, timesToDecel[1]);
                accelProfile.appendControl(j, timesToDecel[2]);
                dxWithoutCoast = accelProfile.end().x;
                dxCoast = dX - dxWithoutCoast;
            }
        }

        profile.appendControl(j, timesToAccel[0]);
        profile.appendControl(0, timesToAccel[1]);
        profile.appendControl(-j, timesToAccel[2]);
        profile.appendControl(0, timeCoast);
        profile.appendControl(-j, timesToDecel[0]);
        profile.appendControl(0, timesToDecel[1]);
        profile.appendControl(j, timesToDecel[2]);

        return profile;
    }

    private static double[] getDeltaVTimes(double dv, MotionConstraints constraints) {
        dv = Math.abs(dv / 2.0); //it should be symmetrical, so we will only deal with one half of it
        double jerkTime = constraints.maxA / constraints.maxJ; //time to max acceleration
        double maxJerkTime = Math.sqrt((2.0 * dv) / constraints.maxJ); //time to delta v
        jerkTime = Math.min(jerkTime, maxJerkTime);
        double dvInJerk = .5 * constraints.maxJ * jerkTime * jerkTime;
        double maxAccelReached = jerkTime * constraints.maxJ;
        dv = dv - dvInJerk; //now we only have to coast at max accel to finish off dv
        double coastTime = dv / maxAccelReached; //cool, now we just jerk for jerkTime, then coast for 2*coastTime, then reverse jerk for jerkTime
        return new double[] {jerkTime, coastTime * 2.0, jerkTime};
    }

    private static MotionProfile generateFlippedProfile(MotionState start, MotionGoal goal, MotionConstraints constraints) {
        MotionProfile profile = generateProfile(start.flipped(), goal.flipped(), constraints);
        SuperArrayList<MotionSegment> segments = profile.segments();
        SuperArrayList<MotionSegment> flipped = new SuperArrayList<>();
        for (MotionSegment seg: segments) {
            flipped.add(new MotionSegment(seg.start().flipped(), seg.dt()));
        }
        return new MotionProfile (flipped);
    }

}
