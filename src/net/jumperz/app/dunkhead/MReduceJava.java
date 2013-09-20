/*package net.jumperz.app.dunkhead;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import net.arnx.jsonic.JSON;
import net.jumperz.ext.org.jrobin.core.RrdDb;
import net.jumperz.ext.org.jrobin.core.RrdException;
import net.jumperz.ext.org.jrobin.core.Sample;
import net.jumperz.ext.org.jrobin.graph.RrdGraph;
import net.jumperz.ext.org.jrobin.graph.RrdGraphDef;
import net.jumperz.util.MRrdUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import java.util.*;
import java.sql.*;
import java.io.*;
//import net.jumperz.sql.*;

public class MReduceJava
extends MapReduceBase implements
Reducer<Text, LongLongDoubleWritable, Text, BytesWritable>, MConstants
{
private static final Color c000000 = new Color( 0x00, 0x00, 0x00 );
private static final Color c222222 = new Color( 0x22, 0x22, 0x22 );
private static final Color c00FF00 = new Color( 0x00, 0xFF, 0x00 );
private static final Color c113311 = new Color( 0x11, 0x33, 0x11 );
private static final Color c777777 = new Color( 0x77, 0x77, 0x77 );
private static final Color c333333 = new Color( 0x33, 0x33, 0x33 );
private static final int IMAGE_HEIGHT = 100;
private static final int IMAGE_WIDTH = 350;

private static final Log LOG = LogFactory.getLog( MReduce.class );
public static boolean test = false;

private String confJsonStr;
private Map confMap;
private OutputStream testOut ;
//--------------------------------------------------------------------------------
public void configure( JobConf job )
{
confJsonStr = job.get( "confJsonStr" );
if( confJsonStr == null )
	{
	LOG.warn( "confJsonStr is null" );
	return;
	}
LOG.debug( confJsonStr );
confMap = JSON.decode( confJsonStr );
}
//--------------------------------------------------------------------------------
private void initTest( String key )
throws IOException
{
final File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
testOut = new FileOutputStream( baseDir.getAbsolutePath() + "/" + key + ".test.txt" );
}
//--------------------------------------------------------------------------------
public void reduce( Text key, Iterator<LongLongDoubleWritable> values, OutputCollector<Text, BytesWritable> outputCollector, Reporter reporter )
throws IOException
{
if( test )
	{
	initTest( key.toString() );
	}

//deLOG.info( "reduce():" + key  );
RrdDb db = null;
long lastDataTimePoint = 0;
long dataTimePoint = 0;
long count = 0;
long timeOfFirstRecord = 0;
long sum = 0;
long totalCount = 0;
double min = Double.MAX_VALUE;
double max = Double.MIN_VALUE;

Connection conn = null;
try
	{
	conn = Util.putValuesToDatabase( values, reporter, false );
	}
catch( Exception e )
	{
	LOG.warn( e );
	return;
	}

try
	{
	PreparedStatement ps = conn.prepareStatement( "select * from data order by t asc" );
	ResultSet rs = ps.executeQuery();
	int index = 0;
	while( rs.next() )
		{
		final long	eachTime	= rs.getLong  ( 1 );
		final long	eachCount	= rs.getLong  ( 2 );
		final double	eachValue	= rs.getDouble( 3 );

		if( timeOfFirstRecord == 0 )
			{
			timeOfFirstRecord = eachTime;
			}
		if( db == null )
			{
			db = initRrd( key.toString(), Util.getTimePoint( eachTime ) - 1000L );
			lastDataTimePoint = Util.getTimePoint( eachTime );
			}
			
		dataTimePoint = Util.getTimePoint( eachTime );
		if( lastDataTimePoint != dataTimePoint )
			{
			try
				{
				double updateValue = 0;				
				if( key.toString().endsWith( TYPE_COUNT ) )
					{
					updateValue = ( double )count;
					}
				else if( key.toString().endsWith( TYPE_AVERAGE ) )
					{
					updateValue = ( double )( sum / count );
					}
				else if( key.toString().endsWith( TYPE_MIN ) )
					{
					updateValue = min;
					}
				else if( key.toString().endsWith( TYPE_MAX ) )
					{
					updateValue = max;
					}
				update( db, lastDataTimePoint, updateValue );
				
					//cleanup variables
				count = 0;
				sum = 0;
				lastDataTimePoint = dataTimePoint;
				min = Double.MAX_VALUE;
				max = Double.MIN_VALUE;
				}
			catch( Exception e )
				{
				e.printStackTrace();
				LOG.warn( e );
				}
			}
		
		if( key.toString().endsWith( TYPE_AVERAGE ) )
			{
			sum += eachValue * eachCount;
			}
		else if(  key.toString().endsWith( TYPE_MIN ) )
			{
			if( eachValue < min )
				{
				min = eachValue;
				}
			}
		else if(  key.toString().endsWith( TYPE_MAX ) )
			{
			if( eachValue > max )
				{
				max = eachValue;
				}
			}
		count += eachCount;
		totalCount += eachCount;
		
		++index;
		if( ( index % 1000 ) == 0 )
			{
			reporter.progress();
			}
		}
	}
catch( SQLException e )
	{
	e.printStackTrace( System.out );
	LOG.warn( e );
	return;
	}
finally
	{
	if( conn != null )
		{
		try
			{
			conn.prepareStatement( "DROP ALL OBJECTS DELETE FILES;" ).executeUpdate();
			conn.close();
			}
		catch( Exception e )
			{
			LOG.info( e );
			}
		}
	}


if( count > 0 )
	{
	//LOG.info( "process remaining data:" + count );
	try
		{
		if( dataTimePoint < lastDataTimePoint )
			{
			throw new RuntimeException( "Not sorted!!" );
			}
		else
			{
			double updateValue = ( double )count;
			if( key.toString().endsWith( TYPE_AVERAGE ) )
				{
				updateValue = ( double )( sum / count );
				}
			update( db, lastDataTimePoint, updateValue );
			count = 0;
			sum = 0;
			lastDataTimePoint = dataTimePoint;
			}
		}
	catch( Exception e )
		{
		e.printStackTrace();
		LOG.warn( e );
		}	
	}

int threshold = 100;
if( confMap.containsKey( "threshold" ) )
	{
	try
		{
		threshold = Integer.parseInt( ( String )confMap.get( "threshold" ) );
		}
	catch( Exception e )
		{
		LOG.info( e );
		}
	}

	//draw graph
if( totalCount > threshold )
	{
	try
		{
		String keyStr = toFileName( key.toString() );
		final byte[] graphBytes = getGraphBytes( db.getPath(), ( ( dataTimePoint + ( 1000 * 60 * 5 ) ) - timeOfFirstRecord ) / 1000 , keyStr, dataTimePoint + ( 1000 * 60 * 5 ) );
		outputCollector.collect( new Text( keyStr.replaceAll( ":", "-" ) + ".gif" ), new BytesWritable( graphBytes ) );
		db = null;
		}
	catch( RrdException e )
		{
		throw new IOException( e );
		}
	}
else
	{
	LOG.info( key.toString() + " is ignored. Record count : " + totalCount );
	}

if( test )
	{
	testOut.flush();
	testOut.close();
	}
}
//--------------------------------------------------------------------------------
public static String toFileName( String keyStr )
{
if( keyStr.length() > 70 )
	{
	keyStr = keyStr.substring( 0, 50 ) + "_" + System.currentTimeMillis() + "_" + ( new Random() ).nextInt( 1000 );
	}
return keyStr;
}
//--------------------------------------------------------------------------------
public static final byte[] getGraphBytes( String rrdFileName, long period, String title, long end )
throws IOException, RrdException
{
	//create graph
RrdGraphDef gd = new RrdGraphDef();
end = end / 1000;
long start = end - period;
gd.setTimePeriod( start, end );
	
String graphSourceName = "graphSource";
	
gd.datasource( graphSourceName, rrdFileName, "ds", "AVERAGE" );
Font verdana = Font.decode( "Verdana-BOLD-11" );
gd.setTitle( title );
gd.setTitleFont( verdana );

gd.line( graphSourceName, c000000, null, 1 );

gd.setTitleFontColor( c00FF00 );
gd.setMinorGridY( true );
//gd.setGridY( true );
gd.setDefaultFontColor( c00FF00 );
gd.setAntiAliasing( false );
gd.setBackColor( c000000 );
gd.setCanvasColor( c000000 );
gd.setShowSignature( false );
gd.setImageBorder( null, 0 );
gd.setMinorGridColor( c333333 );
gd.setMajorGridColor( c777777 );
gd.setAxisColor( c333333 );
gd.setFrameColor( c333333 );

gd.area( graphSourceName, c113311, null );
gd.line( graphSourceName, c00FF00, null, 1 );

return ( new RrdGraph( gd ) ).getGIFBytes();
}
//--------------------------------------------------------------------------------
public void update( RrdDb db, long time, double value )
throws Exception
{
if( test )
	{
	testOut.write( ( new Date( time ) + "\t" + value ).getBytes() );
	testOut.write( 0x0A );
	}
//LOG.info( new Date( time ) + ":" + value );
time = ( time + 500L ) / 1000L;
Sample sample = db.createSample( time );
sample.setValue( 0, value );
sample.update();
}
//--------------------------------------------------------------------------------
public static RrdDb initRrd( String keyStr, long startTime )
throws IOException
{
keyStr = toFileName( keyStr );
final File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
final String fileName = baseDir.getAbsolutePath() + "/" + keyStr + ".rrd";
final File rrdFile = new File( fileName );
if( rrdFile.exists() )
	{
	rrdFile.delete();
	}
RrdDb db = MRrdUtil.createStandardRrdFile( startTime - ( 1000 * 5 ), "GAUGE", fileName, "ds" );
( new File( fileName ) ).deleteOnExit();
return db;
}
//--------------------------------------------------------------------------------
}
*/