package bigsky.pika.integration.model.beans;

import bigsky.pika.PikaORM;
import bigsky.pika.PikaORM.PikaClassFinder;

public class TrackBean extends PikaORM.EnterprisePikaBean {

    Long trackId;
    String name;
    Long albumId;
    Long mediaTypeId;
    Long genreId;
    String composer;
    Long milliseconds;
    Long bytes;
    Long unitPrice;

    public Long getTrackId() {
        return trackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public Long getMediaTypeId() {
        return mediaTypeId;
    }

    public void setMediaTypeId(Long mediaTypeId) {
        this.mediaTypeId = mediaTypeId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public Long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(Long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public PikaORM.PikaManyThroughQuery<PlaylistTrackBean, PlaylistBean> getPlaylists() {
        return loadManyThrough(PlaylistTrackBean.class, PlaylistBean.class);
    }

    public static PikaClassFinder<TrackBean> find() {
        return find(TrackBean.class);
    }

}
