package com.fastvisa.services;

import com.fastvisa.manipulatepdf.Receipt;
import com.google.gson.Gson;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Questionnaire PDF rendering")
class QuestionnaireReceiptTest {

  private static final Gson GSON = new Gson();

  private ReceiptService receiptService;
  private Path tempFile;
  private int linkCount;
  private String testName = "questionnaire";

  /** Rendered PDFs are kept here (not deleted) so they can be opened and checked by eye. */
  private static final Path KEEP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "questionnaire-pdf-samples");

  @BeforeEach
  void setUp(TestInfo info) throws IOException {
    testName = info.getTestMethod().map(m -> m.getName()).orElse("questionnaire");
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
    f.put("kind", "answer");
    f.put("label", label);
    f.put("display_value", value);
    f.put("provided", value != null);
    return f;
  }

  private static Map<String, Object> content(String label, String text, String html) {
    Map<String, Object> f = new LinkedHashMap<>();
    f.put("kind", "content");
    f.put("label", label);
    f.put("text", text);
    f.put("html", html);
    return f;
  }

  private static Map<String, Object> block(int index, int sectionTotal, String title, String description,
                                           int provided, int total, List<Map<String, Object>> fields) {
    Map<String, Object> b = new LinkedHashMap<>();
    b.put("section_index", index);
    b.put("section_total", sectionTotal);
    b.put("title", title);
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
    data.put("submitted_at", "06/28/2026");
    data.put("resubmitted_at", null);
    data.put("verified_at", "10/07/2026 at 2:30 PM EDT");
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

  /** One block per section, like Rails sends it: section 1 is long enough to flow onto a second page. */
  private static List<Map<String, Object>> twoSections(List<Map<String, Object>> firstFields) {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 2, "Personal information", "Identity and contact details.", 17, 17, firstFields));
    blocks.add(block(2, 2, "Additional information", "Disclosures and notes.", 1, 2, Arrays.asList(
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
    Files.createDirectories(KEEP_DIR);
    Files.copy(tempFile, KEEP_DIR.resolve(testName + ".pdf"), StandardCopyOption.REPLACE_EXISTING);

    List<String> pages = new ArrayList<>();
    linkCount = 0;
    try (PdfDocument doc = new PdfDocument(new PdfReader(tempFile.toString()))) {
      for (int i = 1; i <= doc.getNumberOfPages(); i++) {
        pages.add(PdfTextExtractor.getTextFromPage(doc.getPage(i)));
        for (PdfAnnotation annotation : doc.getPage(i).getAnnotations()) {
          if (annotation instanceof PdfLinkAnnotation) {
            linkCount++;
          }
        }
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

  private static int count(String text, String regex) {
    Matcher m = Pattern.compile(regex).matcher(text);
    int n = 0;
    while (m.find()) {
      n++;
    }
    return n;
  }

  // ---- tests --------------------------------------------------------------

  @Test
  @DisplayName("Cover, a long section flowing onto a second page with (continued), Not provided")
  void rendersCoverAndFlowingSection() throws IOException {
    List<String> pages = render(payload(18, 19, Collections.singletonList("Additional notes for the attorney"),
        twoSections(fields("Field", 17))));

    assertThat(pages.size()).isGreaterThanOrEqualTo(3);

    String cover = pages.get(0);
    assertThat(cover).contains("Verified H-1B Extension Questionnaire");
    assertThat(cover).contains("VERIFIED");
    assertThat(cover).contains("18 OF 19 RESPONSES PROVIDED");
    assertThat(cover).contains("#238547");
    assertThat(cover).contains("Yusen Lee");
    // dates and zone are printed exactly as Rails sends them
    assertThat(cover).contains("06/28/2026");
    assertThat(cover).contains("10/07/2026 at 2:30 PM EDT");
    assertThat(flat(cover)).contains("One response was not provided and is identified in the final section.");
    assertThat(cover).doesNotContain("Personal information");

    assertThat(pages.get(1)).contains("SECTION 1 OF 2 / 17 OF 17 ANSWERED");
    assertThat(pages.get(1)).contains("Personal information");
    assertThat(pages.get(1)).doesNotContain("(continued)");
    assertThat(pages.get(1)).contains("FIELD 1\n");

    assertThat(pages.get(2)).contains("SECTION 1 OF 2 / CONTINUED");
    assertThat(pages.get(2)).contains("Personal information (continued)");
    assertThat(pages.get(2)).contains("FIELD 17\n");

    String all = String.join("\n", pages);
    for (int i = 1; i <= 17; i++) {
      assertThat(all).contains("FIELD " + i + "\n");
    }
    assertThat(count(all, "SECTION 2 OF 2 / 1 OF 2 ANSWERED")).isEqualTo(1);
    assertThat(all).contains("Not provided");
    assertThat(all).doesNotContain("COMPLETION NOTE");

    String last = pages.get(pages.size() - 1);
    assertThat(last).contains("VERIFICATION CERTIFICATION");
  }

  @Test
  @DisplayName("Resubmitted date is shown under Submitted only when present")
  void rendersResubmittedAt() throws IOException {
    Map<String, Object> data = payload(1, 1, Collections.emptyList(), twoSections(fields("Field", 2)));
    data.put("resubmitted_at", "06/29/2026");

    assertThat(flat(render(data).get(0))).contains("Resubmitted 06/29/2026");

    data.put("resubmitted_at", null);
    assertThat(render(data).get(0)).doesNotContain("Resubmitted");
  }

  @Test
  @DisplayName("Content fields: heading and body, never 'Not provided', not counted in the eyebrow")
  void rendersContentField() throws IOException {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 1, "Consent", null, 0, 0, Collections.singletonList(content(
        "GENERAL DATA PROTECTION REGULATION PRIVACY NOTICE & CONSENT",
        "I consent to the processing of my personal data.\n\nRead the privacy notice.",
        "<p>I consent to the processing of my <strong>personal</strong> data.</p>"
            + "<p>Read the <a href=\"https://example.com/privacy\" target=\"_blank\">privacy notice</a>.</p>"))));

    List<String> pages = render(payload(0, 0, Collections.emptyList(), blocks));
    String page2 = pages.get(1);

    assertThat(page2).contains("SECTION 1 OF 1");
    assertThat(page2).doesNotContain("ANSWERED");
    assertThat(page2).contains("GENERAL DATA PROTECTION REGULATION PRIVACY NOTICE & CONSENT");
    assertThat(flat(page2)).contains("I consent to the processing of my personal data.");
    assertThat(flat(page2)).contains("Read the privacy notice.");
    assertThat(page2).doesNotContain("Not provided");
    assertThat(linkCount).as("link inside the content html stays clickable").isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("Content falls back to plain text (escaped, line breaks kept) when html is null")
  void contentFallsBackToText() throws IOException {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 1, "Consent", null, 0, 0, Collections.singletonList(
        content("Notice", "first <b>line</b> & more\nsecond line", null))));

    String page2 = render(payload(0, 0, Collections.emptyList(), blocks)).get(1);

    assertThat(page2).contains("first <b>line</b> & more");
    assertThat(page2).containsPattern("more\\s*\\n\\s*second line");
  }

  @Test
  @DisplayName("A content field with 400 paragraphs renders completely over many pages")
  void rendersVeryLongContent() throws IOException {
    StringBuilder html = new StringBuilder();
    StringBuilder text = new StringBuilder();
    for (int i = 1; i <= 400; i++) {
      html.append("<p>Clause ").append(i).append(" lorem ipsum dolor sit amet consectetur.</p>");
      text.append("Clause ").append(i).append(" lorem ipsum dolor sit amet consectetur.\n\n");
    }
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(block(1, 2, "Personal information", null, 2, 2, fields("Field", 2)));
    blocks.add(block(2, 2, "Consent", null, 0, 0, Arrays.asList(
        content("Privacy notice", text.toString(), html.toString()),
        content("Closing", "THE END", "<p>THE END</p>"))));

    long start = System.nanoTime();
    List<String> pages = render(payload(2, 2, Collections.emptyList(), blocks));
    long millis = (System.nanoTime() - start) / 1_000_000;
    System.out.println("QUESTIONNAIRE long content: " + millis + " ms, pages=" + pages.size());

    String all = String.join("\n", pages);
    for (int i = 1; i <= 400; i++) {
      assertThat(all).containsPattern("Clause " + i + " lorem");
    }
    assertThat(count(all, "Clause \\d+ lorem")).isEqualTo(400);
    assertThat(all).contains("THE END");
    assertThat(pages.size()).isGreaterThan(8);

    // footer and running header stay right on every page, however long the content
    for (int i = 0; i < pages.size(); i++) {
      assertThat(pages.get(i)).contains("Page " + (i + 1));
      assertThat(pages.get(i)).contains("Zhang Ling He | Case #238547");
      if (i > 0) {
        assertThat(pages.get(i)).contains("VERIFIED QUESTIONNAIRE");
      }
    }
    // every page the Consent section flows onto repeats its heading
    int continued = 0;
    for (String page : pages) {
      if (page.contains("Consent (continued)")) {
        continued++;
      }
    }
    assertThat(continued).isGreaterThanOrEqualTo(pages.size() - 4);
    assertThat(millis).isLessThan(10_000);
  }

  @Test
  @DisplayName("Latin-extended names (outside Helvetica/WinAnsi) render via Noto Sans")
  void rendersUnicodeName() throws IOException {
    Assumptions.assumeTrue(unicodeFontAvailable(), "Noto Sans not in src/main/resources/fonts yet");
    List<Map<String, Object>> first = fields("Field", 8);
    first.add(field("Full name", "Łukasz Şahin Nguyễn"));

    List<String> pages = render(payload(17, 17, Collections.emptyList(), twoSections(first)));

    assertThat(String.join("\n", pages)).contains("Łukasz Şahin Nguyễn");
  }

  @Test
  @DisplayName("Hostile values render as text and newlines become line breaks")
  void escapesHostileValues() throws IOException {
    List<Map<String, Object>> first = fields("Field", 8);
    first.add(field("Notes", "<script>alert(1)</script> & \"quoted\"\nline two"));

    List<String> pages = render(payload(17, 17, Collections.emptyList(), twoSections(first)));
    String page2 = pages.get(1);

    assertThat(page2).contains("<script>alert(1)</script> & \"quoted\"");
    assertThat(page2).containsPattern("\"quoted\"\\s*\\n\\s*line two");
  }

  @Test
  @DisplayName("Footer on every page, running header on pages 2+, no version text")
  void footerAndRunningHeader() throws IOException {
    List<String> pages = render(payload(18, 19, Collections.emptyList(), twoSections(fields("Field", 17))));

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
  @DisplayName("Questionnaire without a case renders without a dangling Case #")
  void rendersWithoutCase() throws IOException {
    Map<String, Object> data = payload(18, 19, Collections.emptyList(), twoSections(fields("Field", 17)));
    @SuppressWarnings("unchecked")
    Map<String, Object> caseInfo = (Map<String, Object>) data.get("case");
    caseInfo.put("number", "");
    caseInfo.put("name", "");

    List<String> pages = render(data);

    for (String page : pages) {
      assertThat(page).doesNotContain("Case #");
      assertThat(page).contains("Zhang Ling He");
    }
    assertThat(pages.get(0)).doesNotContain("CASE NUMBER\n#");
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
    blocks.add(block(1, 1, "Empty section", null, 0, 0, new ArrayList<>()));

    List<String> pages = render(payload(0, 0, Collections.emptyList(), blocks));

    assertThat(String.join("\n", pages)).contains("Empty section");
  }

  @Test
  @DisplayName("Several missing answers use the multi-missing wording")
  void rendersManyMissing() throws IOException {
    List<String> pages = render(payload(9, 12, Arrays.asList("Alpha", "Beta", "Gamma"),
        twoSections(fields("Field", 9))));

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
    blocks.add(block(1, 4, "Personal information", "Identity.", 17, 17, fields("Personal", 17)));
    blocks.add(block(2, 4, "Current employment", "Employer.", 5, 5, fields("Job", 5)));
    blocks.add(block(3, 4, "H-1B history", "History.", 4, 4, fields("H1B", 4)));
    blocks.add(block(4, 4, "Additional information", "Notes.", 1, 2, Arrays.asList(
        field("Arrested?", "No"), field("Notes", null))));

    long start = System.nanoTime();
    List<String> pages = render(payload(27, 28, Collections.singletonList("Notes"), blocks));
    long millis = (System.nanoTime() - start) / 1_000_000;
    System.out.println("QUESTIONNAIRE render time: " + millis + " ms, pages=" + pages.size());

    assertThat(millis).isLessThan(10_000);
  }
}
