/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Class used to read application settings. Provides getters for all settings
 * used by the application in the configuration file.
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic@gmail.com">zlogic@gmail.com</a>
 */
public class Settings {

	/**
	 * The logger
	 */
	private static final Logger log = Logger.getLogger(Settings.class.getName());

	/**
	 * Handler type for new or updated items
	 */
	public enum Handler {

		/**
		 * Send items via SMTP
		 */
		SMTP,
		/**
		 * Upload items via IMAP
		 */
		IMAP,
		/**
		 * Save items in a folder (WARNING: this is for testing purposes only,
		 * see org.zlogic.voidreader.handler.impl.FileHandler javadoc for more
		 * info
		 */
		FILE
	};
	/**
	 * The OPML file
	 */
	private File opmlFile;
	/**
	 * The temporary directory for the Handler.FILE handler
	 */
	private File tempDir;
	/**
	 * The XML file where feed state will be stored
	 */
	private File storageFile;
	/**
	 * The selected handler
	 */
	private Handler handler;
	/**
	 * javax.mail properties
	 */
	private Properties mailProperties = new Properties();
	/**
	 * Email From address
	 */
	private InternetAddress mailFrom;
	/**
	 * Email To address
	 */
	private InternetAddress mailTo;
	/**
	 * Email authentication username
	 */
	private String mailUser;
	/**
	 * Email authentication password
	 */
	private String mailPassword;
	/**
	 * IMAP store name
	 */
	private String imapStore;
	/**
	 * IMAP target upload folder name
	 */
	private String imapFolder;
	/**
	 * Days to keep items after they're removed from the feed
	 */
	private int cacheExpireDays;
	/**
	 * Maximum time application can run before being forcefully terminated
	 */
	private int maxRunSeconds;
	/**
	 * Enable downloading and sending of PDF copies of original articles
	 */
	private boolean enablePdf;

	/**
	 * Constructs settings by loading a settings file
	 *
	 * @param source the settings file
	 * @throws AddressException if an email or internet address cannot be parsed
	 */
	public Settings(File source) throws AddressException {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(source));
		} catch (IOException ex) {
			log.log(Level.SEVERE, null, ex);
		}
		opmlFile = new File(properties.getProperty("input.opml", "subscriptions.xml")); //NOI18N
		storageFile = new File(properties.getProperty("output.storage", "feeds.xml")); //NOI18N
		tempDir = new File(properties.getProperty("output.tempdir", "temp")); //NOI18N
		handler = Handler.valueOf(properties.getProperty("output.handler").toString().toUpperCase()); //NOI18N

		for (String prop : properties.stringPropertyNames())
			if (prop.startsWith("mail.")) //NOI18N
				mailProperties.setProperty(prop, properties.getProperty(prop));

		mailFrom = new InternetAddress(properties.getProperty("email.from")); //NOI18N
		mailTo = new InternetAddress(properties.getProperty("email.to")); //NOI18N
		mailUser = properties.getProperty("email.user"); //NOI18N
		mailPassword = properties.getProperty("email.password"); //NOI18N
		imapStore = properties.getProperty("email.imap.store"); //NOI18N
		imapFolder = properties.getProperty("email.imap.folder"); //NOI18N

		cacheExpireDays = Integer.parseInt(properties.getProperty("cache.expire_days", "3")); //NOI18N
		maxRunSeconds = Integer.parseInt(properties.getProperty("core.max_run_seconds", "-1")); //NOI18N
		enablePdf = Boolean.parseBoolean(properties.getProperty("pdf.enable", "true")); //NOI18N
	}

	/**
	 * Returns the OPML file
	 *
	 * @return the OPML file
	 */
	public File getOpmlFile() {
		return opmlFile;
	}

	/**
	 * Returns the temporary directory for the Handler.FILE handler
	 *
	 * @return the temporary directory for the Handler.FILE handler
	 */
	public File getTempDir() {
		return tempDir;
	}

	/**
	 * Returns the XML file where feed state will be stored
	 *
	 * @return the XML file where feed state will be stored
	 */
	public File getStorageFile() {
		return storageFile;
	}

	/**
	 * Returns the days to keep items after they're removed from the feed
	 *
	 * @return the days to keep items after they're removed from the feed
	 */
	public int getCacheExpireDays() {
		return cacheExpireDays;
	}

	/**
	 * Returns the maximum time application can run before being forcefully
	 * terminated
	 *
	 * @return the maximum time application can run before being forcefully
	 * terminated
	 */
	public int getMaxRunSeconds() {
		return maxRunSeconds;
	}

	/**
	 * Returns true id downloading and sending of PDF copies of original
	 * articles should be enabled
	 *
	 * @return true id downloading and sending of PDF copies of original
	 * articles should be enabled
	 */
	public boolean isEnablePdf() {
		return enablePdf;
	}

	/**
	 * Returns the javax.mail properties
	 *
	 * @return the javax.mail properties
	 */
	public Properties getMailProperties() {
		return mailProperties;
	}

	/**
	 * Returns the email From address
	 *
	 * @return the email From address
	 */
	public InternetAddress getMailFrom() {
		return mailFrom;
	}

	/**
	 * Returns the email To address
	 *
	 * @return the email To address
	 */
	public InternetAddress getMailTo() {
		return mailTo;
	}

	/**
	 * Returns the selected handler
	 *
	 * @return the selected handler
	 */
	public Handler getHandler() {
		return handler;
	}

	/**
	 * Returns the email authentication username
	 *
	 * @return the email authentication username
	 */
	public String getMailUser() {
		return mailUser;
	}

	/**
	 * Returns the email authentication password
	 *
	 * @return the email authentication password
	 */
	public String getMailPassword() {
		return mailPassword;
	}

	/**
	 * Returns the IMAP store name
	 *
	 * @return the IMAP store name
	 */
	public String getImapStore() {
		return imapStore;
	}

	/**
	 * Returns the IMAP target upload folder name
	 *
	 * @return the IMAP target upload folder name
	 */
	public String getImapFolder() {
		return imapFolder;
	}
}
