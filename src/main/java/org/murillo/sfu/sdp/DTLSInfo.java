package org.murillo.sfu.sdp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.murillo.MediaServer.Codecs.Setup;

@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class DTLSInfo
{
	@XmlElement
	Setup setup;
	@XmlElement
	String hash;
	@XmlElement
	String fingerprint;

	public DTLSInfo(Setup setup,String hash, String fingerprint) {
   		this.setup = setup;
		this.hash = hash;
		this.fingerprint = fingerprint;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public String getHash() {
		return hash;
	}

	public Setup getSetup() {
		return setup;
	}

	public void setSetup(Setup setup) {
		this.setup = setup;
	}
	
	
}