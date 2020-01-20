# Communication between client, discovery service and gateway

This document was created to summarize all knowledge gathered during implementation PassTickets, where like 
side-effect delays were shortcuted.

## Client and discovery service (server)

On the begin of communication, client should register into discovery server. Client says to server at least:
- serviceId (or applicationId)
- instanceId 
    - to be unique, it is usually concatenated from serviceId, hostname and port (hostname and port should 
      be unique itself)
    - structure of instanceId usually is "${hostname}:${serviceId}:${port}", as third part could be use also
      random number (or any string)
 - health URL
    - URL where service answers status of client
    - discovery client can check this state during the time, but registration is solved via heart beats
 - timeout about heartbeat
    - client define, how often it will send heartbeat and what is time to unregister
    - staying in discovery service is until heartbeat is not delivered in timeout or client unregister itself  
 - service location information
    - information to callback (IP, hostname, port, securePort for HTTPS)
    - VipAddress - usually same like serviceId
 - information about environment (shortly AWS or own)
 - status
    - here in registration it replaced first heartbeat
 - other information
    - you can find other parameters, but their have not impact to this communication
    - custom data (outer Eureka's scope) could be stored into metadata
        - btw. metadata are just once data, which could be changed after registration (there exists REST method
          to update them) 

After registration client should send heartbeat. Until this heartbeat is received, the client is up and it is 
possible call it.

On the end client can unregister from discovery service. This call is optional, because Eureka should solve
failover. Unregister just speed up process of client removing. If this call is missing, discovery should wait
for heartbeat's time out (keep in mind, this timeout is longer than interval to client renew via heartbeat).

All communication is usually covered with many caches and this is reason why client cannot be used immediately
after registration. In whole system exists many cached places and it takes time to go through all of them. 
Usually there is a thread pool and one thread which periodically update caches. All of them are independent by 
themself.

## Caches

In this paragraph I will describe all caches which I found in improving of time. The main idea was to shortcut
process of registration and unregistration to allows using caches on gateway side (avoid race condition between
different settings in discovery services and gateways). It each description of cache will be also link to solution
how it is solved.

### Discovery service & ResponseCache

On discovery service is implemented ResponseCache. This cache is responsible from minimize call's overhead about
registered instances. If any application call discovery service, at first use the cache or create there a record
for next calls.

This cache as default contains two spaces: read and readWrite. Other application look in read and just in case
record is missing there, it look in readWrite or create it again. Read cache is updated by internal thread which 
periodically compare records in both spaces (on references level, null including) and in case of different copy
records from readWrite into read.

Two spaces has evidently reason just for NetFlix purpose and was changed with pull 
request https://github.com/Netflix/eureka/pull/544. This improvement allow to use configuration to use only 
readWrite space, read will be ignored and user look directly to readWrite. This cache could be updated by 
discovery service. It also evict records in there on registation and unregistration (after client register 
readWrite has evicted records about service, delta and full registry, but read still contains old record).

```
eureka:
    server:
        useReadOnlyResponseCache: false
```

### Gateway & Discovery client

On gateways site is discovery client which support queries about services and instances on gateway side. It will
cache all information from discovery service and the serve those old cached data.
Data are updated with thread asynchronous. It time by time fetch new registry and then you can use them. For
updating exists two ways: delta, full.

Full update is using on the very begin. It is necessary to download all information. After that is is using very
rare because performance. Gateway could call full update, but it happens only if data are not correct to fix them.
Probably there is just one possible reason - to long delay between fetching.

Delta update fetch only changes and project them into local cache. But delta doesn't mean different between 
fetching. To save resources delta means just delta in last time interval. Discovery service store changed 
instances for a time period (as default 180s). Those changes are stored in the queue which is periodically 
cleaned - removed changes older than the limit (next separed thread, asynchronous). When gateway ask for delta
it will return mirror of this queue stored in ResponseCache. Gateway then detect which updates were applied in
the past and which updates are new. For that it uses version (discovery service somehow mark changes with 
numbers).

**solution**

This cache was minimized via allowing run asynchronous fetching any time. Used classes for that:
- ApimlDiscoveryClient
    - custom implementation of discovery client
    - via reflection it takes reference to queue responsible for fetching of registry
    - contains method ```public void fetchRegistry()```, which add new asynchronous command to fetch registry
 - DiscoveryClientConfig
    - configuration bean to construct custom discovery client
    - DiscoveryClient also support event, especially CacheRefreshedEvent after fetching
        - it is used to notify other bean to evict caches (route locators, ZUUL handle mapping, CacheNotifier)
 - ServiceCacheController
    - controller to accept information about service changes (ie. new instance, removed instance)
    - this controller ask ApimlDiscoveryClient to make fetching (it makes delta and send event about)
    - after this call process is asynchronous and cannot be directly checked (only by events)
    
### Gateway & Route locators

In gateway exists bean ApimlRouteLocator. This bean is responsible for collecting of client's routes. It means there are 
available information about path and services. Those information are required for map URI to service. The most important is
the filter PreDecorationFilter. It call method ```Route getMatchingRoute(String path)``` on locator to translate URI into
information about service. Filter than store information (ie. serviceId) into ZUUL context. 

In out implementation we use custom locator, which add information about static routing. Route locators could be composite
from many. Eureka use CompositeRouteLocator which contains ApimlRouteLocator and a default. Implementation of static routing
could also make as different locator. In similar way super class of ApimlRouteLocator use ZuulProperties, this can be also use 
for storing of static route. **This is only for information, could be changed in the future, now it is without any change**.

**solution**

Anyway this bean should evicted. It is realized via event from fetching registry (implemented in DiscoveryClientConfig) and
call on each locator method refresh(). This method call discoveryClient and then construct location mapping again. Now after 
fetching new version of registry is constructed well, with new services.

### Gateway & ZuulHandlerMapping

This bean serve method to detect endpoint and return by it handler. Handlers are created on the begin and then just looked up
by URI. In there is mechanism of dirty data. It means, that it create handlers and they are available (dont use locators) 
until they are mark as dirty. Then next call refresh all handlers by data from locators.

**solution**

In DiscoveryClientConfig is implemented listener of fetched registry. It will mark ZuulHandlerMapping as dirty.

### Ribbon load balancer

On the end of ZUUL is load balancer. For that we use Ribbon (before implementation implementation was ZoneAwareLoadBalancer).
Ribbon has also own cache it is use to have information about instances. Shortly, ZUUL give to Ribbon request and it should 
send to an instance. ZUUL contains information about servers (serviceId -> 1-N instances) and information about state of load
balancing (depends on selected mechanism way to select next instance). If this cache is not evicted, Ribbon can try send
request to server which was removed, don't know any server to send or just overload an instance, because don't know about other.
Ribbon can throw many exception in this time, and it is not sure, that it retry sending in right way.

**solution**

Now we use as load balancer implementation ApimlZoneAwareLoadBalancer (it extends original ZoneAwareLoadBalancer). This
implementation only add method ```public void serverChanged()``` which call super class to reload information about servers,
it means about instances and their addresses.

This is call from ServiceCacheEvictor to be sure, that before custom EhCaches are evicted and load balancer get right 
information from ZUUL.

### Service cache - our custom EhCache

For own purpose was added EhCache, which can collect many information about processes. It is highly recommended to synchronize
state of EhCache with discovery client. If not, it is possible to use old values (ie. before registering new service's 
instance with different data than old one). It can make many problems in logic (based on race condition).

It was reason to add CacheServiceController. This controller is called from discovery service (exactly from
EurekaInstanceRegisteredListener by event EurekaInstanceRegisteredEvent). For cleaning caches gateway uses interface
ServiceCacheEvict. It means each bean can be called about any changes in registry and evict EhCache (or different cache).

Controller evict all custom caches via interface ServiceCacheEvict and as ApimlDiscoveryClient to fetch new registry. After
than other beans are notified (see CacheRefreshedEvent from discovery client).

This mechanism is working, but not strictly right. There is one case:

1. instance changes in discovery client
2. gateway are notified, clean custome caches and ask for new registry fetching
3. new request accept and make a cache (again with old state) - **this is wrong**
4. fetching of registry is done, evict all Eureka caches

For this reason there was added new bean CacheEvictor.
 
#### CacheEvictor

This bean collect all calls from CacheServiceController and is waiting for registry fetching. On this event it will clean all
custom caches (via interface ServiceCacheEvict). On the end it means that custom caches are evicted twice (before Eureka parts
and after). It fully supported right state.

## Other improvements

Implementation of this improvement wasn't just about caches, but discovery service contains one bug with notification. 

### Event from InstanceRegistry

In Discovery service exist bean InstanceRegistry. This bean is call for register, renew and unregister of service. 
Unfortunately, this bean contains also one problem. It notified about newly registered instances before it register it, in
similar way about unregister (cancellation) and renew. It doesnt matter about renew, but other makes problem for us. We
can clean caches before update in InstanceRegistry happened. On this topic exists issue:
```
#2659 Race condition with registration events in Eureka server
https://github.com/spring-cloud/spring-cloud-netflix/issues/2659
```

This issue takes long time and it is not good wait for implementation, for this reason was implemented ApimlInstanceRegistry.
This bean replace implementation and make notification in right order. It is via java reflection and it will be removed when
Eureka will be fixed. 

## Using caches and their evicting 

If you use anywhere custom cache, implement interface ServiceCacheEvict to evict. It offer to methods:
- public void evictCacheService(String serviceId)
    - to evict only part of caches for service with serviceId
    - if there is no way how to do it, you can evict all records
- public void evictCacheAllService()
    - to evict all records in the caches, which can has a relationship with any service
    - this method will be call very rare, only in case that, there is impossible to get serviceId (ie. wrong format of instanceId)

## Order to clean caches

From Instance registry is information distributed in this order:
```
Discovery service > ResponseCache in discovery service > Discovery client in gateway > Route locators in gateway > ZUUL handler mapping
```

After those chain is our EhCache (because this is first time, which could cache new data)

From user point of view after ZUUL handler mapping exists Ribbon load balancer cache

---

**REST API**

```
There is possible to use REST API, described at https://github.com/Netflix/eureka/wiki/Eureka-REST-operations.
``` 
 
