/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.handler.file;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.zlogic.voidreader.Settings;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 *
 * @author Dmitry
 */
public class EmailHandler implements ErrorHandler, FeedItemHandler {

	private static final Logger log = Logger.getLogger(EmailHandler.class.getName());
	private static final int MAX_NAME = 100;
	private Settings settings;
	private Session mailSession;

	public EmailHandler(Settings settings) {
		this.settings = settings;
	}

	@Override
	public void handle(Feed feed, Exception ex) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private void openEmailSession() {
		if (mailSession == null) {
			Authenticator authenticator = new Authenticator() {
				private PasswordAuthentication passwordAuthentication;

				public Authenticator setSettings(Settings settings) {
					this.passwordAuthentication = new PasswordAuthentication(settings.getMailProperties().getProperty("mail.smtp.user"), settings.getMailProperties().getProperty("mail.smtp.password"));
					return this;
				}

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return passwordAuthentication;
				}
			}.setSettings(settings);
			mailSession = Session.getDefaultInstance(settings.getMailProperties(), authenticator);
		}
	}

	@Override
	public void handle(Feed feed, FeedItem item) throws RuntimeException {
		openEmailSession();
		MimeMessage message = new MimeMessage(mailSession);

		try {
			message.setFrom(settings.getMailFrom());
			message.addRecipient(Message.RecipientType.TO, settings.getMailTo());
			message.setSubject(MessageFormat.format("{0} : {1}", new Object[]{feed.getUserTitle().replaceAll("[\r\n]+", ""), item.getTitle().replaceAll("[\r\n]+", "")}));

			Multipart multipart = new MimeMultipart();

			MimeBodyPart body = new MimeBodyPart();

			MimeMultipart bodyAlternatives = new MimeMultipart("alternative");

			BodyPart messageTextPart = new MimeBodyPart();
			messageTextPart.setContent(item.getItemText(), "text/plain; charset=\"utf-8\"");
			messageTextPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
			bodyAlternatives.addBodyPart(messageTextPart);

			BodyPart messageHtmlBodyPart = new MimeBodyPart();
			messageHtmlBodyPart.setContent(item.getItemHtml(), "text/html; charset=\"utf-8\"");
			messageHtmlBodyPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
			bodyAlternatives.addBodyPart(messageHtmlBodyPart);

			body.setContent(bodyAlternatives);

			multipart.addBodyPart(body);

			try {
				BodyPart pdfBodyPart = new MimeBodyPart();
				pdfBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
				pdfBodyPart.setFileName("source.pdf");
				pdfBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(createPdf(item), "application/pdf")));
				multipart.addBodyPart(pdfBodyPart);
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Cannot generate PDF", ex);
			}
			message.setContent(multipart);
			message.setSentDate(item.getPublishedDate());
			Transport.send(message);
		} catch (MessagingException ex) {
			throw new RuntimeException("Cannot send or prepare email message for item " + item.getLink(), ex);
		}
	}

	private byte[] createPdf(FeedItem item) throws Exception {
		/*
		 * See
		 * http://stackoverflow.com/questions/10493837/how-to-export-html-page-to-pdf-format
		 * http://stackoverflow.com/questions/235851/using-itext-to-convert-html-to-pdf
		 */
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Document doc;
		HttpClient httpClient = new HttpClient();
		HttpMethod getMethod = new GetMethod(item.getLink());
		try {
			httpClient.executeMethod(getMethod);
			Tidy tidy = new Tidy();
			tidy.setXmlOut(true);
			tidy.setQuiet(true);
			tidy.setDropProprietaryAttributes(true);
			tidy.setShowErrors(0);
			tidy.setShowWarnings(false);
			doc = tidy.parseDOM(getMethod.getResponseBodyAsStream(), null);
		} finally {
			getMethod.releaseConnection();
		}

		//Convert to PDF
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocument(doc, item.getLink());
		renderer.layout();
		renderer.createPDF(out);
		return out.toByteArray();
	}
}
