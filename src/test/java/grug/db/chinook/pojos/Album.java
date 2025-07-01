package grug.db.chinook.pojos;

public class Album {

    Long albumId;
    Long artistId;
    String title;

    public Long getAlbumId() {
        return albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Object getArtistId() {
        return artistId;
    }
}
