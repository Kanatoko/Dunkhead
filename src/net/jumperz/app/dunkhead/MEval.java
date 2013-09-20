package net.jumperz.app.dunkhead;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.text.*;
import java.net.*;

public class MEval
{
//--------------------------------------------------------------------------------
public static void main( String[] args )
throws Exception
{
List< String > l = new ArrayList< String >();
String code = "";
eval( l, code );
}
//--------------------------------------------------------------------------------
public static double eval( List<String> values, String code )
throws Exception
{
/* code example
$1 + $2
$1 * 100
( $2 + $3 ) * 100 - $1
*/

final File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
String now = System.currentTimeMillis() + "";
final String fileName = baseDir.getAbsolutePath() + "/tmp" + now + ".java";
OutputStream out = new FileOutputStream( fileName );
out.write( ( "public class tmp" + now + "{}" ).getBytes( "US-ASCII" ) );
out.close();



return 0;
}
//--------------------------------------------------------------------------------
}