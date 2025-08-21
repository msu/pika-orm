package bigsky.pika.models.chinook.beans;

import bigsky.pika.PikaORM;
import bigsky.pika.PikaORM.PikaClassFinder;

public class PlaylistBean extends PikaORM.EnterprisePikaBean {

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

    public PikaORM.PikaManyThroughQuery<PlaylistTrackBean, TrackBean> getTracks() {
        return loadManyThrough(PlaylistTrackBean.class, TrackBean.class);
    }

    public static PikaClassFinder<PlaylistBean> find() {
        return find(PlaylistBean.class);
    }
}