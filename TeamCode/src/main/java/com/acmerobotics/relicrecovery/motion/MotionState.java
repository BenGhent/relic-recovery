package com.acmerobotics.relicrecovery.motion;

/**
 * Created by kelly on 10/12/2017.
 *
 */

public class MotionState {

    public double x; //position
    public double v; //velocity
    public double a; //acceleration
    public double j; //jerk
    public long t; // time

    public MotionState (double x, double y, double a, double j, long t) {
        this.x = x;
        this.v = v;
        this.a = a;
        this.j = j;
        this.t = t;
    }

    public MotionState (double x, double v, double a, double j) {
        this(x, v, a, j, System.nanoTime());
    }

    public double v2() {
        return v * v;
    }

    public double a2() {
        return a * a;
    }

    public double j2() {
        return j * j;
    }
}
