/**
 * Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)
 * @author git-hemant@github.com
 */
package github.hemant.lc;

import github.hemant.lc.config.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Support logging in the application.
 */
public class Log {

	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int ERROR = 2;
	
	private int logLevel;
	//private final static boolean DEBUG = false;
	private PrintWriter logWriter;
	private static Log instance;
	private Log() {
	}
	
	public void close() {
		if (logWriter != null) {
			logWriter.close();
			instance = null;
		}
	}
	
	public static Log instance(Config config) {
		if (instance == null) {
			instance = new Log();
			instance.initialize(config);
		}
		return instance;
	}
	
	private void initialize(Config config) {
		if (config.logFile() != null) {
			logLevel = config.logLevel();
			File file = new File(config.logFile());
			try {
				info("Log is being redirected to: " + file.getAbsolutePath());
				logWriter = new PrintWriter(new FileWriter(file, true));
			} catch(IOException e) {
				info("Unable to use log file at location: " + file.getAbsolutePath() + ". Error: " + e.getMessage());
			}
		}
	}
	
	public void error(Throwable t) {
		if (logWriter != null) {
			showMessage("Exception occurred : " + t.toString());
			t.printStackTrace(logWriter);
			logWriter.flush();
		} else {	
			t.printStackTrace(System.err);
		}
	}
	
	public void debug(String msg) {
		if (DEBUG == logLevel) showMessage(msg);
	}
	
	public void error(String msg) {
		showMessage(msg);
	}

	public void info(String msg) {
		if (logLevel == DEBUG || logLevel == INFO) showMessage(msg);
	}
	
	public boolean isDebug() {
		return logLevel == DEBUG;
	}
	
	public void showMessage(String msg) {
		msg = new Date().toString() + " - " + msg;
		if (logWriter != null) {
			logWriter.print(msg);
			logWriter.print(System.getProperty("line.separator"));
			logWriter.flush();
		} else {
			System.out.println(msg);
		}
	}	
}
