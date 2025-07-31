package bigsky.pika.models.chinook.beans;

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

    public static PikaORM.PikaListFinder<MediaTypeBean> find() {
        return find(MediaTypeBean.class);
    }

}
