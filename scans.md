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

do clean + node_modules
https://gradle.com/s/4ebfssvbrnqv6
if npm cache is warm, npm install takes 40s (biggest hog)
npm test takes 12s
built under minute
