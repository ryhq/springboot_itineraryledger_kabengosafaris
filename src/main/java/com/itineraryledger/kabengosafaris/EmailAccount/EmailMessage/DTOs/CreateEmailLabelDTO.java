package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabelColor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEmailLabelDTO {

    @NotBlank
    @Size(max = 60)
    private String name;

    /**
     * The semantic family — optional now.
     *
     * It used to be @NotNull, which made a label impossible to create from any client that thinks
     * in colours: the four values are QUOTE/BOOKING/VENDOR/INTERNAL, so "yellow" was a 400 every
     * time. Absent means INTERNAL, which is the neutral one.
     */
    private EmailLabelColor color;

    /** The exact colour, #rrggbb. Optional; without it the family decides. */
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "A colour must look like #1f2421")
    private String colorHex;
}
