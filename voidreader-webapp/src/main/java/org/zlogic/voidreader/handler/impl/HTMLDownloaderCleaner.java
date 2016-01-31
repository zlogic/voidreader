/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * Class for downloading and cleaning HTML pages.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class HTMLDownloaderCleaner {

	/**
	 * Downloads a feed item's URL with JSoup, converts to XHTML and performs a
	 * tag cleanup.
	 *
	 * @param url the URL to download
	 * @return the cleaned up XHTML document
	 * @throws IOException when the document cannot be downloaded
	 */
	protected String downloadCleanHTML(URL url) throws IOException {
		Document htmlDocument = Jsoup.connect(url.toString()).followRedirects(true).timeout(60000).get();//TODO: make timeout configurable
		Cleaner cleaner = new Cleaner(Whitelist.relaxed().preserveRelativeLinks(false)
				.addTags("span") //NOI18N
				.addAttributes("span", "id", "style")); //NOI18N
		Document htmlDocumentClean = cleaner.clean(htmlDocument);
		htmlDocumentClean.setBaseUri(url.toString());
		htmlDocumentClean.charset(Charset.forName("utf-8"));
		htmlDocumentClean.outputSettings().prettyPrint(false);
		htmlDocumentClean.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
		htmlDocumentClean.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		return htmlDocumentClean.html();
	}
}
