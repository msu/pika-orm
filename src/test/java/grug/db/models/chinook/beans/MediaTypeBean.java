package grug.db.models.chinook.beans;

import grug.db.GrugORM;

public class MediaTypeBean extends GrugORM.EnterpriseGrugBean {

    int mediaTypeId;
    String name;

    // Getters and setters
    public int getMediaTypeId() {
        return mediaTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static GrugORM.GrugListFinder<MediaTypeBean> find() {
        return find(MediaTypeBean.class);
    }

}
