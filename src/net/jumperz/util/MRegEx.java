package net.jumperz.util;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.*;
import java.util.*;

public class MRegEx
{
private static Map patternMap = new HashMap();
public static final String WORD_HEAD = "(?:\\A|[^a-zA-Z]{1})";
public static final String WORD_TAIL = "(?:$|[^a-zA-Z]{1})";
public static final String WORD_BETWEEN = "(?:\\W+?.*\\W+?|\\W+?)";

private static final Map evalDoubleCache = new HashMap();
private static final String magic = "::<991929>::";

//--------------------------------------------------------------------------------
public static double evalDouble( String patternStr, String eval, String input )
{
Class clazz = getEvalDoubletClass( patternStr, eval );
if( clazz == null )
	{
		//error
	return 0;
	}

try
	{
	Object evaluator = clazz.newInstance();
	Pattern pattern = Pattern.compile( patternStr );
	Matcher matcher = pattern.matcher( input );
	matcher.find();
	evaluator.equals( matcher );
	return Double.parseDouble( evaluator.toString() );
	
	/*
	MEvalDoubleBase edb = ( MEvalDoubleBase )( clazz.newInstance() );
	Pattern pattern = Pattern.compile( patternStr );
	Matcher matcher = pattern.matcher( input );
	matcher.find();
	return edb.eval( matcher );
	*/
	}
catch( Exception e )
	{
	e.printStackTrace();
	return 0;
	}
}
//--------------------------------------------------------------------------------
public static Class getEvalDoubletClass( String patternStr, String eval )
{
final String key = patternStr + magic + eval;

synchronized( evalDoubleCache )
	{
	Class clazz = ( Class )evalDoubleCache.get( key );
	if( clazz != null )
		{
		//System.out.println( key + "class found." );
		return clazz;
		}
	}

try
	{
	final File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
	
	/*{
	( new File( baseDir.getAbsolutePath() + "/net/jumperz/util/" ) ).mkdirs();
	final String baseClassFileName = baseDir.getAbsolutePath() + "/net/jumperz/util/MEvalDoubleBase.class";
	FileOutputStream out = new FileOutputStream( baseClassFileName );
	final byte[] buf = new byte[ 1024 * 10 ];
	final URL url = MRegEx.class.getClassLoader().getResource( "net/jumperz/util/MEvalDoubleBase.class" );
	final int size = url.openStream().read( buf );
	out.write( buf, 0, size );
	out.close();
	}*/
	
	String now = System.currentTimeMillis() + "";
	final String javaFileName = baseDir.getAbsolutePath() + "/tmp" + now + ".java";
	
	StringBuffer buf = new StringBuffer( 1024 );
	buf.append( "import java.util.regex.*;\n" );
	buf.append( "public class tmp" + now + " {" );
	buf.append( "private String result = null;\n" );
	buf.append( "public boolean equals( Object o ) {\n" );
	buf.append( "Matcher matcher = ( Matcher )o;" );
	buf.append( "try{" );
	buf.append( "result = (" );
	buf.append( eval.replaceAll( "\\$([0-9]+)", "Double.parseDouble( matcher.group($1) )" ) );
	buf.append( ") + \"\";\n" );
	buf.append( "return false;" );
	buf.append( "}catch( Exception ignored ){}return false;" );
	buf.append( "}\n" );
	buf.append( "public String toString() {\n" );
	buf.append( "return result;" );
	buf.append( "}\n}" );
	
	File javaFile = new File( javaFileName );
	//System.out.println( "========" + javaFile.getAbsolutePath() );
	javaFile.deleteOnExit();
	OutputStream out = new FileOutputStream( javaFile );
	out.write( buf.toString().getBytes( "US-ASCII" ) );
	out.close();
	
	MCompiler compiler = new MCompiler();
	int result = compiler.compile( new String[]{ "-cp", ".", javaFileName } );
	if( result != 0 )
		{
		throw new MCompilationException( "compile error" );
		}
	
	URL tmpDirUrl = new URL( "file:" + baseDir + "/" );
	
	URLClassLoader cl = new URLClassLoader( new URL[]{ tmpDirUrl } );
	Class clazz = cl.loadClass( "tmp" + now );
	
	synchronized( evalDoubleCache )
		{
		evalDoubleCache.put( key, clazz );
		}
	
	File classFile = new File( baseDir.getAbsolutePath() + "/tmp" + now + ".class" );
	classFile.deleteOnExit();
	
	return clazz;
	}
catch( Exception e )
	{
	e.printStackTrace();
	return null;
	}
}
//--------------------------------------------------------------------------------
public static String replaceAllIgnoreCase( String target, String regex, String to  )
{
return Pattern.compile( regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL ).matcher( target ).replaceAll( to );
/*
int len = regex.length();
if( len == 0 )
	{
	return target;
	}
StringBuffer buf = new StringBuffer( target.length() );
while( true )
	{
	String matchStr = getMatch( regex, target );
	len = matchStr.length();
	if( len == 0 )
		{
		break;
		}
	int pos = target.indexOf( matchStr );
	buf.append( target.substring( 0, pos ) );
	buf.append( to );
	target = target.substring( pos + len );
	}
buf.append( target );
System.out.println( buf.toString() );
return buf.toString();
*/
}
//--------------------------------------------------------------------------------
public static String replaceFirst( String target, String regex, String to  )
{
String matchStr = getMatch( regex, target );
if( matchStr.length() == 0 )
	{
	return target;
	}
else
	{
	int index = target.indexOf( matchStr );
	StringBuffer buf = new StringBuffer( target.length() );
	buf.append( target.substring( 0, index ) );
	buf.append( to );
	buf.append( target.substring( index + matchStr.length() ) );
	//System.out.println( buf.toString() );
	return buf.toString();
	}
}
//------------------------------------------------------------------------------------------
public static String getMatch( String patternStr, String target )
{
Pattern pattern = Pattern.compile( patternStr, Pattern.DOTALL );
Matcher matcher = pattern.matcher( target );
if( matcher.find() )
	{
	if( matcher.groupCount() > 0 )
		{
		return matcher.group( 1 );
		}
	else
		{
		return target.substring( matcher.start(), matcher.end() );
		}
	}
else
	{
	return "";
	}
}
//--------------------------------------------------------------------------------
private static int indexOf( String target, Matcher matcher )
{
if( matcher.find() )
	{
	return target.indexOf( target.substring( matcher.start(), matcher.end() ) );
	}
else
	{
	return -1;
	}
}
//--------------------------------------------------------------------------------
public static int indexOfIgnoreCase( String target, String regex )
{
Pattern pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
Matcher matcher = pattern.matcher( target );
return indexOf( target, matcher );
}
//--------------------------------------------------------------------------------
public static int indexOf( String target, String regex )
{
Pattern pattern = Pattern.compile( regex, Pattern.DOTALL );
Matcher matcher = pattern.matcher( target );
return indexOf( target, matcher );
}
//--------------------------------------------------------------------------------
public static String getMatchIgnoreCase( String patternStr, String target )
{
Pattern pattern = Pattern.compile( patternStr, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
Matcher matcher = pattern.matcher( target );
if( matcher.find() )
	{
	if( matcher.groupCount() > 0 )
		{
		return matcher.group( 1 );
		}
	else
		{
		return target.substring( matcher.start(), matcher.end() );
		}
	}
else
	{
	return "";
	}
}
// --------------------------------------------------------------------------------
public static boolean containsIgnoreCase( String target, String regex )
{
Pattern pattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
Matcher matcher = pattern.matcher( target );
return matcher.find();
}
//--------------------------------------------------------------------------------
public static boolean contains( String target, String patternStr )
{
Pattern pattern = Pattern.compile( patternStr, Pattern.DOTALL );
Matcher matcher = pattern.matcher( target );
return matcher.find();
}
//------------------------------------------------------------------------------------------
public static String[] split( String patternStr, String target )
{
return Pattern.compile( patternStr ).split( target, -1 );
}
//------------------------------------------------------------------------------------------
}
