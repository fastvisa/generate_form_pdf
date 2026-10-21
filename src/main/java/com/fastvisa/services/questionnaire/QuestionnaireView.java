package com.fastvisa.services.questionnaire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QuestionnaireView {

  private static final String NOTICE_PREFIX = "This document is an immutable snapshot of the questionnaire responses at the time of verification. ";

  private final Map<String, Object> data;
  private final Map<String, Object> caseInfo;
  private final List<Object> blocks;
  private final List<String> missingOptional;
  private final int provided;
  private final int total;
  private boolean htmlEnabled = true;

  @SuppressWarnings("unchecked")
  public QuestionnaireView(Object formData) {
    if (!(formData instanceof Map)) {
      throw new IllegalArgumentException("Questionnaire form_data is missing or not an object");
    }
    this.data = (Map<String, Object>) formData;

    Object blocksValue = data.get("blocks");
    if (!(blocksValue instanceof List)) {
      throw new IllegalArgumentException("Questionnaire form_data is missing 'blocks'");
    }
    this.blocks = (List<Object>) blocksValue;

    Object totalsValue = data.get("totals");
    if (!(totalsValue instanceof Map)) {
      throw new IllegalArgumentException("Questionnaire form_data is missing 'totals'");
    }
    Map<String, Object> totals = (Map<String, Object>) totalsValue;
    this.provided = toInt(totals.get("provided"), "totals.provided");
    this.total = toInt(totals.get("total"), "totals.total");

    Object caseValue = data.get("case");
    this.caseInfo = caseValue instanceof Map ? (Map<String, Object>) caseValue : Collections.emptyMap();

    List<String> optional = new ArrayList<>();
    Object optionalValue = data.get("missing_optional");
    if (optionalValue instanceof List) {
      for (Object name : (List<Object>) optionalValue) {
        if (name != null) {
          optional.add(String.valueOf(name));
        }
      }
    }
    this.missingOptional = optional;
  }

  // JSON numbers arrive as Double through Gson
  private static int toInt(Object value, String name) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    throw new IllegalArgumentException("Questionnaire form_data is missing numeric '" + name + "'");
  }

  private int missing() {
    return Math.max(total - provided, 0);
  }

  // ---- wording -------------------------------------------------------

  public String verifiedTitle() {
    return "Verified " + getQuestionnaire();
  }

  public String statusRight() {
    return missing() == 0
        ? "ALL " + total + " RESPONSES PROVIDED"
        : provided + " OF " + total + " RESPONSES PROVIDED";
  }

  public String recordNotice() {
    int missing = missing();
    String sentence;
    if (missing == 0) {
      sentence = "All responses were provided.";
    } else if (missing == 1) {
      sentence = "One response was not provided and is identified in the final section.";
    } else {
      sentence = missing + " responses were not provided and are identified in the final section.";
    }
    return NOTICE_PREFIX + sentence;
  }

  public String completionNote() {
    int missing = missing();
    if (missing == 0) {
      return "No responses were missing.";
    }
    if (missing == 1 && missingOptional.size() == 1) {
      return "The optional field '" + missingOptional.get(0)
          + "' was left blank when this questionnaire was verified. No other responses were missing.";
    }
    if (!missingOptional.isEmpty()) {
      return "The following responses were left blank when this questionnaire was verified: "
          + String.join("; ", missingOptional) + ".";
    }
    return missing == 1
        ? "1 response was left blank when this questionnaire was verified."
        : missing + " responses were left blank when this questionnaire was verified.";
  }


  public String footerLeft() {
    // case.number is an empty string when the questionnaire is not attached to a case
    String text = getCaseNumber().isEmpty() ? getClient() : getClient() + " | Case #" + getCaseNumber();
    return text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r\n", " ")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("<", "\\3C ");
  }

  // ---- sections and fields (template helpers) -----------------------------

  /** First-page eyebrow of a section. The "N OF M ANSWERED" part is left out when the section has no questions. */
  public String sectionEyebrow(Map<String, Object> block) {
    String base = "SECTION " + intOf(block.get("section_index")) + " OF " + intOf(block.get("section_total"));
    int blockTotal = intOf(block.get("total"));
    return blockTotal > 0
        ? base + " / " + intOf(block.get("provided")) + " OF " + blockTotal + " ANSWERED"
        : base;
  }

  /** Eyebrow repeated on the pages a section continues onto. */
  public String continuedEyebrow(Map<String, Object> block) {
    return "SECTION " + intOf(block.get("section_index")) + " OF " + intOf(block.get("section_total")) + " / CONTINUED";
  }

  /** Informational text (e.g. a consent notice) rather than a question. Fields without a kind are answers. */
  public boolean isContent(Map<String, Object> field) {
    return field != null && "content".equals(field.get("kind"));
  }

  /** True when the sanitized html body of a content field should be rendered (vs. its plain text). */
  public boolean useHtml(Map<String, Object> field) {
    if (!htmlEnabled || field == null) {
      return false;
    }
    Object html = field.get("html");
    return html != null && !String.valueOf(html).trim().isEmpty();
  }

  /** Used when rendering the html bodies failed: every content field falls back to its plain text. */
  public void disableHtml() {
    this.htmlEnabled = false;
  }

  private static int intOf(Object value) {
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }

  /** Splits a display value into lines so the template can emit explicit line breaks. */
  public List<String> lines(Object value) {
    if (value == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(String.valueOf(value).split("\\r?\\n|\\r", -1));
  }

  // ---- plain accessors for the template -----------------------------------

  private String str(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  public String getCaseNumber() {
    return str(caseInfo.get("number"));
  }

  public String caseNumberDisplay() {
    return getCaseNumber().isEmpty() ? "-" : "#" + getCaseNumber();
  }

  public String caseNameDisplay() {
    return getCaseName().isEmpty() ? "-" : getClient() + " - " + getCaseName();
  }

  public String getCaseName() {
    return str(caseInfo.get("name"));
  }

  public String getClient() {
    return str(data.get("client"));
  }

  public String getQuestionnaire() {
    return str(data.get("questionnaire"));
  }

  public String getSubmittedAt() {
    String value = str(data.get("submitted_at"));
    return value.isEmpty() ? "-" : value;
  }

  /** Null unless the client resubmitted the questionnaire before it was verified. */
  public String getResubmittedAt() {
    String value = str(data.get("resubmitted_at"));
    return value.isEmpty() ? null : value;
  }

  public String getVerifiedAt() {
    return str(data.get("verified_at"));
  }

  public String getVerifiedBy() {
    return str(data.get("verified_by"));
  }

  public int getProvided() {
    return provided;
  }

  public int getTotal() {
    return total;
  }

  public List<Object> getBlocks() {
    return blocks;
  }

  public List<String> getMissingOptional() {
    return missingOptional;
  }
}
