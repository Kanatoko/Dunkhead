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

import java.awt.*;

import net.jumperz.ext.org.jrobin.core.RrdException;
import net.jumperz.ext.org.jrobin.core.Util;
import net.jumperz.ext.org.jrobin.core.XmlWriter;

/**
 * <p>Class used to represent a datasource plotted as a line in a graph.</p>
 * 
 * @author Arne Vandamme (cobralord@jrobin.org)
 */
class Line extends PlotDef
{
	// ================================================================
	// -- Members
	// ================================================================
	protected static BasicStroke DEF_LINE_STROKE 	= new BasicStroke(1.0f);
	protected int lineWidth							= 1;			// Default line width of 1 pixel
	
	
	// ================================================================
	// -- Constructors
	// ================================================================
	Line() {
		super();
	} 

	/**
	 * Constructs a <code>Line</code> PlotDef object based on a datasource name and a graph color.
	 * The resulting line will have a width of 1 pixel.
	 * @param sourceName Name of the graph definition <code>Source</code> containing the datapoints.
	 * @param color Color of the resulting line, if no color is specified, the Line will not be drawn.
	 */		
	Line( String sourceName, Color color )
	{
		super( sourceName, color );
	}
	
	/**
	 * Constructs a <code>Line</code> PlotDef object based on a datasource name, a graph color and a line width. 
	 * @param sourceName Name of the graph definition <code>Source</code> containing the datapoints.
	 * @param color Color of the resulting line, if no color is specified, the Line will not be drawn.
	 * @param lineWidth Width in pixels of the line to draw.
	 */
	Line( String sourceName, Color color, int lineWidth )
	{
		this( sourceName, color );
		this.lineWidth	= lineWidth;
	}
	
	/**
	 * Constructs a <code>Line</code> object based on a Source containing all necessary datapoints and
	 * a color to draw the resulting graph in.  The last two parameters define if the
	 * Area should be drawn, and if it is stacked onto a previous PlotDef yes or no.
	 * @param source Source containing all datapoints for this Line.
	 * @param color Color of the resulting graphed line.
	 * @param stacked True if this PlotDef is stacked on the previous one, false if not.
	 * @param visible True if this PlotDef should be graphed, false if not.
	 */
	Line( Source source, double[] values, Color color, boolean stacked, boolean visible )
	{
		super( source, values, color, stacked, visible);
	}
	
	
	// ================================================================
	// -- Protected methods
	// ================================================================
	/**
	 * Draws the actual Line on the chart.
	 * @param g ChartGraphics object representing the graphing area.
	 * @param xValues List of relative chart area X positions corresponding to the datapoints.
	 * @param stackValues Datapoint values of previous PlotDefs, used to stack on if necessary.
	 * @param lastPlotType Type of the previous PlotDef, used to determine PlotDef type of a stack.
	 */
	void draw( ChartGraphics g, int[] xValues, double[] stackValues, int lastPlotType ) throws RrdException
	{
		g.setColor( color );
		g.setStroke( lineWidth != 1 ? new BasicStroke(lineWidth) : DEF_LINE_STROKE );

		Graphics2D gd 	= g.getGraphics();
		int len			= values.length;

		double value;
		int ax = 0, ay = 0, nx = 0, ny = 0;

		for ( int i = 0; i < len; i++ )
		{
			value	= values[i];
			nx 		= xValues[i];

			if ( stacked )
				value += stackValues[i];

			ny 		= g.getY( value );

			if ( visible && nx != 0 && ay != Integer.MIN_VALUE && ny != Integer.MIN_VALUE )
				gd.drawLine(ax, -ay, nx, -ny);

			stackValues[i] 	= value;
			ax 				= nx;
			ay 				= ny;
		}

		g.setStroke( STROKE );
	}
	
	int getLineWidth() {
		return lineWidth;
	}
	
	void exportXmlTemplate( XmlWriter xml, String legend )
	{
		xml.startTag("line");
		xml.writeTag("datasource", sourceName);
		xml.writeTag("color", color);
		xml.writeTag("legend", legend);
		xml.writeTag("width", lineWidth);
		xml.closeTag(); // area
	}
}
