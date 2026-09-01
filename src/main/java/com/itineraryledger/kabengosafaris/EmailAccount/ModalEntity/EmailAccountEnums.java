package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

/**
 * Reading the provider, sending method and receiving protocol off a request.
 *
 * These arrived as 1-based codes — 6 meant CUSTOM, 2 meant SMTP, 1 meant IMAP — numbers that appear
 * in no enum and no response, so a caller had to know them by heart. The panel sends the names it
 * displays, which is the reasonable thing to send; the mismatch made Jackson reject the WHOLE body,
 * and the caller was told "Required request body is missing or malformed" — a message that names
 * neither the field nor the value, for a form where everything looked filled in.
 *
 * So both are accepted. The codes still work for anything that already sends them, and a name is
 * matched case-insensitively. Anything else returns null, which every caller already treats as
 * "invalid" and reports with the field named.
 */
public final class EmailAccountEnums {

    private EmailAccountEnums() {}

    /** GMAIL=1 … CUSTOM=6, RESEND=7, or the name itself. Null when it is neither. */
    public static EmailAccountProvider provider(String raw) {
        Integer code = asCode(raw);
        if (code != null) {
            return switch (code) {
                case 1 -> EmailAccountProvider.GMAIL;
                case 2 -> EmailAccountProvider.OUTLOOK;
                case 3 -> EmailAccountProvider.SENDGRID;
                case 4 -> EmailAccountProvider.MAILGUN;
                case 5 -> EmailAccountProvider.AWS_SES;
                case 6 -> EmailAccountProvider.CUSTOM;
                case 7 -> EmailAccountProvider.RESEND;
                default -> null;
            };
        }
        return byName(EmailAccountProvider.values(), raw);
    }

    /** API=1, SMTP=2, or the name. Null when it is neither. */
    public static SendingMethod sendingMethod(String raw) {
        Integer code = asCode(raw);
        if (code != null) {
            return switch (code) {
                case 1 -> SendingMethod.API;
                case 2 -> SendingMethod.SMTP;
                default -> null;
            };
        }
        return byName(SendingMethod.values(), raw);
    }

    /** IMAP=1, POP3=2, anything else NONE — or the name. Null only when the name is unknown. */
    public static ReceivingProtocol receivingProtocol(String raw) {
        Integer code = asCode(raw);
        if (code != null) {
            return switch (code) {
                case 1 -> ReceivingProtocol.IMAP;
                case 2 -> ReceivingProtocol.POP3;
                default -> ReceivingProtocol.NONE;
            };
        }
        return byName(ReceivingProtocol.values(), raw);
    }

    private static Integer asCode(String raw) {
        if (raw == null) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static <E extends Enum<E>> E byName(E[] values, String raw) {
        if (raw == null) return null;
        String wanted = raw.trim();
        for (E value : values) {
            if (value.name().equalsIgnoreCase(wanted)) return value;
        }
        return null;
    }
}
