/* 
 *  Copyright (C) 2000 - 2014 TagServlet Ltd
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
 *  http://openbd.org/
 *  
 *  $Id: MongoObjectId.java 2426 2014-03-30 18:53:18Z alan $
 */
package com.bluedragon.mongo;

import org.bson.types.ObjectId;

import com.naryx.tagfusion.cfm.engine.cfArgStructData;
import com.naryx.tagfusion.cfm.engine.cfData;
import com.naryx.tagfusion.cfm.engine.cfJavaObjectData;
import com.naryx.tagfusion.cfm.engine.cfSession;
import com.naryx.tagfusion.cfm.engine.cfmRunTimeException;

public class MongoObjectId extends MongoDatabaseList {
	private static final long serialVersionUID = 1L;

	public MongoObjectId(){  min = 1; max = 1; setNamedParams( new String[]{ "_id" } ); }
  
	public String[] getParamInfo(){
		return new String[]{
			"The string to convert to a Mongo ObjectId() instance"
		};
	}
	
	
	public java.util.Map getInfo(){
		return makeInfo(
				"mongo", 
				"Creates a ObjectID function for use with Mongo generated ID's", 
				ReturnType.OBJECT );
	}
	
	
	public cfData execute(cfSession _session, cfArgStructData argStruct ) throws cfmRunTimeException {
		cfData	data	= getNamedParam(argStruct, "_id", null );
		if ( data == null )
			throwException(_session, "please specify a _id");
		
		String id;
		if ( data.getDataType() == cfData.CFJAVAOBJECTDATA ){
			id	= ((cfJavaObjectData)data).getInstance().toString();
		}else{
			id	= data.getString();
		}

		return new cfJavaObjectData( new ObjectId(id) );
	}
}