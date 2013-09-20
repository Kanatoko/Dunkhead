package net.jumperz.util;

import java.util.regex.*;

public class MEvalDoubleBaseExample
extends MEvalDoubleBase
{
//--------------------------------------------------------------------------------
public double eval( Matcher matcher ) throws Exception
{
return Double.parseDouble( matcher.group( 1 ) + matcher.group( 2 ) );
}
//--------------------------------------------------------------------------------
}
