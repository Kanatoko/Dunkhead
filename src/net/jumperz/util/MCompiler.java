package net.jumperz.util;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

public class MCompiler
{
public static final String CLASSNAME = "_CLASSNAME_";
// --------------------------------------------------------------------------------
public static boolean available()
{
String javaHome = System.getProperty( "java.home" );
File tools = new File( javaHome + "/../lib/tools.jar" );
if( !tools.exists() )
	{
	return false;
	}
return true;
}
// --------------------------------------------------------------------------------
public int compile( String[] args )
throws MCompilationException
{
try
	{
	String javaHome = System.getProperty( "java.home" );
	File tools = new File( javaHome + "/../lib/tools.jar" );
	String toolsFileName = tools.getCanonicalPath();

	URL toolsURL = new URL( "file:" + toolsFileName );
	URLClassLoader cl = new URLClassLoader( new URL[]{ toolsURL } );
	Class clazz = cl.loadClass( "com.sun.tools.javac.Main" );
	Method m = clazz.getDeclaredMethod( "compile", new Class[]{ String[].class } );

	//long start = System.currentTimeMillis();
	Object result = m.invoke( null, new Object[]{ args } );
	//System.out.println( System.currentTimeMillis() - start );

	return ( ( Integer )result ).intValue();
	}
catch( Exception e )
	{
	throw new MCompilationException( e.getMessage() );
	}
}
// --------------------------------------------------------------------------------
}