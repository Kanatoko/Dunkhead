package net.jumperz.app.dunkhead;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

import net.arnx.jsonic.JSON;
import net.jumperz.util.MEvalDoubleBase;
import net.jumperz.util.MRegEx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

public class MMap
extends MapReduceBase
implements Mapper<LongWritable, Text, Text, LongLongDoubleWritable>, MConstants
{
private static final Log LOG = LogFactory.getLog( MMap.class );
private String confJsonStr;
private Map confMap;
private DateFormat df;
private String dfStr;
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

dfStr =( ( Map )confMap.get( "datetime" ) ).get( "format" ) + "";

if( dfStr.equalsIgnoreCase( DF_UNIX_MILLI ) )
	{
	}
else if( dfStr.equalsIgnoreCase( DF_UNIX_SECOND ) )
	{
	}
else
	{
	df = new SimpleDateFormat( dfStr, Locale.ENGLISH );
	}
}
//--------------------------------------------------------------------------------
public static String getNameFromMatcher( String name, Matcher matcher )
{
try
	{
	while( true )
		{
		String s = MRegEx.getMatch( "\\$([0-9]+)", name );
		if( s.equals( "" ) )
			{
			return name;
			}
		else
			{
			int index = Integer.parseInt( s );
			name = name.replaceAll( "\\$" + index, matcher.group( index ) );
			}
		}	
	}
catch( Exception e )
	{
	return name;
	}
}
//--------------------------------------------------------------------------------
public void map( LongWritable key, Text value, OutputCollector<Text, LongLongDoubleWritable> output, Reporter reporter )
throws IOException  
{
final String line = value.toString();
final String datetimeRegex = ( String )( ( Map )confMap.get( "datetime" ) ).get( "regex" );
final String datePart = MRegEx.getMatch( datetimeRegex, line );
if( datePart == null || datePart.length() == 0 )
	{
	return;
	}
try
	{
	java.util.Date date = null;
	if( df != null )
		{
		date = df.parse( datePart );
		}
	else
		{
		if( dfStr.equalsIgnoreCase( DF_UNIX_MILLI ) )
			{
			final long  _longDate = Long.parseLong( datePart );
			date = new java.util.Date( _longDate );
			}
		else if( dfStr.equalsIgnoreCase( DF_UNIX_SECOND ) )
			{
			final long  _longDate = ( long )( Double.parseDouble( datePart ) * 1000L );
			date = new java.util.Date( _longDate );
			}
		else
			{
			LOG.debug( "DateFormat error" );
			return;
			}
		}
	final long _longDate = date.getTime();
	final Long _LongDate = new Long( _longDate );
	
			//all
	{
	Text logKey = new Text( "All_" + TYPE_COUNT );
	output.collect( logKey, new LongLongDoubleWritable( _longDate, 1, 1 ) );
	}

	List fields = ( List )confMap.get( "fields" );
	if( fields == null )
		{
		return;
		}
	for( int i = 0; i < fields.size(); ++i )
		{
		final Map field = ( Map )fields.get( i );
		final String name		= ( String )field.get( "name"  );
		final String regex	= ( String )field.get( "regex" );
		final String typeStr	= ( String )field.get( "type"  );
		final String evalStr	= ( String )field.get( "eval"  ); //may be null
		
		if( name  == null
		 || regex == null
		 || typeStr  == null
		  )
			{
			LOG.warn( "Invalid configuration [ " + field + " ] ignored." );
			continue;
			}

		if( evalStr != null )
			{
			final Pattern pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
			final Matcher matcher = pattern.matcher( line );
			matcher.find();
			
			final String symbol = getNameFromMatcher( name, matcher );
			double outputValue = 0;
			try
				{
				final Object evaluator = MRegEx.getEvalDoubletClass( regex, evalStr ).newInstance();
				evaluator.equals( matcher );
				final String resultStr = evaluator.toString();
				if( resultStr == null )
					{
					continue;
					}
				else
					{
					outputValue = Double.parseDouble( resultStr );
					}
				}
			catch( Exception e )
				{
				LOG.info( e );
				continue;
				}
			
			if( typeStr.equalsIgnoreCase( "average" )
			 || typeStr.equalsIgnoreCase( "min" )
			 || typeStr.equalsIgnoreCase( "max" )
			  )
				{
				//ok
				}
			else if( typeStr.equals( "count" ) )
				{
				LOG.debug( "Invalid configuration. 'count' with 'eval'" );
				outputValue = 1;
				}
			final Text logKey = new Text( symbol + "_" + typeStr );
			output.collect( logKey, new LongLongDoubleWritable( _longDate, 1, outputValue ) );
			}
		else
			{
			final Pattern pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
			final Matcher matcher = pattern.matcher( line );
			if( matcher.find() )
				{
				String matchStr = null;
				if( matcher.groupCount() > 0 )
					{
					matchStr = matcher.group( 1 );
					}
				else
					{
					matchStr = line.substring( matcher.start(), matcher.end() );
					}
				
				String symbol = getNameFromMatcher( name, matcher );
				double outputValue = 0;
				if( typeStr.equalsIgnoreCase( "average" )
				 || typeStr.equalsIgnoreCase( "min" )
				 || typeStr.equalsIgnoreCase( "max" )
				  )
					{
					symbol = name;
					outputValue = Double.parseDouble( matchStr );
					}
				else if( typeStr.equalsIgnoreCase( "count" ) )
					{
					outputValue = 1;
					}
				Text logKey = new Text( symbol + "_" + typeStr );
				output.collect( logKey, new LongLongDoubleWritable( _longDate, 1, outputValue ) );
				}			
			}
		}
	}
catch( ParseException e )
	{
	LOG.debug( e );
	}
}
//--------------------------------------------------------------------------------
}
