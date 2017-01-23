package org.murillo.sfu.sdp;

import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class ICEInfo
{
	@XmlElement
	String ufrag;
	@XmlElement
	String pwd;

	public static ICEInfo Generate()
	{
		//Create ICE info for media
		ICEInfo info = new ICEInfo();
		 //Get random
		SecureRandom random = new SecureRandom();
		//Create key bytes
		byte[] frag = new byte[8];
		byte[] pwd = new byte[22];
		//Generate them
		random.nextBytes(frag);
		random.nextBytes(pwd);
		//Create ramdom pwd
		info.ufrag = DatatypeConverter.printHexBinary(frag);
		info.pwd   =  DatatypeConverter.printHexBinary(pwd);
		//return it
		return info;
	}

	private ICEInfo() {

	}

	public ICEInfo(String ufrag, String pwd) {
		this.ufrag = ufrag;
		this.pwd = pwd;
	}

	public String getUfrag() {
		return ufrag;
	}

	public String getPwd() {
		return pwd;
	}
	
	
}
