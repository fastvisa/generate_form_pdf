package com.fastvisa.services.questionnaire;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.DefaultTagWorkerFactory;
import com.itextpdf.html2pdf.attach.impl.tags.TableTagWorker;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Table;
import com.itextpdf.styledxmlparser.node.IElementNode;

/**
 * html2pdf repeats a table's &lt;thead&gt; on every page, including the first. The questionnaire uses that header
 * for the "Title (continued)" heading of a section, which must appear only on the pages the section flows onto.
 * Tables with class "section-table" therefore skip their header on the first page.
 */
public class QuestionnaireTagWorkerFactory extends DefaultTagWorkerFactory {

  private static final String SECTION_TABLE_CLASS = "section-table";

  @Override
  public ITagWorker getCustomTagWorker(IElementNode tag, ProcessorContext context) {
    if ("table".equals(tag.name()) && isSectionTable(tag)) {
      return new SkipFirstHeaderTableWorker(tag, context);
    }
    return null;
  }

  private static boolean isSectionTable(IElementNode tag) {
    String classes = tag.getAttribute("class");
    return classes != null && (" " + classes + " ").contains(" " + SECTION_TABLE_CLASS + " ");
  }

  private static class SkipFirstHeaderTableWorker extends TableTagWorker {
    SkipFirstHeaderTableWorker(IElementNode element, ProcessorContext context) {
      super(element, context);
    }

    @Override
    public IPropertyContainer getElementResult() {
      IPropertyContainer result = super.getElementResult();
      if (result instanceof Table) {
        ((Table) result).setSkipFirstHeader(true);
      }
      return result;
    }
  }
}
