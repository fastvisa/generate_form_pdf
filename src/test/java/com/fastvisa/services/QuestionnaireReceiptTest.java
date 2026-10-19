package com.fastvisa.services;

import com.fastvisa.manipulatepdf.Receipt;
import com.google.gson.Gson;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Questionnaire PDF rendering")
class QuestionnaireReceiptTest {

  private static final Gson GSON = new Gson();

  private ReceiptService receiptService;
  private Path tempFile;

  @BeforeEach
  void setUp() throws IOException {
    receiptService = new ReceiptService();
    tempFile = Files.createTempFile("questionnaire-test", ".pdf");
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFile);
  }

  // ---- helpers ------------------------------------------------------------

  private static Map<String, Object> field(String label, String value) {
    Map<String, Object> f = new LinkedHashMap<>();
    f.put("label", label);
    f.put("display_value", value);
    f.put("provided", value != null);
    return f;
  }

  private static Map<String, Object> block(int index, int sectionTotal, String title, boolean continued,
                                           String description, int provided, int total,
                                           List<Map<String, Object>> fields) {
    Map<String, Object> b = new LinkedHashMap<>();
    b.put("section_index", index);
    b.put("section_total", sectionTotal);
    b.put("title", title);
    b.put("continued", continued);
    b.put("description", description);
    b.put("provided", provided);
    b.put("total", total);
    b.put("fields", fields);
    return b;
  }

  private static Map<String, Object> payload(int provided, int total, List<String> missingOptional,
                                             List<Map<String, Object>> blocks) {
    Map<String, Object> caseInfo = new LinkedHashMap<>();
    caseInfo.put("number", "238547");
    caseInfo.put("name", "H-1B Extension 2026");
    Map<String, Object> totals = new LinkedHashMap<>();
    totals.put("provided", provided);
    totals.put("total", total);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("case", caseInfo);
    data.put("client", "Zhang Ling He");
    data.put("questionnaire", "H-1B Extension Questionnaire");
    data.put("submitted_at", "Jun 28, 2026");
    data.put("verified_at", "Oct 7, 2026 at 2:30 PM WIB");
    data.put("verified_by", "Yusen Lee");
    data.put("totals", totals);
    data.put("blocks", blocks);
    data.put("missing_optional", missingOptional);
    return data;
  }

  private static List<Map<String, Object>> fields(String prefix, int count) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      list.add(field(prefix + " " + i, "Value " + i));
    }
    return list;
  }

  private static List<Map<String, Object>> threeBlocks(List<Map<String, Object>> firstFields) {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 2, "Personal information", false, "Identity and contact details.", 10, 10, firstFields));
    blocks.add(block(1, 2, "Personal information", true, null, 10, 10, fields("Detail", 3)));
    blocks.add(block(2, 2, "Additional information", false, "Disclosures and notes.", 1, 2, java.util.Arrays.asList(
        field("Have you ever been arrested?", "No"),
        field("Additional notes for the attorney", null))));
    return blocks;
  }

  /** Round-trips through Gson like the controller does, so numbers become Double. */
  private Receipt receiptFrom(Map<String, Object> formData) {
    String json = GSON.toJson(Map.of(
        "receipt_type", "Questionnaire",
        "output_name", "Questionnaire-1-1791358200",
        "form_data", formData));
    return GSON.fromJson(json, Receipt.class);
  }

  private List<String> render(Map<String, Object> formData) throws IOException {
    try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
      receiptService.generateReceipt(receiptFrom(formData), out);
    }
    List<String> pages = new ArrayList<>();
    try (PdfDocument doc = new PdfDocument(new PdfReader(tempFile.toString()))) {
      for (int i = 1; i <= doc.getNumberOfPages(); i++) {
        pages.add(PdfTextExtractor.getTextFromPage(doc.getPage(i)));
      }
      assertThat(doc.getPage(1).getPageSize().getWidth()).isEqualTo(612f);
      assertThat(doc.getPage(1).getPageSize().getHeight()).isEqualTo(792f);
    }
    return pages;
  }

  /** Collapses wrapped lines so long sentences can be asserted as one string. */
  private static String flat(String text) {
    return text.replaceAll("\\s+", " ");
  }

  private static boolean unicodeFontAvailable() {
    return ReceiptServiceTest.class.getResource("/fonts/NotoSans-Regular.ttf") != null;
  }

  // ---- tests --------------------------------------------------------------

  @Test
  @DisplayName("Cover page, continued block on its own page, Not provided")
  void rendersCoverAndBlocks() throws IOException {
    List<String> pages = render(payload(11, 12, Collections.singletonList("Additional notes for the attorney"),
        threeBlocks(fields("Field", 9))));

    assertThat(pages.size()).isGreaterThanOrEqualTo(3);

    String cover = pages.get(0);
    assertThat(cover).contains("Verified H-1B Extension Questionnaire");
    assertThat(cover).contains("VERIFIED");
    assertThat(cover).contains("11 OF 12 RESPONSES PROVIDED");
    assertThat(cover).contains("#238547");
    assertThat(cover).contains("Yusen Lee");
    assertThat(flat(cover)).contains("One response was not provided and is identified in the final section.");
    assertThat(cover).doesNotContain("Personal information");

    assertThat(pages.get(1)).contains("SECTION 1 OF 2 / 10 OF 10 ANSWERED");
    assertThat(pages.get(1)).contains("Personal information");
    assertThat(pages.get(1)).doesNotContain("(continued)");

    assertThat(pages.get(2)).contains("SECTION 1 OF 2 / CONTINUED");
    assertThat(pages.get(2)).contains("Personal information (continued)");
    assertThat(pages.get(2)).contains("Remaining details provided by the client.");

    String all = String.join("\n", pages);
    assertThat(all).contains("Not provided");

    String last = pages.get(pages.size() - 1);
    assertThat(all).doesNotContain("COMPLETION NOTE");
    assertThat(last).contains("VERIFICATION CERTIFICATION");
  }

  @Test
  @DisplayName("Latin-extended names (outside Helvetica/WinAnsi) render via Noto Sans")
  void rendersUnicodeName() throws IOException {
    Assumptions.assumeTrue(unicodeFontAvailable(), "Noto Sans not in src/main/resources/fonts yet");
    List<Map<String, Object>> first = fields("Field", 8);
    first.add(field("Full name", "Łukasz Şahin Nguyễn"));

    List<String> pages = render(payload(11, 12, Collections.emptyList(), threeBlocks(first)));

    assertThat(String.join("\n", pages)).contains("Łukasz Şahin Nguyễn");
  }

  @Test
  @DisplayName("Hostile values render as text and newlines become line breaks")
  void escapesHostileValues() throws IOException {
    List<Map<String, Object>> first = fields("Field", 8);
    first.add(field("Notes", "<script>alert(1)</script> & \"quoted\"\nline two"));

    List<String> pages = render(payload(12, 12, Collections.emptyList(), threeBlocks(first)));
    String page2 = pages.get(1);

    assertThat(page2).contains("<script>alert(1)</script> & \"quoted\"");
    assertThat(page2).containsPattern("\"quoted\"\\s*\\n\\s*line two");
  }

  @Test
  @DisplayName("Footer on every page, running header on pages 2+, no version text")
  void footerAndRunningHeader() throws IOException {
    List<String> pages = render(payload(11, 12, Collections.emptyList(), threeBlocks(fields("Field", 9))));

    for (int i = 0; i < pages.size(); i++) {
      assertThat(pages.get(i)).contains("Zhang Ling He | Case #238547");
      assertThat(pages.get(i)).contains("Page " + (i + 1));
      assertThat(pages.get(i)).doesNotContain("Locked snapshot");
      assertThat(pages.get(i)).doesNotContainPattern("of \\d+\\s*$");
    }
    assertThat(pages.get(0)).doesNotContain("VERIFIED QUESTIONNAIRE");
    assertThat(pages.get(1)).contains("VERIFIED QUESTIONNAIRE");
  }

  @Test
  @DisplayName("Empty blocks and all-provided still render")
  void rendersEmptyBlocks() throws IOException {
    List<String> pages = render(payload(0, 0, Collections.emptyList(), new ArrayList<>()));

    assertThat(pages.get(0)).contains("ALL 0 RESPONSES PROVIDED");
    assertThat(flat(pages.get(0))).contains("All responses were provided.");
  }

  @Test
  @DisplayName("A block with no fields does not crash")
  void rendersBlockWithoutFields() throws IOException {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 1, "Empty section", false, null, 0, 0, new ArrayList<>()));

    List<String> pages = render(payload(0, 0, Collections.emptyList(), blocks));

    assertThat(String.join("\n", pages)).contains("Empty section");
  }

  @Test
  @DisplayName("Several missing answers use the multi-missing wording")
  void rendersManyMissing() throws IOException {
    List<String> pages = render(payload(9, 12, java.util.Arrays.asList("Alpha", "Beta", "Gamma"),
        threeBlocks(fields("Field", 9))));

    assertThat(pages.get(0)).contains("9 OF 12 RESPONSES PROVIDED");
    assertThat(flat(pages.get(0))).contains("3 responses were not provided");
  }

  @Test
  @DisplayName("Malformed payload fails loudly instead of producing an empty PDF")
  void malformedPayloadFails() throws IOException {
    Map<String, Object> data = payload(1, 1, Collections.emptyList(), new ArrayList<>());
    data.remove("blocks");

    try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
      assertThatThrownBy(() -> receiptService.generateReceipt(receiptFrom(data), out))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("blocks");
    }
  }

  @Test
  @DisplayName("Realistic 28-answer payload renders well inside the time budget")
  void rendersWithinBudget() throws IOException {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 4, "Personal information", false, "Identity.", 17, 17, fields("Personal", 9)));
    blocks.add(block(1, 4, "Personal information", true, null, 17, 17, fields("Personal more", 8)));
    blocks.add(block(2, 4, "Current employment", false, "Employer.", 5, 5, fields("Job", 5)));
    blocks.add(block(3, 4, "H-1B history", false, "History.", 4, 4, fields("H1B", 4)));
    blocks.add(block(4, 4, "Additional information", false, "Notes.", 1, 2, java.util.Arrays.asList(
        field("Arrested?", "No"), field("Notes", null))));

    long start = System.nanoTime();
    List<String> pages = render(payload(27, 28, Collections.singletonList("Notes"), blocks));
    long millis = (System.nanoTime() - start) / 1_000_000;
    System.out.println("QUESTIONNAIRE render time: " + millis + " ms, pages=" + pages.size());

    assertThat(millis).isLessThan(10_000);
  }
}
