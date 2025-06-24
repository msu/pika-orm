package grug.db.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.GrugRecord;

public class AlbumBean extends GrugRecord {
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
        return load1(ArtistBean.class);
    }

    public static GrugORM.GrugFinder<AlbumBean> find() {
        return orm().finder(AlbumBean.class);
    }
}
