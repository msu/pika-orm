package grug.db.models.chinook.beans;

import grug.db.GrugORM.*;

public class ArtistBean extends EnterpriseGrugBean {

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

    public static GrugListFinder<ArtistBean> find() {
        return find(ArtistBean.class);
    }
}
