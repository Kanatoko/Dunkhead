/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org)
 * 
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * (C) Copyright 2003, by Sasa Markovic.
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
package net.jumperz.ext.org.jrobin.graph;

import net.jumperz.ext.org.jrobin.core.RrdException;
import net.jumperz.ext.org.jrobin.core.XmlWriter;

/**
 * <p>Represents the Title used in the graph.  The title object has the same alignment
 * possibilities as all other text/comment objects in a graph.</p>
 * 
 * @author Arne Vandamme (cobralord@jrobin.org)
 */
class Title extends Comment 
{
	// ================================================================
	// -- Constructors
	// ================================================================	
	/**
	 * Constructs a Title object based on a text string.
	 * @param text Text string with alignment markers representing the title.
	 * @throws RrdException Thrown in case of a JRobin specific error.
	 */
	Title( String text ) throws RrdException
	{
		this.text 	= text;
		lfToken		= Comment.TKN_ACF; 
		super.parseComment();	
	}

	void exportXmlTemplate(XmlWriter xml) {
		xml.writeTag("title", getText());
	}
}
