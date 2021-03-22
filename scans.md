## After new cache key:

Run actions/cache@v2
Cache not found for input keys: Linux-gradlex-8f4a04a3f7f761dc704b97b8274bc8d0aab5ec4491462bee75f5838d33c8042a, Linux-gradlex-8f4a04a3f7f761dc704b97b8274bc8d0aab5ec4491462bee75f5838d33c8042a, Linux-gradlex-

https://gradle.com/s/37vlybji7ydgw

Post job cleanup.
/usr/bin/tar --posix --use-compress-program zstd -T0 -cf cache.tzst -P -C /home/runner/work/api-layer/api-layer --files-from manifest.txt
Cache saved successfully

===================================

Run actions/cache@v2
Received 171966464 of 560605967 (30.7%), 164.0 MBs/sec
Received 406847488 of 560605967 (72.6%), 193.7 MBs/sec
Received 560605967 of 560605967 (100.0%), 128.5 MBs/sec
Cache Size: ~535 MB (560605967 B)
/usr/bin/tar --use-compress-program zstd -d -xf /home/runner/work/_temp/805d628b-01d3-44ad-a000-fca080b3234a/cache.tzst -P -C /home/runner/work/api-layer/api-layer
Cache restored from key: Linux-gradlex-8f4a04a3f7f761dc704b97b8274bc8d0aab5ec4491462bee75f5838d33c8042a

https://gradle.com/s/gsbte4wh52qmo


## Effects of clear local cache:

first build:
https://scans.gradle.com/s/vj3ko57dygzem
almost everything misses
10:34/8:37

IT:
https://scans.gradle.com/s/crj2rffkmg5va
almost all hits
8:46/1m 4.009s for build, 6m 38.957s for IT

build on jdk8: https://scans.gradle.com/s/42vlt4tjisndu
all hits
1:53

## Repeated execution:

first build:
https://gradle.com/s/4jvziqptzcg5u
3:49

IT:
https://gradle.com/s/5gq6qpaz7brbo
9:30

build on jdk8:
2:02
https://scans.gradle.com/s/wrajxxyd37zye

## Without local gradle cache:

Build around 10:36/9m (build)
negligible ammount of cache hits
https://gradle.com/s/pypnhkqzgx3so

## with local gradle cache with just artefacts

gradle build:
8:40/6m 34s(just build)
https://gradle.com/s/k3ukoobxv5obo
