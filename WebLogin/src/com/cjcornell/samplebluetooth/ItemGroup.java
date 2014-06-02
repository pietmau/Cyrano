/**
 * CLASS: ItemGroup
 *   A group of troubleshooting items.
 */

package com.cjcornell.samplebluetooth;

import org.json.JSONException;
import org.json.JSONObject;

public class ItemGroup {
    private int groupId;
    private String name;
    private String description;

    // These variables hold and handle default control flags
    private int stoppable;
    private int pausable;
    private int canAdvance;
    private int canGoBack;

    private String filename;

    /** Constructor */
    public ItemGroup(JSONObject json) throws JSONException {
        this.groupId = json.getInt("groupID");
        this.name = json.getString("name");
        this.description = json.getString("description");

        // Initializing control flag vars
        this.stoppable = json.getInt("cfStop");
        this.pausable = json.getInt("cfPause");
        this.canAdvance = json.getInt("cfNext");
        this.canGoBack = json.getInt("cfPrevious");

        this.filename = json.getString("filename");
    }
    
    /** Getters */
    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFilename() {
        return filename;
    }

    /** Play control settngs */
    public boolean isStoppable() {
        return stoppable > 0;
    }

    public boolean isPausable() {
        return pausable > 0;
    }

    public boolean canAdvance() {
        return canAdvance > 0;
    }

    public boolean canGoBack() {
        return canGoBack > 0;
    }
}
