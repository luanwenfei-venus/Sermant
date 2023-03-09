package com.huaweicloud.sermant.core.event;

import java.util.List;

/**
 * 事件消息
 *
 * @author luanwenfei
 * @since 2023-03-07
 */
public class EventMessage {
    String metaHash;

    List<Event> events;

    public EventMessage(String metaHash, List<Event> events) {
        this.metaHash = metaHash;
        this.events = events;
    }

    public String getMetaHash() {
        return metaHash;
    }

    public void setMetaHash(String metaHash) {
        this.metaHash = metaHash;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "EventMessage{" + "metaHash='" + metaHash + '\'' + ", events=" + events + '}';
    }
}
