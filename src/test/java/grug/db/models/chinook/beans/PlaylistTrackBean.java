package grug.db.models.chinook.beans;

import grug.db.GrugORM;

public class PlaylistTrackBean extends GrugORM.EnterpriseGrugBean {

    int playlistId;
    int trackId;

    // Getters and setters
    public int getPlaylistId() {
        return playlistId;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }
}