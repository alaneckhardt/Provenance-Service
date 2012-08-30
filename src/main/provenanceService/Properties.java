package provenanceService;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Class providing the access to the configuration files.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 *
 */
public class Properties {

	private static String file = "provenanceService.properties";
	private static String baseFolder = "./";
	private static Configuration values = null;

	public static synchronized void init() {
		try {
			values = new PropertiesConfiguration(baseFolder + file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized Configuration getValues() {

		if (values == null) {
			init();
		}
		return values;
	}

	public static String getString(String para) {
		if (values == null) {
			init();
		}
		return values.getString(para);
	}

	public static String getFile() {
		return file;
	}

	public static void setFile(String file) {
		Properties.file = file;
	}

	public static String getBaseFolder() {
		return baseFolder;
	}

	public static void setBaseFolder(String baseFolder) {
		Properties.baseFolder = baseFolder;
	}

}