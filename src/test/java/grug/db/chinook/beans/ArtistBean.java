package grug.db.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.GrugBean;
import grug.db.GrugORM.ResultList;

public class ArtistBean extends GrugBean {

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

    public ResultList<AlbumBean> getAlbums() {
        return loadN(AlbumBean.class);
    }

    public static GrugORM.GrugFinder<ArtistBean> find() {
        return orm().finder(ArtistBean.class);
    }
}
