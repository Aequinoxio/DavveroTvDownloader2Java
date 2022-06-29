package org.davverotvdownloader2.prefs;

import java.util.prefs.Preferences;

public enum AppPrefs {
    LoadFileLocation,
    SaveFolderLocation;
    private static final Preferences prefs = Preferences.userRoot().node(AppPrefs.class.getName());
    private static final String defaultDir = System.getProperty("user.home");

    public String get(String defaultValue) {
        return prefs.get(this.name(), defaultValue);
    }

    public String get() {
        return prefs.get(this.name(), defaultDir);
    }

    public void put(String value) {
        prefs.put(this.name(), value);
    }

}
