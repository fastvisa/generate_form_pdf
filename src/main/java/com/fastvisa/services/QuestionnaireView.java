package com.fastvisa.services;

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
