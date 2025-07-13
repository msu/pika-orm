package grug.db.models.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.GrugListFinder;
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

    public static GrugListFinder<PlaylistBean> find() {
        return find(PlaylistBean.class);
    }
}