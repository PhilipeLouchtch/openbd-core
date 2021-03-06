/* 
 *  Copyright (C) 2000 - 2010 TagServlet Ltd
 *
 *  This file is part of Open BlueDragon (OpenBD) CFML Server Engine.
 *  
 *  OpenBD is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  Free Software Foundation,version 3.
 *  
 *  OpenBD is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with OpenBD.  If not, see http://www.gnu.org/licenses/
 *  
 *  Additional permission under GNU GPL version 3 section 7
 *  
 *  If you modify this Program, or any covered work, by linking or combining 
 *  it with any of the JARS listed in the README.txt (or a modified version of 
 *  (that library), containing parts covered by the terms of that JAR, the 
 *  licensors of this Program grant you additional permission to convey the 
 *  resulting work. 
 *  README.txt @ http://www.openbluedragon.org/license/README.txt
 *  
 *  http://www.openbluedragon.org/
 */

package com.naryx.tagfusion.cfm.parser.script;

import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;
import com.naryx.tagfusion.cfm.parser.CFContext;
import com.naryx.tagfusion.cfm.parser.CFExpression;
import com.naryx.tagfusion.cfm.parser.CFIdentifier;
import com.naryx.tagfusion.cfm.parser.CFLiteral;

public class CFFunctionParameter implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;

	private String name;       // the name of the parameter
	private boolean required;  // whether the parameter is a required one
	private String type;       // the expected type of the parameter (for validation), if specified  
	private CFExpression defaultExp;  // the default value to give the parameter
	
	public CFFunctionParameter( String name ){
		this.name				= name;
		this.required		= true;
		this.type				= null;
		this.defaultExp	= null;
	}
	
	public CFFunctionParameter( CFIdentifier t, boolean _required, String _type, CFExpression _default ){
		name = t.getName();
		required = _required;
		type = _type;
		defaultExp = _default;
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isRequired(){
		return required;
	}
	
	public boolean isDefaulted(){
		return defaultExp != null;
	}

	public boolean isFormallyTyped(){
		return type != null;
	}

	public cfData getDefaultValue( CFContext _context ) throws cfmRunTimeException{
		return defaultExp.Eval( _context );
	}

	public String getDefaultAsString() {
		if ( defaultExp instanceof CFLiteral ){
			return defaultExp.Decompile(0);
		}else{
			return "[runtime expression]";
		}
	}

	public String getType(){
		return type;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if ( required ){
			sb.append( "required " );
		}
		sb.append( name );
		if ( defaultExp != null ){
			sb.append( '=' );
			sb.append( defaultExp.Decompile(0) );
		}
		
		return sb.toString();
	}
}
