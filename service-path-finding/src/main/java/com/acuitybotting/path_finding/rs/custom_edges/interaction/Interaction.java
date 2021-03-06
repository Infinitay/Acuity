package com.acuitybotting.path_finding.rs.custom_edges.interaction;

import com.google.gson.annotations.Expose;

/**
 * Created by Zachary Herridge on 6/21/2018.
 */
public class Interaction {

    public static final int NPC = 1;
    public static final int SCENE_ENTITY = 2;
    public static final int SPELL = 3;
    public static final int INTERFACE = 4;

    @Expose
    private final int type;

    @Expose
    private String queryName;

    @Expose
    private String action;

    @Expose
    private String[] dialogs;

    @Expose
    private String spellName;

    @Expose
    private int[] interfacePath;

    public Interaction(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getQueryName() {
        return queryName;
    }

    public Interaction setQueryName(String queryName) {
        this.queryName = queryName;
        return this;
    }

    public String getAction() {
        return action;
    }

    public Interaction setAction(String action) {
        this.action = action;
        return this;
    }

    public String[] getDialogs() {
        return dialogs;
    }

    public Interaction setDialogs(String[] dialogs) {
        this.dialogs = dialogs;
        return this;
    }

    public String getSpellName() {
        return spellName;
    }

    public Interaction setSpellName(String spellName) {
        this.spellName = spellName;
        return this;
    }

    public int[] getInterfacePath() {
        return interfacePath;
    }

    public Interaction setInterfacePath(int[] interfacePath) {
        this.interfacePath = interfacePath;
        return this;
    }
}
