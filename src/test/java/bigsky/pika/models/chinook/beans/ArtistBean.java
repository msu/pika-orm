package bigsky.pika.models.chinook.beans;

import bigsky.pika.PikaORM.*;

public class ArtistBean extends EnterprisePikaBean {

    Long artistId;
    String name;

    public Long getArtistId() {
        return artistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QueryResult<AlbumBean> getAlbums() {
        return loadMany(AlbumBean.class);
    }

    public static PikaListFinder<ArtistBean> find() {
        return find(ArtistBean.class);
    }
}
