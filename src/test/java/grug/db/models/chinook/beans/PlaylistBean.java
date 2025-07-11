package grug.db.models.chinook.beans;

import grug.db.GrugORM;

import java.util.List;

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

    public List<TrackBean> getTracks() {
        return loadNtoN(PlaylistTrackBean.class, TrackBean.class);
    }

    public static GrugORM.GrugListFinder<PlaylistBean> find() {
        return orm().find(PlaylistBean.class);
    }
}