### Initial build
https://gradle.com/s/s4t6xycrw6tjy

and with tasks up-to-date:
https://gradle.com/s/jskytw3x2pspk
https://gradle.com/s/jswy5d54birpe

- missing package-lock.json
- packageApiMediationZowe is done every time, taking 8 sec, do we need it?

after above fixed:
https://gradle.com/s/ywukd6ldxuzle

do clean:
https://gradle.com/s/qw3d37dwzw5ye
https://gradle.com/s/eljurudpc6jhg

do clean + node:
https://gradle.com/s/g3nvjjq6trnie
node downloaded twice but it's right at the start+
npm install is very fast

do clean + node_modules:
https://gradle.com/s/4ebfssvbrnqv6
if npm cache is warm, npm install takes 40s (biggest hog)
npm test takes 12s
built under minute

after gradle bump to 6.8.3:
https://gradle.com/s/mzqaaarwjd2p6
3m 11s
test tasks show up with :discoverable-client:test taking 1m 21s
generatePom tasks take some time
api catalog services is waiting for frontend to be built before starting compile

enable caching for node_modules folder
full build
https://gradle.com/s/ezkdpfax3vh7e
npm ci could be used to optimize the time
restore cache 17s, save cache 35s

full build with cache hit: 1m 10s, npm install 19s
https://gradle.com/s/oaa77ihel272y
