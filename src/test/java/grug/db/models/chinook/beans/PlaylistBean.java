package grug.db.models.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.ResultList;

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

    public ResultList<TrackBean> getTracks() {
        return loadNtoN(PlaylistTrackBean.class, TrackBean.class);
    }

    public static GrugORM.GrugListFinder<PlaylistBean> find() {
        return orm().find(PlaylistBean.class);
    }

    public void addTrack(TrackBean track) {
        if (track == null) {
            throw new IllegalArgumentException("Track cannot be null");
        }

        if (track.isNew()) {
            track.insert();
        }

        PlaylistTrackBean.associate(this, track);
    }
}