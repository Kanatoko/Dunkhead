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

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;

/**
 * Class used to represent data fetched from the RRD.
 * Object of this class is created when the method
 * {@link net.jumperz.ext.org.jrobin.core.FetchRequest#fetchData() fetchData()} is
 * called on a {@link net.jumperz.ext.org.jrobin.core.FetchRequest FetchRequest} object.<p>
 *
 * Data returned from the RRD is, simply, just one big table filled with
 * timestamps and corresponding datasource values.
 * Use {@link #getRowCount() getRowCount()} method to count the number
 * of returned timestamps (table rows).<p>
 *
 * The first table column is filled with timestamps. Time intervals
 * between consecutive timestamps are guaranteed to be equal. Use
 * {@link #getTimestamps() getTimestamps()} method to get an array of
 * timestamps returned.<p>
 *
 * Remaining columns are filled with datasource values for the whole timestamp range,
 * on a column-per-datasource basis. Use {@link #getColumnCount() getColumnCount()} to find
 * the number of datasources and {@link #getValues(int) getValues(i)} method to obtain
 * all values for the i-th datasource. Returned datasource values correspond to
 * the values returned with {@link #getTimestamps() getTimestamps()} method.<p>
 */
public class FetchData implements RrdDataSet {
	private FetchRequest request;
	private Archive matchingArchive;
	private String[] dsNames;
	private long[] timestamps;
	private double[][] values;

	FetchData(Archive matchingArchive, FetchRequest request) throws IOException {
		this.matchingArchive = matchingArchive;
		this.dsNames = request.getFilter();
		if(this.dsNames == null) {
			this.dsNames = matchingArchive.getParentDb().getDsNames();
		}
		this.request = request;
	}

	void setTimestamps(long[] timestamps) {
		this.timestamps = timestamps;
	}

	void setValues(double[][] values) {
		this.values = values;
	}

	/**
	 * Returns the number of rows fetched from the corresponding RRD.
	 * Each row represents datasource values for the specific timestamp.
	 * @return Number of rows.
	 */
    public int getRowCount() {
		return timestamps.length;
	}

    /**
	 * Returns the number of columns fetched from the corresponding RRD.
	 * This number is always equal to the number of datasources defined
	 * in the RRD. Each column represents values of a single datasource.
	 * @return Number of columns (datasources).
	 */
	public int getColumnCount() {
		return dsNames.length;
	}

	/**
	 * Returns the number of rows fetched from the corresponding RRD.
	 * Each row represents datasource values for the specific timestamp.
	 * @param rowIndex Row index.
	 * @return FetchPoint object which represents datasource values for the
	 * specific timestamp.
	 */
	public FetchPoint getRow(int rowIndex) {
		int numCols = getColumnCount();
		FetchPoint point = new FetchPoint(timestamps[rowIndex], getColumnCount());
		for(int dsIndex = 0; dsIndex < numCols; dsIndex++) {
			point.setValue(dsIndex, values[dsIndex][rowIndex]);
		}
		return point;
	}

	/**
	 * Returns an array of timestamps covering the whole range specified in the
	 * {@link FetchRequest FetchReguest} object.
	 * @return Array of equidistant timestamps.
	 */
	public long[] getTimestamps() {
		return timestamps;
	}

	/**
	 * Returns the step with which this data was fetched.
	 * @return Step as long.
	 */
	public long getStep() {
		return timestamps[1] - timestamps[0];
	}

	/**
	 * Returns all archived values for a single datasource.
	 * Returned values correspond to timestamps
	 * returned with {@link #getTimestamps() getTimestamps()} method.
	 * @param dsIndex Datasource index.
	 * @return Array of single datasource values.
	 */
	public double[] getValues(int dsIndex) {
		return values[dsIndex];
	}

	/**
	 * Returns all archived values for all datasources.
	 * Returned values correspond to timestamps
	 * returned with {@link #getTimestamps() getTimestamps()} method.
	 * @return Two-dimensional aray of all datasource values.
	 */
	public double[][] getValues() {
		return values;
	}
	
	/**
	 * Returns all archived values for a single datasource.
	 * Returned values correspond to timestamps
	 * returned with {@link #getTimestamps() getTimestamps()} method.
	 * @param dsName Datasource name.
	 * @return Array of single datasource values.
	 * @throws RrdException Thrown if no matching datasource name is found.
	 */
	public double[] getValues(String dsName) throws RrdException {
		for(int dsIndex = 0; dsIndex < getColumnCount(); dsIndex++) {
			if(dsName.equals(dsNames[dsIndex])) {
				return getValues(dsIndex);
			}
		}
		throw new RrdException("Datasource [" + dsName + "] not found");
	}

	/**
	 * Returns {@link FetchRequest FetchRequest} object used to create this FetchData object.
	 * @return Fetch request object.
	 */
	public FetchRequest getRequest() {
		return request;
	}

    /**
	 * Returns the first timestamp in this FetchData object.
	 * @return The smallest timestamp.
	 */
	public long getFirstTimestamp() {
		return timestamps[0];
	}

	/**
	 * Returns the last timestamp in this FecthData object.
	 * @return The biggest timestamp.
	 */
	public long getLastTimestamp() {
		return timestamps[timestamps.length - 1];
	}

	/**
	 * Returns Archive object which is determined to be the best match for the
	 * timestamps specified in the fetch request. All datasource values are obtained
	 * from round robin archives belonging to this archive.
	 * @return Matching archive.
	 */
	public Archive getMatchingArchive() {
		return matchingArchive;
	}

	/**
	 * Returns array of datasource names found in the corresponding RRD. If the request
	 * was filtered (data was fetched only for selected datasources), only datasources selected
	 * for fetching are returned.
	 * @return Array of datasource names.
	 */
	public String[] getDsNames() {
		return dsNames;
	}
	
	/**
	 * Retrieve the table index number of a datasource by name.  Names are case sensitive.
	 * @param dsName Name of the datasource for which to find the index.
	 * @return Index number of the datasources in the value table.
	 */
	public int getDsIndex(String dsName) {
		// Let's assume the table of dsNames is always small, so it is not necessary to use a hashmap for lookups
		for (int i = 0; i < dsNames.length; i++)
			if ( dsNames[i].equals(dsName) )
				return i;
		
		return -1;		// Datasource not found !
	}

	/**
	 * Dumps the content of the whole FetchData object to stdout. Useful for debugging.
	 */
	public void dump() {
		for(int i = 0; i < getRowCount(); i++) {
			System.out.println(getRow(i).dump());
		}
	}

	/**
	 * Returns string representing fetched data in a RRDTool-like form.
	 * @return Fetched data as a string in a rrdfetch-like output form.
	 */
	public String toString() {
		final DecimalFormat df = new DecimalFormat("+0.0000000000E00");
		// print header row
		StringBuffer buff = new StringBuffer();
		buff.append(padWithBlanks("", 10));
		buff.append(" ");
		for(int i = 0; i < dsNames.length; i++) {
			buff.append(padWithBlanks(dsNames[i], 18));
		}
		buff.append("\n \n");
		for(int i = 0; i < timestamps.length; i++) {
			buff.append(padWithBlanks("" + timestamps[i], 10));
			buff.append(":");
			for(int j = 0; j < dsNames.length; j++) {
				double value = values[j][i];
				String valueStr = Double.isNaN(value)? "nan": df.format(value);
				buff.append(padWithBlanks(valueStr, 18));
			}
			buff.append("\n");
		}
		return buff.toString();
	}

	private static String padWithBlanks(String input, int width) {
		StringBuffer buff = new StringBuffer("");
		int diff = width - input.length();
		while(diff-- > 0) {
			buff.append(' ');
		}
		buff.append(input);
		return buff.toString();
	}

	/**
	 * Returns aggregated value from the fetched data for a single datasource.
	 * @param dsName Datasource name
	 * @param consolFun Consolidation function to be applied to fetched datasource values.
	 * Valid consolidation functions are MIN, MAX, LAST and AVERAGE
	 * @return MIN, MAX, LAST or AVERAGE value calculated from the fetched data
	 * for the given datasource name
	 * @throws RrdException Thrown if the given datasource name cannot be found in fetched data.
	 */
	public double getAggregate(String dsName, String consolFun) throws RrdException {
		return getAggregate(dsName, consolFun, null);
	}

	/**
	 * Returns aggregated value from the fetched data for a single datasource.
	 * Before applying aggrregation functions, specified RPN expression is applied to fetched
	 * data. For example, if you have a gauge datasource named 'foots' but you wont to
	 * find the maximum fetched value in meters use something like:</p>
	 * <code>getAggregate("foots", "MAX", "value,0.3048,*");</code>
	 * Note that 'value' in the RPN expression is a reserved word and stands for the
	 * original value (value fetched from RRD)</p>
	 * @param dsName Datasource name
	 * @param consolFun Consolidation function to be applied to fetched datasource values.
	 * Valid consolidation functions are MIN, MAX, LAST and AVERAGE
	 * @return MIN, MAX, LAST or AVERAGE value calculated from the fetched data
	 * for the given datasource name
	 * @throws RrdException Thrown if the given datasource name cannot be found in fetched data.
	 */
	public double getAggregate(String dsName, String consolFun, String rpnExpression)
		throws RrdException {
		if(consolFun.equals("MAX")) {
			return getMax(dsName, rpnExpression);
		}
		else if(consolFun.equals("MIN")) {
			return getMin(dsName, rpnExpression);
		}
		else if(consolFun.equals("LAST")) {
			return getLast(dsName, rpnExpression);
		}
		else if(consolFun.equals("AVERAGE")) {
			return getAverage(dsName, rpnExpression);
		}
		else {
			throw new RrdException("Unsupported consolidation function [" + consolFun + "]");
		}
	}

	private double getMax(String dsName, String rpnExpression) throws RrdException {
		RpnCalculator rpnCalculator = null;
		if(rpnExpression != null) {
			rpnCalculator = new RpnCalculator(rpnExpression);
		}
		double vals[] = getValues(dsName), max = Double.NaN;
		for(int i = 0; i < vals.length - 1; i++) {
			double value = vals[i + 1];
			if(rpnCalculator != null) {
				rpnCalculator.setValue(value);
				value = rpnCalculator.calculate();
			}
			max = Util.max(max, value);
		}
		return max;
	}

	private double getMin(String dsName, String rpnExpression) throws RrdException {
		RpnCalculator rpnCalculator = null;
		if(rpnExpression != null) {
			rpnCalculator = new RpnCalculator(rpnExpression);
		}
		double vals[] = getValues(dsName), min = Double.NaN;
		for(int i = 0; i < vals.length - 1; i++) {
			double value = vals[i + 1];
			if(rpnCalculator != null) {
				rpnCalculator.setValue(value);
				value = rpnCalculator.calculate();
			}
			min = Util.min(min, value);
		}
		return min;
	}

	private double getLast(String dsName, String rpnExpression) throws RrdException {
		RpnCalculator rpnCalculator = null;
		if(rpnExpression != null) {
			rpnCalculator = new RpnCalculator(rpnExpression);
		}
		double vals[] = getValues(dsName);
		double value = vals[vals.length - 1];
		if(rpnCalculator != null) {
			rpnCalculator.setValue(value);
			value = rpnCalculator.calculate();
		}
		return value;
	}

	private double getAverage(String dsName, String rpnExpression) throws RrdException {
		RpnCalculator rpnCalculator = null;
		if(rpnExpression != null) {
			rpnCalculator = new RpnCalculator(rpnExpression);
		}
		double vals[] = getValues(dsName);
		double totalVal = 0;
		long totalSecs = 0;
		for(int i = 0; i < vals.length - 1; i++) {
			long t1 = Math.max(request.getFetchStart(), timestamps[i]);
			long t2 = Math.min(request.getFetchEnd(), timestamps[i + 1]);
			double value = vals[i + 1];
			if(rpnCalculator != null) {
				rpnCalculator.setValue(value);
				value = rpnCalculator.calculate();
			}
			if(!Double.isNaN(value)) {
                totalSecs += (t2 - t1);
				totalVal += (t2 - t1) * value;
			}
		}
		return totalSecs > 0? totalVal / totalSecs: Double.NaN;
	}

	/**
	 * Dumps fetch data to output stream in XML format.
	 * @param outputStream Output stream to dump fetch data to
	 * @throws IOException Thrown in case of I/O error
	 */
	public void exportXml(OutputStream outputStream) throws IOException {
		XmlWriter writer = new XmlWriter(outputStream);
		writer.startTag("fetch_data");
		writer.startTag("request");
		writer.writeTag("file", request.getParentDb().getPath());
		writer.writeComment(Util.getDate(request.getFetchStart()));
		writer.writeTag("start", request.getFetchStart());
		writer.writeComment(Util.getDate(request.getFetchEnd()));
		writer.writeTag("end", request.getFetchEnd());
		writer.writeTag("resolution", request.getResolution());
		writer.writeTag("cf", request.getConsolFun());
		writer.closeTag(); // request
		writer.startTag("datasources");
		for(int i = 0; i < dsNames.length; i++) {
			writer.writeTag("name", dsNames[i]);
		}
		writer.closeTag(); // datasources
		writer.startTag("data");
		for(int i = 0; i < timestamps.length; i++) {
			writer.startTag("row");
			writer.writeComment(Util.getDate(timestamps[i]));
            writer.writeTag("timestamp", timestamps[i]);
			writer.startTag("values");
			for(int j = 0; j < dsNames.length; j++) {
				writer.writeTag("v", values[j][i]);
			}
			writer.closeTag(); // values
			writer.closeTag(); // row
		}
		writer.closeTag(); // data
		writer.closeTag(); // fetch_data
		writer.flush();
	}

	/**
	 * Dumps fetch data to file in XML format.
	 * @param filepath Path to destination file
	 * @throws IOException Thrown in case of I/O error
	 */
	public void exportXml(String filepath) throws IOException {
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(filepath);
			exportXml(outputStream);
		}
		finally {
			if(outputStream != null) {
				outputStream.close();
			}
		}
	}

	/**
	 * Dumps fetch data in XML format.
	 * @return String containing XML formatted fetch data
	 * @throws IOException Thrown in case of I/O error
	 */
	public String exportXml() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		exportXml(outputStream);
		return outputStream.toString();
	}
}
