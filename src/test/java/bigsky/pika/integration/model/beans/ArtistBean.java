package bigsky.pika.integration.model.beans;

import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;
import bigsky.pika.bean.PikaManyRelation;

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

    public PikaManyRelation<AlbumBean> getAlbums() {
        return loadMany(AlbumBean.class);
    }

    public static PikaClassFinder<ArtistBean> find() {
        return find(ArtistBean.class);
    }

    @Override
    protected void validation() {
        if (name == null || name.trim().isEmpty()) {
            addError("name", "Name cannot be null or empty");
        }
    }
}
