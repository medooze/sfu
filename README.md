# Medooze SFU 
A future proof, experimental WebRTC VP9 SVC SFU.

#Motivation
There are already several good production ready alternatives for implementing multiconferencing on webrtc, like Jitsi, Janus or SwitchRTC SFUs and even if you need more legacy support you can try our [MCU](http://www.medooze.com/products/mcu.aspx). Our goal is to experiment and provide an early access to the functionalities that will be available in the near future that will improve drastically the performance and quality of multiconferencing services on WebRTC. 
 
Due to the experimental nature of this functionalities we will only officially support Chrome Canary to be able to access the very latest functionalities available (sometimes even running behind a flag). We don't care about interporeability with other browsers (they will eventually catch up) nor SDP legacy support.

#Goal
It is our goal to implement only the We intent to implement support the following features:

- VP9 SVC
- Flex FEC
- RTP transport congestion control and REMB
- Sender side BitRate estimation
- RTCP reduced size
- Bundle only 
- No simulcast

This is a moving target as new functionalities will be available on Chrome and some others will be removed, we will update our targets appropiatelly.

To enable Flex FEC and VP9 SVC on Chrome Canary you must use the following command line:

```
chrome.exe --force-fieldtrials=WebRTC-SupportVP9SVC/EnabledByFlag2SL3TL/WebRTC-FlexFEC-03/Enabled
```

#Compilation
## SFU controller
```
maven package
```
You can get the latest binaries on the release section

## SFU Media Server
```
cd /usr/local/src
git clone https://github.com/cisco/libsrtp
svn checkout svn://svn.code.sf.net/p/mcumediaserver/code/trunk medooze
wget http://downloads.sourceforge.net/project/xmlrpc-c/Xmlrpc-c%20Super%20Stable/1.16.35/xmlrpc-c-1.16.35.tgz
tar xvzf xmlrpc-c-1.16.35.tgz


#
# Compiling XMLRPC-C
#
cd /usr/local/src/xmlrpc-c-1.16.35
./configure
make
make install
cd ..

#
# Compiling libsrtp
#
cd /usr/local/src/libsrtp
./configure
make
make install

#
# Compiling SFU Media Server
#
cd /usr/local/src/medooze/mcu
make sfu
```

# Setup
In order to run the SFU you need to configure in the SFU controller the location and properties of the SFU Media Server, this is done in the ```sfu.conf``:

```
# Set the media server xmlrpc endpoint url 
mixer.url = http://localhost:8080
# Set the media server IP address
mixer.ip = 127.0.0.1
# Set the media server public IP address (i.e. the one facing the clients and which will be used on the SDP)
mixer.publicIp = 169.50.169.84
# Private subnet, in case you want to test it on a private network
mixer.subnet = 0.0.0.0/32
# Websocket server URL
server.port = 8084
```

Currently the server does not support WSS transport, and while it could be supported, it is preffered to run it behind an nginx server which can server the HTML client and proxy the WSS request to the server internally:
```
# Add to your server section of the nginx configuration
	location /sfu {
                proxy_pass http://localhost:8084;
                proxy_http_version 1.1;
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                proxy_read_timeout 3600;
                proxy_set_header        X-Real-IP       $remote_addr;
                proxy_set_header        Host            $host;
                proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
                error_log  /var/log/nginx/sfu.log info;
        }
```

# Run
```
#
# SFU controller
# Usage: sfu [<config-file>] 
#
java -jar sfu-x.x.x.jar 

#
# SFU media server
#  Usage: sfu [-h] [--help] [--sfu-log logfile] [--sfu-pid pidfile] [--http-port port] [--min-rtp-port port] [--max-rtp-port port] 
#  Options:
#   -h,--help        Print help
#   -f               Run as daemon in safe mode
#   -d               Enable debug logging
#   -dd              Enable more debug logging
#   -g               Dump core on SEG FAULT
#   --sfu-log        Set sfu log file path (default: sfu.log)
#   --sfu-pid        Set sfu pid file path (default: sfu.pid)
#   --sfu-crt        Set sfu SSL certificate file path (default: sfu.crt)
#   --sfu-key        Set sfu SSL key file path (default: sfu.pid)
#   --http-port      Set HTTP xmlrpc api port
#   --http-ip        Set HTTP xmlrpc api listening interface ip
#   --min-rtp-port   Set min rtp port
#   --max-rtp-port   Set max rtp port
#   --rtmp-port      Set RTMP port

sfu -f
````

# Client code
Coming soon

#Demo
[Comming soon] (https://sfu.medooze.com) . You must use Chrome Canary in order to use it.




