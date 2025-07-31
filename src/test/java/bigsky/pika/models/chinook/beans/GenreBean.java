package grug.db.models.chinook.beans;

import grug.db.GrugORM;

public class GenreBean extends GrugORM.EnterpriseGrugBean {

    int genreId;
    String name;

    // Getters and setters
    public int getGenreId() {
        return genreId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static GrugORM.GrugListFinder<GenreBean> find() {
        return find(GenreBean.class);
    }
}
