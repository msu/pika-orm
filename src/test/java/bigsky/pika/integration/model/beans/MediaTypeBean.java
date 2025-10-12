package bigsky.pika.integration.model.beans;

import bigsky.pika.PikaORM;

public class MediaTypeBean extends PikaORM.EnterprisePikaBean {

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

    public static PikaORM.PikaClassFinder<MediaTypeBean> find() {
        return find(MediaTypeBean.class);
    }

}
