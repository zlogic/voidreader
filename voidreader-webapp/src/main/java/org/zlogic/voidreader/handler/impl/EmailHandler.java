/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler.impl;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zlogic.voidreader.Settings;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 * FeedItemHandler and ErrorHandler implementation which sends an email for
 * every new feed item.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class EmailHandler extends AbstractPdfHandler implements ErrorHandler, FeedItemHandler {

	/**
	 * The logger
	 */
	private static final Logger log = LoggerFactory.getLogger(EmailHandler.class);
	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * The user settings
	 */
	private final Settings settings;
	/**
	 * The current email session
	 */
	private final Session mailSession;

	/**
	 * Constructor for EmailHandler
	 *
	 * @param settings the user settings
	 */
	public EmailHandler(Settings settings) {
		this.settings = settings;
		mailSession = Session.getDefaultInstance(new Properties(), null);
	}

	@Override
	public void handle(Feed feed, Exception ex) {
		throw new UnsupportedOperationException(messages.getString("NOT_SUPPORTED_YET")); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handle(Feed feed, FeedItem item) {
		MimeMessage message = new MimeMessage(mailSession);

		try {
			InternetAddress mailFrom = settings.getMailFrom();
			mailFrom.setPersonal(feed.getTitle().replaceAll("[\r\n]+", ""), "utf-8");//TODO: make encoding configurable //NOI18N
			message.setFrom(mailFrom);

			message.addRecipient(Message.RecipientType.TO, settings.getMailTo());
			message.setSubject(MessageFormat.format("{0}", new Object[]{item.getTitle().replaceAll("[\r\n]+", "")})); //NOI18N

			Multipart multipart = new MimeMultipart();

			MimeBodyPart body = new MimeBodyPart();

			MimeMultipart bodyAlternatives = new MimeMultipart("alternative"); //NOI18N

			BodyPart messageTextPart = new MimeBodyPart();
			messageTextPart.setContent(item.getItemText(), "text/plain; charset=\"utf-8\"");//TODO: make encoding configurable  //NOI18N
			messageTextPart.setHeader("Content-Transfer-Encoding", "quoted-printable"); //NOI18N
			bodyAlternatives.addBodyPart(messageTextPart);

			BodyPart messageHtmlBodyPart = new MimeBodyPart();
			messageHtmlBodyPart.setContent(item.getItemHtml(), "text/html; charset=\"utf-8\"");//TODO: make encoding configurable //NOI18N
			messageHtmlBodyPart.setHeader("Content-Transfer-Encoding", "quoted-printable"); //NOI18N
			bodyAlternatives.addBodyPart(messageHtmlBodyPart);

			body.setContent(bodyAlternatives);

			multipart.addBodyPart(messageTextPart);
			multipart.addBodyPart(messageHtmlBodyPart);

			FeedItem.State newState = item.getState();
			if (item.getState() != FeedItem.State.SENT_PDF && settings.isEnablePdf()) {
				try {
					BodyPart pdfBodyPart = new MimeBodyPart();
					pdfBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
					pdfBodyPart.setFileName("source.pdf"); //TODO: make filename configurables //NOI18N
					pdfBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(createPdf(item), "application/pdf"))); //NOI18N
					multipart.addBodyPart(pdfBodyPart);
					newState = FeedItem.State.SENT_PDF;
				} catch (Exception ex) {
					log.error(messages.getString("CANNOT_GENERATE_PDF"), ex);
				}
			}
			message.setContent(multipart);
			if (item.getPublishedDate() != null)
				message.setSentDate(item.getPublishedDate());
			boolean pdfFailedAgain = newState == FeedItem.State.SENT_ENTRY && item.getState() == FeedItem.State.SENT_ENTRY;
			if (!pdfFailedAgain)
				Transport.send(message);
			if (newState != FeedItem.State.SENT_PDF)
				newState = FeedItem.State.SENT_ENTRY;
			item.setState(newState);
		} catch (MessagingException | UnsupportedEncodingException ex) {
			throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_SEND_OR_PREPARE_EMAIL_MESSAGE_FOR_ITEM"), new Object[]{item.getLink()}), ex);
		}
	}

	/**
	 * Downloads a feed item's contents as a PDF file.
	 *
	 * @param item the feed item to download
	 * @throws Exception if PDF download or rendering failed
	 * @return the PDF as a byte array
	 */
	private byte[] createPdf(FeedItem item) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		downloadRenderPdf(item.getLink()).createPDF(out);
		return out.toByteArray();
	}
}
