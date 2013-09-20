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
//import net.jumperz.sql.*;

public class MCombiner
extends MapReduceBase implements
Reducer<Text, LongLongDoubleWritable, Text, LongLongDoubleWritable>, MConstants
{

private static final Log LOG = LogFactory.getLog( MCombiner.class );

private String confJsonStr;
private Map confMap;

	//rrd
private long step = DEFAULT_STEP;
private long heartbeat = DEFAULT_HEARTBEAT;
//--------------------------------------------------------------------------------
public void configure( JobConf job )
{
confJsonStr = job.get( "confJsonStr" );
if( confJsonStr == null )
	{
	LOG.info( "confJsonStr is null" );
	return;
	}
LOG.debug( confJsonStr );
confMap = JSON.decode( confJsonStr );
//LOG.debug( JSON.encode( confMap, true ) );

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
public void reduce( Text key, Iterator<LongLongDoubleWritable> values, OutputCollector<Text, LongLongDoubleWritable> outputCollector, Reporter reporter )
throws IOException
{
//LOG.info( key.toString() );
Connection conn = null;
try
	{
	if( confMap.containsKey( "combiner.inmemory" ) &&  confMap.get( "combiner.inmemory" ).equals( Boolean.TRUE ) )
		{
		conn = Util.putValuesToDatabase( values, reporter, true, step );
		}
	else
		{
		conn = Util.putValuesToDatabase( values, reporter, false, step );	
		}
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
		queryString = "select sum( count ), sum( count),  t from data group by t order by t asc;";
		}
	else if( key.toString().endsWith( TYPE_AVERAGE ) )
		{
		queryString = "select sum( count * value ) / sum( count ), sum( count), t from data group by t order by t asc;";
		}
	else if( key.toString().endsWith( TYPE_MIN ) )
		{
		queryString = "select min( value ), sum( count ), t from data group by t order by t asc";
		}
	else if( key.toString().endsWith( TYPE_MAX ) )
		{
		queryString = "select max( value ), sum( count ), t from data group by t order by t asc";
		}
	
	PreparedStatement ps = conn.prepareStatement( queryString  );
	ResultSet rs = ps.executeQuery();
	while( rs.next() )
		{
		final double updateValue = rs.getDouble( 1 );
		final long count = rs.getLong( 2 );
		final long tp = rs.getLong( 3 );
		try
			{
			//update( db, tp, updateValue );
			outputCollector.collect( key, new LongLongDoubleWritable( tp, count, updateValue ) );
			}
		catch( Exception e )
			{
			e.printStackTrace();
			LOG.warn( e );
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
}
//--------------------------------------------------------------------------------
}
