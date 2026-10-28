package com.itineraryledger.kabengosafaris.ClientPortal.DTOs;

import java.time.LocalDateTime;

/**
 * One comment, as either side reads it.
 *
 * <p>One record for both audiences, because the difference between them is <em>which rows</em>
 * are returned and not which fields. A second shape would be a second place to decide what a
 * client may see, and the first thing anybody does with two shapes is copy one into the other.
 *
 * <p>There is no author email and no user id. A client does not need to know which of us typed
 * something, and staff reading the thread in the panel have the access log if they do.
 */
public record PortalComment(
    String id,
    /** WHOLE, DAY or LINE. */
    String anchor,
    Integer dayNumber,
    /** What the line was called when this was written, so it reads even if the row has gone. */
    String targetLabel,
    String body,
    /** CLIENT or STAFF: which side of the conversation. */
    String authorKind,
    String authorName,
    /** Which round it was written against. */
    Integer revisionNumber,
    boolean resolved,
    /** Absent from every response a client can reach; false means we were talking among ourselves. */
    Boolean visibleToClient,
    LocalDateTime createdAt
) {}
