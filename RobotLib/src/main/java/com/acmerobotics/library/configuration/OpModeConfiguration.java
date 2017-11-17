package com.acmerobotics.library.configuration;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.qualcomm.ftccommon.configuration.RobotConfigFileManager;

public class OpModeConfiguration {

    public static final String PREFS_NAME = "opmode";
    public static final String PREF_ALLIANCE_COLOR = "alliance_color";
    public static final String PREF_DELAY = "delay";
    public static final String PREF_BALANCING_STONE = "balancing_stone";
    public static final String PREF_MATCH_TYPE = "match_type";
    public static final String PREF_MATCH_NUMBER = "match_number";
    public static final String PREF_AUTO_HEADING = "auto_heading";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public OpModeConfiguration(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public AllianceColor getAllianceColor() {
        return AllianceColor.fromIndex(preferences.getInt(PREF_ALLIANCE_COLOR, 0));
    }

    public void setAllianceColor(AllianceColor color) {
        editor.putInt(PREF_ALLIANCE_COLOR, color.getIndex());
    }

    public int getDelay() {
        return preferences.getInt(PREF_DELAY, 0);
    }

    public void setDelay(int delay) {
        if (0 <= delay && delay <= 30) {
            editor.putInt(PREF_DELAY, delay);
        }
    }

    public BalancingStone getBalancingStone() {
        return BalancingStone.fromIndex(preferences.getInt(PREF_BALANCING_STONE, 0));
    }

    public void setBalancingStone(BalancingStone balancingStone) {
        editor.putInt(PREF_BALANCING_STONE, balancingStone.getIndex());
    }

    public MatchType getMatchType() {
        return MatchType.fromIndex(preferences.getInt(PREF_MATCH_TYPE, 0));
    }

    public void setMatchType(MatchType type) {
        editor.putInt(PREF_MATCH_TYPE, type.getIndex());
    }

    public int getMatchNumber() {
        return preferences.getInt(PREF_MATCH_NUMBER, 1);
    }

    public void setMatchNumber(int num) {
        editor.putInt(PREF_MATCH_NUMBER, num);
    }

    public double getLastHeading() {
        return preferences.getFloat(PREF_AUTO_HEADING, 0);
    }

    public void setLastHeading(double heading) {
        editor.putFloat(PREF_AUTO_HEADING, (float) heading);
    }

    public String getActiveConfigName() {
        RobotConfigFileManager manager = new RobotConfigFileManager((Activity) context);
        return manager.getActiveConfig().getName();
    }

    public boolean commit() {
        return editor.commit();
    }

}
