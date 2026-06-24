package edu.montana.pika.integration.model.beans;

import edu.montana.pika.bean.PikaBean;
import edu.montana.pika.query.PikaClassFinder;
import edu.montana.pika.bean.PikaManyRelation;

public class ArtistBean extends PikaBean {

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
