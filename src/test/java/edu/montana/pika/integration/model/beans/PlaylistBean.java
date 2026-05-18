package edu.montana.pika.integration.model.beans;

import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.query.PikaClassFinder;
import edu.montana.pika.bean.PikaManyThroughRelation;

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