/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic42@outlook.com>
 */
package org.zlogic.voidreader.handler.impl;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import java.io.IOException;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.render.FSFont;
import org.zlogic.voidreader.fonts.FontsList;

/**
 * Abstract class with PDF generation functions. Can be inherited by other
 * handlers to provide PDF generations features.
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic42@outlook.com">zlogic42@outlook.com</a>
 */
public abstract class AbstractPdfHandler {

	/**
	 * The list of fonts included with this application
	 */
	private FontsList fontsList = new FontsList();

	/**
	 * Renders an XHTML page to PDF
	 *
	 * @param html the page XHTML contents
	 * @param baseUrl the page base URL
	 * @return an ITextRenderer which can be used to produce the final PDF
	 * document
	 * @throws IOException when fonts cannot be located
	 * @throws DocumentException when an error prevents the PDF document from
	 * being generated
	 */
	protected ITextRenderer renderPdf(String html, String baseUrl) throws IOException, DocumentException {
		ITextRenderer renderer = new ITextRenderer();

		//Replace with custom font resolver
		ITextFontResolver fontResolver = new ITextFontResolver(renderer.getSharedContext()) {
			@Override
			public org.xhtmlrenderer.render.FSFont resolveFont(org.xhtmlrenderer.layout.SharedContext renderingContext, org.xhtmlrenderer.css.value.FontSpecification spec) {
				spec.families = new String[]{"Open Sans"}; //NOI18N
				FSFont font = super.resolveFont(renderingContext, spec);
				return font;
			}
		};
		renderer.getSharedContext().setFontResolver(fontResolver);

		//Override fonts
		for (URL url : fontsList.getFontUrls())
			if (url.toExternalForm().endsWith(".ttf")) //NOI18N
				fontResolver.addFont(url.toExternalForm(), BaseFont.IDENTITY_H, true);

		renderer.setDocumentFromString(html, baseUrl);
		renderer.layout();
		return renderer;
	}

	/**
	 * Downloads a feed item's URL with Flying Saucer, converts to XHTML and
	 * performs a tag cleanup, and finally renders the result to PDF.
	 *
	 * @param url the URL to download
	 * @return an ITextRenderer which can be used to produce the final PDF
	 * document
	 * @throws IOException when fonts cannot be located
	 * @throws DocumentException when an error prevents the PDF document from
	 * being generated
	 */
	protected ITextRenderer downloadRenderPdf(String url) throws IOException, DocumentException {
		Document htmlDocument = Jsoup.connect(url).followRedirects(true).timeout(60000).get();//TODO: make timeout configurable
		Cleaner cleaner = new Cleaner(Whitelist.relaxed().preserveRelativeLinks(false)
				.addTags("span") //NOI18N
				.addAttributes("span", "id", "style")); //NOI18N
		Document htmlDocumentClean = cleaner.clean(htmlDocument);
		htmlDocumentClean.setBaseUri(url);
		htmlDocumentClean.outputSettings().prettyPrint(false);
		htmlDocumentClean.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		/*
		 for (Element element : htmlDocument.head().getAllElements()) {
		 switch (element.tagName().toLowerCase()) {
		 case "style":
		 Element styleElement = htmlDocumentClean.head().appendElement(element.tagName());
		 styleElement.appendText(element.text());
		 for (Node dataNode : element.dataNodes())
		 styleElement.appendChild(dataNode);
		 for (Attribute attribute : element.attributes())
		 if (attribute.getKey().toLowerCase().equals("id") || attribute.getKey().toLowerCase().equals("type"))
		 styleElement.attr(attribute.getKey(), attribute.getValue());
		 break;
		 case "link":
		 Element linkElement = htmlDocumentClean.head().appendElement(element.tagName());
		 linkElement.appendText(element.text());
		 for (Attribute attribute : element.attributes())
		 if (attribute.getKey().toLowerCase().equals("rel") || attribute.getKey().toLowerCase().equals("href") || attribute.getKey().toLowerCase().equals("type"))
		 linkElement.attr(attribute.getKey(), attribute.getValue());
		 for (Node dataNode : element.dataNodes())
		 linkElement.appendChild(dataNode);
		 break;
		 }
		 }
		 */
		return renderPdf(htmlDocumentClean.html(), url);
	}
}
