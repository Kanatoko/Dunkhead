/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package net.jumperz.ext.org.jrobin.core;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.io.*;

/**
 * Class defines various utility functions used in JRobin.
 *
 * @author <a href="mailto:saxon@jrobin.org">Sasa Markovic</a>
 */
public class Util {

	// pattern RRDTool uses to format doubles in XML files
	static final String PATTERN = "0.0000000000E00";
	// directory under $USER_HOME used for demo graphs storing
	static final String JROBIN_DIR = "jrobin-demo";

	static final DecimalFormat df;
	static {
		df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
		df.applyPattern(PATTERN);
		df.setPositivePrefix("+");
	}

	/**
	 * Returns current timestamp in seconds (without milliseconds). Returned timestamp
	 * is obtained with the following expression: <p>
	 *
	 * <code>(System.currentTimeMillis() + 500L) / 1000L</code>
	 * @return Current timestamp
	 */
	public static long getTime() {
		return (System.currentTimeMillis() + 500L) / 1000L;
	}

	/**
	 * Just an alias for {@link #getTime()} method.
	 * @return Current timestamp (without milliseconds)
	 */
	public static long getTimestamp() {
		return getTime();
	}

	/**
	 * Rounds the given timestamp to the nearest whole &quote;step&quote;. Rounded value is obtained
	 * from the following expression:<p>
	 * <code>timestamp - timestamp % step;</code>
	 * @param timestamp Timestamp in seconds
	 * @param step Step in seconds
	 * @return "Rounded" timestamp
	 */
	public static long normalize(long timestamp, long step) {
		return timestamp - timestamp % step;
	}

	/**
	 * Returns the greater of two double values, but treats NaN as the smallest possible
	 * value. Note that <code>Math.max()</code> behaves differently for NaN arguments.
	 *
	 * @param x an argument
	 * @param y another argument
	 * @return the lager of arguments
	 */
	public static double max(double x, double y) {
		return Double.isNaN(x)? y: Double.isNaN(y)? x: Math.max(x, y);
	}

	/**
	 * Returns the smaller of two double values, but treats NaN as the greatest possible
	 * value. Note that <code>Math.min()</code> behaves differently for NaN arguments.
	 *
	 * @param x an argument
	 * @param y another argument
	 * @return the smaller of arguments
	 */
	public static double min(double x, double y) {
		return Double.isNaN(x)? y: Double.isNaN(y)? x: Math.min(x, y);
	}

	static double sum(double x, double y) {
		return Double.isNaN(x)? y: Double.isNaN(y)? x: x + y;
	}

	static String formatDouble(double x, String nanString, boolean forceExponents) {
		if(Double.isNaN(x)) {
			return nanString;
		}
		if(forceExponents) {
			return df.format(x);
		}
		return "" + x;
	}

	static String formatDouble(double x, boolean forceExponents) {
		return formatDouble(x, "" + Double.NaN, forceExponents);
	}

	/**
	 * Returns <code>Date</code> object for the given timestamp (in seconds, without
	 * milliseconds)
	 * @param timestamp Timestamp in seconds.
	 * @return Corresponding Date object.
	 */
	public static Date getDate(long timestamp) {
		return new Date(timestamp * 1000L);
	}

    /**
	 * Returns <code>GregorianCalendar</code> object for the given timestamp
	 * (in seconds, without milliseconds)
	 * @param timestamp Timestamp in seconds.
	 * @return Corresponding GregorianCalendar object.
	 */
	public static GregorianCalendar getGregorianCalendar(long timestamp) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(timestamp * 1000L);
		return gc;
	}

    /**
	 * Returns <code>GregorianCalendar</code> object for the given Date object
	 * @param date Date object
	 * @return Corresponding GregorianCalendar object.
	 */
	public static GregorianCalendar getGregorianCalendar(Date date) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		return gc;
	}

	/**
	 * Returns timestamp (unix epoch) for the given Date object
	 * @param date Date object
	 * @return Corresponding timestamp (without milliseconds)
	 */
	public static long getTimestamp(Date date) {
		// round to whole seconds, ignore milliseconds
		return (date.getTime() + 499L) / 1000L;
	}

	/**
	 * Returns timestamp (unix epoch) for the given GregorianCalendar object
	 * @param gc GregorianCalendar object
	 * @return Corresponding timestamp (without milliseconds)
	 */
	public static long getTimestamp(GregorianCalendar gc) {
		return getTimestamp(gc.getTime());
	}

	/**
	 * Returns timestamp (unix epoch) for the given year, month, day, hour and minute.
	 * @param year Year
	 * @param month Month (zero-based)
	 * @param day Day in month
	 * @param hour Hour
	 * @param min Minute
	 * @return Corresponding timestamp
	 */
	public static long getTimestamp(int year, int month, int day, int hour, int min) {
		GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, min);
		return Util.getTimestamp(gc);
	}

	/**
	 * Returns timestamp (unix epoch) for the given year, month and day.
	 * @param year Year
	 * @param month Month (zero-based)
	 * @param day Day in month
	 * @return Corresponding timestamp
	 */
	public static long getTimestamp(int year, int month, int day) {
		return Util.getTimestamp(year, month, day, 0, 0);
	}

	/**
	 * Parses input string as a double value. If the value cannot be parsed, Double.NaN
	 * is returned (NumberFormatException is never thrown).
	 * @param valueStr String representing double value
	 * @return a double corresponding to the input string
	 */
	public static double parseDouble(String valueStr) {
		double value;
		try {
			value = Double.parseDouble(valueStr);
		}
		catch(NumberFormatException nfe) {
			value = Double.NaN;
		}
		return value;
	}

	/**
	 * Parses input string as a boolean value. The parser is case insensitive.
	 * @param valueStr String representing boolean value
	 * @return <code>true</code>, if valueStr equals to 'true', 'on', 'yes', 'y' or '1';
	 * <code>false</code> in all other cases.
	 */
	public static boolean parseBoolean(String valueStr) {
		return valueStr.equalsIgnoreCase("true") ||
			valueStr.equalsIgnoreCase("on") ||
			valueStr.equalsIgnoreCase("yes") ||
			valueStr.equalsIgnoreCase("y") ||
			valueStr.equalsIgnoreCase("1");
	}

	/**
	 * Returns file system separator string.
	 * @return File system separator ("/" on Unix, "\" on Windows)
	 */
	public static String getFileSeparator() {
		return System.getProperty("file.separator");
	}

	/**
	 * Returns path to user's home directory.
	 * @return Path to users home directory, with file separator appended.
	 */
	public static String getUserHomeDirectory() {
		return System.getProperty("user.home") + getFileSeparator();
	}

	private static final File homeDirFile;
	private static final String homeDirPath;

	static {
		homeDirPath = getUserHomeDirectory() + JROBIN_DIR + getFileSeparator();
		homeDirFile = new File(homeDirPath);
	}

	/**
	 * Returns path to directory used for placement of JRobin demo graphs and creates it
	 * if necessary.
	 * @return Path to demo directory (defaults to $HOME/jrobin/) if directory exists or
	 * was successfully created. Null if such directory could not be created.
	 */
	public static String getJRobinDemoDirectory() {
		return (homeDirFile.exists() || homeDirFile.mkdirs())? homeDirPath: null;
	}

	/**
	 * Returns full path to the file stored in the demo directory of JRobin
	 * @param filename Partial path to the file stored in the demo directory of JRobin
	 * (just name and extension, without parent directories)
	 * @return Full path to the file
	 */
	public static String getJRobinDemoPath(String filename) {
		String demoDir = getJRobinDemoDirectory();
		if(demoDir != null) {
			return demoDir + filename;
		}
		else {
			return null;
		}
	}

	static boolean sameFilePath(String path1, String path2) throws IOException {
		File file1 = new File(path1);
		File file2 = new File(path2);
		return file1.getCanonicalPath().equals(file2.getCanonicalPath());
	}

	static int getMatchingDatasourceIndex(RrdDb rrd1, int dsIndex, RrdDb rrd2) throws IOException {
		String dsName = rrd1.getDatasource(dsIndex).getDsName();
		try {
			return rrd2.getDsIndex(dsName);
		} catch (RrdException e) {
			return -1;
		}
	}

	static int getMatchingArchiveIndex(RrdDb rrd1, int arcIndex, RrdDb rrd2)
		throws IOException {
		Archive archive = rrd1.getArchive(arcIndex);
		String consolFun = archive.getConsolFun();
		int steps = archive.getSteps();
		try {
			return rrd2.getArcIndex(consolFun, steps);
		} catch (RrdException e) {
			return -1;
		}
	}

	static String getTmpFilename() throws IOException {
		return File.createTempFile("JROBIN_", ".tmp").getCanonicalPath();
	}

	static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";   // ISO

	/**
	 * Creates GregorianCalendar object from a string. The string should represent
	 * either a long integer (UNIX timestamp in seconds without milliseconds,
	 * like "1002354657") or a human readable date string in the format "yyyy-MM-dd HH:mm:ss"
	 * (like "2004-02-25 12:23:45").
	 * @param timeStr Input string
	 * @return GregorianCalendar object
	 */
	public static GregorianCalendar getGregorianCalendar(String timeStr) {
		// try to parse it as long
		try {
			long timestamp = Long.parseLong(timeStr);
			return Util.getGregorianCalendar(timestamp);
		} catch (NumberFormatException e) { }
		// not a long timestamp, try to parse it as data
		SimpleDateFormat df = new SimpleDateFormat(ISO_DATE_FORMAT);
		df.setLenient(false);
		try {
			Date date = df.parse(timeStr);
            return Util.getGregorianCalendar(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Time/date not in " + ISO_DATE_FORMAT +
				" format: " + timeStr);
		}
	}

	/**
	 * Various DOM utility functions
	 */
	public static class Xml {
		public static Node[] getChildNodes(Node parentNode) {
			return getChildNodes(parentNode, null);
		}

		public static Node[] getChildNodes(Node parentNode, String childName) {
			ArrayList nodes = new ArrayList();
			NodeList nodeList = parentNode.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (childName == null || node.getNodeName().equals(childName)) {
					nodes.add(node);
				}
			}
			return (Node[]) nodes.toArray(new Node[0]);
		}

		public static Node getFirstChildNode(Node parentNode, String childName) throws RrdException {
			Node[] childs = getChildNodes(parentNode, childName);
			if (childs.length > 0) {
				return childs[0];
			}
			throw new RrdException("XML Error, no such child: " + childName);
		}

		public static boolean hasChildNode(Node parentNode, String childName) {
			Node[] childs = getChildNodes(parentNode, childName);
			return childs.length > 0;
		}

		// -- Wrapper around getChildValue with trim
		public static String getChildValue( Node parentNode, String childName ) throws RrdException {
			return getChildValue( parentNode, childName, true );
		}

		public static String getChildValue( Node parentNode, String childName, boolean trim ) throws RrdException {
			NodeList children = parentNode.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeName().equals(childName)) {
					return getValue(child, trim);
				}
			}
			throw new RrdException("XML Error, no such child: " + childName);
		}

		// -- Wrapper around getValue with trim
		public static String getValue(Node node) {
			return getValue( node, true );
		}

		public static String getValue(Node node, boolean trimValue ) {
			String value = null;
			Node child = node.getFirstChild();
			if(child != null) {
				value = child.getNodeValue();
				if( value != null && trimValue ) {
					value = value.trim();
				}
			}
			return value;
		}

		public static int getChildValueAsInt(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Integer.parseInt(valueStr);
		}

		public static int getValueAsInt(Node node) {
			String valueStr = getValue(node);
			return Integer.parseInt(valueStr);
		}

		public static long getChildValueAsLong(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Long.parseLong(valueStr);
		}

		public static long getValueAsLong(Node node) {
			String valueStr = getValue(node);
			return Long.parseLong(valueStr);
		}

		public static double getChildValueAsDouble(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Util.parseDouble(valueStr);
		}

		public static double getValueAsDouble(Node node) {
			String valueStr = getValue(node);
			return Util.parseDouble(valueStr);
		}

		public static boolean getChildValueAsBoolean(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Util.parseBoolean(valueStr);
		}

		public static boolean getValueAsBoolean(Node node) {
			String valueStr = getValue(node);
			return Util.parseBoolean(valueStr);
		}

		public static Element getRootElement(InputSource inputSource) throws RrdException, IOException	{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(inputSource);
				return doc.getDocumentElement();
			} catch (ParserConfigurationException e) {
				throw new RrdException(e);
			} catch (SAXException e) {
				throw new RrdException(e);
			}
		}

		public static Element getRootElement(String xmlString)	throws RrdException, IOException {
			return getRootElement(new InputSource(new StringReader(xmlString)));
		}

		public static Element getRootElement(File xmlFile)	throws RrdException, IOException {
			Reader reader = null;
			try {
				reader = new FileReader(xmlFile);
				return getRootElement(new InputSource(reader));
			}
			finally {
				if(reader != null) {
					reader.close();
				}
			}
		}
	}

	private static Date lastLap = new Date();

	/**
	 * Function used for debugging purposes and performance bottlenecks detection.
	 * Probably of no use for end users of JRobin.
	 * @return String representing time in seconds since last
	 * <code>getLapTime()</code> method call.
	 */
	public static String getLapTime() {
		Date newLap = new Date();
		double seconds = (newLap.getTime() - lastLap.getTime()) / 1000.0;
		lastLap = newLap;
		return "[" + seconds + " sec]";
	}

	/**
	 * Returns the root directory of the JRobin distribution. Useful in some demo applications,
	 * probably of no use anywhere else.<p>
	 *
	 * The function assumes that all JRobin .class files are placed under
	 * the &lt;root&gt;/classes subdirectory and that all jars (libraries) are placed in the
	 * &lt;root&gt;/lib subdirectory (the original JRobin directory structure).<p>
	 *
	 * @return absolute path to JRobin's home directory
	 */
	public static String getJRobinHomeDirectory() {
		String className = Util.class.getName().replace('.', '/');
		String uri = Util.class.getResource("/" + className + ".class").toString();
		if(uri.startsWith("file:/")) {
			uri = uri.substring(6);
			File file = new File(uri);
			// let's go 5 steps backwards
			for(int i = 0; i < 5; i++) {
				file = file.getParentFile();
			}
			uri = file.getAbsolutePath();
		}
		else if(uri.startsWith("jar:file:/")) {
			uri = uri.substring(10, uri.lastIndexOf('!'));
			File file = new File(uri);
			// let's go 2 steps backwards
			for(int i = 0; i < 2; i++) {
				file = file.getParentFile();
			}
			uri = file.getAbsolutePath();
		}
		else {
			uri = null;
		}
		return uri;
	}

	/**
	 * Compares two doubles, but returns true if x = y = Double.NaN
	 * @param x First double
	 * @param y Second double
	 * @return true, if doubles are equal, false otherwise.
	 */
	public static boolean equal(double x, double y) {
		if(Double.isNaN(x) && Double.isNaN(y)) {
			return true;
		}
		else {
			return x == y;
		}
	}

}