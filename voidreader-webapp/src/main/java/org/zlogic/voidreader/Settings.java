/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
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
	 * The DatastoreService instance
	 */
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	/**
	 * The settings owner user
	 */
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
	 * Constructs a Settings instance from a Datastore Entity.
	 *
	 * @param entity the Datastore Entity
	 */
	private Settings(Entity entity) {
		username = entity.getKey().getName();
		mailFrom = ((Email) entity.getProperty("mailFrom")).getEmail(); //NOI18N
		mailTo = ((Email) entity.getProperty("mailTo")).getEmail(); //NOI18N
		enablePdf = ((Boolean) entity.getProperty("enablePdf")); //NOI18N
		maxRunSeconds = ((Long) entity.getProperty("maxRunSeconds")).intValue(); //NOI18N
		cacheExpireDays = ((Long) entity.getProperty("cacheExpireDays")).intValue(); //NOI18N
		opml = ((Text) entity.getProperty("opml")).getValue(); //NOI18N
	}

	/**
	 * Loads all Settings from Datastore.
	 *
	 * @return list of all Settings from Datastore
	 */
	public static List<Settings> loadAll() {
		List<Settings> results = new LinkedList<>();
		Query query = new Query(Settings.class.getSimpleName());
		PreparedQuery preparedQuery = datastore.prepare(query);
		for (Entity entity : preparedQuery.asIterable())
			results.add(new Settings(entity));
		return results;
	}

	/**
	 * Loads Settings for user from Datastore.
	 *
	 * @param username the username for lookup
	 * @return the Settings instance for user
	 * @throws EntityNotFoundException if not Settings instance for user is
	 * found
	 */
	public static Settings load(String username) throws EntityNotFoundException {
		Key userKey = new Entity(Settings.class.getSimpleName(), username).getKey();
		Entity entity = datastore.get(userKey);
		return new Settings(entity);
	}

	/**
	 * Saves the Settings into Datastore.
	 */
	public void save() {
		Entity settings = new Entity(getKey());
		settings.setProperty("mailFrom", new Email(mailFrom)); //NOI18N
		settings.setProperty("mailTo", new Email(mailTo)); //NOI18N
		settings.setProperty("enablePdf", enablePdf); //NOI18N
		settings.setProperty("maxRunSeconds", maxRunSeconds); //NOI18N
		settings.setProperty("cacheExpireDays", cacheExpireDays); //NOI18N
		settings.setProperty("opml", new Text(opml)); //NOI18N
		datastore.put(settings);
	}

	/**
	 * Returns the key for this Settings instance.
	 *
	 * @return the key for this Settings instance
	 */
	public Key getKey() {
		return new Entity(Settings.class.getSimpleName(), username).getKey();
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
