package bigsky.pika.models.chinook.beans;

import bigsky.pika.PikaORM.EnterprisePikaBean;
import bigsky.pika.PikaORM.PikaListFinder;

public class AlbumBean extends EnterprisePikaBean {
    Long artistId;
    Long albumId;
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

    public ArtistBean getArtist() {
        return load(ArtistBean.class);
    }

    public static PikaListFinder<AlbumBean> find() {
        return find(AlbumBean.class);
    }
}
