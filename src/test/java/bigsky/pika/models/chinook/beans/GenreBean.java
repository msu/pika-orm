package bigsky.pika.models.chinook.beans;

import bigsky.pika.PikaORM;

public class GenreBean extends PikaORM.EnterprisePikaBean {

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

    public static PikaORM.PikaClassFinder<GenreBean> find() {
        return find(GenreBean.class);
    }
}
