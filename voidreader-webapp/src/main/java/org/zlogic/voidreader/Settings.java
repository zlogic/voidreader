/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to read application settings. Provides getters for all settings
 * used by the application in the configuration file.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
@Entity
public class Settings {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * The logger
	 */
	private static final Logger log = LoggerFactory.getLogger(Settings.class);
	/**
	 * The settings owner user
	 */
	@Id
	private String username;
	/**
	 * Email From address
	 */
	private String mailFrom;
	/**
	 * Email To address
	 */
	private String mailTo;
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
	 * The OPML data
	 */
	private String opml;
	/**
	 * The thread cool size for Executor instances
	 */
	private int threadPoolSize = Runtime.getRuntime().availableProcessors();

	/**
	 * Default constructor
	 */
	private Settings() {

	}

	/**
	 * Constructs a Settings instance.
	 *
	 * @param username the Settings owner username
	 * @param properties the settings data
	 */
	public Settings(String username, Properties properties) {
		this.username = username;
		this.opml = properties.getProperty("opml", ""); //NOI18N
		try {
			mailFrom = new InternetAddress(properties.getProperty("email.from")).toString(); //NOI18N
			mailTo = new InternetAddress(properties.getProperty("email.to")).toString(); //NOI18N
		} catch (AddressException ex) {
			throw new RuntimeException(ex);
		}

		cacheExpireDays = Integer.parseInt(properties.getProperty("cache.expire_days", "3")); //NOI18N
		maxRunSeconds = Integer.parseInt(properties.getProperty("core.max_run_seconds", "-1")); //NOI18N
		enablePdf = properties.getProperty("pdf.enable", "false").equals("on"); //NOI18N
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
	 * Returns the email From address
	 *
	 * @return the email From address
	 */
	public InternetAddress getMailFrom() {
		try {
			return new InternetAddress(mailFrom);
		} catch (AddressException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Returns the email To address
	 *
	 * @return the email To address
	 */
	public InternetAddress getMailTo() {
		try {
			return new InternetAddress(mailTo);
		} catch (AddressException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Returns the username for these Settings.
	 *
	 * @return the username for these Settings.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the OPML data.
	 *
	 * @return the OPML data
	 */
	public String getOpml() {
		return opml;
	}

	/**
	 * Returns the thread cool size for Executor instances.
	 *
	 * @return the thread cool size for Executor instances
	 */
	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	@Override
	public String toString() {
		return MessageFormat.format(messages.getString("SETTINGS_TOSTRING_FORMAT"),
				username, cacheExpireDays, maxRunSeconds, enablePdf, mailFrom, mailTo, threadPoolSize, opml);
	}

}
