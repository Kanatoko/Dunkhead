package net.jumperz.app.dunkhead;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;


public class MRecordWriter< K, V > implements RecordWriter<K, V>
{
private OutputStream fileOut;
private JobConf jobConf;
private Progressable progress;

private static final Log LOG = LogFactory.getLog( MRecordWriter.class );

//--------------------------------------------------------------------------------
public MRecordWriter( JobConf jobConf, Progressable progress )
{
LOG.info( "MRecordWriter()" );
this.jobConf = jobConf;
this.progress = progress;
}
//--------------------------------------------------------------------------------
@Override
public void close( Reporter arg0 )
throws IOException
{
}
//--------------------------------------------------------------------------------
@Override
public void write( K key, V value )
throws IOException
{
LOG.info( "MRecordWriter:write():" + key );
Path file = FileOutputFormat.getTaskOutputPath( jobConf, ( ( Text )key ).toString() );
FileSystem fs = file.getFileSystem( jobConf );
fileOut = fs.create( file, progress );
BytesWritable bytes = ( BytesWritable )value;
fileOut.write( bytes.getBytes() );
fileOut.flush();
fileOut.close();
}
//--------------------------------------------------------------------------------
}