package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.util.List;

import lombok.Data;

/** Body for PUT /messages/{id}/labels — replaces the message's label set. */
@Data
public class ApplyLabelsDTO {
    private List<String> labelIds;
}
