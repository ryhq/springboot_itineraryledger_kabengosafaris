package com.itineraryledger.kabengosafaris.Legal.Services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocument;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocumentType;
import com.itineraryledger.kabengosafaris.Legal.Repository.LegalDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The published privacy notice and terms, and the rendering that makes them safe to show.
 *
 * <p>One source for three surfaces: the client portal, and both public websites, which are copies
 * of one another and would otherwise each carry their own hand-written copy of the text.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentService {

    private final LegalDocumentRepository repository;

    @Transactional(readOnly = true)
    public Optional<LegalDocument> published(LegalDocumentType type) {
        return repository.findByDocumentTypeAndPublishedTrue(type);
    }

    @Transactional(readOnly = true)
    public List<LegalDocument> versionsOf(LegalDocumentType type) {
        return repository.findByDocumentTypeOrderByEffectiveFromDesc(type);
    }

    /**
     * The version string to stamp on somebody who has just consented.
     *
     * <p>Null when nothing is published, which is honest: it records that they verified before
     * any notice existed, rather than inventing a version they were never shown.
     */
    @Transactional(readOnly = true)
    public String publishedPrivacyVersion() {
        return published(LegalDocumentType.PRIVACY_POLICY)
            .map(LegalDocument::getVersion)
            .orElse(null);
    }

    /** Publishing one unpublishes the rest, so no page has to choose between two live notices. */
    @Transactional
    public LegalDocument publish(Long id) {
        LegalDocument document = repository.findById(id).orElse(null);
        if (document == null) return null;
        document.setPublished(true);
        LegalDocument saved = repository.save(document);
        repository.unpublishOthers(saved.getDocumentType(), saved.getId());
        log.info("Published {} version {}", saved.getDocumentType(), saved.getVersion());
        return saved;
    }

    /**
     * The notice as HTML, built from the stored text with everything escaped.
     *
     * <p>The markup is deliberately tiny, and anything it does not recognise is a literal: a blank
     * line separates paragraphs, a line starting {@code ## } is a heading, a line starting
     * {@code - } is a bullet. There is no way to write a tag, an attribute or a URL handler
     * through it, which is the point. This text is edited in the panel and rendered on two public
     * websites; accepting markup would put a stored-script hole on all of them.
     */
    public String renderHtml(String body) {
        if (body == null || body.isBlank()) return "";

        StringBuilder html = new StringBuilder();
        boolean inList = false;

        for (String block : body.replace("\r\n", "\n").split("\n\n+")) {
            String[] lines = block.strip().split("\n");
            StringBuilder paragraph = new StringBuilder();

            for (String raw : lines) {
                String line = raw.strip();
                if (line.isEmpty()) continue;

                if (line.startsWith("## ")) {
                    inList = closeList(html, inList);
                    flush(html, paragraph);
                    html.append("<h2>").append(escape(line.substring(3).strip())).append("</h2>\n");
                } else if (line.startsWith("- ")) {
                    flush(html, paragraph);
                    if (!inList) { html.append("<ul>\n"); inList = true; }
                    html.append("<li>").append(escape(line.substring(2).strip())).append("</li>\n");
                } else {
                    inList = closeList(html, inList);
                    if (paragraph.length() > 0) paragraph.append(' ');
                    paragraph.append(escape(line));
                }
            }
            flush(html, paragraph);
            inList = closeList(html, inList);
        }
        closeList(html, inList);
        return html.toString();
    }

    private void flush(StringBuilder html, StringBuilder paragraph) {
        if (paragraph.length() == 0) return;
        html.append("<p>").append(paragraph).append("</p>\n");
        paragraph.setLength(0);
    }

    private boolean closeList(StringBuilder html, boolean inList) {
        if (inList) html.append("</ul>\n");
        return false;
    }

    /** Every one of the five, in this order, or the first undoes the rest. */
    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
