/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.sdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.murillo.MediaServer.Codecs;
import org.murillo.sdp.Attribute;
import org.murillo.sdp.ExtMapAttribute;
import org.murillo.sdp.FingerprintAttribute;
import org.murillo.sdp.MediaDescription;
import org.murillo.sdp.ParserException;
import org.murillo.sdp.RTPMapAttribute;
import org.murillo.sdp.SSRCAttribute;
import org.murillo.sdp.GroupAttribute;
import org.murillo.sdp.SSRCGroupAttribute;
import org.murillo.sdp.SessionDescription;
import org.murillo.sfu.utils.JSON;

/**
 *
 * @author Sergio
 */
@XmlType()
@XmlAccessorType(XmlAccessType.NONE)
public class SDPInfo {
	@XmlElement
	private HashMap<String,StreamInfo> streams = new HashMap<>();
	@XmlElement
	private ArrayList<MediaInfo> medias = new ArrayList<>();
	@XmlElement
	private Integer version = 1;
	private MediaInfo audio;
	private MediaInfo video;
	
	public void setVersion(Integer version) {
		this.version = version;
	}
	
	public void addMedia(MediaInfo media){
		//Store media
		medias.add(media);
		//It is an audio one?
		if ("audio".equalsIgnoreCase(media.getType()))
			//Quick access
			audio = media;
		//It is an video one?
		if ("video".equalsIgnoreCase(media.getType()))
			//Quick access
			video = media;
	}
	
	public MediaInfo getMedia(String type){
		for (MediaInfo media : medias)
			if (media.getType().equalsIgnoreCase(type))
				return media;
		return null;
	}

	public List<MediaInfo> getMedias() {
		return medias;
	}
	
	public Integer getVersion() {
		return version;
	}

	public MediaInfo getAudio() {
		return audio;
	}

	public MediaInfo getVideo() {
		return video;
	}
	
	public StreamInfo getStream(String id) {
		return streams.get(id);
	}
	
	public StreamInfo getFirstStream() {
		for (StreamInfo stream : streams.values())
			return stream;
		return null;
	}

	public void addStream(StreamInfo stream) {
		streams.put(stream.getId(), stream);
	}
	
	public void removeStream(StreamInfo stream) {
		streams.remove(stream.getId());
	}
	
	@Override
	public String toString(){
		SessionDescription sdp = new SessionDescription();
		
		String ip = "127.0.0.1";
		
		//Set version
		sdp.setVersion(version);
		//Set origin
		sdp.setOrigin("-", 1L, new Date().getTime(), "IN", "IP4", "127.0.0.1");
		//Set name
		sdp.setSessionName("MediaMixerSession");
		//Set connection info
		sdp.setConnection("IN", "IP4", "0.0.0.0");
		//Set time
		sdp.addTime(0,0);
		//Add ice lite attribute
		sdp.addAttribute("ice-lite");
		//Enable msids
		sdp.addAttribute("msid-semantic","WMS");
		//Bundle
		GroupAttribute bundle = new GroupAttribute("BUNDLE");
		
		//For each media
		for (MediaInfo media : medias)
		{
			//Create new meida description with default values
			MediaDescription md = new MediaDescription(media.getType(),9,"UDP/TLS/RTP/SAVPF");
		
			//Send and receive
			md.addAttribute("sendrecv");

			//Enable rtcp muxing
			md.addAttribute("rtcp-mux");
			
			//Enable rtcp reduced size
			md.addAttribute("rtcp-rsize");
			
			//Enable x-google-flag
			md.addAttribute("x-google-flag","conference");

			//Set media id semantiv
			md.addAttribute("mid",media.getId());
			//Add to bundle
			bundle.addTag(media.getId());

			//For each candidate
			for (CandidateInfo candidate : media.getCandidates())
				//Add host candidate for RTP
				md.addCandidate(candidate.getFundation(),candidate.getComponentId(),candidate.getTransport(),candidate.getPriority(),candidate.getAddress(),candidate.getPort(),candidate.getType());
			
			//Set ICE credentials
			md.addAttribute("ice-ufrag",media.getICE().getUfrag());
			md.addAttribute("ice-pwd",media.getICE().getPwd());
			
			//Add fingerprint attribute
			md.addAttribute(new FingerprintAttribute(media.getDTLS().getHash(), media.getDTLS().getFingerprint()));
			//Add setup atttribute
			md.addAttribute("setup",media.getDTLS().getSetup().valueOf());
			//Add connection attribute
			md.addAttribute("connection","new");
			
			//for each codec
			for(CodecInfo codec : media.getCodecs().values())
			{
				//Append tyè
				md.addFormat(codec.getType());

				
				if ("opus".equalsIgnoreCase(codec.getCodec()))
				{
					//Add rtmpmap
					md.addRTPMapAttribute(codec.getType(), codec.getCodec(), 48000, "2");
				} else {
					//Add rtmpmap
					md.addRTPMapAttribute(codec.getType(), codec.getCodec(), 90000);
					//Add rtcp-fb nack support
					md.addAttribute("rtcp-fb", codec.getType()+" nack pli");
					//Add fir
					md.addAttribute("rtcp-fb", codec.getType()+" ccm fir");
					//Add Remb
					md.addAttribute("rtcp-fb", codec.getType()+" goog-remb");
				}
				//If it has rtx
				if (codec.hasRtx())
				{
					//Add it also
					md.addFormat(codec.getRtx());
					//Add rtmpmap for rtx
					md.addRTPMapAttribute(codec.getRtx(), "rtx", 90000);
					//Add apt
					md.addFormatAttribute(codec.getRtx(),"apt="+codec.getType());
				}
			}
						
			//For each extension
			/*
			for (Entry<String,Integer> pair : extensions.entrySet())
				//Add new extension attribute
				md.addAttribute(new ExtMapAttribute(pair.getValue(), pair.getKey()));
			*/
			
			//add media description
			sdp.addMedia(md);
		}
		
		//Process streams now
		for (StreamInfo stream : streams.values())
		{
			//For each track
			for (TrackInfo track : stream.getTracks().values())
			{
				//Get media
				for (MediaDescription md: sdp.getMedias())
				{
					//If it is same type
					if (md.getMedia().equalsIgnoreCase(track.getMedia()))
					{
						//For each group
						for (SourceGroupInfo group : track.getSourceGroups())
							//Add ssrc group 
							md.addAttribute(new SSRCGroupAttribute(group.getSemantics(), group.getSSRCs()));
						//For each ssrc
						for (Long ssrc : track.getSSRCs())
						{
							//Add ssrc info
							md.addAttribute(new SSRCAttribute(ssrc, "cname"	  ,stream.getId()));
							md.addAttribute(new SSRCAttribute(ssrc, "msid"	  ,stream.getId() + " " + track.getId()));	
						}
						//Done
						break;
					}
				}
			}
		}
		//Add bundle
		sdp.addAttribute(bundle);
		
		return sdp.toString();
	}
	    
	public static SDPInfo process(String offer) throws IllegalArgumentException, ParserException {
		
		//Parse SDP
		SessionDescription sdp = SessionDescription.Parse(offer);
		
		//Create sdp info object
		SDPInfo sdpInfo = new SDPInfo();
		
		//Set version
		sdpInfo.setVersion(sdp.getVersion());
		
		//For each media description
		for (MediaDescription md : sdp.getMedias())
		{
			//Get media type
			String media = md.getMedia();
			
			//And media id
			String mid = md.getAttribute("mid").getValue();
			
			//Create media info
			MediaInfo mediaInfo = new MediaInfo(mid,media);
			
			//Get ICE info
			String ufrag = md.getAttribute("ice-ufrag").getValue();
			String pwd = md.getAttribute("ice-pwd").getValue();
			
			//Create iceInfo
			mediaInfo.setICE(new ICEInfo(ufrag,pwd));
			
			//Check media fingerprint attribute
			FingerprintAttribute fingerprintAttr = (FingerprintAttribute) md.getAttribute("fingerprint");

			//Get remote fingerprint and hash
			String	remoteHash        = fingerprintAttr.getHashFunc();
			String	remoteFingerprint = fingerprintAttr.getFingerprint();
				
			//Set deault setup
			Codecs.Setup setup = Codecs.Setup.ACTPASS;
			
			//Get setup attribute
			Attribute setupAttr = md.getAttribute("setup");
			//Chekc it
			if (setupAttr!=null)
				//Set it
				setup = Codecs.Setup.byValue(setupAttr.getValue());
			
			//Create new DTLS info
			mediaInfo.setDTLS(new DTLSInfo(setup,remoteHash,remoteFingerprint));
			
			//Store RTX apts so we can associate them later
			HashMap<Integer,Integer> apts = new HashMap<>();
			
			//For each format
			for (String fmt : md.getFormats())
			{
				Integer type;
				String codec;
				try {
					//Get codec
					type = Integer.parseInt(fmt);
				} catch (Exception e) {
					//Ignore non integer codecs, like '*' on application
					continue;
				}
				
				//If it is dinamic
				if (type>=96)
				{
					//Get map
					RTPMapAttribute rtpMapAttr = md.getRTPMap(type);
					//Check it has mapping
					if (rtpMapAttr==null)
						//Skip this one
						continue;
					//Get codec name
					codec = rtpMapAttr.getName();
					
					//If it is RTX
					if ("RTX".equalsIgnoreCase(codec))
					{
						//Get format
						Map<String,String> fp = md.getFormatParameters(type);
						//Get associated payload type
						if (fp.containsKey("apt"))
							try {
								//Get associated payload type
								Integer apt = Integer.parseInt(fp.get("apt"));	
								//Append it
								apts.put(apt, type);
							} catch (Exception e) {
								
							}
						//Do not process it further
						continue;
					} else if ("RED".equalsIgnoreCase(codec) || "ULPFEC".equalsIgnoreCase(codec)) {
						//FUCK YOU!!!
						continue;
					}
				} else {
					//Static
					codec = Codecs.getNameForCodec(media, type);
				}
				//Get format parameters
				Map<String,String> params = md.getFormatParameters(type);
				
				//Create codec
				mediaInfo.addCodec(new CodecInfo(codec,type,params));
			}
			
			//Set the rtx
			for (Map.Entry<Integer, Integer> apt : apts.entrySet())
			{
				//Get codec
				CodecInfo codecInfo = mediaInfo.getCodec(apt.getKey());
				//IF it was not red
				if (codecInfo!=null)
					//Set rtx codec
					codecInfo.setRTX(apt.getValue());
			}
			
			//Get extmap atrributes
			ArrayList<Attribute> extmaps = md.getAttributes("extmap");
			//For each one
			for (Attribute attr : extmaps)
			{
				//Cast
				ExtMapAttribute extmap = (ExtMapAttribute) attr;
				//Add it
				mediaInfo.addExtension(extmap.getId(),extmap.getName());
			}
			
			//Temporal source list
			HashMap<Long,SourceInfo> sources = new HashMap<>();
			
			//Doubel check
			if (md.hasAttribute("ssrc"))
			{
				//Get all ssrcs
				for (Attribute attr : md.getAttributes("ssrc"))
				{
					//Cast
					SSRCAttribute ssrcAttr = (SSRCAttribute) attr;
					//Get data
					Long ssrc = ssrcAttr.getSSRC();
					String key = ssrcAttr.getAttrField();
					String value =  ssrcAttr.getAttrValue();
					//Try to get it
					SourceInfo source = sources.get(ssrc);
					//If we dont have ssrc yet
					if (source==null)
					{
						//Create one
						source = new SourceInfo(ssrc);
						//Add it
						sources.put(source.getSSRC(),source);
					}  
					//Check key
					if ("cname".equalsIgnoreCase(key))
					{
						//Set it
						source.setCName(value);
					} else if ("msid".equalsIgnoreCase(key)) {
						//Split
						String[] ids = value.split(" ");
						//Get stream and track ids
						String streamId = ids[0];
						String trackId  = ids[1];
						//Set ids
						source.setStreamId(streamId);
						source.setTrakcId(trackId);
						//Get stream
						StreamInfo stream = sdpInfo.getStream(streamId);
						//Check if the media stream exists
						if (stream==null)
						{
							//Create one
							stream = new StreamInfo(streamId);
							//Append
							sdpInfo.addStream(stream);
						}
						//Get track
						TrackInfo track = stream.getTrack(trackId);
						//If not found
						if (track==null)
						{
							//Create track
							track = new TrackInfo(media,trackId);
							//Append to stream
							stream.addTrack(track);
						}
						//Add ssrc
						track.addSSRC(ssrc);
					}	
				}
			}
			
			//Double check
			if (md.hasAttribute("ssrc-group"))
			{
				//Get all groups
				for (Attribute attr : md.getAttributes("ssrc-group"))
				{
					//Cast
					SSRCGroupAttribute ssrcGroupAttr = (SSRCGroupAttribute)attr;
					//Get ssrcs
					ArrayList<Long> ssrcs = ssrcGroupAttr.getSSRCIds();
					//Create new group
					SourceGroupInfo group = new SourceGroupInfo(ssrcGroupAttr.getSemantics(),ssrcs);
					//Get media track for ssrc
					SourceInfo source = sources.get(ssrcs.get(0));
					//Add group to track
					sdpInfo
					    .getStream(source.getStreamId())
					    .getTrack(source.getTrakcId())
					    .addSourceGroup(group);
				}
			}
			//Append media
			sdpInfo.addMedia(mediaInfo);
		}
		return sdpInfo;
	}
	
	public static void main(String[] args) throws IllegalArgumentException, ParserException, JsonProcessingException 	{
		
		String sdp = "v=0\r\n" +
	       "o=- 4327261771880257373 2 IN IP4 127.0.0.1\r\n" +
	       "s=-\r\n" +
	       "t=0 0\r\n" +
	       "a=group:BUNDLE audio video\r\n" +
	       "a=msid-semantic: WMS xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj\r\n" +
	       "m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 110 112 113 126\r\n" +
	       "c=IN IP4 0.0.0.0\r\n" +
	       "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
	       "a=ice-ufrag:ez5G\r\n" +
	       "a=ice-pwd:1F1qS++jzWLSQi0qQDZkX/QV\r\n" +
	       "a=fingerprint:sha-256 D2:FA:0E:C3:22:59:5E:14:95:69:92:3D:13:B4:84:24:2C:C2:A2:C0:3E:FD:34:8E:5E:EA:6F:AF:52:CE:E6:0F\r\n" +
	       "a=setup:actpass\r\n" +
	       "a=mid:audio\r\n" +
	       "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
	       "a=sendrecv\r\n" +
	       "a=rtcp-mux\r\n" +
	       "a=rtpmap:111 opus/48000/2\r\n" +
	       "a=rtcp-fb:111 transport-cc\r\n" +
	       "a=fmtp:111 minptime=10;useinbandfec=1\r\n" +
	       "a=rtpmap:103 ISAC/16000\r\n" +
	       "a=rtpmap:104 ISAC/32000\r\n" +
	       "a=rtpmap:9 G722/8000\r\n" +
	       "a=rtpmap:0 PCMU/8000\r\n" +
	       "a=rtpmap:8 PCMA/8000\r\n" +
	       "a=rtpmap:106 CN/32000\r\n" +
	       "a=rtpmap:105 CN/16000\r\n" +
	       "a=rtpmap:13 CN/8000\r\n" +
	       "a=rtpmap:110 telephone-event/48000\r\n" +
	       "a=rtpmap:112 telephone-event/32000\r\n" +
	       "a=rtpmap:113 telephone-event/16000\r\n" +
	       "a=rtpmap:126 telephone-event/8000\r\n" +
	       "a=ssrc:3510681183 cname:loqPWNg7JMmrFUnr\r\n" +
	       "a=ssrc:3510681183 msid:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj 7ea47500-22eb-4815-a899-c74ef321b6ee\r\n" +
	       "a=ssrc:3510681183 mslabel:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj\r\n" +
	       "a=ssrc:3510681183 label:7ea47500-22eb-4815-a899-c74ef321b6ee\r\n" +
	       "m=video 9 UDP/TLS/RTP/SAVPF 96 98 100 102 127 125 97 99 101 124\r\n" +
	       "c=IN IP4 0.0.0.0\r\n" +
	       "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
	       "a=ice-ufrag:ez5G\r\n" +
	       "a=ice-pwd:1F1qS++jzWLSQi0qQDZkX/QV\r\n" +
	       "a=fingerprint:sha-256 D2:FA:0E:C3:22:59:5E:14:95:69:92:3D:13:B4:84:24:2C:C2:A2:C0:3E:FD:34:8E:5E:EA:6F:AF:52:CE:E6:0F\r\n" +
	       "a=setup:actpass\r\n" +
	       "a=mid:video\r\n" +
	       "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
	       "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
	       "a=extmap:4 urn:3gpp:video-orientation\r\n" +
	       "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
	       "a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\n" +
	       "a=sendrecv\r\n" +
	       "a=rtcp-mux\r\n" +
	       "a=rtcp-rsize\r\n" +
	       "a=rtpmap:96 VP8/90000\r\n" +
	       "a=rtcp-fb:96 ccm fir\r\n" +
	       "a=rtcp-fb:96 nack\r\n" +
	       "a=rtcp-fb:96 nack pli\r\n" +
	       "a=rtcp-fb:96 goog-remb\r\n" +
	       "a=rtcp-fb:96 transport-cc\r\n" +
	       "a=rtpmap:98 VP9/90000\r\n" +
	       "a=rtcp-fb:98 ccm fir\r\n" +
	       "a=rtcp-fb:98 nack\r\n" +
	       "a=rtcp-fb:98 nack pli\r\n" +
	       "a=rtcp-fb:98 goog-remb\r\n" +
	       "a=rtcp-fb:98 transport-cc\r\n" +
	       "a=rtpmap:100 H264/90000\r\n" +
	       "a=rtcp-fb:100 ccm fir\r\n" +
	       "a=rtcp-fb:100 nack\r\n" +
	       "a=rtcp-fb:100 nack pli\r\n" +
	       "a=rtcp-fb:100 goog-remb\r\n" +
	       "a=rtcp-fb:100 transport-cc\r\n" +
	       "a=fmtp:100 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\n" +
	       "a=rtpmap:102 red/90000\r\n" +
	       "a=rtpmap:127 ulpfec/90000\r\n" +
	       "a=rtpmap:125 flexfec-03/90000\r\n" +
	       "a=rtcp-fb:125 ccm fir\r\n" +
	       "a=rtcp-fb:125 nack\r\n" +
	       "a=rtcp-fb:125 nack pli\r\n" +
	       "a=rtcp-fb:125 goog-remb\r\n" +
	       "a=rtcp-fb:125 transport-cc\r\n" +
	       "a=fmtp:125 repair-window=10000000\r\n" +
	       "a=rtpmap:97 rtx/90000\r\n" +
	       "a=fmtp:97 apt=96\r\n" +
	       "a=rtpmap:99 rtx/90000\r\n" +
	       "a=fmtp:99 apt=98\r\n" +
	       "a=rtpmap:101 rtx/90000\r\n" +
	       "a=fmtp:101 apt=100\r\n" +
	       "a=rtpmap:124 rtx/90000\r\n" +
	       "a=fmtp:124 apt=102\r\n" +
	       "a=ssrc-group:FID 3004364195 1126032854\r\n" +
	       "a=ssrc-group:FEC-FR 3004364195 1080772241\r\n" +
	       "a=ssrc:3004364195 cname:loqPWNg7JMmrFUnr\r\n" +
	       "a=ssrc:3004364195 msid:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj cf093ab0-0b28-4930-8fe1-7ca8d529be25\r\n" +
	       "a=ssrc:3004364195 mslabel:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj\r\n" +
	       "a=ssrc:3004364195 label:cf093ab0-0b28-4930-8fe1-7ca8d529be25\r\n" +
	       "a=ssrc:1126032854 cname:loqPWNg7JMmrFUnr\r\n" +
	       "a=ssrc:1126032854 msid:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj cf093ab0-0b28-4930-8fe1-7ca8d529be25\r\n" +
	       "a=ssrc:1126032854 mslabel:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj\r\n" +
	       "a=ssrc:1126032854 label:cf093ab0-0b28-4930-8fe1-7ca8d529be25\r\n" +
	       "a=ssrc:1080772241 cname:loqPWNg7JMmrFUnr\r\n" +
	       "a=ssrc:1080772241 msid:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj cf093ab0-0b28-4930-8fe1-7ca8d529be25\r\n" +
	       "a=ssrc:1080772241 mslabel:xIKmAwWv4ft4ULxNJGhkHzvPaCkc8EKo4SGj\r\n" +
	       "a=ssrc:1080772241 label:cf093ab0-0b28-4930-8fe1-7ca8d529be25\r\n";
		
		SDPInfo offer = SDPInfo.process(sdp);
		    
		System.out.println(JSON.Stringify(offer));

		ArrayList<StreamInfo> streams = new ArrayList<>();
		
		streams.add(new StreamInfo("1"));
		streams.add(new StreamInfo("2"));
		streams.add(new StreamInfo("3"));
		
		SDPInfo answer = new SDPInfo();

		Long ssrc = 1L;
		//Create audio media
		MediaInfo audio = new MediaInfo("audio", "audio");
		//Add ice and dtls info
		audio.setDTLS( new DTLSInfo(Codecs.Setup.PASSIVE, "hash", "finger"));
		audio.setICE( new ICEInfo("ufrag", "passwds"));
		//Get codec type
		CodecInfo opus = offer.getAudio().getCodec("Opus");
		//Add opus codec
		audio.addCodec(opus);
		
		//Add it to answer
		answer.addMedia(audio);
		
		//Create video media
		MediaInfo video = new MediaInfo("video", "video");
		//Add ice and dtls info
		video.setDTLS( new DTLSInfo(Codecs.Setup.PASSIVE, "hash", "finger"));
		video.setICE( new ICEInfo("ufrag", "passwds"));
		   //Get codec type
		CodecInfo vp9 = offer.getVideo().getCodec("vp8");
		//Add opus codec
		video.addCodec(vp9);
		
		//For each stream
		for (StreamInfo stream : streams)
		{
			TrackInfo track;
			//Create track
			track = new TrackInfo("video", "track1");
			//Add ssrc
			track.addSSRC(ssrc++);
			//Add it
			stream.addTrack(track);
			//Create track
			track = new TrackInfo("audio", "track2");
			//Add ssrc
			track.addSSRC(ssrc++);
			//Add it
			stream.addTrack(track);
			//Add stream
			answer.addStream(stream);
		}
		
		//Add it to answer
		answer.addMedia(video);
		

		System.out.println(answer.toString());

	}

}

