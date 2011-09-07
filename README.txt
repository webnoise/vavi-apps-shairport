WHATIS:

this software is the Apple AirPort clone client for Android.

originals are

	shairport	https://github.com/albertz/shairport
	jmdns		http://jmdns.sourceforge.net/
	RPlay		https://github.com/bencall/RPlay
	JAirPort	https://github.com/froks/JAirPort

BUILD:

before build this software, you should make the private key.
JAirPort says,

---- JAirePort README ----
As I'm not sure about the legal implications of providing the private key, it is not included - you can get it
from shairport or extract it from your router though. It has to be converted to the pk8 format by:
openssl pkcs8 -inform pem -outform der -topk8 -nocrypt -in key.pem -out key.pk8
---- JAirePort README ----

then place "key.pk8" into the directory "res/raw"

USAGE:

	1. run the application on your android terminal named ShairPort
	2. input the AirPlay client name
	3. push the Start button
	4. select AirPort client named in (2.) on iTunes
	5. play iTunes

this software is released under the GPL 2.0 (see LICENSE.txt)
