package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.util.List;

import lombok.Data;

/**
 * Body for PUT /messages/batch/labels.
 *
 *   add:    label ids to attach to every message
 *   remove: label ids to detach from every message
 *
 * Both lists may be supplied; remove runs after add so callers can use a
 * single request to atomically swap a label set.
 */
@Data
public class BulkApplyLabelsDTO {
    private List<String> messageIds;
    private List<String> add;
    private List<String> remove;
}
