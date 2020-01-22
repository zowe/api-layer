# Communication between Client, Discovery service, and Gateway

This document is a summary of knowledge gathered during the implementation of PassTickets, and includes how to 
short-cut side-effect delays.

## Client and discovery service (server)

To begin communication between the client and the Discovery service, the client should register with the discovery server. Minimum information requirements from the client to communicate to the server include the following:

- **serviceId (or applicationId)**
- **instanceId** 
    - Ensure that the instanceId is unique. Typically this is concatenated from `serviceId`, `hostname`, and `port`. Note that `hostname` and `port` should be unique themselves.
    - The structure of the instanceId is typically `${hostname}:${serviceId}:${port}`. The third part could also be a random number or string.
 - **health URL**
    - The URL where the service responds with the status of client
    - The discovery client can check this state but active registration occurs via heart beats.
 - **timeout about heartbeat**
    - The client defines how often a heartbeat is sent and when it is time to unregister.
    - The client's active registration with the Discovery service is maintained until a heartbeat is not delivered within the timeout setting or the client unregisters itself.  
 - **service location information**
    - Service location information is used for callback and includes `IP`, `hostname`, `port`, and `securePort` for HTTPS.
    - `VipAddress` can be used and is usually the same as the `serviceId`.
 - **information about environment (shortly AWS or own)**
 - **status**
    - In the registration it replaces the first heartbeat
 - **other information** (Optional)
    - Other parameters can be included, but do not affect communication.
    - Customized data outside of the scope of Eureka can be stored in the metadata.
    - Note: Metadata are data for one time use, and can changed after registration. However, a REST method can be used to update these metadata. 

After registration, the client must send a heartbeat. Once the Discovery service receives this heartbeat, it is possible to call the client.

The client can drop communication by unregistering with the Discovery service. This call is optional as Eureka should resolves failover. Unregistering the client simply speeds up the process of client removal. Without the unregistration call, the Discovery service waits for the heartbeat to time out. Note: This timeout is longer than the interval of the renewal of client registration via the heartbest.

Typically, all communication is covered with caches. As such, a client cannot be used immediately
after registration. Caching occurs in many places for the system as a whole and it takes time to go through all of them. 
Typically, there is a thread pool whereby one thread periodically updates caches. All of caches are independent and do not affect other caches.  

## Caches

The following section describes all of the caches. The main idea is to shortcut
the process of registration and unregistration to allow the use of caches on the Gateway side. As such, the race condition between
different settings in discovery services and gateways is avoided. Descriptions of cache also include a link to the solution describing how it is solved.

### Discovery service & ResponseCache

`ResponseCache` is implemented on the Discovery service. This cache is responsible for minimizing the call's overhead of
registered instances. If any application calls the Discovery service, initially the cache is used or a record is created for subsequent calls.

The default for this cache contains two spaces: `read` and `readWrite`. Other application look in `read`. If the
record is missing, it looks in `readWrite` or recreates a record. `Read` cache is updated by an internal thread which 
periodically compares records in both spaces (on references level, null including).

<font color = "red"> in case of different copy
records from readWrite into read. </font>

The two spaces was evidently created for NetFlix purposes and was changed with pull 
request https://github.com/Netflix/eureka/pull/544. This improvement allows configuration to use only 
`readWrite` space, `read` will be ignored and the user looks directly to `readWrite`. This cache could be updated by  the Discovery service. It also produces records on registation and unregistration (after the client registers
readWrite has evicted records about service, delta and full registry, but read still contains old record). <font color = "red"> This description needs to be refactored to improve clarity.</font>

```
eureka:
    server:
        useReadOnlyResponseCache: false
```

### Gateway & Discovery client

On the Gateways site there is a discovery client which supports queries about services and instances on the gateway side. It will
cache all information from the discovery service and serve the old cached data.
Data are updated with thread asynchronously. From time to time fetch new registries so you can use them. 
Updating can be performed either as delta, or full.

The full update fetch is the initial fetch as it is necessary to download all required information. After that it is rarely used due to performance. The Gateway could call the full update, but it happens only if data are not correct to fix them. One possible reason could be the long delay between fetching.

Delta update fetch only changes and projects them into local cache. Delta does not mean different <font color = "red"> Different what? </font>between 
fetching. To save resources delta means just delta <font color = "red"> delta what? </font>in the last time interval. Discovery service store changed 
instances for a time period (as default 180s). <font color = "red"> This description needs to be refactored for clarity.</font> Those changes are stored in the queue which is periodically 
cleaned by removing changes older than the limit (next separed thread, <font color = "red"> What is a "separed"  thread?.</font> asynchronous). When the gateway asks for delta it will return a mirror of this queue stored in `ResponseCache`. The Gateway then detects which updates were applied in the past and which updates are new. Fornew updates, it uses a version (discovery service somehow mark changes with 
numbers).


<font color = "red"> This description needs to be rewritten to make it comprehensible.</font> 


**solution**

This cache was minimized by allowing run asynchronous fetching at any time. The following classes are used:

- **ApimlDiscoveryClient**
    - custom implementation of discovery client
    - via reflection it takes reference to queue responsible for fetching of registry
    - contains method ```public void fetchRegistry()```, which add new asynchronous command to fetch registry
 - **DiscoveryClientConfig**
    - configuration bean to construct custom discovery client
    - `DiscoveryClient` also support event, especially `CacheRefreshedEvent` after fetching
        - it is used to notify other bean to evict caches (route locators, ZUUL handle mapping, `CacheNotifier`)
 - **ServiceCacheController**
    - The controller to accept information about service changes (ie. new instance, removed instance)
    - This controller asks `ApimlDiscoveryClient` to fetch (it makes a delta and sends an event)
    - After this call, the process is asynchronous and cannot be directly checked (only by events).
    
### Gateway & Route locators

The gateway includes the bean `ApimlRouteLocator`. This bean is responsible for collecting the client's routes. It indicates that information is available about the path and services. This information is required to map the URI to a service. The most important is
the filter `PreDecorationFilter`. It calls the method ```Route getMatchingRoute(String path)``` on the locator to translate the URI into
information about the service. A filter then stores information (ie. `serviceId`) into the ZUUL context. 

In out implementation we use a custom locator, which adds information about static routing. Route locators could be composed of 
from many <font color = "red">. ...of many what?</font>
Eureka uses `CompositeRouteLocator` which contains `ApimlRouteLocator` and a default. Implementation of static routing
could also be performed by a different locator. In a similar way a super class of `ApimlRouteLocator` uses `ZuulProperties`. This can be also be used 
to store a static route. 

**Note:** This is only for information, and could be changed in the future.

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

Now we use as load balancer implementation `ApimlZoneAwareLoadBalancer` (it extends original ZoneAwareLoadBalancer). This
implementation only add method ```public void serverChanged()``` which call super class to reload information about servers,
it means about instances and their addresses.

This is call from `ServiceCacheEvictor` to be sure, that before custom EhCaches are evicted and load balancer get right 
information from ZUUL.

### Service cache - our custom EhCache

For own purpose was added EhCache, which can collect many information about processes. It is highly recommended to synchronize
state of EhCache with discovery client. If not, it is possible to use old values (ie. before registering new service's 
instance with different data than old one). It can make many problems in logic (based on race condition).

It was reason to add `CacheServiceController`. This controller is called from discovery service (exactly from
`EurekaInstanceRegisteredListener` by event `EurekaInstanceRegisteredEvent`). For cleaning caches gateway uses interface
`ServiceCacheEvict`. It means each bean can be called about any changes in registry and evict EhCache (or different cache).

Controller evict all custom caches via interface `ServiceCacheEvict` and as `ApimlDiscoveryClient` to fetch new registry. After
than other beans are notified (see `CacheRefreshedEvent` from discovery client).

This mechanism is working, but not strictly right. There is one case:

1. instance changes in discovery client
2. gateway are notified, clean custome caches and ask for new registry fetching
3. new request accept and make a cache (again with old state) - **this is wrong**
4. fetching of registry is done, evict all Eureka caches

For this reason there was added new bean `CacheEvictor`.
 
#### CacheEvictor

This bean collect all calls from `CacheServiceController` and is waiting for registry fetching. On this event it will clean all
custom caches (via interface `ServiceCacheEvict`). On the end it means that custom caches are evicted twice (before Eureka parts
and after). It fully supported right state.

## Other improvements

Implementation of this improvement wasn't just about caches, but discovery service contains one bug with notification. 

### Event from InstanceRegistry

In Discovery service exist bean `InstanceRegistry`. This bean is call for register, renew and unregister of service. 
Unfortunately, this bean contains also one problem. It notified about newly registered instances before it register it, in
similar way about unregister (cancellation) and renew. It doesnt matter about renew, but other makes problem for us. We
can clean caches before update in `InstanceRegistry` happened. On this topic exists issue:
```
#2659 Race condition with registration events in Eureka server
https://github.com/spring-cloud/spring-cloud-netflix/issues/2659
```

This issue takes long time and it is not good wait for implementation, for this reason was implemented ApimlInstanceRegistry.
This bean replace implementation and make notification in right order. It is via java reflection and it will be removed when
Eureka will be fixed. 

## Using caches and their evicting 

If you use anywhere custom cache, implement interface ServiceCacheEvict to evict. It offer to methods:
- `public void evictCacheService(String serviceId)`
    - to evict only part of caches for service with serviceId
    - if there is no way how to do it, you can evict all records
- public void `evictCacheAllService()`
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
 
