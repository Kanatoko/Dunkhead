package net.jumperz.app.dunkhead;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.util.Progressable;

public class MFileOutputFormat< K, V >
extends FileOutputFormat< K, V >
{
//--------------------------------------------------------------------------------
@Override
public RecordWriter<K, V> getRecordWriter( FileSystem ignored, JobConf jobConf, String partName, Progressable progress )
throws IOException
{
/*
Path file = FileOutputFormat.getTaskOutputPath( jobConf, partName );
FileSystem fs = file.getFileSystem( jobConf );
FSDataOutputStream fileOut = fs.create( file, progress );
*/

return new MRecordWriter<K, V>( jobConf, progress );
}
//--------------------------------------------------------------------------------
}