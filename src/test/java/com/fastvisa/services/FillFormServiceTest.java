package com.fastvisa.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.renderer.IRenderer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FillFormService Tests")
class FillFormServiceTest {

    private FillFormService fillFormService;
    private JSONParser jsonParser;

    @BeforeEach
    void setUp() {
        fillFormService = new FillFormService();
        jsonParser = new JSONParser();
    }

    @Test
    @DisplayName("Should parse form data from JSON string")
    void shouldParseFormDataFromJsonString() throws Exception {
        String jsonData = "[{\"name\":\"field1\",\"value\":\"value1\"},{\"name\":\"field2\",\"value\":\"value2\"}]";
        File tempFile = createTempJsonFile(jsonData);

        JSONArray result = fillFormService.getFormArray(tempFile.getAbsolutePath());

        assertThat(result).hasSize(2);
        JSONObject field1 = (JSONObject) result.get(0);
        assertThat(field1.get("name")).isEqualTo("field1");
        assertThat(field1.get("value")).isEqualTo("value1");

        Files.deleteIfExists(tempFile.toPath());
    }

    @Test
    @DisplayName("Should parse form data from object")
    void shouldParseFormDataFromObject() throws Exception {
        Map<String, Object> formData1 = new HashMap<>();
        formData1.put("name", "field1");
        formData1.put("value", "value1");

        Map<String, Object> formData2 = new HashMap<>();
        formData2.put("name", "field2");
        formData2.put("value", "value2");

        Object formData = Arrays.asList(formData1, formData2);

        JSONArray result = fillFormService.getFormArray(formData);

        assertThat(result).hasSize(2);
        JSONObject field1 = (JSONObject) result.get(0);
        assertThat(field1.get("name")).isEqualTo("field1");
    }

    @Test
    @DisplayName("Should parse custom fields from object")
    void shouldParseCustomFieldsFromObject() throws Exception {
        Map<String, Object> custom1 = new HashMap<>();
        custom1.put("field_name", "field1");
        custom1.put("x", 100);
        custom1.put("y", 200);

        Object customFields = Arrays.asList(custom1);

        JSONArray result = fillFormService.getCustomFieldsArray(customFields);

        assertThat(result).hasSize(1);
        JSONObject field = (JSONObject) result.get(0);
        assertThat(field.get("field_name")).isEqualTo("field1");
        assertThat(field.get("x")).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should not collapse font for short text in short multiline field")
    void shouldNotCollapseFontForShortTextInShortMultilineField() throws Exception {
        // Real rect of I-914 Supp A field pEWhyCharged1 (174.21 x 28.346pt, ~2 lines)
        Rectangle rect = new Rectangle(54f, 320.899f, 174.21f, 28.346f);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);

        Method method = FillFormService.class.getDeclaredMethod(
            "getDynamicMultiLineFontSize", String.class, Rectangle.class, PdfFont.class, float.class);
        method.setAccessible(true);
        float fontSize = (float) method.invoke(fillFormService, "Theft", rect, font, 14f);

        assertThat(fontSize).isGreaterThan(4f);
    }

    @Test
    @DisplayName("Should tighten leading, not collapse font, when a value's forced line breaks don't fit the default leading")
    void shouldTightenLeadingForUnfittableLineBreaksInShortMultilineField() throws Exception {
        // Real rect of I-914 Supp A field pEOutcome1 (121.93 x 28.346pt, ~1 line at the default
        // 14pt leading). A value with an embedded newline (e.g. the user pressed Enter, as the
        // browser's own textarea preview for this field shows) forces 2 lines, which this field
        // can never hold at the default leading no matter how small the font gets. fillForm()
        // tightens the leading to fit those forced lines instead of collapsing the font.
        Rectangle rect = new Rectangle(54f, 320.899f, 121.93f, 28.346f);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
        float defaultLineHeight = 14f;
        String value = "no charges filed,\ncharges dismissed, jail";

        Method tighten = FillFormService.class.getDeclaredMethod(
            "tightenLineHeightForForcedLines", String.class, float.class, float.class);
        tighten.setAccessible(true);
        float tightenedLineHeight = (float) tighten.invoke(
            fillFormService, value, rect.getHeight() - 8f, defaultLineHeight);
        assertThat(tightenedLineHeight).isLessThan(defaultLineHeight);

        Method sizeMethod = FillFormService.class.getDeclaredMethod(
            "getDynamicMultiLineFontSize", String.class, Rectangle.class, PdfFont.class, float.class);
        sizeMethod.setAccessible(true);
        float fontSize = (float) sizeMethod.invoke(fillFormService, value, rect, font, tightenedLineHeight);

        // Both explicit lines still render (no text lost), at a legible size - not the 1pt floor.
        assertThat(fontSize).isGreaterThan(4f);
    }

    @Test
    @DisplayName("Should tighten leading, not collapse font, when a compact single-line cell is shorter than the default leading")
    void shouldTightenLeadingForCompactSingleLineCell() throws Exception {
        // Real rect of the I-485 "Means-Tested Public Benefit Received" table cell (167.3 x
        // 18pt). usableHeight is only 10pt - shorter than the 14pt default leading even for a
        // single line, so a plain value with no line breaks at all (e.g. "balala") still fails the
        // fit-check forever and collapses to the 1pt floor unless the leading itself shrinks.
        Rectangle rect = new Rectangle(60f, 150f, 167.3f, 18f);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
        float defaultLineHeight = 14f;
        String value = "balala";

        Method tighten = FillFormService.class.getDeclaredMethod(
            "tightenLineHeightForForcedLines", String.class, float.class, float.class);
        tighten.setAccessible(true);
        float tightenedLineHeight = (float) tighten.invoke(
            fillFormService, value, rect.getHeight() - 8f, defaultLineHeight);
        assertThat(tightenedLineHeight).isLessThan(defaultLineHeight);

        Method sizeMethod = FillFormService.class.getDeclaredMethod(
            "getDynamicMultiLineFontSize", String.class, Rectangle.class, PdfFont.class, float.class);
        sizeMethod.setAccessible(true);
        float fontSize = (float) sizeMethod.invoke(fillFormService, value, rect, font, tightenedLineHeight);

        assertThat(fontSize).isGreaterThan(4f);
    }

    @Test
    @DisplayName("Should not pick a font taller than a tightened leading, or lines will overlap")
    void shouldNotExceedTightenedLeadingWithFontSize() throws Exception {
        // Real I-485 "Means-Tested Public Benefit Received" cell (167.3 x 18pt). A value with a
        // forced newline but short lines (e.g. "Medicaid,\nhealth coverage") has plenty of width to
        // grow toward the 10pt base font, but at the leading tightened for 2 forced lines (~5pt)
        // a 10pt font would make the two lines overlap/collide instead of stacking legibly.
        Rectangle rect = new Rectangle(60f, 150f, 167.3f, 18f);
        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
        float defaultLineHeight = 14f;
        String value = "Medicaid,\nhealth coverage";

        Method tighten = FillFormService.class.getDeclaredMethod(
            "tightenLineHeightForForcedLines", String.class, float.class, float.class);
        tighten.setAccessible(true);
        float tightenedLineHeight = (float) tighten.invoke(
            fillFormService, value, rect.getHeight() - 8f, defaultLineHeight);

        Method sizeMethod = FillFormService.class.getDeclaredMethod(
            "getDynamicMultiLineFontSize", String.class, Rectangle.class, PdfFont.class, float.class);
        sizeMethod.setAccessible(true);
        float fontSize = (float) sizeMethod.invoke(fillFormService, value, rect, font, tightenedLineHeight);

        assertThat(fontSize).isLessThanOrEqualTo(tightenedLineHeight);
    }

    @Test
    @DisplayName("Should render every multiline field combination inside its box without overlapping lines")
    void shouldRenderMultilineFieldsWithoutOverflowOrOverlapAcrossManySizesAndValues() throws Exception {
        // Broad regression sweep, not a single hand-picked case: every bug found in this file so
        // far (lineCount off-by-one, forced newlines, boxes shorter than the default leading,
        // runs of spaces, font size exceeding a tightened leading) was a real USCIS field/value
        // combination the unit tests above didn't happen to cover. Rather than add one more
        // narrow case each time a new one turns up, this renders a wide grid of box sizes x
        // leadings x values through the real iText layout engine (not just the internal size
        // estimate) and fails if any combination overflows its box or lets the font size exceed
        // its own leading (which makes consecutive lines overlap).
        Method tighten = FillFormService.class.getDeclaredMethod(
            "tightenLineHeightForForcedLines", String.class, float.class, float.class);
        tighten.setAccessible(true);
        Method sizeMethod = FillFormService.class.getDeclaredMethod(
            "getDynamicMultiLineFontSize", String.class, Rectangle.class, PdfFont.class, float.class);
        sizeMethod.setAccessible(true);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
        PdfDocument pdf = new PdfDocument(new PdfWriter(new ByteArrayOutputStream()));
        pdf.addNewPage();

        String longText = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore ";
        String manySpaces = "word1          word2     word3 word4";
        String[] shortNewlineValues = {
            "Medicaid,\nhealth coverage",
            "a\nb\nc",
            "Supplemental Nutrition Assistance Program (SNAP),\nTANF,\nhousing assistance",
        };
        // Real field heights/widths seen so far: I-485 compact table cells (18pt tall, 41.9-167.3pt
        // wide), I-914 table cells (28.346pt), and larger free-text boxes.
        float[] heights = {18f, 20f, 28.346f, 35f, 50f, 80f, 150f, 300f, 500f, 700f};
        float[] widths = {41.9f, 72f, 100f, 167.3f, 300f, 500f};
        float[] leadings = {14f, 12f, 18f, 24f};

        int checked = 0;
        StringBuilder failures = new StringBuilder();

        for (float h : heights) {
            for (float w : widths) {
                for (float lead : leadings) {
                    for (int reps = 0; reps <= 40; reps += 4) {
                        String seed;
                        switch (reps % 4) {
                            case 0: seed = manySpaces; break;
                            case 1: seed = "balala"; break;
                            case 2: seed = shortNewlineValues[reps % shortNewlineValues.length]; break;
                            default: seed = "N/A";
                        }
                        StringBuilder sb = new StringBuilder(seed);
                        for (int r = 0; r < reps; r++) {
                            sb.append(r % 7 == 0 ? "\n" : " ").append(longText);
                        }
                        String value = sb.toString();
                        Rectangle rect = new Rectangle(0, 0, w, h);

                        float effectiveLead = (float) tighten.invoke(fillFormService, value, rect.getHeight() - 8f, lead);
                        float size = (float) sizeMethod.invoke(fillFormService, value, rect, font, effectiveLead);
                        if (size <= 1f) {
                            continue; // field too small to hold any content - not what this test is checking
                        }

                        if (size > effectiveLead + 0.1f) {
                            failures.append(String.format(
                                "FONT>LEADING h=%s w=%s lead=%s reps=%s size=%s effLead=%s%n",
                                h, w, lead, reps, size, effectiveLead));
                            checked++;
                            continue;
                        }

                        Paragraph p = new Paragraph(new Text(value).setFont(font).setFontSize(size))
                            .setFixedLeading(effectiveLead).setPaddingTop(-5);
                        LayoutResult result;
                        try (Canvas canvas = new Canvas(new PdfCanvas(pdf.getFirstPage()), rect)) {
                            IRenderer renderer = p.createRendererSubTree().setParent(canvas.getRenderer());
                            result = renderer.layout(new LayoutContext(new LayoutArea(1, rect.clone())));
                        }
                        checked++;
                        if (result.getStatus() != LayoutResult.FULL) {
                            failures.append(String.format(
                                "OVERFLOW h=%s w=%s lead=%s reps=%s size=%s effLead=%s%n",
                                h, w, lead, reps, size, effectiveLead));
                        }
                    }
                }
            }
        }
        pdf.close();

        assertThat(checked).isGreaterThan(500); // sanity check the sweep actually ran
        assertThat(failures.toString()).isEmpty();
    }

    private File createTempJsonFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("test-form-data", ".json");
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write(content);
        }
        return tempFile.toFile();
    }
}
