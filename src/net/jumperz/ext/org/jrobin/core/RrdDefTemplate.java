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

import org.xml.sax.InputSource;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.File;
import java.util.GregorianCalendar;

/**
 * Class used to create an arbitrary number of {@link RrdDef} (RRD definition) objects
 * from a single XML template. XML template can be supplied as an XML InputSource,
 * XML file or XML formatted string.<p>
 *
 * Here is an example of a properly formatted XML template with all available
 * options in it (unwanted options can be removed):<p>
 * <pre>
 * &lt;rrd_def&gt;
 *     &lt;path&gt;test.rrd&lt;/path&gt;
 *     &lt;!-- not mandatory --&gt;
 *     &lt;start&gt;1000123456&lt;/start&gt;
 *     &lt;!-- not mandatory --&gt;
 *     &lt;step&gt;300&lt;/step&gt;
 *     &lt;!-- at least one datasource must be supplied --&gt;
 *     &lt;datasource&gt;
 *         &lt;name&gt;input&lt;/name&gt;
 *         &lt;type&gt;COUNTER&lt;/type&gt;
 *         &lt;heartbeat&gt;300&lt;/heartbeat&gt;
 *         &lt;min&gt;0&lt;/min&gt;
 *         &lt;max&gt;U&lt;/max&gt;
 *     &lt;/datasource&gt;
 *     &lt;datasource&gt;
 *         &lt;name&gt;temperature&lt;/name&gt;
 *         &lt;type&gt;GAUGE&lt;/type&gt;
 *         &lt;heartbeat&gt;400&lt;/heartbeat&gt;
 *         &lt;min&gt;U&lt;/min&gt;
 *         &lt;max&gt;1000&lt;/max&gt;
 *     &lt;/datasource&gt;
 *     &lt;!-- at least one archive must be supplied --&gt;
 *     &lt;archive&gt;
 *         &lt;cf&gt;AVERAGE&lt;/cf&gt;
 *         &lt;xff&gt;0.5&lt;/xff&gt;
 *         &lt;steps&gt;1&lt;/steps&gt;
 *         &lt;rows&gt;600&lt;/rows&gt;
 *     &lt;/archive&gt;
 *     &lt;archive&gt;
 *         &lt;cf&gt;MAX&lt;/cf&gt;
 *         &lt;xff&gt;0.6&lt;/xff&gt;
 *         &lt;steps&gt;6&lt;/steps&gt;
 *         &lt;rows&gt;7000&lt;/rows&gt;
 *     &lt;/archive&gt;
 * &lt;/rrd_def&gt;
 * </pre>
 * Notes on the template syntax:<p>
 * <ul>
 * <li>There is a strong relation between the XML template syntax and the syntax of
 * {@link RrdDef} class methods. If you are not sure what some XML tag means, check javadoc
 * for the corresponding class.
 * <li>starting timestamp can be supplied either as a long integer
 * (like: 1000243567) or as an ISO formatted string (like: 2004-02-21 12:25:45)
 * <li>whitespaces are not harmful
 * <li>floating point values: anything that cannot be parsed will be treated as Double.NaN
 * (like: U, unknown, 12r.23)
 * <li>comments are allowed.
 * </ul>
 * Any template value (text between <code>&lt;some_tag&gt;</code> and
 * <code>&lt;/some_tag&gt;</code>) can be replaced with
 * a variable of the following form: <code>${variable_name}</code>. Use
 * {@link XmlTemplate#setVariable(String, String) setVariable()}
 * methods from the base class to replace template variables with real values
 * at runtime.<p>
 *
 * Typical usage scenario:<p>
 * <ul>
 * <li>Create your XML template and save it to a file (template.xml, for example)
 * <li>Replace hardcoded template values with variables if you want to change them during runtime.
 * For example, RRD path should not be hardcoded in the template - you probably want to create
 * many different RRD files from the same XML template. For example, your XML
 * template could start with:
 * <pre>
 * &lt;rrd_def&gt;
 *     &lt;path&gt;${path}&lt;/path&gt;
 *     &lt;step&gt;300&lt;/step&gt;
 *     ...
 * </pre>
 * <li>In your Java code, create RrdDefTemplate object using your XML template file:
 * <pre>
 * RrdDefTemplate t = new RrdDefTemplate(new File(template.xml));
 * </pre>
 * <li>Then, specify real values for template variables:
 * <pre>
 * t.setVariable("path", "demo/test.rrd");
 * </pre>
 * <li>Once all template variables are set, just use the template object to create RrdDef
 * object. This object is actually used to create JRobin RRD files:
 * <pre>
 * RrdDef def = t.getRrdDef();
 * RrdDb rrd = new RrdDb(def);
 * rrd.close();
 * </pre>
 * </ul>
 * You should create new RrdDefTemplate object only once for each XML template. Single template
 * object can be reused to create as many RrdDef objects as needed, with different values
 * specified for template variables. XML synatax check is performed only once - the first
 * definition object gets created relatively slowly, but it will be created much faster next time.
 */
public class RrdDefTemplate extends XmlTemplate {
	/**
	 * Creates RrdDefTemplate object from any parsable XML input source. Read general information
	 * for this class to find an example of a properly formatted RrdDef XML source.
	 * @param xmlInputSource Xml input source
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown in case of XML related error (parsing error, for example)
	 */
	public RrdDefTemplate(InputSource xmlInputSource) throws IOException, RrdException {
		super(xmlInputSource);
	}

	/**
	 * Creates RrdDefTemplate object from the string containing XML template.
	 * Read general information for this class to see an example of a properly formatted XML source.
	 * @param xmlString String containing XML template
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown in case of XML related error (parsing error, for example)
	 */
	public RrdDefTemplate(String xmlString) throws IOException, RrdException {
		super(xmlString);
	}

    /**
	 * Creates RrdDefTemplate object from the file containing XML template.
	 * Read general information for this class to see an example of a properly formatted XML source.
	 * @param xmlFile File object representing file with XML template
	 * @throws IOException Thrown in case of I/O error
	 * @throws RrdException Thrown in case of XML related error (parsing error, for example)
	 */
	public RrdDefTemplate(File xmlFile) throws IOException, RrdException {
		super(xmlFile);
	}

    /**
	 * Returns RrdDef object constructed from the underlying XML template. Before this method
	 * is called, values for all non-optional placeholders must be supplied. To specify
	 * placeholder values at runtime, use some of the overloaded
	 * {@link XmlTemplate#setVariable(String, String) setVariable()} methods. Once this method
	 * returns, all placeholder values are preserved. To remove them all, call inhereted
	 * {@link XmlTemplate#clearValues() clearValues()} method explicitly.<p>
	 *
	 * @return RrdDef object constructed from the underlying XML template,
	 * with all placeholders replaced with real values. This object can be passed to the constructor
	 * of the new RrdDb object.
	 * @throws RrdException Thrown (in most cases) if the value for some placeholder
	 * was not supplied through {@link XmlTemplate#setVariable(String, String) setVariable()}
	 * method call
	 */
	public RrdDef getRrdDef() throws RrdException {
		if (!root.getTagName().equals("rrd_def")) {
			throw new RrdException("XML definition must start with <rrd_def>");
		}
		validateTagsOnlyOnce(root, new String[] {
			"path", "start", "step", "datasource*", "archive*"
		});
		// PATH must be supplied or exception is thrown
		String path = getChildValue(root, "path");
		RrdDef rrdDef = new RrdDef(path);
		try {
			String startStr = getChildValue(root, "start");
			GregorianCalendar startGc = Util.getGregorianCalendar(startStr);
			rrdDef.setStartTime(startGc);
		} catch (RrdException e) {
			// START is not mandatory
		}
		try {
			long step = getChildValueAsLong(root, "step");
			rrdDef.setStep(step);
		} catch (RrdException e) {
			// STEP is not mandatory
		}
		// datsources
		Node[] dsNodes = getChildNodes(root, "datasource");
		for (int i = 0; i < dsNodes.length; i++) {
			validateTagsOnlyOnce(dsNodes[i], new String[] {
				"name", "type", "heartbeat", "min", "max"
			});
			String name = getChildValue(dsNodes[i], "name");
			String type = getChildValue(dsNodes[i], "type");
			long heartbeat = getChildValueAsLong(dsNodes[i], "heartbeat");
			double min = getChildValueAsDouble(dsNodes[i], "min");
			double max = getChildValueAsDouble(dsNodes[i], "max");
			rrdDef.addDatasource(name, type, heartbeat, min, max);
		}
		// archives
		Node[] arcNodes = getChildNodes(root, "archive");
		for (int i = 0; i < arcNodes.length; i++) {
			validateTagsOnlyOnce(arcNodes[i], new String[] {
				"cf", "xff", "steps", "rows"
			});
			String consolFun = getChildValue(arcNodes[i], "cf");
			double xff = getChildValueAsDouble(arcNodes[i], "xff");
			int steps = getChildValueAsInt(arcNodes[i], "steps");
			int rows = getChildValueAsInt(arcNodes[i], "rows");
			rrdDef.addArchive(consolFun, xff, steps, rows);
		}
		return rrdDef;
	}
}