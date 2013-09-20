package net.jumperz.app.dunkhead;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import java.sql.*;
import net.arnx.jsonic.JSON;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;

public class Util
{
private static final Log LOG = LogFactory.getLog( Util.class );
//--------------------------------------------------------------------------------
public static Connection putValuesToDatabase( Iterator<LongLongDoubleWritable> values, Reporter reporter, boolean inmemory, long step )
throws Exception
{
	//put all values to H2
final File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );

Class.forName( "org.h2.Driver" );

Connection conn = null;
if( inmemory )
	{
	conn = DriverManager.getConnection( "jdbc:h2:mem:" );
	}
else
	{
	conn = DriverManager.getConnection(
		"jdbc:h2:" + baseDir.getAbsolutePath() + "/" + System.currentTimeMillis() + "_" + ( new Random() ).nextInt( 10000 )
		+ ";LOG=0;CACHE_SIZE=100000;LOCK_MODE=0;UNDO_LOG=0"
		, "sa", "sa" );
	}

conn.prepareStatement( "create table if not exists data ( t long, count long, value double )" ).executeUpdate();
conn.prepareStatement( "create index if not exists t_index_a on data( t )" ).executeUpdate();
//MSqlUtil.executeUpdate( conn, "create index if not exists t_index_d on data( t desc )" );

PreparedStatement ps = conn.prepareStatement( "insert into data values( ?, ?, ? )" );
int index = 0;
while( values.hasNext() )
	{
	LongLongDoubleWritable value = values.next();
	final long time = value.getTime();
	final long count = value.getCount();
	final double eachValue = value.getValue();
	ps.setLong( 1, getTimePoint( time, step ) );
	ps.setLong( 2, count );
	ps.setDouble( 3, eachValue );
	ps.addBatch();
	
	++index;
	if( ( index % 10000 ) == 0 )
		{
		ps.executeBatch();
		reporter.progress();
		LOG.info( index + " record inserted." );
		}
	}
ps.executeBatch();

return conn;
}
//--------------------------------------------------------------------------------
public static long getTimePoint( long time, long step )
{
long mod = time % ( 1000 * step );
return time - mod;
}
//--------------------------------------------------------------------------------
public static Path[] getRecursivePaths(FileSystem fs, String basePath) 
  throws IOException, URISyntaxException {
    List<Path> result = new ArrayList<Path>();
    basePath = fs.getUri() + basePath;
    FileStatus[] listStatus = fs.globStatus(new Path(basePath+"/*"));
    for (FileStatus fstat : listStatus) {
      readSubDirectory(fstat, basePath, fs, result);
    }
    return (Path[]) result.toArray(new Path[result.size()]);  
}
//--------------------------------------------------------------------------------
private static void readSubDirectory(FileStatus fileStatus, String basePath,
  FileSystem fs, List<Path> paths) throws IOException, URISyntaxException {
  if (!fileStatus.isDir()) {
   paths.add(fileStatus.getPath());
  }
  else {
    String subPath = fileStatus.getPath().toString();
    FileStatus[] listStatus = fs.globStatus(new Path(subPath + "/*"));
    if (listStatus.length == 0) {
      paths.add(fileStatus.getPath());
    }
    for (FileStatus fst : listStatus) {
      readSubDirectory(fst, subPath, fs, paths);
    }
  }
}
//--------------------------------------------------------------------------------
}