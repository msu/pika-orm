package bigsky.pika.integration.model.beans;

import bigsky.pika.PikaORM;
import bigsky.pika.bean.EnterprisePikaBean;

import java.util.Map;

public class PlaylistTrackBean extends EnterprisePikaBean {

    Long playlistId;
    Long trackId;

    // Getters and setters
    public long getPlaylistId() {
        return playlistId;
    }

    public long getTrackId() {
        return trackId;
    }

    // no constructor
    private PlaylistTrackBean(){}

    // playlisttrack doesn't have an id :/ so we need to override and implement this
    @Override
    public boolean delete() {
        orm().exec("DELETE FROM playlist_track WHERE PlaylistId=:pid AND TrackId=:trackid", Map.of("pid", playlistId, "trackid", trackId));
        return true;
    }
}