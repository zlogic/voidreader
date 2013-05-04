/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.synd.SyndEntry;
import java.io.IOException;
import java.util.ResourceBundle;
import org.apache.commons.io.IOUtils;
import org.stringtemplate.v4.ST;

/**
 *
 * @author Dmitry
 */
public class FeedItem {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	private Feed feed;
	private String uri;
	private String itemText;
	private String itemHtml;

	protected FeedItem(Feed feed, SyndEntry entry) throws IOException {
		this.feed = feed;
		this.uri = entry.getUri();

		ST textTemplate = new ST(IOUtils.toString(FeedItem.class.getResourceAsStream("templates/FeedItem.txt")), '$', '$');
		textTemplate.add("feed", feed);
		textTemplate.add("entry", entry);
		itemText = textTemplate.render();

		ST htmlTemplate = new ST(IOUtils.toString(FeedItem.class.getResourceAsStream("templates/FeedItem.html")), '$', '$');
		htmlTemplate.add("feed", feed);
		htmlTemplate.add("entry", entry);
		itemHtml = htmlTemplate.render();
	}

	public void send() {
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FeedItem
				&& ((FeedItem) obj).feed.equals(feed)
				&& ((FeedItem) obj).uri.equals(uri);
	}

	/*
	 * Getters
	 */
	public String getItemText() {
		return itemText;
	}

	public String getItemHtml() {
		return itemHtml;
	}
}
