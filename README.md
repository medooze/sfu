# Medooze SFU 
A future proof, experimental WebRTC VP9 SVC SFU.

#Motivation
There are already several good production ready alteranatives for implementing multiconferencing on webrtc, like Jitsi, Janus or SwitchRTC SFUs and even if you need more legacy support you can try our [MCU](http://www.medooze.com/products/mcu.aspx). Our goal is to epxeriment and provide an early access to the functionalities that will be available in the near future that will improve drastically the performance and quality of multiconferencing services on WebRTC.

Due to the expirmental nature of this functionalities we will only officially support Chrome Canary to be able to access the very latest functionality available (sometimes even running behind a flag). We don't care about interporeability with other browsers (they will eventually catch up) nor SDP legacy support.

#Goal
It is our goal to implement only the We intent to implement support the following features:

- VP9 SVC
- Flex FEC
- RTP transport congestion control and REMB
- Sender side BitRate estimation
- RTCP reduced size
- Bundle only 
- No simulcast

This is a moving target as new functionalities will be available on Chrome and some others will be removed, we will update our targets approiatelly.

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

```
cd /usr/local/src/medooze/mcu
make sfu
```

# Setup


# Run



# Client code
Coming soon

#Demo
[Comming soon] (https://sfu.medooze.com)




