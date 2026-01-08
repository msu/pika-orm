package bigsky.pika.integration.model.beans;

import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;
import bigsky.pika.bean.PikaManyThroughRelation;

public class PlaylistBean extends EnterprisePikaBean {

    Long playlistId;
    String name;

    // Getters and setters
    public Long getPlaylistId() {
        return playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PikaManyThroughRelation<PlaylistTrackBean, TrackBean> getTracks() {
        return loadManyThrough(PlaylistTrackBean.class, TrackBean.class);
    }

    public static PikaClassFinder<PlaylistBean> find() {
        return find(PlaylistBean.class);
    }
}