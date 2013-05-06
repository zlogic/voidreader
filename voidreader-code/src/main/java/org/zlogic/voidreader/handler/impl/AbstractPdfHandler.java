/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author Dmitry
 */
public abstract class AbstractPdfHandler {

	public AbstractPdfHandler() {
	}

	protected ITextRenderer renderPdf(String html, String baseUrl) throws IOException, DocumentException {
		ITextRenderer renderer = new ITextRenderer();

		ITextFontResolver fontResolver = new ITextFontResolver(renderer.getSharedContext()) {
			@Override
			public org.xhtmlrenderer.render.FSFont resolveFont(org.xhtmlrenderer.layout.SharedContext renderingContext, org.xhtmlrenderer.css.value.FontSpecification spec) {
				spec.families = new String[]{"Open Sans"};
				FSFont font = super.resolveFont(renderingContext, spec);
				return font;
			}
		};
		renderer.getSharedContext().setFontResolver(fontResolver);

		//Override fonts
		for (URL url : new FontsList().getFontUrls()) {
			if (url.toExternalForm().endsWith(".ttf"))
				fontResolver.addFont(url.toExternalForm(), BaseFont.IDENTITY_H, true);
		}
		renderer.setDocumentFromString(html, baseUrl);
		renderer.layout();
		return renderer;
	}

	protected ITextRenderer downloadRenderPdf(String url) throws IOException, DocumentException {
		Document htmlDocument = Jsoup.connect(url).followRedirects(true).timeout(60000).get();//TODO: make timeout configurable
		Cleaner cleaner = new Cleaner(Whitelist.relaxed().preserveRelativeLinks(false)
				.addTags("span")
				.addAttributes("span", "id", "style"));
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
