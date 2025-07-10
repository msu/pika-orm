package grug.db.models.chinook.beans;

import grug.db.GrugORM;

public class PlaylistBean extends GrugORM.EnterpriseGrugBean {

    int playlistId;
    String name;

    // Getters and setters
    public int getPlaylistId() {
        return playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}