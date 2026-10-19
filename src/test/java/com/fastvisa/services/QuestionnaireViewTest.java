package com.fastvisa.services;

import com.fastvisa.manipulatepdf.Receipt;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestionnaireView Tests")
class QuestionnaireViewTest {

  private static final Gson GSON = new Gson();

  private static final String SAMPLE_JSON = "{"
      + "\"case\":{\"number\":\"238547\",\"name\":\"H-1B Extension 2026\"},"
      + "\"client\":\"Zhang Ling He\","
      + "\"questionnaire\":\"H-1B Extension Questionnaire\","
      + "\"submitted_at\":\"Jun 28, 2026\","
      + "\"verified_at\":\"Oct 7, 2026 at 2:30 PM WIB\","
      + "\"verified_by\":\"Yusen Lee\","
      + "\"totals\":{\"provided\":27,\"total\":28},"
      + "\"blocks\":[{\"section_index\":1,\"section_total\":4,\"title\":\"Personal information\","
      + "\"continued\":false,\"description\":null,\"provided\":17,\"total\":17,"
      + "\"fields\":[{\"label\":\"First name\",\"display_value\":\"Ling He\",\"provided\":true}]}],"
      + "\"missing_optional\":[\"Additional notes for the attorney\"]}";

  private static Map<String, Object> payload(int provided, int total, List<String> missingOptional) {
    Map<String, Object> totals = new HashMap<>();
    // Gson delivers every JSON number as Double
    totals.put("provided", (double) provided);
    totals.put("total", (double) total);

    Map<String, Object> caseInfo = new HashMap<>();
    caseInfo.put("number", "238547");
    caseInfo.put("name", "H-1B Extension 2026");

    Map<String, Object> data = new HashMap<>();
    data.put("case", caseInfo);
    data.put("client", "Zhang Ling He");
    data.put("questionnaire", "H-1B Extension Questionnaire");
    data.put("totals", totals);
    data.put("blocks", new ArrayList<>());
    data.put("missing_optional", missingOptional);
    return data;
  }

  // ---- plumbing -----------------------------------------------------------

  @Test
  @DisplayName("Nested form_data survives a Gson round trip into Receipt")
  @SuppressWarnings("unchecked")
  void receiptGsonRoundTrip() {
    String json = "{\"receipt_type\":\"Questionnaire\",\"output_name\":\"Questionnaire-1-1791358200\","
        + "\"form_data\":" + SAMPLE_JSON + "}";

    Receipt receipt = GSON.fromJson(json, Receipt.class);

    assertThat(receipt.getReceipt_type()).isEqualTo("Questionnaire");
    Map<String, Object> formData = (Map<String, Object>) receipt.getForm_data();
    List<Object> blocks = (List<Object>) formData.get("blocks");
    assertThat(blocks).hasSize(1);
    Map<String, Object> block = (Map<String, Object>) blocks.get(0);
    assertThat(((List<?>) block.get("fields"))).hasSize(1);
    assertThat(((Map<String, Object>) formData.get("totals")).get("provided")).isEqualTo(27.0);
  }

  @Test
  @DisplayName("View built from a Gson-parsed payload exposes the cover data")
  void viewFromGsonPayload() {
    Receipt receipt = GSON.fromJson("{\"form_data\":" + SAMPLE_JSON + "}", Receipt.class);

    QuestionnaireView view = new QuestionnaireView(receipt.getForm_data());

    assertThat(view.getProvided()).isEqualTo(27);
    assertThat(view.getTotal()).isEqualTo(28);
    assertThat(view.getCaseNumber()).isEqualTo("238547");
    assertThat(view.getVerifiedBy()).isEqualTo("Yusen Lee");
    assertThat(view.getBlocks()).hasSize(1);
    assertThat(view.verifiedTitle()).isEqualTo("Verified H-1B Extension Questionnaire");
    assertThat(view.statusRight()).isEqualTo("27 OF 28 RESPONSES PROVIDED");
  }

  // ---- wording rules (§5) -------------------------------------------------

  @ParameterizedTest(name = "statusRight provided={0} total={1}")
  @CsvSource({
      "28, 28, ALL 28 RESPONSES PROVIDED",
      "27, 28, 27 OF 28 RESPONSES PROVIDED",
      "25, 28, 25 OF 28 RESPONSES PROVIDED"
  })
  void statusRight(int provided, int total, String expected) {
    assertThat(new QuestionnaireView(payload(provided, total, Collections.emptyList())).statusRight())
        .isEqualTo(expected);
  }

  @ParameterizedTest(name = "recordNotice provided={0} total={1}")
  @CsvSource({
      "28, 28, All responses were provided.",
      "27, 28, One response was not provided and is identified in the final section.",
      "25, 28, 3 responses were not provided and are identified in the final section."
  })
  void recordNotice(int provided, int total, String sentence) {
    assertThat(new QuestionnaireView(payload(provided, total, Collections.emptyList())).recordNotice())
        .isEqualTo("This document is an immutable snapshot of the questionnaire responses at the time of "
            + "verification. " + sentence);
  }

  @Test
  @DisplayName("completionNote: nothing missing")
  void completionNoteNoneMissing() {
    assertThat(new QuestionnaireView(payload(28, 28, Collections.emptyList())).completionNote())
        .isEqualTo("No responses were missing.");
  }

  @Test
  @DisplayName("completionNote: one missing, one optional name")
  void completionNoteOneMissingOneName() {
    QuestionnaireView view = new QuestionnaireView(
        payload(27, 28, Collections.singletonList("Additional notes for the attorney")));
    assertThat(view.completionNote()).isEqualTo(
        "The optional field 'Additional notes for the attorney' was left blank when this questionnaire "
            + "was verified. No other responses were missing.");
  }

  @Test
  @DisplayName("completionNote: one missing, optional list empty -> generic")
  void completionNoteOneMissingNoNames() {
    assertThat(new QuestionnaireView(payload(27, 28, Collections.emptyList())).completionNote())
        .isEqualTo("1 response was left blank when this questionnaire was verified.");
  }

  @Test
  @DisplayName("completionNote: one missing, several names -> joined names")
  void completionNoteOneMissingManyNames() {
    assertThat(new QuestionnaireView(payload(27, 28, Arrays.asList("A", "B"))).completionNote())
        .isEqualTo("The following responses were left blank when this questionnaire was verified: A; B.");
  }

  @Test
  @DisplayName("completionNote: several missing with names")
  void completionNoteManyMissingWithNames() {
    assertThat(new QuestionnaireView(payload(25, 28, Arrays.asList("A", "B", "C"))).completionNote())
        .isEqualTo("The following responses were left blank when this questionnaire was verified: A; B; C.");
  }

  @Test
  @DisplayName("completionNote: several missing, optional list empty")
  void completionNoteManyMissingNoNames() {
    assertThat(new QuestionnaireView(payload(25, 28, Collections.emptyList())).completionNote())
        .isEqualTo("3 responses were left blank when this questionnaire was verified.");
  }

  @Test
  @DisplayName("missing_optional absent is treated as empty")
  void missingOptionalAbsent() {
    Map<String, Object> data = payload(27, 28, Collections.emptyList());
    data.remove("missing_optional");
    assertThat(new QuestionnaireView(data).getMissingOptional()).isEmpty();
  }

  // ---- footer -------------------------------------------------------------

  @Test
  @DisplayName("footerLeft joins client and case number")
  void footerLeft() {
    assertThat(new QuestionnaireView(payload(28, 28, Collections.emptyList())).footerLeft())
        .isEqualTo("Zhang Ling He | Case #238547");
  }

  @Test
  @DisplayName("footerLeft escapes quotes, backslashes, newlines and style-closing '<'")
  void footerLeftEscapes() {
    Map<String, Object> data = payload(28, 28, Collections.emptyList());
    data.put("client", "A\"B\\C\nD</style>");

    String footer = new QuestionnaireView(data).footerLeft();

    assertThat(footer).doesNotContain("\n").doesNotContain("<");
    assertThat(footer).startsWith("A\\\"B\\\\C D\\3C /style>");
  }

  @Test
  @DisplayName("lines splits on LF, CRLF and CR, and null gives no lines")
  void lines() {
    QuestionnaireView view = new QuestionnaireView(payload(28, 28, Collections.emptyList()));

    assertThat(view.lines("a\nb\r\nc\rd")).containsExactly("a", "b", "c", "d");
    assertThat(view.lines("single")).containsExactly("single");
    assertThat(view.lines(null)).isEmpty();
  }

  // ---- edge inputs --------------------------------------------------------

  @Test
  @DisplayName("Empty blocks is allowed")
  void emptyBlocks() {
    assertThat(new QuestionnaireView(payload(0, 0, Collections.emptyList())).getBlocks()).isEmpty();
  }

  @Test
  @DisplayName("Null form_data fails with a clear message")
  void nullFormData() {
    assertThatThrownBy(() -> new QuestionnaireView(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("form_data");
  }

  @Test
  @DisplayName("Missing blocks fails with a clear message")
  void missingBlocks() {
    Map<String, Object> data = payload(1, 1, Collections.emptyList());
    data.remove("blocks");
    assertThatThrownBy(() -> new QuestionnaireView(data))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blocks");
  }

  @Test
  @DisplayName("Missing totals fails with a clear message")
  void missingTotals() {
    Map<String, Object> data = payload(1, 1, Collections.emptyList());
    data.remove("totals");
    assertThatThrownBy(() -> new QuestionnaireView(data))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("totals");
  }

  @Test
  @DisplayName("Totals without numbers fails with a clear message")
  void totalsNotNumbers() {
    Map<String, Object> data = payload(1, 1, Collections.emptyList());
    data.put("totals", new HashMap<>());
    assertThatThrownBy(() -> new QuestionnaireView(data))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("totals.provided");
  }
}
