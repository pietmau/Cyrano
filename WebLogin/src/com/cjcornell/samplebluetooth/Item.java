/**
 * CLASS: Item
 *   A troubleshooting item. This class represents various attributes of a troubleshooting
 *   item, as well as what is allowed to do to it. There are no setters, because it should
 *   be initialized directly from the database.
 */

package com.cjcornell.samplebluetooth;

import com.cjcornell.samplebluetooth.data.AppSettings;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Item {
    // Basic attributes of a troubleshooting attribute
    private int itemNumber;
    private String name;
    private String description;
    private int type;
    private String filename;
    private String url;
    private double delay; // Time delay

    // These variables hold and handle control flags
    private boolean first;
    private boolean last;
    private int stoppable;
    private int pausable;
    private int canAdvance;
    private int canGoBack;

    private ItemGroup parent;
    
    // A list of branches on the particular item
    private List<Branch> branches;

    // This class keeps tracks of branches contained within an item
    public class Branch {
        public String label;
        public int groupId;
        public int instructionId;

        public Branch(JSONObject json) throws JSONException {
            try {
                label = json.getString("label");
                String record = json.getString("record");
                if (label == null || label.equals("") || label.equals("null")) {
                    label = null;
                    groupId = instructionId = -1;
                } else if (record == null || record.equals("") || record.equals("null")) {
                    groupId = instructionId = -1;
                } else if (record.contains(",")) {
                    String[] data = record.split(",");
                    if (data.length != 2) {
                        label = null;
                        return;
                    }
                    groupId = Integer.parseInt(data[0]);
                    instructionId = Integer.parseInt(data[1]);
                } else {
                    groupId = -1;
                    instructionId = Integer.parseInt(record);
                }
            } catch (NumberFormatException e) {
                label = null;
            }
        }
    }
    
    /** Constructor */
    public Item(JSONObject json, boolean isLast, ItemGroup parent) throws JSONException {
       // Initialize the basic attributes
       this.itemNumber = json.getInt("instructionNumber");
       this.name = json.getString("name");
       this.description = json.getString("description");
       this.type = json.getInt("type");
       this.filename = json.getString("filename");
       this.url = json.getString("url");
       this.delay = json.getDouble("delay");
       
       // Initialize the control flag variables - this is first if we don't have a parent
       this.first = (itemNumber == 1);
       this.last = isLast;
       this.stoppable = json.getInt("cfStop");
       this.pausable = json.getInt("cfPause");
       this.canAdvance = json.getInt("cfNext");
       this.canGoBack = json.getInt("cfPrevious");
       
       this.parent = parent;
       
       // Add the branches
       this.branches = new ArrayList<Branch>();
       JSONArray branching = json.getJSONArray("branching");
       for (int i = 0; i < branching.length(); ++i) {
           try {
               Branch tmp = new Branch(branching.getJSONObject(i));
               if (tmp.label != null) {
                   this.branches.add(tmp);
               } else {
                   this.branches.add(null);
               }
           } catch (JSONException e) {
               this.branches.add(null);
           }
       }
    }
    
    /** Getters */
    public int getGroupId() {
        return parent.getGroupId();
    }
    public String getGroupName() {
        return parent.getName();
    }
    public int getItemNumber() {
        return itemNumber;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public int getType() {
        return type;
    }
    public String getFilename() {
        return filename;
    }
    public String getURL() {
        return url;
    }
    public List<Branch> getBranches() {
        return branches;
    }
    public boolean hasBranches() {
        return branches.size() > 0;
    }

    public double getDelay() {
        return delay;
    }

    /** Play controls methods */
    public boolean isFirst() {
        return first;
    }
    public boolean isLast() {
        return last;
    }
    
    public boolean isStoppable() {
        if (stoppable < 0) return parent.isStoppable();
        return stoppable > 0;
    }
    public boolean isPausable() {
        if (pausable < 0) return parent.isPausable();
        return pausable > 0;
    }
    public boolean isPlayable() {
        return isStoppable() || isPausable();
    }

    public boolean canAdvance() {
        // can't advance if you're last
        if (isLast()) return false;
        if (canAdvance < 0) return parent.canAdvance();
        return canAdvance > 0;
    }
    public boolean canGoBack() {
        // can't go back if you're first
        if (isFirst()) return false;
        if (canGoBack < 0) return parent.canGoBack();
        return canGoBack > 0;
    }
    
    public void play(CyranoActivity activity) {
        // Do not play any audio if the tsAudio setting is off
        if (AppSettings.tsAudio){ 
            if (this.type == 1) {
                // notify upon completion so we can go to the next item, if delay is 0.
                // we can check == 0, because we aren't doing any calculations with it.
                AudioMethods.textToSpeech(activity, description, (delay == 0) ? activity : null);
            } else if (this.type == 2) {
                AudioMethods.streamAudio(filename, (delay == 0) ? activity : null);
            }
        }
    }
    
    public void stop() {
        if (this.type == 1) {
            AudioMethods.stopTextToSpeech();
        } else if (this.type == 2) {
            AudioMethods.shutdownMediaPlayer();
        }
    }
    
    public void pause() {
        AudioMethods.pauseMediaPlayer();
    }
    
}
