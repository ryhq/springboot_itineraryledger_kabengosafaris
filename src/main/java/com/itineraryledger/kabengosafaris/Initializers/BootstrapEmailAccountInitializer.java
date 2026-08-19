package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The one mail account a brand-new installation needs to be reachable at all.
 *
 * Without it, provisioning deadlocks: mail is sent through an account stored in the DATABASE, a fresh
 * database has none, so the first account's activation email cannot be sent — and nobody can sign in
 * to add the mail account that would have sent it.
 *
 * So the credentials the operator already has at provisioning time are written once, here, and the
 * panel manages accounts normally from then on. Only ever runs when there are ZERO accounts: an
 * installation with mail configured is never touched, and nothing is overwritten.
 *
 * The password is encrypted with the same EncryptionUtil the panel uses, so the row is
 * indistinguishable from one created through the UI.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapEmailAccountInitializer implements ApplicationRunner, Ordered {

    private final EmailAccountRepository repository;

    @Value("${app.bootstrap.smtp.host:}")
    private String host;

    @Value("${app.bootstrap.smtp.port:587}")
    private Integer port;

    @Value("${app.bootstrap.smtp.username:}")
    private String username;

    @Value("${app.bootstrap.smtp.password:}")
    private String password;

    @Value("${app.bootstrap.smtp.from:}")
    private String fromAddress;

    @Value("${app.company.name:}")
    private String companyName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;

        if (host.isBlank() || username.isBlank() || password.isBlank() || fromAddress.isBlank()) {
            log.warn("""
                No mail account exists and app.bootstrap.smtp.* is incomplete, so this installation \
                cannot send email — including the first account's activation link. Set the SMTP \
                properties and restart, or add the account through the panel once somebody can sign in.""");
            return;
        }

        try {
            EmailAccount account = new EmailAccount();
            account.setEmail(fromAddress.trim());
            account.setName(companyName.isBlank() ? fromAddress.trim() : companyName.trim());
            account.setDescription("Created at provisioning. Edit or replace it in the panel.");
            account.setSmtpHost(host.trim());
            account.setSmtpPort(port);
            account.setSmtpUsername(username.trim());
            /* encrypted exactly as the panel would store it */
            account.setSmtpPassword(EncryptionUtil.encrypt(password.trim()));
            account.setUseTls(true);
            account.setUseSsl(false);
            /* CUSTOM is this system's word for "a plain SMTP server", which is what a Brevo or
             * Mailgun relay is from its point of view */
            account.setProviderType(EmailAccountProvider.CUSTOM);
            account.setEnabled(true);
            account.setIsDefault(true);

            repository.save(account);
            log.info("Mail account created for {} via {} — the first activation email can be sent",
                account.getEmail(), account.getSmtpHost());

        } catch (Exception e) {
            log.error("Could not create the bootstrap mail account for {}. Add it through the panel.",
                fromAddress, e);
        }
    }

    /** Before the first user, whose activation email depends on this existing. */
    @Override
    public int getOrder() {
        return 26;
    }
}
