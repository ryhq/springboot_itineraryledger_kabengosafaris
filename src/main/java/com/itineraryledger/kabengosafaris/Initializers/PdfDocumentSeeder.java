package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.PdfDocumentVariables;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds ONE document type, in a transaction of its own.
 *
 * This exists because of an outage. The initializer already wrapped each
 * document in try/catch, which looked like isolation and was not: the whole run
 * shared one transaction, so a failed insert marked it rollback-only, the catch
 * swallowed the exception, and the commit then threw
 * {@code UnexpectedRollbackException} — after the initializer had reported
 * success. Spring shut the context down, systemd restarted it, and the same
 * insert failed again. A description five characters too long took the API off
 * the air in a loop.
 *
 * <p>A separate bean with REQUIRES_NEW is what makes the catch real: self
 * invocation would not go through the proxy, so the transaction boundary has to
 * live on a different object. One document type failing now costs that document
 * type, not the application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfDocumentSeeder {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateCreateService pdfTemplateCreateService;

    /**
     * Creates the document type if missing, then makes sure its templates exist.
     *
     * @return true when the type is present and usable afterwards
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean seed(String documentName) {
        PdfDocument document = pdfDocumentRepository.findByName(documentName).orElse(null);

        if (document == null) {
            document = pdfDocumentRepository.save(PdfDocument.builder()
                .name(documentName)
                .displayName(PdfDocumentVariables.getDisplayName(documentName))
                .description(PdfDocumentVariables.getDescription(documentName))
                .dataSourceClass(PdfDocumentVariables.getDataSourceClass(documentName))
                .rootVariableName(PdfDocumentVariables.getRootVariableName(documentName))
                .enabled(true)
                .variablesJson(PdfDocumentVariables.getVariablesForDocument(documentName))
                .build());
            log.info("Created PDF document: {} ({})", documentName, document.getDisplayName());
        }

        /*
         * Templates are checked even for a document that already existed: one
         * created before a template shipped would otherwise never receive it,
         * and the only symptom would be a missing option in the generate drawer.
         */
        if (!pdfTemplateCreateService.createSystemDefaultTemplate(document)) {
            log.warn("No system default template for document: {}", documentName);
        }

        for (String[] extra : PdfDocumentVariables.getExtraTemplates(documentName)) {
            pdfTemplateCreateService.createSeededTemplate(document, extra[0], extra[1], extra[2]);
        }

        return true;
    }
}
