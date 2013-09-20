package test;

import net.jumperz.util.*;
import java.util.regex.*;
import net.jumperz.app.dunkhead.*;
import org.apache.commons.logging.*;

public class MTest
{
private static final Log LOG = LogFactory.getLog( MTest.class );
//--------------------------------------------------------------------------------
public static void main( String[] args )
throws Exception
{
test1();
test2();

LOG.info( "OK" );
}
//--------------------------------------------------------------------------------
private static void test2()
throws Exception
{
if( MRegEx.evalDouble( "([0-9]+) [a-z]+ ([0-9]+)", "$1 + $2", "123 foobar 444" ) != 567 ){ ex(); }
if( MRegEx.evalDouble( "([0-9]+) [a-z]+ ([0-9]+)", "$2 - $1", "123 foobar 444" ) != 321 ){ ex(); }
}
//--------------------------------------------------------------------------------
private static void test1()
throws Exception
{

{
Pattern pattern = Pattern.compile( "([a-z]+)" );
Matcher matcher = pattern.matcher( "123 foo 456" );
matcher.find();
if( !MMap.getNameFromMatcher( "$1", matcher ).equals( "foo" ) ){ ex(); }
}

{
Pattern pattern = Pattern.compile( "([a-z]+) ([a-z]+) [0-9]+" );
Matcher matcher = pattern.matcher( "123 foo bar 456" );
matcher.find();
if( !MMap.getNameFromMatcher( "$2", matcher ).equals( "bar" ) ){ ex(); }
}

{
Pattern pattern = Pattern.compile( "([a-z]+) ([a-z]+) [0-9]+" );
Matcher matcher = pattern.matcher( "123 foo bar 456" );
matcher.find();
if( !MMap.getNameFromMatcher( "FOO:$1/$2", matcher ).equals( "FOO:foo/bar" ) ){ ex(); }
}

}
//--------------------------------------------------------------------------------
public static void ex()
throws Exception
{
throw new Exception();
}
//--------------------------------------------------------------------------------
}