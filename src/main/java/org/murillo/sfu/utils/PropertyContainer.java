/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Sergio
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class PropertyContainer {
	private static final Logger logger = Logger.getLogger(PropertyContainer.class.getName());
	
	@XmlElement
	private final HashMap<String, String> properties = new HashMap<String, String>();
	
	public PropertyContainer () {
	}
	
	public PropertyContainer(HashMap<String, String> properties) {
		this.properties.putAll(properties);
	}
	
	public HashMap<String, String> getProperties() {
		return properties;
	}

	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	public String getProperty(String key) {
		return properties.get(key);
	}
	
	public String getProperty(String key,String defaultValue) {
		//Get value
		String value = properties.get(key);
		//If not found
		if (value==null)
			//Return default
			return defaultValue;
		//Return actual value
		return value;
	}

	public Integer getIntProperty(String key, Integer defaultValue) {
		//Define default value
		Integer value = defaultValue;
		//Try to conver ti
		try {
			value = Integer.parseInt(getProperty(key));
		} catch (Exception e) {
		}
		//return converted or default
		return value;
	}

	public void addProperty(String key, String value) {
		//Add property
		properties.put(key, value);
	}

	public void addProperties(HashMap<String, String> props) {
		//Add all
		properties.putAll(props);
	}

	public boolean addProperties(String properties) {
		try {
			//Create template properties
			Properties props = new Properties();
			//Parse them
			props.load(new ByteArrayInputStream(properties.getBytes()));
			//For each one
			for (Map.Entry entry : props.entrySet())
			{
				//Add them
				addProperty(entry.getKey().toString(), entry.getValue().toString());
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}

	public boolean setProperties(String properties) {
		try {
			//Create template properties
			Properties props = new Properties();
			//Parse them
			props.load(new ByteArrayInputStream(properties.getBytes()));
			//Clear properties
			this.properties.clear();
			//For each one
			for (Map.Entry entry : props.entrySet()) 
			{
				//Add them
				addProperty(entry.getKey().toString(), entry.getValue().toString());
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}

	public void setProperties(HashMap<String, String> properties) {
		this.properties.clear();
		this.properties.putAll(properties);
	}
	
	public HashMap<String, String> getChildrenProperties(String base) {
		//Crehate children properties map
		HashMap<String, String> children = new HashMap<String, String>();
		//For each property
		for (Entry<String,String> property : properties.entrySet())
		{
			//If key is from base
			if (property.getKey().startsWith(base+"."))
				//Add property to children without base
				children.put(property.getKey().substring(base.length()+1), property.getValue());
		}
		//return children
		return children;
	}
}
