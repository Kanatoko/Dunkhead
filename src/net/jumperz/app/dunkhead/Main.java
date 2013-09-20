package net.jumperz.app.dunkhead;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import java.net.URI;
import net.arnx.jsonic.JSON;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;

public class Main
{
private static Map<Object, Object> confMap;
private static DateFormat df;

private static final Log LOG = LogFactory.getLog( Main.class );
//--------------------------------------------------------------------------------
public static void main( String[] args )
throws Exception
{
if( args.length < 3 )
	{
	LOG.warn( "Usage: net.jumperz.app.dunkhead.Main input-path output-path conf-path" );
	return;
	}

LOG.info( Arrays.asList( args ) );
LOG.info( "Dunkhead main()" );

JobConf conf = new JobConf( Main.class );
conf.setJobName( "DunkHead" );

parseConf( conf,  args[ 2 ] );

conf.setMapOutputKeyClass( Text.class );
conf.setMapOutputValueClass( LongLongDoubleWritable.class );

conf.setOutputKeyClass(	Text.class );
conf.setOutputValueClass( BytesWritable.class );

conf.setMapperClass(	MMap.class );

if( System.getProperty( "dunkhead.combiner", "true" ).equals( "true" ) )
	{
	conf.setCombinerClass(  MCombiner.class );
	}
if( System.getProperty( "dunkhead.test", "false" ).equals( "true" ) )
	{
	LOG.info( "====== TEST =====" );
	MReduce.test = true;
	}

conf.setReducerClass(	MReduce.class );
//conf.setReducerClass(	MReduceJava.class );

//conf.setNumReduceTasks( 1 );

conf.setInputFormat(	TextInputFormat.class );
//conf.setOutputFormat(	TextOutputFormat.class );
conf.setOutputFormat(	MFileOutputFormat.class );

FileOutputFormat.setOutputPath(	conf, new Path( args[ 1 ] ) );

//FileInputFormat.setInputPaths(	conf, new Path( args[ 0 ] ) );

processInputPath( conf, args[ 0 ] );
JobClient.runJob( conf );
}
//--------------------------------------------------------------------------------
private static void processInputPath( JobConf conf, String inputStr )
throws Exception
{
URI inputUri = new URI( inputStr );
FileSystem fs = FileSystem.get( inputUri, conf );
Path[] inputPaths = Util.getRecursivePaths( fs, inputUri.getPath() );
LOG.info( Arrays.asList( inputPaths ) );
FileInputFormat.setInputPaths( conf, inputPaths );
}
//--------------------------------------------------------------------------------
private static void parseConf( JobConf conf, String confPathStr  )
throws Exception
{
URI confUri = new URI( confPathStr );
FileSystem fs = FileSystem.get( confUri, conf );

StringWriter writer = new StringWriter();
IOUtils.copy( fs.open( new Path( confPathStr ) ), writer, "ISO-8859-1" );
String confJsonStr = writer.toString();
conf.set( "confJsonStr", confJsonStr );
LOG.debug( confUri + " loaded." );
LOG.debug( confJsonStr );
confMap = JSON.decode( confJsonStr );
LOG.debug( "conf parsed to:" );
LOG.debug( JSON.encode( confMap, true ) );

/*
df = new SimpleDateFormat( ( ( Map )confMap.get( "datetime" ) ).get( "format" ) + "" , Locale.ENGLISH );

LOG.info( df.format( new Date() ) );
*/
}
//--------------------------------------------------------------------------------
}
