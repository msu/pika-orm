package bigsky.pika.integration.model.beans;

import bigsky.pika.PikaORM;
import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;

public class GenreBean extends EnterprisePikaBean {

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

    public static PikaClassFinder<GenreBean> find() {
        return find(GenreBean.class);
    }
}
