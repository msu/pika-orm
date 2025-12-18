package bigsky.pika.integration.model.beans;


import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;

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

    public static PikaClassFinder<AlbumBean> find() {
        return find(AlbumBean.class);
    }
}
