package grug.db.models.chinook.pojos;

public class PlaylistTrack {

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