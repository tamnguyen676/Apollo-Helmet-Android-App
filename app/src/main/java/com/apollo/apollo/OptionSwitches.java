package com.apollo.apollo;

import android.widget.Switch;

public class OptionSwitches {
    private Switch hudSwitch, blindspotSwitch, crashSwitch;

    public OptionSwitches(Switch hudSwitch, Switch blindspotSwitch, Switch crashSwitch) {
        this.hudSwitch = hudSwitch;
        this.blindspotSwitch = blindspotSwitch;
        this.crashSwitch = crashSwitch;
    }

    public Switch getHudSwitch() {
        return hudSwitch;
    }

    public void setHudSwitch(Switch hudSwitch) {
        this.hudSwitch = hudSwitch;
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
