/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dmitry
 */
public class Properties {

	private static final Logger log = Logger.getLogger(Properties.class.getName());
	private File opmlFile;
	private File tempDir;
	private File storageFile;

	public Properties(File source) {
		java.util.Properties properties = new java.util.Properties();
		try {
			properties.load(new FileReader(source));
		} catch (IOException ex) {
			log.log(Level.SEVERE, null, ex);
		}
		opmlFile = new File(properties.getProperty("opml_file", "subscriptions.xml"));
		storageFile = new File(properties.getProperty("storage_file", "feeds.xml"));
		tempDir = new File(properties.getProperty("temp_dir", "temp"));

		try {
			deleteTree(Paths.get(tempDir.toURI()));
		} catch (IOException ex) {
			log.log(Level.SEVERE, null, ex);
		}

		Runnable deleteFile = new Runnable() {
			@Override
			public void run() {
				try {
					deleteTree(Paths.get(tempDir.toURI()));
				} catch (IOException ex) {
					log.log(Level.SEVERE, null, ex);
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(deleteFile));
	}

	/**
	 * Recursively deletes a path
	 *
	 * @param parent the parent path
	 * @throws IOException if deletion fails
	 */
	private void deleteTree(Path parent) throws IOException {
		Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else
					throw exc;
			}
		});
	}

	public File getOpmlFile() {
		return opmlFile;
	}

	public File getTempDir() {
		return tempDir;
	}

	public File getStorageFile() {
		return storageFile;
	}
}