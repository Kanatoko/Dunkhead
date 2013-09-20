package net.jumperz.app.dunkhead;

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

public class MReduce
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

	//rrd
private long step = DEFAULT_STEP;
private long heartbeat = DEFAULT_HEARTBEAT;
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

try
	{
	if( confMap.containsKey( "rrd" ) )
		{
		Map rrdMap = ( Map )confMap.get( "rrd" );
		if( rrdMap.containsKey( "step" ) )
			{
			step = Long.parseLong( rrdMap.get( "step" ) + "" );
			}
		if( rrdMap.containsKey( "heartbeat" ) )
			{
			heartbeat = Long.parseLong( rrdMap.get( "heartbeat" ) + "" );
			}		
		}
	}
catch( Exception e )
	{
	LOG.warn( e );
	}
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

RrdDb db = null;
long lastDataTimePoint = 0;
long timeOfFirstRecord = 0;
long totalCount = 0;
long threshold = Long.MIN_VALUE;

Connection conn = null;
try
	{
	conn = Util.putValuesToDatabase( values, reporter, false, step );
	}
catch( Exception e )
	{
	LOG.warn( e );
	return;
	}

try
	{
	String queryString = "";
	if( key.toString().endsWith( TYPE_COUNT ) )
		{
		queryString = "select sum( count ), t from data group by t order by t asc;";
		}
	else if( key.toString().endsWith( TYPE_AVERAGE ) )
		{
		queryString = "select sum( count * value ) / sum( count ), t from data group by t order by t asc;";
		}
	else if( key.toString().endsWith( TYPE_MIN ) )
		{
		queryString = "select min( value ), t from data group by t order by t asc";
		}
	else if( key.toString().endsWith( TYPE_MAX ) )
		{
		queryString = "select max( value ), t from data group by t order by t asc";
		}
	
	{
	final PreparedStatement ps = conn.prepareStatement( queryString  );
	final ResultSet rs = ps.executeQuery();
	while( rs.next() )
		{
		final double updateValue = rs.getDouble( 1 );
		final long tp = rs.getLong( 2 );
		lastDataTimePoint = tp;
		
		if( timeOfFirstRecord == 0 )
			{
			timeOfFirstRecord = tp;
			}
		if( db == null )
			{
			try
				{
				db = initRrd( key.toString(), Util.getTimePoint( tp, step ) - heartbeat );
				}
			catch( IOException e )
				{
				LOG.warn( e );
				e.printStackTrace();
				return;
				}
			}
		
		try
			{
			update( db, tp, updateValue );
			}
		catch( Exception e )
			{
			e.printStackTrace();
			LOG.warn( e );
			}
		}
	ps.close();
	rs.close();
	}
	
	if( confMap.containsKey( "threshold" ) )
		{
		try
			{
			threshold = Long.parseLong( confMap.get( "threshold" ) + "" );
			}
		catch( Exception e )
			{
			LOG.info( e );
			}
		
		final PreparedStatement ps = conn.prepareStatement( "select sum( count ) from data" );
		final ResultSet rs = ps.executeQuery();
		if( rs.next() )
			{
			totalCount = rs.getLong( 1 );
			LOG.info( key.toString() + ":totalCount:" + totalCount );
			}
		ps.close();
		rs.close();
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



	//draw graph
if( totalCount > threshold )
	{
	try
		{
		final byte[] graphBytes = getGraphBytes( db.getPath(), ( ( lastDataTimePoint + ( 1000 * step ) ) - timeOfFirstRecord ) / 1000 , key.toString(), lastDataTimePoint + ( 1000 * step ) );
		outputCollector.collect( new Text( toFileName( key.toString() ) + ".gif" ), new BytesWritable( graphBytes ) );
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
keyStr = keyStr.replaceAll( "/" , "-" );
keyStr = keyStr.replaceAll( ":" , "-" );
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
public RrdDb initRrd( String keyStr, long startTime )
throws IOException
{
final File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
final String fileName = baseDir.getAbsolutePath() + "/" + System.currentTimeMillis() + "_" + ( new Random() ).nextInt( 1000 ) + ".rrd";
final File rrdFile = new File( fileName );
if( rrdFile.exists() )
	{
	rrdFile.delete();
	}
RrdDb db = MRrdUtil.createStandardRrdFile( startTime , "GAUGE", fileName, "ds", step, heartbeat );
( new File( fileName ) ).deleteOnExit();
return db;
}
//--------------------------------------------------------------------------------
}
