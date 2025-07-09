package grug.db.models.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.EnterpriseGrugBean;

public class AlbumBean extends EnterpriseGrugBean {
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

    public static GrugORM.GrugListFinder<AlbumBean> find() {
        return orm().find(AlbumBean.class);
    }
}
