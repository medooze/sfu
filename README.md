# Medooze SFU 
A future proof, experimental WebRTC VP9 SVC SFU.

# Motivation
There are already several good production ready alternatives for implementing multiconferencing on webrtc, like Jitsi, Janus or SwitchRTC SFUs and even if you need more legacy support you can try our [MCU](http://www.medooze.com/products/mcu.aspx). Our goal is to experiment and provide an early access to the functionalities that will be available in the near future that will improve drastically the performance and quality of multiconferencing services on WebRTC. 
 
Due to the experimental nature of this functionalities we will only officially support Chrome Canary to be able to access the very latest functionalities available (sometimes even running behind a flag). We don't care about interporeability with other browsers (they will eventually catch up) nor SDP legacy support.

# Goal
It is our goal to implement only the We intent to implement support the following features:

- [VP9 SVC](https://tools.ietf.org/html/draft-ietf-payload-vp9-02)
- [RTP transport wide congestion control](https://tools.ietf.org/html/draft-holmer-rmcat-transport-wide-cc-extensions-01)
- Sender side BitRate estimation: algorithm not decided yet candidates are [GCC](https://tools.ietf.org/html/draft-ietf-rmcat-gcc-02), [NADA](https://tools.ietf.org/html/draft-ietf-rmcat-nada-03) or [SCREAM](https://tools.ietf.org/html/draft-ietf-rmcat-scream-cc-07)
- [RTCP reduced size] (https://tools.ietf.org/html/rfc5506)
- Bundle only 
- No simulcast

This is a moving target as new functionalities will be available on Chrome and some others will be removed, we will update our targets appropiatelly.

To enable VP9 SVC on Chrome Canary you must use the following command line:

```
chrome.exe --force-fieldtrials=WebRTC-SupportVP9SVC/EnabledByFlag2SL3TL/
```
# End to end encrytpion

A full version of SFrame end to end encryption is under works via insertable streams. Current implementation just uses frame counter as IV  which is then inserted in the AES-GCM encrypted frame payload for emoing all required capabilities.

# Install

You just need to install all the depencencies and generate the ssl certificates:

```
npm install 
openssl req -sha256 -days 3650 -newkey rsa:1024 -nodes -new -x509 -keyout server.key -out server.cert
```

If you get an error like this
```
gyp verb build dir attempting to create "build" dir: /usr/local/src/medooze/sfu/node_modules/medooze-media-server/build
gyp ERR! configure error
gyp ERR! stack Error: EACCES: permission denied, mkdir '/usr/local/src/medooze/sfu/node_modules/medooze-media-server/build'

```

You may try instead with:
```
npm install --unsafe-perm
```

# Usage

In order to run the sfu just:

```
node index.js [ip]
```

where the `ip` is the ICE candidate ip address used for RTP media. To test a simple web client just browse to `https://[ip]:8000/`.

# License
MIT
