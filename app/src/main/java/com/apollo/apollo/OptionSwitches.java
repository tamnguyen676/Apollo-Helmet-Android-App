package com.apollo.apollo;

import android.widget.Switch;

public class OptionSwitches {
    private Switch rearviewSwitch, navigationSwitch, blindspotSwitch, crashSwitch;

    public OptionSwitches(Switch rearviewSwitch, Switch navigationSwitch, Switch blindspotSwitch, Switch crashSwitch) {
        this.rearviewSwitch = rearviewSwitch;
        this.navigationSwitch = navigationSwitch;
        this.blindspotSwitch = blindspotSwitch;
        this.crashSwitch = crashSwitch;
    }

    public Switch getRearviewSwitch() {
        return rearviewSwitch;
    }

    public void setRearviewSwitch(Switch rearviewSwitch) {
        this.rearviewSwitch = rearviewSwitch;
    }

    public Switch getNavigationSwitch() {
        return navigationSwitch;
    }

    public void setNavigationSwitch(Switch navigationSwitch) {
        this.navigationSwitch = navigationSwitch;
    }

    public Switch getBlindspotSwitch() {
        return blindspotSwitch;
    }

    public void setBlindspotSwitch(Switch blindspotSwitch) {
        this.blindspotSwitch = blindspotSwitch;
    }

    public Switch getCrashSwitch() {
        return crashSwitch;
    }

    public void setCrashSwitch(Switch crashSwitch) {
        this.crashSwitch = crashSwitch;
    }


}
