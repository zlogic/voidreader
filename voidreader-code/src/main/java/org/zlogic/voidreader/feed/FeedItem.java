/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.feed;

import com.sun.syndication.feed.synd.SyndEntry;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.xml.bind.annotation.XmlAttribute;
import org.apache.commons.io.IOUtils;
import org.stringtemplate.v4.ST;

/**
 *
 * @author Dmitry
 */
public class FeedItem {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	@XmlAttribute(name = "uuid")
	private String id;
	private String link;
	private String itemText;
	private String itemHtml;

	private FeedItem() {
	}

	protected FeedItem(Feed feed, SyndEntry entry) throws IOException {
		this.id = feed.getUrl() + "@@" + entry.getUri() + "@@" + entry.getLink() + "@@" + entry.getTitle();//Unique ID
		this.link = entry.getLink();

		ST textTemplate = new ST(IOUtils.toString(FeedItem.class.getResourceAsStream("templates/FeedItem.txt")), '$', '$');
		textTemplate.add("feed", feed);
		textTemplate.add("entry", entry);
		itemText = textTemplate.render();

		ST htmlTemplate = new ST(IOUtils.toString(FeedItem.class.getResourceAsStream("templates/FeedItem.html")), '$', '$');
		htmlTemplate.add("feed", feed);
		htmlTemplate.add("entry", entry);
		itemHtml = htmlTemplate.render();
		//TODO: extract alt-text from images for comics
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FeedItem
				&& ((FeedItem) obj).id.equals(id);
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 97 * hash + Objects.hashCode(this.id);
		return hash;
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

	public String getLink() {
		return link;
	}
}
