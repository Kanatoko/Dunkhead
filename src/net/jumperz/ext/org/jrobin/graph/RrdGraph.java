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

import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.*;

import net.jumperz.ext.org.jrobin.core.RrdException;
import net.jumperz.ext.org.jrobin.core.RrdOpener;

/**
 * <p>Class to represent JRobin graphs.  This class needs an appropriate RrdGraphDef to generate graphs.</p>
 * 
 * @author Arne Vandamme (cobralord@jrobin.org)
 * @author Sasa Markovic (saxon@jrobin.org)
 */
public class RrdGraph extends RrdOpener implements Serializable
{
	// ================================================================
	// -- Members
	// ================================================================
	private Grapher grapher;
	private BufferedImage img;

	private boolean useImageSize		= false;
	
	
	// ================================================================
	// -- Constructors
	// ================================================================
	/**
	 * Constructs a new JRobin graph object, without a shared database pool.
	 */
	public RrdGraph() 
	{
		super( false, true );
	}

	/**
	 * Constructs a new JRobin graph object.
	 * @param usePool True if this object should use RrdDbPool
	 */
	public RrdGraph( boolean usePool )
	{
		super( usePool, true );
	}

	/**
	 * Constructs a new JRobin graph object from the supplied definition.
	 * @param graphDef Graph definition.
	 */
	public RrdGraph( RrdGraphDef graphDef )
	{
		this( graphDef, false );
	}

	/**
	 * Constructs a new JRobin graph from the supplied definition.
	 * @param graphDef Graph definition.
	 * @param usePool True if this should object should use RrdDbPool
	 */
	public RrdGraph( RrdGraphDef graphDef, boolean usePool )
	{
		super( usePool, true );
		grapher		= new Grapher( graphDef, this );
	}
	
	
	// ================================================================
	// -- Public mehods
	// ================================================================
	/**
	 * Determines if graph creation should specify dimensions for the chart graphing
	 * are, of for the entire image size.  Default is the only the chart graphing
	 * area, this has an impact on the entire image size.
	 * @param specImgSize True if the dimensions for the entire image will be specified, false if only for the chart area. 
	 */
	public void specifyImageSize( boolean specImgSize )
	{
		this.useImageSize = specImgSize;
	}
	
	/**
	 * Sets the graph definition to use for the graph construction.
	 * @param graphDef Graph definition.
	 */
	public void setGraphDef( RrdGraphDef graphDef ) 
	{
		img		= null;
		grapher = new Grapher( graphDef, this );
	}
	
	/**
	 * Creates and saves a graph image with default dimensions as a PNG file.
	 * By default the chart area is 400 by 100 pixels, the size of the entire image is dependant
	 * on number of title/legend/comment lines and some other settings.
	 * @param path Path to the PNG file to be created.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public void saveAsPNG( String path ) throws RrdException, IOException
	{
		saveAsPNG( path, 0, 0 );
	}
	
	/**
	 * Creates and saves a graph image with custom chart dimensions as a PNG file.
	 * The resulting size of the entire image is also influenced by many other settings like number of comment lines.
	 * @param path Path to the PNG file to be created.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public void saveAsPNG( String path, int width, int height ) throws RrdException, IOException
	{
		File imgFile = new File( path );

		if ( shouldGenerate(imgFile) )
			ImageIO.write( getBufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", imgFile );
	}

	/**
	 * Creates and saves a graph image with default dimensions as a GIF file.
	 * By default the chart area is 400 by 100 pixels, the size of the entire image is dependant
	 * on number of title/legend/comment lines and some other settings.
	 * @param path Path to the GIF file to be created.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public void saveAsGIF( String path ) throws RrdException, IOException
	{
		saveAsGIF( path, 0, 0 );
	}
	
	/**
	 * Creates and saves a graph image with custom chart dimensions as a GIF file.
	 * The resulting size of the entire image is also influenced by many other settings like number of comment lines.
	 * @param path Path to the GIF file to be created.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public void saveAsGIF(String path, int width, int height) throws RrdException, IOException
	{
		File imgFile = new File( path );

		if ( shouldGenerate(imgFile) )
		{
			GifEncoder gifEncoder 		= new GifEncoder( getBufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED) );
			FileOutputStream stream 	= new FileOutputStream( path, false );

			gifEncoder.encode(stream);

			stream.close();
		}
	}

	/**
	 * Creates and saves a graph image with default dimensions as a JPEG file.
	 * By default the chart area is 400 by 100 pixels, the size of the entire image is dependant
	 * on number of title/legend/comment lines and some other settings.
	 * @param path Path to the JPEG file to be created.
	 * @param quality JPEG quality, between 0 (= low) and 1.0f (= high).
	 * @throws IOException Thrown in case of I/O error.
	 */
	public void saveAsJPEG( String path, float quality ) throws RrdException, IOException
	{
		saveAsJPEG( path, 0, 0, quality );
	}
	
	/**
	 * Creates and saves a graph image with custom chart dimensions as a JPEG file.
	 * The resulting size of the entire image is also influenced by many other settings like number of comment lines.
	 * @param path Path to the JPEG file to be created.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @param quality JPEG quality, between 0 (= low) and 1.0f (= high).
	 * @throws IOException Thrown in case of I/O error.
	 */
	public void saveAsJPEG( String path, int width, int height, float quality ) throws RrdException, IOException
	{
		File imgFile = new File( path );

		if ( !shouldGenerate(imgFile) )
			return;

		// Based on http://javaalmanac.com/egs/javax.imageio/JpegWrite.html?l=rel
		// Retrieve jpg image to be compressed
		RenderedImage rndImage	= getBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	
		// Find a jpeg writer
		ImageWriter writer = null;
		Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
		if (iter.hasNext()) {
			writer = (ImageWriter)iter.next();
		}

		// Prepare output file
		ImageOutputStream ios = ImageIO.createImageOutputStream(new File(path));
		writer.setOutput(ios);

		// Set the compression quality
		ImageWriteParam iwparam = new JpegImageWriteParam();
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
		iwparam.setCompressionQuality(quality);

		// Write the image
		writer.write(null, new IIOImage(rndImage, null, null), iwparam);

		// Cleanup
		ios.flush();
		writer.dispose();
		ios.close();
	}
	
	/**
	 * Returns graph with default chart dimensions (400 by 100) as an array of PNG bytes.
	 * @return Array of PNG bytes.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public byte[] getPNGBytes() throws IOException, RrdException
	{
		return getPNGBytes( 0, 0 );
	}
	
	/**
	 * Returns graph with custom chart dimensions as an array of PNG bytes.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @return Array of PNG bytes.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public byte[] getPNGBytes( int width, int height ) throws IOException, RrdException
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		ImageIO.write(getBufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", outputStream );
				
		return outputStream.toByteArray();
	}

	/**
	 * Returns graph with default chart dimensions (400 by 100) as an array of JPEG bytes.
	 * @param quality JPEG quality, between 0 (= low) and 1.0f (= high).
	 * @return Array of PNG bytes.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public byte[] getJPEGBytes( float quality ) throws IOException, RrdException
	{
		return getJPEGBytes( 0, 0, quality );
	}
	
	/**
	 * Returns graph with custom chart dimensions as an array of JPEG bytes.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @param quality JPEG quality, between 0 (= low) and 1.0f (= high).
	 * @return Array of JPEG bytes.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public byte[] getJPEGBytes( int width, int height, float quality ) throws IOException, RrdException
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		// Retrieve jpg image to be compressed
		RenderedImage rndImage	= getBufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	
		// Find a jpeg writer
		ImageWriter writer = null;
		Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
		if (iter.hasNext()) {
			writer = (ImageWriter)iter.next();
		}

		// Prepare output file
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
		writer.setOutput(ios);

		// Set the compression quality
		ImageWriteParam iwparam = new JpegImageWriteParam();
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
		iwparam.setCompressionQuality(quality);

		// Write the image
		writer.write(null, new IIOImage(rndImage, null, null), iwparam);

		// Cleanup
		ios.flush();
		writer.dispose();
		ios.close();
		
		return outputStream.toByteArray();
	}

	/**
	 * Returns graph with default chart dimensions (400 by 100) as an array of GIF bytes.
	 * @return Array of GIF bytes.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public byte[] getGIFBytes() throws RrdException, IOException
	{
		return getGIFBytes( 0, 0 );	
	}
	
	/**
	 * Returns graph with custom chart dimensions as an array of GIF bytes.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @return Array of GIF bytes.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public byte[] getGIFBytes(int width, int height) throws RrdException, IOException
	{
		BufferedImage image 			= getBufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
		ByteArrayOutputStream bStream 	= new ByteArrayOutputStream();
	
		GifEncoder gifEncoder 			= new GifEncoder( image );
		gifEncoder.encode( bStream );
		
		return bStream.toByteArray();
	}

	/**
	 * Returns the underlying BufferedImage of a graph with custom dimensions.
	 * Specifying 0 for both width and height will result in a auto-sized graph.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @return BufferedImage containing the graph.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public BufferedImage getBufferedImage( int width, int height ) throws IOException, RrdException
	{
		return getBufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
	}

	/**
	 * Returns panel object so that graph can be easily embedded in swing applications.
	 * @return Swing JPanel object with graph embedded in panel.
	 */
	public ChartPanel getChartPanel() throws RrdException, IOException
	{
		ChartPanel p = new ChartPanel();
		p.setChart( getBufferedImage(0, 0, BufferedImage.TYPE_INT_RGB) );
		
		return p;
	}

	/**
	 * Renders the graph onto a specified Graphics2D object.
	 * Specifying 0 for both width and height will result in a auto-sized graph.
	 * @param graphics Handle to a Graphics2D object to render the graph on.
	 * @param width Width of the chart area in pixels.
	 * @param height Height of the chart area in pixels.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 * @throws IOException Thrown in case of I/O error
	 */
	public void renderImage( Graphics2D graphics, int width, int height ) throws RrdException, IOException
	{
		if ( useImageSize )
			grapher.renderImage( width, height, graphics, true );
		else
			grapher.renderImage( width, height, graphics, false );
	}

	/**
	 * This retrieves the ExportData object associated with the reduced dataset of this Graph.
	 * This method assumes the graph or at least the dataset has already been calculated.
	 *
	 * @return ExportData object containing the reduced dataset.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	public ExportData getExportData() throws RrdException {
		return grapher.createExportData();
	}

	/**
	 * This retrieves the ExportData object associated with the reduced dataset of this Graph,
	 * by calculating the dataset on the spot.  Use this if you want to retrieve the associated
	 * ExportData without generating the actual graph.
	 *
	 * @return ExportData object containing the reduced dataset.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 * @throws IOException Thrown in case of I/O error
	 */
	public ExportData fetchExportData() throws RrdException, IOException {
		return grapher.fetch( Grapher.DEFAULT_WIDTH );
	}

	/**
	 * This retrieves the ExportData object associated with the reduced dataset of this Graph,
	 * by calculating the dataset on the spot.  Use this if you want to retrieve the associated
	 * ExportData without generating the actual graph, or if you wish to re-calculate the
	 * associated dataset for a different number of rows.
	 *
	 * @param maxRows Ballpark figure 'maximum number of rows' that the dataset can contain.
	 * 				  Note that this is not an absolute maximum and can be overruled in some cases.
	 * @return ExportData object containing the reduced dataset.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 * @throws IOException Thrown in case of I/O error
	 */
	public ExportData fetchExportData( int maxRows ) throws RrdException, IOException {
		return grapher.fetch( maxRows );
	}

	// ================================================================
	// -- Private methods
	// ================================================================
	/**
	 * This method checks if the graph should be generated.  This would be the case if the requested
	 * image file does not yet exist, or (in case the generation is set to be lazy) the last modified
	 * timestamp of the image file is before the last updated timestamp of the used datasources.
	 * @param imgFile Image file to check against.
	 * @return True if graph generation should be done, false if not.
	 * @throws IOException Thrown in case of I/O error.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 */
	private boolean shouldGenerate( File imgFile ) throws RrdException, IOException
	{
		if ( !imgFile.exists() )
			return true;

		return grapher.shouldGenerate( imgFile.lastModified() );
	}

	private BufferedImage getBufferedImage(int width, int height, int colorType) throws RrdException, IOException
	{
		// Always regenerate graph
		if ( useImageSize )
			img = grapher.createImageGlobal( width, height, colorType );
		else
			img = grapher.createImage( width, height, colorType );
		
		return img;
	}
//--------------------------------------------------------------------------------
public void writeGifToStream( OutputStream out )
throws RrdException, IOException
{
BufferedImage image = getBufferedImage( 0, 0, BufferedImage.TYPE_BYTE_INDEXED );
	
GifEncoder gifEncoder 			= new GifEncoder( image );
gifEncoder.encode( out );
}
//--------------------------------------------------------------------------------

}
