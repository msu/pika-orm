package bigsky.pika.integration.model.beans;


import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;

public class MediaTypeBean extends EnterprisePikaBean {

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

    public static PikaClassFinder<MediaTypeBean> find() {
        return find(MediaTypeBean.class);
    }

}
