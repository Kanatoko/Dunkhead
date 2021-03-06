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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Main class used to create and manipulate round robin databases (RRDs). Use this class to perform
 * update and fetch operations on exisiting RRDs, to create new RRD from
 * the definition (object of class {@link net.jumperz.ext.org.jrobin.core.RrdDef RrdDef}) or
 * from XML file (dumped content of RRDTool's or JRobin's RRD file).</p>
 *
 * <p>Each RRD is backed with some kind of storage. For example, RRDTool supports only one kind of
 * storage (disk file). On the contrary, JRobin gives you freedom to use other storage (backend) types
 * even to create your own backend types for some special purposes. JRobin by default stores
 * RRD data in files (as RRDTool), but you might choose to store RRD data in memory (this is
 * supported in JRobin), to use java.nio.* instead of java.io.* package for file manipulation
 * (also supported) or to store whole RRDs in the SQL database
 * (you'll have to extend some classes to do this).</p>
 *
 * <p>Note that JRobin uses binary format different from RRDTool's format. You cannot
 * use this class to manipulate RRD files created with RRDTool. <b>However, if you perform
 * the same sequence of create, update and fetch operations, you will get exactly the same
 * results from JRobin and RRDTool.</b><p>
 *
 * <p>
 * You will not be able to use JRobin API if you are not familiar with
 * basic RRDTool concepts. Good place to start is the
 * <a href="http://people.ee.ethz.ch/~oetiker/webtools/rrdtool/tutorial/rrdtutorial.html">official RRD tutorial</a>
 * and relevant RRDTool man pages: <a href="../../../../man/rrdcreate.html" target="man">rrdcreate</a>,
 * <a href="../../../../man/rrdupdate.html" target="man">rrdupdate</a>,
 * <a href="../../../../man/rrdfetch.html" target="man">rrdfetch</a> and
 * <a href="../../../../man/rrdgraph.html" target="man">rrdgraph</a>.
 * For RRDTool's advanced graphing capabilities (RPN extensions), also supported in JRobin,
 * there is an excellent
 * <a href="http://people.ee.ethz.ch/~oetiker/webtools/rrdtool/tutorial/cdeftutorial.html" target="man">CDEF tutorial</a>.
 * </p>
 *
 * @see RrdBackend
 * @see RrdBackendFactory
 */
public class RrdDb implements RrdUpdater {
	/** See {@link #getLockMode() getLockMode()} for explanation */
	public static final int NO_LOCKS = 0;
	/** See {@link #getLockMode() getLockMode()} for explanation */
	public static final int WAIT_IF_LOCKED = 1;
	/** See {@link #getLockMode() getLockMode()} for explanation */
	public static final int EXCEPTION_IF_LOCKED = 2;

    // static final String RRDTOOL = "rrdtool";
	static final int XML_INITIAL_BUFFER_CAPACITY = 100000; // bytes
	private static int lockMode = NO_LOCKS;

	private RrdBackend backend;
	private RrdAllocator allocator = new RrdAllocator();

	private Header header;
	private Datasource[] datasources;
	private Archive[] archives;

	private boolean closed = false;

	/**
	 * <p>Constructor used to create new RRD object from the definition. This RRD object will be backed
	 * with a storage (backend) of the default type. Initially, storage type defaults to "NIO"
	 * (RRD bytes will be put in a file on the disk). Default storage type can be changed with a static
	 * {@link RrdBackendFactory#setDefaultFactory(String)} method call.</p>
	 *
	 * <p>New RRD file structure is specified with an object of class
	 * {@link org.jrobin.core.RrdDef <b>RrdDef</b>}. The underlying RRD storage is created as soon
	 * as the constructor returns.</p>
	 *
	 * <p>Typical scenario:</p>
	 *
	 * <pre>
	 * // create new RRD definition
     * RrdDef def = new RrdDef("test.rrd", 300);
     * def.addDatasource("input", "COUNTER", 600, 0, Double.NaN);
     * def.addDatasource("output", "COUNTER", 600, 0, Double.NaN);
     * def.addArchive("AVERAGE", 0.5, 1, 600);
     * def.addArchive("AVERAGE", 0.5, 6, 700);
     * def.addArchive("AVERAGE", 0.5, 24, 797);
     * def.addArchive("AVERAGE", 0.5, 288, 775);
     * def.addArchive("MAX", 0.5, 1, 600);
     * def.addArchive("MAX", 0.5, 6, 700);
     * def.addArchive("MAX", 0.5, 24, 797);
     * def.addArchive("MAX", 0.5, 288, 775);
     *
     * // RRD definition is now completed, create the database!
     * RrdDb rrd = new RrdDb(def);
	 * // new RRD file has been created on your disk
	 * </pre>
	 *
	 * @param rrdDef Object describing the structure of the new RRD file.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown if invalid RrdDef object is supplied.
	 */
	public RrdDb(RrdDef rrdDef) throws RrdException, IOException {
		this(rrdDef, RrdFileBackendFactory.getDefaultFactory());
	}

	/**
	 * <p>Constructor used to create new RRD object from the definition object but with a storage
	 * (backend) different from default.</p>
	 *
	 * <p>JRobin uses <i>factories</i> to create RRD backend objecs. There are three different
	 * backend factories supplied with JRobin, and each factory has its unique name:</p>
	 *
	 * <ul>
	 * <li><b>FILE</b>: backends created from this factory will store RRD data to files by using
	 * java.io.* classes and methods
	 * <li><b>NIO</b>: backends created from this factory will store RRD data to files by using
	 * java.nio.* classes and methods
	 * <li><b>MEMORY</b>: backends created from this factory will store RRD data in memory. This might
	 * be useful in runtime environments which prohibit disk utilization, or for storing temporary,
	 * non-critical data (it gets lost as soon as JVM exits).
	 * </ul>
	 *
	 * <p>For example, to create RRD in memory, use the following code</p>
	 * <pre>
	 * RrdBackendFactory factory = RrdBackendFactory.getFactory("MEMORY");
	 * RrdDb rrdDb = new RrdDb(rrdDef, factory);
	 * rrdDb.close();
	 * </pre>
	 *
	 * <p>New RRD file structure is specified with an object of class
	 * {@link org.jrobin.core.RrdDef <b>RrdDef</b>}. The underlying RRD storage is created as soon
	 * as the constructor returns.</p>
	 * @param rrdDef RRD definition object
	 * @param factory The factory which will be used to create storage for this RRD
	 * @throws RrdException Thrown if invalid factory or definition is supplied
	 * @throws IOException Thrown in case of I/O error
	 * @see RrdBackendFactory
	 */
	public RrdDb(RrdDef rrdDef, RrdBackendFactory factory) throws RrdException, IOException {
		rrdDef.validate();
		String path = rrdDef.getPath();
		backend = factory.open(path, false, lockMode);
		backend.setLength(rrdDef.getEstimatedSize());
		// create header
		header = new Header(this, rrdDef);
		// create datasources
        DsDef[] dsDefs = rrdDef.getDsDefs();
		datasources = new Datasource[dsDefs.length];
		for(int i = 0; i < dsDefs.length; i++) {
			datasources[i] = new Datasource(this, dsDefs[i]);
		}
		// create archives
		ArcDef[] arcDefs = rrdDef.getArcDefs();
		archives = new Archive[arcDefs.length];
		for(int i = 0; i < arcDefs.length; i++) {
			archives[i] = new Archive(this, arcDefs[i]);
		}
		backend.afterCreate();
	}

	/**
	 * <p>Constructor used to open already existing RRD. This RRD object will be backed
	 * with a storage (backend) of the default type (file on the disk). Constructor
	 * obtains read or read/write access to this RRD.</p>
	 *
	 * @param path Path to existing RRD.
	 * @param readOnly Should be set to <code>false</code> if you want to update
	 * the underlying RRD. If you want just to fetch data from the RRD file
	 * (read-only access), specify <code>true</code>. If you try to update RRD file
	 * open in read-only mode (<code>readOnly</code> set to <code>true</code>),
	 * <code>IOException</code> will be thrown.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public RrdDb(String path, boolean readOnly)	throws IOException, RrdException {
		this(path, readOnly, RrdBackendFactory.getDefaultFactory());
	}

	/**
	 * <p>Constructor used to open already existing RRD backed
	 * with a storage (backend) different from default. Constructor
	 * obtains read or read/write access to this RRD.</p>
	 *
	 * @param path Path to existing RRD.
	 * @param readOnly Should be set to <code>false</code> if you want to update
	 * the underlying RRD. If you want just to fetch data from the RRD file
	 * (read-only access), specify <code>true</code>. If you try to update RRD file
	 * open in read-only mode (<code>readOnly</code> set to <code>true</code>),
	 * <code>IOException</code> will be thrown.
	 * @param factory Backend factory which will be used for this RRD.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 * @see RrdBackendFactory
	 */
	public RrdDb(String path, boolean readOnly, RrdBackendFactory factory)
			throws IOException, RrdException {
		// opens existing RRD file - throw exception if the file does not exist...
		if(!factory.exists(path)) {
			throw new IOException("Could not open " + path + " [non existent]");
		}
		backend = factory.open(path, readOnly, lockMode);
		// restore header
		header = new Header(this, (RrdDef) null);
		header.validateHeader();
		// restore datasources
		int dsCount = header.getDsCount();
		datasources = new Datasource[dsCount];
		for(int i = 0; i < dsCount; i++) {
			datasources[i] = new Datasource(this, null);
		}
		// restore archives
		int arcCount = header.getArcCount();
		archives = new Archive[arcCount];
		for(int i = 0; i < arcCount; i++) {
			archives[i] = new Archive(this, null);
		}
	}

	/**
	 * <p>Constructor used to open already existing RRD in R/W mode, with a default storage
	 * (backend) type (file on the disk).
	 *
	 * @param path Path to existing RRD.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public RrdDb(String path) throws IOException, RrdException {
		this(path, false);
	}

	/**
	 * <p>Constructor used to open already existing RRD in R/W mode with a storage (backend) type
	 * different from default.</p>
	 *
	 * @param path Path to existing RRD.
	 * @param factory Backend factory used to create this RRD.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
 	 * @see RrdBackendFactory
	 */
	public RrdDb(String path, RrdBackendFactory factory) throws IOException, RrdException {
		this(path, false, factory);
	}

	/**
	 * <p>Constructor used to create new RRD from XML dump. Newly created RRD will be backed
	 * with a default storage (backend) type (file on the disk). JRobin and RRDTool
	 * use the same format for XML dump and this constructor should be used to
	 * (re)create JRobin RRD from XML. In other words, it is possible to convert
	 * RRDTool RRD files to JRobin RRD files: first, dump the content of RRDTool
	 * RRD file (use command line):</p>
	 *
	 * <code>rrdtool dump original.rrd > original.xml</code>
	 *
	 * <p>Than, use file <code>original.xml</code> to create JRobin RRD file
	 * <code>copy.rrd</code>:</p>
	 *
	 * <code>RrdDb rrd = new RrdDb("copy.rrd", "original.xml");</code>
	 *
	 * <p>See documentation for {@link #dumpXml(java.lang.String) dumpXml()} method
	 * how to convert JRobin files to RRDTool format.</p>
	 *
	 * @param rrdPath Path to RRD file which will be created
	 * @param xmlPath Path to file containing XML dump of RRDTool's or JRobin's RRD file
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown in case of JRobin specific error
	 */
	public RrdDb(String rrdPath, String xmlPath) throws IOException, RrdException {
		this(rrdPath, xmlPath, RrdBackendFactory.getDefaultFactory());
	}

    /**
	 * <p>Constructor used to create new RRD from XML dump but with a storage (backend) type
	 * different from default.</p>
	 *
	 * @param rrdPath Path to RRD which will be created
	 * @param xmlPath Path to file containing XML dump of RRDTool's or JRobin's RRD file
	 * @param factory Backend factory which will be used to create storage (backend) for this RRD.
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown in case of JRobin specific error
	 * @see RrdBackendFactory
	 */
	public RrdDb(String rrdPath, String xmlPath, RrdBackendFactory factory)
			throws IOException, RrdException {
		backend = factory.open(rrdPath, false, lockMode);
		DataImporter reader;
		if(xmlPath.startsWith("rrdtool:/")) {
			String rrdToolPath = xmlPath.substring("rrdtool:/".length());
			reader = new RrdToolReader(rrdToolPath);
		}
		else if(xmlPath.startsWith("xml:/")) {
			xmlPath = xmlPath.substring("xml:/".length());
			reader = new XmlReader(xmlPath);
		}
		else {
			reader = new XmlReader(xmlPath);
		}
		backend.setLength(reader.getEstimatedSize());
		// create header
		header = new Header(this, reader);
		// create datasources
		datasources = new Datasource[reader.getDsCount()];
		for(int i = 0; i < datasources.length; i++) {
			datasources[i] = new Datasource(this, reader, i);
		}
		// create archives
		archives = new Archive[reader.getArcCount()];
		for(int i = 0; i < archives.length; i++) {
			archives[i] = new Archive(this, reader, i);
		}
		reader.release();
		// XMLReader is a rather huge DOM tree, release memory ASAP
		reader = null;
		backend.afterCreate();
	}

	/**
	 * Closes RRD. No further operations are allowed on this RrdDb object.
	 *
	 * @throws IOException Thrown in case of I/O related error.
	 */
	public synchronized void close() throws IOException {
		if(!closed) {
			backend.close();
			closed = true;
		}
	}

	/**
	 * Returns true if the RRD is closed.
	 * @return true if closed, false otherwise
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Returns RRD header.
	 * @return Header object
	 */
	public Header getHeader() {
		return header;
	}

	/**
	 * Returns Datasource object for the given datasource index.
	 * @param dsIndex Datasource index (zero based)
	 * @return Datasource object
	 */
	public Datasource getDatasource(int dsIndex) {
		return datasources[dsIndex];
	}

	/**
	 * Returns Archive object for the given archive index.
	 * @param arcIndex Archive index (zero based)
	 * @return Archive object
	 */
	public Archive getArchive(int arcIndex) {
		return archives[arcIndex];
	}

	/**
	 * <p>Returns an array of datasource names defined in RRD.</p>
	 *
	 * @return Array of datasource names.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public String[] getDsNames() throws IOException {
		int n = datasources.length;
		String[] dsNames = new String[n];
		for(int i = 0; i < n; i++) {
			dsNames[i] = datasources[i].getDsName();
		}
		return dsNames;
	}

	/**
	 * <p>Creates new sample with the given timestamp and all datasource values set to
	 * 'unknown'. Use returned <code>Sample</code> object to specify
	 * datasource values for the given timestamp. See documentation for
	 * {@link net.jumperz.ext.org.jrobin.core.Sample Sample} for an explanation how to do this.</p>
	 *
	 * <p>Once populated with data source values, call Sample's
	 * {@link net.jumperz.ext.org.jrobin.core.Sample#update() update()} method to actually
	 * store sample in the RRD associated with it.</p>
	 * @param time Sample timestamp rounded to the nearest second (without milliseconds).
	 * @return Fresh sample with the given timestamp and all data source values set to 'unknown'.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public Sample createSample(long time) throws IOException {
		return new Sample(this, time);
	}

	/**
	 * <p>Creates new sample with the current timestamp and all data source values set to
	 * 'unknown'. Use returned <code>Sample</code> object to specify
	 * datasource values for the current timestamp. See documentation for
	 * {@link net.jumperz.ext.org.jrobin.core.Sample Sample} for an explanation how to do this.</p>
	 *
	 * <p>Once populated with data source values, call Sample's
	 * {@link net.jumperz.ext.org.jrobin.core.Sample#update() update()} method to actually
	 * store sample in the RRD associated with it.</p>
	 * @return Fresh sample with the current timestamp and all
	 * data source values set to 'unknown'.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public Sample createSample() throws IOException {
		return createSample(Util.getTime());
	}

	/**
	 * <p>Prepares fetch request to be executed on this RRD. Use returned
	 * <code>FetchRequest</code> object and its {@link net.jumperz.ext.org.jrobin.core.FetchRequest#fetch() fetch()}
	 * method to actually fetch data from the RRD file.</p>
	 * @param consolFun Consolidation function to be used in fetch request. Allowed values are
	 * "AVERAGE", "MIN", "MAX" and "LAST".
	 * @param fetchStart Starting timestamp for fetch request.
	 * @param fetchEnd Ending timestamp for fetch request.
	 * @param resolution Fetch resolution (see RRDTool's
	 * <a href="../../../../man/rrdfetch.html" target="man">rrdfetch man page</a> for an
	 * explanation of this parameter.
	 * @return Request object that should be used to actually fetch data from RRD.
	 * @throws RrdException In case of JRobin related error (invalid consolidation function or
	 * invalid time span).
	 */
	public FetchRequest createFetchRequest(String consolFun, long fetchStart, long fetchEnd,
		long resolution) throws RrdException {
        return new FetchRequest(this, consolFun, fetchStart, fetchEnd, resolution);
	}

	/**
	 * <p>Prepares fetch request to be executed on this RRD. Use returned
	 * <code>FetchRequest</code> object and its {@link net.jumperz.ext.org.jrobin.core.FetchRequest#fetch() fetch()}
	 * method to actually fetch data from this RRD. Data will be fetched with the smallest
	 * possible resolution (see RRDTool's
	 * <a href="../../../../man/rrdfetch.html" target="man">rrdfetch man page</a>
	 * for the explanation of the resolution parameter).</p>
	 *
	 * @param consolFun Consolidation function to be used in fetch request. Allowed values are
	 * "AVERAGE", "MIN", "MAX" and "LAST".
	 * @param fetchStart Starting timestamp for fetch request.
	 * @param fetchEnd Ending timestamp for fetch request.
	 * @return Request object that should be used to actually fetch data from RRD.
	 * @throws RrdException In case of JRobin related error (invalid consolidation function or
	 * invalid time span).
	 */
	public FetchRequest createFetchRequest(String consolFun, long fetchStart, long fetchEnd)
		throws RrdException {
        return createFetchRequest(consolFun, fetchStart, fetchEnd, 1);
	}

	synchronized void store(Sample sample) throws IOException, RrdException {
		if(closed) {
			throw new RrdException("RRD already closed, cannot store this  sample");
		}
		backend.beforeUpdate();
		long newTime = sample.getTime();
		long lastTime = header.getLastUpdateTime();
		if(lastTime >= newTime) {
			throw new RrdException("Bad sample timestamp " + newTime +
				". Last update time was " + lastTime + ", at least one second step is required");
		}
		double[] newValues = sample.getValues();
        for(int i = 0; i < datasources.length; i++) {
			double newValue = newValues[i];
			datasources[i].process(newTime, newValue);
		}
		header.setLastUpdateTime(newTime);
		backend.afterUpdate();
	}

	synchronized FetchPoint[] fetch(FetchRequest request) throws IOException, RrdException {
		if(closed) {
			throw new RrdException("RRD already closed, cannot fetch data");
		}
		backend.beforeFetch();
		Archive archive = findMatchingArchive(request);
		FetchPoint[] points = archive.fetch(request);
		backend.afterFetch();
		return points;
	}

	synchronized FetchData fetchData(FetchRequest request) throws IOException, RrdException {
		if(closed) {
			throw new RrdException("RRD already closed, cannot fetch data");
		}
		backend.beforeFetch();
		Archive archive = findMatchingArchive(request);
		FetchData fetchData = archive.fetchData(request);
		backend.afterFetch();
		return fetchData;
	}

	public Archive findMatchingArchive(FetchRequest request) throws RrdException, IOException
	{
		String consolFun	= request.getConsolFun();
		long fetchStart 	= request.getFetchStart();
		long fetchEnd 		= request.getFetchEnd();
		long resolution 	= request.getResolution();

		Archive bestFullMatch = null, bestPartialMatch = null;

		long bestStepDiff = 0, bestMatch = 0;

		for ( int i = 0; i < archives.length; i++ )
		{
            if ( archives[i].getConsolFun().equals(consolFun) )
			{
				long arcStep 	= archives[i].getArcStep();
				long arcStart 	= archives[i].getStartTime() - arcStep;
                long arcEnd 	= archives[i].getEndTime();
                long fullMatch 	= fetchEnd - fetchStart;

                if ( arcEnd >= fetchEnd && arcStart <= fetchStart )					// best full match
				{
					long tmpStepDiff = Math.abs( archives[i].getArcStep() - resolution );

                    if ( tmpStepDiff < bestStepDiff || bestFullMatch == null )
					{
                        bestStepDiff = tmpStepDiff;
						bestFullMatch = archives[i];
					}

				}
				else																// best partial match
				{
                    long tmpMatch = fullMatch;
					
                    if ( arcStart > fetchStart )
                        tmpMatch -= (arcStart - fetchStart);
					if( arcEnd < fetchEnd )
						tmpMatch -= (fetchEnd - arcEnd);

                    if ( bestPartialMatch == null || bestMatch < tmpMatch )
					{
                        bestPartialMatch 	= archives[i];
						bestMatch 			= tmpMatch;
					}
				}
			}
		}

		if ( bestFullMatch != null )
			return bestFullMatch;
		else if ( bestPartialMatch != null )
			return bestPartialMatch;
		else
			throw new RrdException("RRD file does not contain RRA:" + consolFun + " archive");
	}

	/**
	 * Finds the archive that best matches to the start time (time period being start-time until now)
	 * and requested resolution.
	 * @param consolFun Consolidation function of the datasource.
	 * @param startTime Start time of the time period in seconds.
	 * @param resolution Requested fetch resolution.
	 * @return Reference to the best matching archive.
	 * @throws IOException Thrown in case of I/O related error.
	 */
	public Archive findStartMatchArchive( String consolFun, long startTime, long resolution ) throws IOException
	{
		long arcStep, diff;
		int fallBackIndex	= 0;
		int arcIndex		= -1;
		long minDiff 		= Long.MAX_VALUE;
		long fallBackDiff	= Long.MAX_VALUE;

		for ( int i = 0; i < archives.length; i++ )
		{
			if ( archives[i].getConsolFun().equals(consolFun) )
			{
				arcStep = archives[i].getArcStep();
				diff	= Math.abs( resolution - arcStep );

				// Now compare start time, see if this archive encompasses the requested interval
				if ( startTime >= archives[i].getStartTime() )
				{
					if ( diff == 0 )				// Best possible match either way
						return archives[i];
					else if ( diff < minDiff ) {
						minDiff 	= diff;
						arcIndex	= i;
					}
				}
				else if ( diff < fallBackDiff ) {
					fallBackDiff 	= diff;
					fallBackIndex	= i;
				}
			}
		}

		return ( arcIndex >= 0 ? archives[arcIndex] : archives[fallBackIndex] );
	}

	/**
	 * <p>Returns string representing complete internal RRD state. The returned
	 * string can be printed to <code>stdout</code> and/or used for debugging purposes.</p>
	 * @return String representing internal RRD state.
	 * @throws IOException Thrown in case of I/O related error.
	 */
	public synchronized String dump() throws IOException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(header.dump());
		for(int i = 0; i < datasources.length; i++) {
			buffer.append(datasources[i].dump());
		}
		for(int i = 0; i < archives.length; i++) {
			buffer.append(archives[i].dump());
		}
		return buffer.toString();
	}

	void archive(Datasource datasource, double value, long numUpdates)
		throws IOException, RrdException {
		int dsIndex = getDsIndex(datasource.getDsName());
		for(int i = 0; i < archives.length; i++) {
			archives[i].archive(dsIndex, value, numUpdates);
		}
	}

	/**
	 * <p>Returns internal index number for the given datasource name. This index is heavily
	 * used by jrobin.graph package and has no value outside of it.</p>
	 * @param dsName Data source name.
	 * @return Internal index of the given data source name in this RRD.
	 * @throws RrdException Thrown in case of JRobin related error (invalid data source name,
	 * for example)
	 * @throws IOException Thrown in case of I/O error.
	 */
	public int getDsIndex(String dsName) throws RrdException, IOException {
		for(int i = 0; i < datasources.length; i++) {
			if(datasources[i].getDsName().equals(dsName)) {
				return i;
			}
		}
		throw new RrdException("Unknown datasource name: " + dsName);
	}

	Datasource[] getDatasources() {
		return datasources;
	}

	Archive[] getArchives() {
		return archives;
	}

	/**
	 * <p>Writes the RRD content to OutputStream using XML format. This format
	 * is fully compatible with RRDTool's XML dump format and can be used for conversion
	 * purposes or debugging.</p>
	 * @param destination Output stream to receive XML data
	 * @throws IOException Thrown in case of I/O related error
	 */
	public synchronized void dumpXml(OutputStream destination) throws IOException {
		XmlWriter writer = new XmlWriter(destination);
		writer.startTag("rrd");
		// dump header
		header.appendXml(writer);
		// dump datasources
		for(int i = 0; i < datasources.length; i++) {
			datasources[i].appendXml(writer);
		}
		// dump archives
		for(int i = 0; i < archives.length; i++) {
			archives[i].appendXml(writer);
		}
		writer.closeTag();
		writer.flush();
	}

	/**
	 * This method is just an alias for {@link #dumpXml(OutputStream) dumpXml} method.
	 * @throws IOException Thrown in case of I/O related error
	 */
	public synchronized void exportXml(OutputStream destination) throws IOException {
		dumpXml(destination);
	}

	/**
	 * <p>Returns string representing internal RRD state in XML format. This format
	 * is fully compatible with RRDTool's XML dump format and can be used for conversion
	 * purposes or debugging.</p>
	 * @return Internal RRD state in XML format.
	 * @throws IOException Thrown in case of I/O related error
	 * @throws RrdException Thrown in case of JRobin specific error
	 */
	public synchronized String getXml() throws IOException, RrdException {
		ByteArrayOutputStream destination = new ByteArrayOutputStream(XML_INITIAL_BUFFER_CAPACITY);
		dumpXml(destination);
		return destination.toString();
	}

	/**
	 * This method is just an alias for {@link #getXml() getXml} method.
	 * @return Internal RRD state in XML format.
	 * @throws IOException Thrown in case of I/O related error
	 * @throws RrdException Thrown in case of JRobin specific error
	 */
	public synchronized String exportXml() throws IOException, RrdException {
		return getXml();
	}

	/**
	 * <p>Dumps internal RRD state to XML file.
	 * Use this XML file to convert your JRobin RRD to RRDTool format.</p>
	 *
	 * <p>Suppose that you have a JRobin RRD file <code>original.rrd</code> and you want
	 * to convert it to RRDTool format. First, execute the following java code:</p>
	 *
	 * <code>RrdDb rrd = new RrdDb("original.rrd");
	 * rrd.dumpXml("original.xml");</code>
	 *
	 * <p>Use <code>original.xml</code> file to create the corresponding RRDTool file
	 * (from your command line):
	 *
	 * <code>rrdtool restore copy.rrd original.xml</code>
	 *
	 * @param filename Path to XML file which will be created.
	 * @throws IOException Thrown in case of I/O related error.
	 * @throws RrdException Thrown in case of JRobin related error.
	 */
	public synchronized void dumpXml(String filename) throws IOException, RrdException {
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(filename, false);
			dumpXml(outputStream);
		}
		finally {
			if(outputStream != null) {
				outputStream.close();
			}
		}
	}

	/**
	 * This method is just an alias for {@link #dumpXml(String) dumpXml(String)} method.
	 * @throws IOException Thrown in case of I/O related error
	 * @throws RrdException Thrown in case of JRobin specific error
	 */
	public synchronized void exportXml(String filename) throws IOException, RrdException {
		dumpXml(filename);
	}

	/**
	 * Returns time of last update operation as timestamp (in seconds).
	 * @return Last update time (in seconds).
	 */
	public synchronized long getLastUpdateTime() throws IOException {
		return header.getLastUpdateTime();
	}

	/**
	 * <p>Returns RRD definition object which can be used to create new RRD
	 * with the same creation parameters but with no data in it.</p>
	 *
	 * <p>Example:</p>
	 *
	 * <pre>
	 * RrdDb rrd1 = new RrdDb("original.rrd");
	 * RrdDef def = rrd1.getRrdDef();
	 * // fix path
	 * def.setPath("empty_copy.rrd");
	 * // create new RRD file
	 * RrdDb rrd2 = new RrdDb(def);
	 * </pre>
	 * @return RRD definition.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public synchronized RrdDef getRrdDef() throws RrdException, IOException {
		// set header
		long startTime = header.getLastUpdateTime();
		long step = header.getStep();
		String path = backend.getPath();
		RrdDef rrdDef = new RrdDef(path, startTime, step);
		// add datasources
		for(int i = 0; i < datasources.length; i++) {
			DsDef dsDef = new DsDef(datasources[i].getDsName(),
				datasources[i].getDsType(), datasources[i].getHeartbeat(),
				datasources[i].getMinValue(), datasources[i].getMaxValue());
			rrdDef.addDatasource(dsDef);
		}
		// add archives
		for(int i = 0; i < archives.length; i++) {
            ArcDef arcDef = new ArcDef(archives[i].getConsolFun(),
				archives[i].getXff(), archives[i].getSteps(), archives[i].getRows());
			rrdDef.addArchive(arcDef);
		}
		return rrdDef;
	}

	/**
	 * <p>Returns current lock mode. This function can return one of the following values:
	 * <ul>
	 * <li><code>NO_LOCKS</code>: RRD files are not locked (default). Simultaneous access
	 * to the same RRD file is allowed. This locking mode provides fastest read/write
	 * (fetch/update) operations, but could lead to inconsisten RRD data.</li>
	 * <li><code>WAIT_IF_LOCKED</code>: RRD files are locked exclusively.
	 * Simultaneous access to the same underlying RRD file is not allowed.
	 * If a <code>RrdDb</code> object tries to access already locked RRD file,
	 * it will wait until the lock is released. The lock is released when
	 * the {@link #close() close()} method is called.</li>
	 * <li><code>EXCEPTION_IF_LOCKED</code>: RRD files are locked exclusively.
	 * Simultaneous access to the same underlying RRD file is not allowed.
	 * If a <code>RrdDb</code> object tries to access already locked RRD file,
	 * an exception is thrown.</li></p>
	 * </ul>
	 * <p>Note that <code>WAIT_IF_LOCKED</code> and <code>EXCEPTION_IF_LOCKED</code>
	 * modes guarantee data consistency but affect the speed of read/write operations.
	 * Call {@link #close() close()} as soon as possible in your code to avoid long wait states
	 * (or locking exceptions). </p>
	 * @return The current locking behaviour.
	 */
	public static int getLockMode() {
		return RrdDb.lockMode;
	}

	/**
	 * Sets the current locking mode. See {@link #getLockMode() getLockMode()} for more
	 * information.
	 * @param lockMode Lock mode. Valid values are <code>NO_LOCKS</code>,
	 * <code>WAIT_IF_LOCKED</code> and <code>EXCEPTION_IF_LOCKED</code>.
	 */
	public static void setLockMode(int lockMode) {
		RrdDb.lockMode = lockMode;
	}

	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * Copies object's internal state to another RrdDb object.
	 * @param other New RrdDb object to copy state to
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown if supplied argument is not a compatible RrdDb object
	 */
	public synchronized void copyStateTo(RrdUpdater other) throws IOException, RrdException {
		if(!(other instanceof RrdDb)) {
			throw new RrdException(
				"Cannot copy RrdDb object to " + other.getClass().getName());
		}
		RrdDb otherRrd = (RrdDb) other;
		header.copyStateTo(otherRrd.header);
		for(int i = 0; i < datasources.length; i++) {
			int j = Util.getMatchingDatasourceIndex(this, i, otherRrd);
			if(j >= 0) {
				datasources[i].copyStateTo(otherRrd.datasources[j]);
			}
		}
		for(int i = 0; i < archives.length; i++) {
			int j = Util.getMatchingArchiveIndex(this, i, otherRrd);
			if(j >= 0) {
				archives[i].copyStateTo(otherRrd.archives[j]);
			}
		}
	}

	/**
	 * Returns Datasource object corresponding to the given datasource name.
	 * @param dsName Datasource name
	 * @return Datasource object corresponding to the give datasource name or null
	 * if not found.
	 * @throws IOException Thrown in case of I/O error
	 */
	public Datasource getDatasource(String dsName) throws IOException {
		try {
			return getDatasource(getDsIndex(dsName));
		}
		catch (RrdException e) {
			return null;
		}
	}

	/**
	 * Returns index of Archive object with the given consolidation function and the number
	 * of steps. Exception is thrown if such archive could not be found.
	 * @param consolFun Consolidation function
	 * @param steps Number of archive steps
	 * @return Requested Archive object
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown if no such archive could be found
	 */
	public int getArcIndex(String consolFun, int steps) throws RrdException, IOException {
		for(int i = 0; i < archives.length; i++) {
			if(archives[i].getConsolFun().equals(consolFun) &&
				archives[i].getSteps() == steps) {
				return i;
			}
		}
		throw new RrdException("Could not find archive " + consolFun + "/" + steps);
	}

	/**
	 * Returns Archive object with the given consolidation function and the number
	 * of steps.
	 * @param consolFun Consolidation function
	 * @param steps Number of archive steps
	 * @return Requested Archive object or null if no such archive could be found
	 * @throws IOException Thrown in case of I/O error
	 */
	public Archive getArchive(String consolFun, int steps) throws IOException {
		try {
			return getArchive(getArcIndex(consolFun, steps));
		}
		catch (RrdException e) {
			return null;
		}
	}

	/**
	 * Returns canonical path to the underlying RRD file. Note that this method makes sense just for
	 * ordinary RRD files created on the disk - an exception will be thrown for RRD objects created in
	 * memory or with custom backends.
	 * @return Canonical path to RRD file;
	 * @throws IOException Thrown in case of I/O error or if the underlying backend is
	 * not derived from RrdFileBackend.
	 */
	public String getCanonicalPath() throws IOException {
		if(backend instanceof RrdFileBackend) {
			return ((RrdFileBackend) backend).getCanonicalPath();
		}
		else {
			throw new IOException("The underlying backend has no canonical path");
		}
	}

	/**
	 * Returns path to this RRD.
	 * @return Path to this RRD.
	 */
	public String getPath() {
		return backend.getPath();
	}

	/**
	 * Returns backend object for this RRD which performs actual I/O operations.
	 * @return RRD backend for this RRD.
	 */
	public RrdBackend getRrdBackend() {
		return backend;
	}

	/**
	 * Required to implement RrdUpdater interface. You should never call this method directly.
	 * @return Allocator object
	 */
	public RrdAllocator getRrdAllocator() {
		return allocator;
	}

	/**
	 * Returns an array of bytes representing the whole RRD.
	 * @return All RRD bytes
	 * @throws IOException Thrown in case of I/O related error.
	 */
	public synchronized byte[] getBytes() throws IOException {
		return backend.readAll();
	}

	/**
	 * This method forces all RRD data cached in memory but not yet stored in the persistant
	 * storage, to be stored in it. This method should be used only if you RrdDb object uses
	 * RrdBackend which implements any kind of data caching (like {@link RrdNioBackend}). This method
	 * need not be called before the {@link #close()} method call since the close() method always
	 * synchronizes all data in memory with the data in the persisant storage.<p>
	 *
	 * When this method returns it is guaranteed that RRD data in the persistent storage is
	 * synchronized with the RrdDb data in memory.<p>  
	 *
	 * @throws IOException Thrown in case of I/O error
	 */
	public synchronized void sync() throws IOException {
		backend.sync();
	}

	/**
	 * Sets default backend factory to be used. This method is just an alias for
	 * {@link RrdBackendFactory#setDefaultFactory(java.lang.String)}.<p>
	 * @param factoryName Name of the backend factory to be set as default.
	 * @throws RrdException Thrown if invalid factory name is supplied, or not called
	 * before the first backend object (before the first RrdDb object) is created.
	 */
	public static void setDefaultFactory(String factoryName) throws RrdException {
		RrdBackendFactory.setDefaultFactory(factoryName);
	}

	public static void main(String[] args) {
		System.out.println("JRobin base directory: " + Util.getJRobinHomeDirectory());
		System.out.println("JRobin Java Library :: RRDTool choice for the Java world");
		System.out.println("http://www.jrobin.org");
		System.out.println("(C) 2004 Sasa Markovic & Arne Vandamme");
	}

}
