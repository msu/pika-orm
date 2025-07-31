package grug.db.models.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.GrugListFinder;
import grug.db.GrugORM.QueryResult;

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

    public QueryResult<TrackBean> getTracks() {
        return loadManyThrough(PlaylistTrackBean.class, TrackBean.class);
    }

    public static GrugListFinder<PlaylistBean> find() {
        return find(PlaylistBean.class);
    }

    public void addTrack(TrackBean track) {
        if (track == null) {
            throw new IllegalArgumentException("Track cannot be null");
        }

        if (!track.isPersisted()) {
            track.insert();
        }

        PlaylistTrackBean.associate(this, track);
    }
}