/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 *
 * @author Sergio
 */
public class JSON {
	
	public static String Stringify(Object object) throws JsonProcessingException {
		JSON json = new JSON();
		return json.stringify(object);
	}

	private ObjectMapper mapper;
	
	JSON(){
		//Create json mapper
		mapper = new ObjectMapper();
		//Set JAXB annotatiion inspector
		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
	}
	
	public String stringify(Object object) throws JsonProcessingException {
		//Convert to JSON
		return mapper.writeValueAsString(object);
	}
}
