/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler.impl;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.net.URL;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.AbstractImageProvider;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import org.zlogic.voidreader.fonts.FontsList;

/**
 * Abstract class with PDF generation functions. Can be inherited by other
 * handlers to provide PDF generations features.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public abstract class AbstractPdfHandler {

	/**
	 * The list of fonts included with this application
	 */
	private static final FontsList fontsList = new FontsList();

	/**
	 * HTMLDownloaderCleaner instance
	 */
	private static final HTMLDownloaderCleaner htmlDownloaderCleaner = new HTMLDownloaderCleaner();

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
	protected byte[] downloadRenderPdf(final URL url) throws IOException, DocumentException {

		AbstractImageProvider imageProvider = new AbstractImageProvider() {

			@Override
			public Image retrieve(final String src) {
				Image img = super.retrieve(src);
				if (img != null)
					return img;
				try {
					super.store(src, Image.getInstance(new URL(src)));
				} catch (BadElementException | IOException ex) {
					return null;
				}
				return super.retrieve(src);
			}

			@Override
			public String getImageRootPath() {
				return url.getPath();
			}
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		Document document = new Document();

		PdfWriter writer = PdfWriter.getInstance(document, baos);
		document.open();

		CSSResolver cssResolver = new StyleAttrCSSResolver();
		CssFile cssFile = XMLWorkerHelper.getCSS(new ByteArrayInputStream("body {font-family:open sans}".getBytes()));
		cssResolver.addCss(cssFile);

		XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
		for (URL fontURL : fontsList.getFontUrls())
			fontProvider.register(fontURL.toString());
		CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);

		HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
		htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
		htmlContext.setImageProvider(imageProvider);

		PdfWriterPipeline pdf = new PdfWriterPipeline(document, writer);
		HtmlPipeline htmlPipeline = new HtmlPipeline(htmlContext, pdf);
		CssResolverPipeline css = new CssResolverPipeline(cssResolver, htmlPipeline);

		XMLWorker worker = new XMLWorker(css, true);
		XMLParser p = new XMLParser(worker);
		p.parse(new StringReader(htmlDownloaderCleaner.downloadCleanHTML(url)));

		document.close();
		return baos.toByteArray();
	}

}
