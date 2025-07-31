package bigsky.pika.models.chinook.beans;

import bigsky.pika.PikaORM;

import java.util.Map;

public class PlaylistTrackBean extends PikaORM.EnterprisePikaBean {

    int playlistId;
    int trackId;

    // Getters and setters
    public int getPlaylistId() {
        return playlistId;
    }

    public int getTrackId() {
        return trackId;
    }

    // no constructor
    private PlaylistTrackBean(){}

    public static void associate(PlaylistBean playlistBean, TrackBean track){
        orm().exec("INSERT INTO playlist_track(PlaylistId, TrackId) VALUES (:playlistId, :trackId)",
                Map.of("playlistId", playlistBean.getPlaylistId(), "trackId", track.getTrackId()));
    }

    public static void unassociate(PlaylistBean playlistBean, TrackBean track){
        orm().exec("DELETE FROM playlist_track WHERE PlaylistId=:playlistId AND TrackId=:trackId",
                Map.of("playlistId", playlistBean.getPlaylistId(), "trackId", track.getTrackId()));
    }

}