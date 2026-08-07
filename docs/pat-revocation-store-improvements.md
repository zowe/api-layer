# PAT revocation store: unbounded growth + O(n) auth hot path

## Context

`ApimlAccessTokenProvider` keeps PAT revocation state in the Caching Service under three logical maps:

| map key | entry key | entry value |
|---|---|---|
| `invalidTokens` | SHA-512(salt+token) | JSON `AccessTokenContainer` (carries `issuedAt`, `expiresAt`) |
| `invalidUsers` | SHA-512(salt+UPPER(trim(userId))) | revocation timestamp (epoch millis) |
| `invalidScopes` | SHA-512(salt+serviceId) | revocation timestamp (epoch millis) |

Two independent defects fall out of how this is stored:

1. **Memory** — nothing ever removes entries automatically. `invalidTokens` grows one entry per
   revoked PAT, forever, replicated to every node and persisted to disk.
2. **Performance** — answering "is this token revoked?" downloads and deserializes the *entire*
   revocation dataset, on every PAT-authenticated request.

Both scale linearly with usage and neither self-heals. PAT is off by default
(`apiml.security.personalAccessToken.enabled: false`), so this bites sites that turn it on.

### Verified root causes

**Storage layout is one cache entry per *map*, not per item.**
`InfinispanStorage.storeMapItem` (`caching-service/src/main/java/org/zowe/apiml/caching/service/infinispan/storage/InfinispanStorage.java:74`)
stores the whole `Map<String,String>` as the value of a single entry keyed `serviceId + mapKey`.
Each append takes a **cluster-wide `ClusteredLock`**, reads the whole map, and writes the whole map
back. The cache is `REPL_SYNC` + soft-index file store (`InfinispanConfig.getDistributedCacheConfig()`
`:208`), so every single revocation re-marshals the entire map and ships it synchronously to every
node plus the file store. Writes are O(n); N revocations cost O(N²).

**No expiration, no bound.** `CACHE_ZOWE_INVALIDATED_TOKEN` is created with no `lifespan`, `maxIdle`
or `maxCount`; `infinispan.xml` adds none either. `caching.storage.size` / `evictionStrategy` are
inert for Infinispan — they are only wired into InMemory and VSAM. Note
`InfinispanConfig.getSimpleCacheConfig(maxCount, lifeSpan)` (`:220`) already exists and does set
expiration; it is used only for modulith-local caches (`:255-270`).

**Eviction is manual-only.** `evictNonRelevantTokensAndRules()` (`ApimlAccessTokenProvider:141`) is
reachable only from the admin endpoints `AuthController:292` and `ReactivePATController:166`
(SAF `SERVICES/UPDATE`). No scheduler calls it — `@EnableScheduling` exists in the modulith
(`apiml/src/main/java/org/zowe/apiml/ModulithConfig.java:79`) and gateway, but **not** in
`zaas-service` or `caching-service`.

**Read path downloads everything.** `isInvalidated()` (`ApimlAccessTokenProvider:81`) does two
storage round trips per call: `read("salt")` (uncached, though the salt is immutable after init) and
`readAllMaps()` → `GET /cachingservice/api/v1/cache-list` → `getAllMaps` (`:103`), which iterates the
whole token-cache keyset and returns all three maps in full. Only 2 + |scopes| keys are needed.
This runs per request via `PATAuthSourceService.isValid`
(`zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/schema/source/PATAuthSourceService.java:89`),
reached from `apiml/src/main/java/org/zowe/apiml/ZaasSchemeTransformApi.java:148,200,235,267` and
`zaas-service/.../zaas/ZaasAuthenticationFilter.java:48`.

**Revocations can be silently lost.** `storeMapItem`, `removeNonRelevantTokens` and
`removeNonRelevantRules` run their body inside `whenComplete((r, ex) -> { if (Boolean.TRUE.equals(r)) {...} })`.
When `tryLock(4, SECONDS)` times out, `r == false`, the body is skipped, **no exception is raised**,
and the caller gets HTTP 201/204. This gets more likely exactly as the map grows and lock hold times
rise — a security defect amplified by the performance defect.

**Bounded blast radius, worth knowing:** `invalidUsers`/`invalidScopes` are keyed by hashed
user/service and `put` overwrites, so they are bounded by distinct user/service cardinality.
`invalidTokens` is the unbounded one. A PAT lives at most 90 days
(`getToken` caps at 90, `ApimlAccessTokenProvider:196`), so an `invalidTokens` entry is *useful* for
at most 90 days — everything older is pure waste.

### Decisions taken

- **Phased**: ship Phase 1 (no data-format or API change) first, then Phase 2 (the real fix).
- **No local caching of the revocation answer.** Revocation must take effect immediately, so the
  hot-path win must come from making the remote lookup itself O(1) — that is Phase 2's job, and it
  is why Phase 2 matters rather than being optional polish. Salt memoization is still in scope: the
  salt is immutable after initialization, so caching it costs no freshness.
- **Lock-step component versions.** Clean cut-over with a one-time migration; no dual-read path.

Only `InfinispanStorage` implements these map operations — `InMemoryStorage`, `RedisStorage` and
`VsamStorage` all throw `INCOMPATIBLE_STORAGE_METHOD` — so storage changes are contained to Infinispan.

---

## Phase 1 — mitigation (no data-format or REST contract change)

### 1.1 Fix the silent lock-timeout loss
`InfinispanStorage:74,179,210` — extract the repeated `tryLock`/`whenComplete`/`completeJoin` block
into one helper, and give writes and maintenance **different** failure semantics:

- **`storeMapItem` (mandatory)** — a lost lock race means a dropped revocation. Throw
  `StorageException(Messages.CACHE_NOT_AVAILABLE...)` (already defined,
  `caching-service/.../service/Messages.java:30` → 503, catalogued as ZWECS703 in
  `caching-log-messages.yml:145`), so `CachingController`'s existing `exceptionToResponse` path
  returns 503 instead of a false 201.
- **`removeNonRelevant*` (best-effort)** — these are idempotent maintenance. A lost lock race means a
  peer is already doing the work; log and return a successful no-op.

That split matters beyond tidiness: it is what makes concurrent runs of the scheduled job in 1.2
harmless, which in turn is why no leader election is *required*.

Two smaller fixes in the same change: join the `CompletableFuture` returned by `lock.unlock()`
(currently discarded, so unlock failures are invisible), and demote `storeMapItem`'s `log.info` —
it prints the full `AccessTokenContainer` JSON on every revoke. Make the lock timeout configurable
(`caching.storage.infinispan.lockTimeoutSeconds`, default 4) as the pressure valve for sites that
start seeing 503s on revocation bursts.

**Behaviour change worth a release note:** `POST /cache-list/{mapKey}` can now return 503 where it
previously returned 201 while dropping the write. On the ZAAS side that surfaces as
`CachingServiceClientException`, which has **no `@ExceptionHandler`**, so the revoke endpoint answers
500 rather than 503. Queue a follow-up mapping.

### 1.2 Scheduled eviction
New `@Configuration` holding a `@Scheduled` job that calls
`AccessTokenProvider.evictNonRelevantTokensAndRules()`. Follow the existing shape of
`gateway-service/src/main/java/org/zowe/apiml/gateway/scheduled/GatewayScanJob.java` — it puts
`@EnableScheduling` on the job class itself, which is what `zaas-service` needs since it has none.

The package `org.zowe.apiml.zaas.security.service.token` is scanned by `ZaasApplication` and
`ApimlApplication` and by nothing else, so one `@Component` there lands in exactly the two runtimes
that need it. `@EnableScheduling` is idempotent, so co-existing with `ModulithConfig:79` is fine.

- **Interval: daily, with a ~5-minute initial delay.** An `invalidTokens` entry only becomes
  removable once the token expires and a PAT lives at most 90 days, so a daily run changes the
  steady-state size by at most one day's worth of newly-expired entries. Running hourly would
  multiply the expensive part — each run re-marshals and REPL_SYNCs all three whole maps and rewrites
  the file store — for a ~1/24 improvement. The initial delay lets Gateway routing and
  caching-service registration come up first (`CachingServiceClient.getGatewayAddress()` needs them)
  and guarantees at least one run per process lifetime, since `fixedDelay` counts from JVM start.
- Guard with `@ConditionalOnProperty` on the PAT feature flag so both the job and `@EnableScheduling`
  disappear when it is off.
- **Cluster safety, two layers.** Layer 1 is 1.1's best-effort semantics: concurrent runs are already
  serialised by the `ClusteredLock` and now degrade to a benign no-op, so correctness never depends
  on electing a leader. Layer 2 is a cheap, *stateless* gate that just saves N−1 whole-map rewrites:
  ask Eureka for the UP instances of this service, sort by hostname, run only if this host sorts
  first. The repo already enumerates its own instances this way in
  `AuthenticationService.invalidateTokenOnAnotherInstance:287-296`. Every failure path must
  **fail open** (run anyway) — a maintenance job that silently stops forever is much worse than one
  that occasionally runs twice.
  A cache-based leader lease was considered and rejected: `zoweCache` has no TTL, so a node dying
  mid-run would wedge the lease indefinitely and silently stop eviction.
- Errors must not kill the schedule: wrap in try/catch and log. Also make
  `evictNonRelevantTokensAndRules` (`ApimlAccessTokenProvider:141`) resilient — today the first of
  its three calls that throws aborts the other two.
- Set `spring.task.scheduling.pool.size: 2`. Spring's default `TaskScheduler` is single-threaded and
  `ModulithConfig.periodicJwtInit` (20 s, flagged "DON'T JUST REMOVE") already runs on it; this job
  can block for seconds.

### 1.3 Memoize the salt
`ApimlAccessTokenProvider.getSalt()` (`:213`) → `initializeSalt()` (`:166`) hits storage on every
call, halving the round trips per validation for free.

Memoize in `getSalt()`, **not** inside `initializeSalt()` — the latter is package-private and
directly exercised by the `SaltInitialization` tests in
`zaas-service/src/test/java/.../ApimlAccessTokenProviderTest.java:272-320`, which assert on its
create/migrate side effects and expect it to reach storage on each call.

**Do not use `@Cacheable`.** `getSalt()` is called from `getHash(String)` on the same bean
(`:152`) — a self-invocation that bypasses the Spring proxy, so the annotation would silently do
nothing on the path that matters. Use an explicit holder.

**Refresh periodically rather than caching forever** (suggest ~5 minutes). `initializeSalt` lazily
*creates* the salt, so if the caching-service store is ever wiped a new one is generated; today that
self-heals on the next request, whereas a permanent memo would leave nodes hashing with divergent
salts indefinitely — every existing revocation would stop matching and new revocations written by one
node would be invisible to another. A refresh interval bounds that divergence while still removing
essentially all the round trips.

Never memoize an empty or failed result. On a refresh error, keep serving the last known salt without
advancing the timestamp so the next call retries — this stays fail-closed, because `isInvalidated`
goes straight on to the store lookup, which will fail too, and `PATAuthSourceService.isValid:92`
catches and denies. Return a `clone()`; `getSalt()` is public and the array is handed to
`MessageDigest.update`.

### 1.4 Honour `expiresAt` on read
`checkInvalidToken` (`ApimlAccessTokenProvider:111`) parses the `AccessTokenContainer` and returns
`Optional.of(c != null)` — it never looks at `expiresAt`. Treat an entry whose `expiresAt` is in the
past as non-matching. The token is expired anyway, so this changes no security outcome, but it stops
stale entries from being load-bearing and makes Phase 2's TTL semantics consistent.

**Return `Optional.empty()`, not `Optional.of(false)`.** `isInvalidated` (`:94-103`) only falls
through to the user and scope rules when the token check is empty; returning `false` would let an
expired-but-listed token bypass a still-valid `invalidUsers` rule. Also keep `expiresAt == null`
meaning "still revoked" — records written before the field was populated must stay revoked.

### 1.5 Observability
Warn when the `invalidTokens` map exceeds a threshold, so operators see growth before it becomes an
incident. Two places: `InfinispanStorage.storeMapItem`, which already knows the size it just wrote,
and the eviction job, which can log before/after sizes.

Route the operator-facing warning through the **message catalog** (`zaas-log-messages.yml`;
`ZWEAZ602` is free), not just a Micrometer gauge — ZAAS exposes only `health,info` on actuator
(`application.yml:138`), so `/application/metrics` is unreachable by default. A gauge is still worth
adding for the sites that widen the exposure, but it cannot be the primary channel.

### 1.6 Do not try to bound the current cache
Adding `maxCount` to `zoweInvalidatedTokenCache` looks attractive and is useless: `maxCount` counts
*cache entries*, and this cache holds about three (one per map key). Any value ≥ 3 is a no-op, and any
value that did bite would evict an entire map — silently re-validating every revoked PAT. `maxIdle`
is worse still: the three entries are only rewritten when someone revokes, so during a quiet period
the whole revocation store would silently vanish. Bound by policy and alerting here; a real bound
only becomes possible after Phase 2 (see 2.2).

Likewise, splitting `readAllMaps()` into per-map reads is **not** worth doing. The common case (not
revoked) needs all three maps, so it turns one round trip into three, and Infinispan unmarshals the
same bytes either way. Only a per-item lookup helps, and that is Phase 2.

**What Phase 1 does NOT fix** — state this in the PR description so nobody mistakes it for the
redesign:

- Writes stay O(n) per revocation: read, mutate, re-marshal the whole map, REPL_SYNC it to every
  member and rewrite the file store, all under a cluster-wide lock. Revoking k tokens costs O(k·n).
  1.1 only makes the failure *visible*.
- Reads stay O(n): the full store still crosses the wire on every PAT request.
- The store is still unbounded in the worst case — steady state after 1.2 is "revocations in the
  trailing 90 days", which for a busy site is still very large.
- The hard cliff remains: a multi-megabyte single cache value pushed synchronously through JGroups
  will eventually stall the cluster. Phase 1 pushes that out; only Phase 2 removes it.
- Still no per-item read and no per-item delete.
- The 90-day rule retention in `InfinispanStorage:221` stays hardcoded.
- Time-zone skew stays: `invalidateToken` writes `expiresAt` via `ZoneId.systemDefault()` on ZAAS,
  while `removeToken` and the new `checkInvalidToken` compare against `LocalDateTime.now()` in
  *their own* zone. In a split deployment across zones these skew by the offset. Fixing it means
  changing the persisted format — Phase 2 does, by computing TTL on the ZAAS side (see 2.2).

---

## Phase 2 — per-entry storage with native TTL and point lookups

### 2.1 One cache entry per item
Introduce a **new** cache (`zoweInvalidatedTokenItemCache`) of type `Cache<String, String>`: key
encodes `(serviceId, mapKey, itemKey)`, value is exactly the string that is the inner-map value
today (the `AccessTokenContainer` JSON, or the epoch-millis string). No new value type, no new
marshalling contract.

**Reusing the existing cache name is not viable.** Mixing `Map` and `String` values in one cache
means an old node's `getAllMaps` (`InfinispanStorage:112-117`) prefix-matches the new String-valued
keys and stuffs them into a `Map<String, Map<String,String>>`; old ZAAS then fails Jackson
deserialization in `readAllMaps()` → `CachingServiceClientException` → `PATAuthSourceService.isValid`
catches it → **every PAT rejected**. A new cache name makes old nodes simply unaware of it. (Both
caches are persisted as directories under the workspace `Data/` folder, so this is on-disk state, not
just heap.)

**Key encoding: length-prefixed strings, not a typed key object.** `serviceId` is a cert subject DN,
`mapKey` is a URL path variable, `itemKey` is a hash — no separator character is guaranteed absent,
so prefix lengths rather than a delimiter: `len(serviceId)|serviceId + len(mapKey)|mapKey + itemKey`.
Still prefix-scannable for `getAllMaps`, and fully decodable. A `Serializable` composite-key class
was considered and rejected: `GenericJBossMarshaller` + `SoftIndexFileStore` key marshalling for a
POJO is extra risk surface for no gain.

This also closes a latent bug: `getAllMaps` matches with `key.startsWith(serviceId)` then
`key.substring(serviceId.length())` over bare-concatenated keys, so if one service's id is a prefix
of another's, entries leak across services. Length-prefixing removes the ambiguity.

Consequences:
- `storeMapItem` becomes a single `put` — atomic per key, **no `ClusteredLock`, no read-modify-write,
  O(1)**, and only that one entry replicates.
- `getAllMaps` / `getAllMapItems` become a keyset scan grouped by `mapKey`, unioned with any
  not-yet-drained legacy entries so the REST shape is byte-identical. Now off the hot path — they
  only serve the public `GET /cache-list` endpoints, which should be marked deprecated.
- Keep the `keySet()`-then-`getAll` pattern rather than `stream()`, for the reason already documented
  at `InfinispanStorage:106-110` (Infinispan marshals lambdas across nodes).

### 2.2 Native per-entry TTL
Store with `cache.put(key, value, lifespan, unit)` (requires `getTokenCache()` to be typed as
`org.infinispan.Cache`, not `ConcurrentMap`). Infinispan's reaper then reclaims entries with no
eviction job at all. Infinispan is 16.2.2 (`gradle/versions.gradle:43`). Use `lifespan` only, never
`maxIdle` — in a clustered cache `maxIdle` requires cross-node touch RPCs.

- tokens: `lifespan = expiresAt - now`
- rules: `lifespan = timestamp + 90d - now` (same semantics as today's hardcoded 90-day cutoff in
  `removeNonRelevantRules`, but now derived per entry)
- unknown map key or unparseable value: no TTL. The HA test writes an arbitrary map `"aMap"` with a
  non-JSON, non-numeric value (`integration-tests/.../ha/CachingServiceTests.java:59-61`), so TTL
  derivation must tolerate it.

**Trap:** in Infinispan `lifespan = -1` means *never expire* and `0` means *expire immediately*. A
rule with an old timestamp — which `integration-tests/.../pat/AccessTokenServiceTest.java:258`
exercises with `1582239600000` (Feb 2020) — computes a negative TTL and would become **permanent**,
the exact opposite of the intent. Guard explicitly: if the computed TTL is `<= 0`, remove rather than
store. Clamp all TTLs to `[1, maxTtl]` with `maxTtl` defaulting to 90 days, so nothing in this cache
can ever be unbounded.

**Compute the TTL in ZAAS, not in the caching service.** ZAAS holds the authoritative
`QueryResponse.getExpiration()` as a `java.util.Date`. This also sidesteps a live bug: today
`AccessTokenContainer.expiresAt` is a zone-less `LocalDateTime` written with `ZoneId.systemDefault()`
(`ApimlAccessTokenProvider:57`) and compared against the *caching service's* `LocalDateTime.now()`
(`InfinispanStorage:199`) — in a split deployment across time zones that comparison is already wrong.
Keep a server-side derivation only as the fallback when the client sends no TTL.

Carrying the TTL to the storage layer: add a nullable `ttlSeconds` to
`common-service-core/src/main/java/org/zowe/apiml/caching/model/KeyValue.java` and to the client-side
`CachingServiceClient.KeyValue`, as a non-final field with a setter (mirroring `serviceId` —
`KeyValue` uses `@RequiredArgsConstructor` over final fields, so a new final field would change the
generated constructor). `@JsonInclude(NON_EMPTY)` keeps the wire format identical when unset.

Mark the field **`transient`**. `KeyValue implements Serializable` with a pinned
`serialVersionUID = 4831101523346346817L` and is the value type of `zoweCache` — which is REPL_SYNC
and persisted, and holds the `salt` entry that all PAT hashing depends on. `transient` keeps the
Java-serialized form byte-identical while Jackson still emits the property (Jackson's
`PROPAGATE_TRANSIENT_MARKER` is off by default and Lombok generates a getter). Verify that with a
test rather than assuming it; fall back to `@JsonProperty` on the getter if it does not hold. Also
check that adding a field to a `@Data` class does not disturb anything relying on `KeyValue.equals`
(no CAS operations on `zoweCache` were found, but confirm).

An alternative — a new `Storage.storeMapItem(..., Duration)` overload — was rejected: it forces a
signature change on all four `Storage` impls and on `CachingController`'s functional interface, for
information that belongs to the entry. An HTTP header was also rejected: it does not exist on the
modulith path at all, since `LocalCachingClient` calls `Storage` directly.

**Bounding memory is now safe.** Add `maxCount` to the new cache. With a non-shared *write-through*
soft-index file store (passivation off, which is the default), eviction only drops the in-memory
copy — reads fall back to disk, so no revocation is lost. That removes the objection to bounding.
Note this only becomes meaningful after 2.1: the current cache holds ~3 giant entries, so an
entry-count bound on it would do nothing.

### 2.3 Batch point lookup
Add to `common-service-core/src/main/java/org/zowe/apiml/cache/Storage.java` a batch query taking
the specific keys per map, implement it in `InfinispanStorage` as plain `get`s (throwing
`INCOMPATIBLE_STORAGE_METHOD` in the other three impls, consistent with the existing map methods),
expose it as a new endpoint on
`caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java`, and add the
matching method to `CachingClient` / `CachingServiceClient` / `LocalCachingClient`.

Use a POST with a JSON body (`{"invalidTokens":[...],"invalidUsers":[...],"invalidScopes":[...]}`)
returning only the entries found — a read via POST is slightly unidiomatic but avoids URL-length
limits, since each hash is 128 hex chars and a multi-scope token needs several. Reject requests over
a configurable total key count.

Path it `/cachingservice/api/v1/**cache-query**`, *not* `/cache-list/query`: `@PostMapping("/cache-list/{mapKey}")`
already exists (`CachingController:112`), and a literal `/cache-list/query` would win Spring's
pattern comparison and silently make `mapKey="query"` unreachable. The gateway route is
`/api/v1` → `${contextPath}/api/v1` (`caching-service/src/main/resources/application.yml:68-70`), so
any new path under `/cachingservice/api/v1/**` routes with no configuration change.

Declare the `Storage` method abstract rather than `default` — `Messages` lives in `caching-service`
and is not reachable from `common-service-core`, and the existing convention is that each impl throws
`INCOMPATIBLE_STORAGE_METHOD` explicitly.

On the fail-open/fail-closed inconsistency: make it deliberate and documented rather than accidental.
An *absent* record means "not revoked" (`false`); a *transport or storage error* throws and
`PATAuthSourceService.isValid` turns it into "invalid" (fail-closed). Drop the
`cacheMap != null && !cacheMap.isEmpty()` guard, which today conflates "no maps at all" with "nothing
matched". Also guard `parsed.getUserId()` against null — the current code would NPE at
`ApimlAccessTokenProvider:85`.

Then rewrite `isInvalidated` (`ApimlAccessTokenProvider:81`) to issue **one** batch lookup for the
token hash + user hash + scope hashes, replacing `readAllMaps()`. Combined with 1.3, a PAT
validation drops from two round trips carrying the whole dataset to one round trip carrying a few
hundred bytes.

Because we are deliberately not caching the answer locally, that one round trip remains on every
request. In the modulith it is an in-process `LocalCachingClient` call and effectively free. In the
split deployment it is real network I/O issued through the blocking `RestTemplate` in
`CachingServiceClient` (`:27,131`) — worth watching under load, and the reason the payload and
per-request work must be kept small.

### 2.4 Migration
The cache is persisted (soft-index file store) and replicated, so an upgrade that ignored existing
data would **resurrect revoked tokens** — a security regression. Two mechanisms, neither needing a
cluster lock:

**Lazy dual-read — immediate correctness.** `getMapItems`, `getAllMapItems` and `getAllMaps` read
*legacy ∪ new* from the first second after upgrade. Nothing pre-existing is ever invisible, and there
is no window in which a revoked token becomes valid. The legacy overlay is cheap: the old cache is
REPL_SYNC with heap storage, so `get(serviceId + mapKey)` returns an already-deserialized in-heap map
reference — O(1), no marshalling. It is read-only through that reference.

**Scheduled drain — bounded growth.** A migrator in the caching service, running every minute until
done, copies each legacy map's items into per-item entries and then removes the legacy entry:
- `putIfAbsent`, so a fresher per-entry write is never clobbered by stale legacy data.
- CAS removal (`remove(key, observedMap)`) rather than a lock, so a concurrent write is never lost —
  it just retries next cycle.
- A grace period (default ~30 min from node start) before legacy entries are removed, so a rolling
  restart completes first.
- Idempotent, so it can run on every node; do **not** gate on `isCoordinator()`.
- Unknown map keys (e.g. the HA test's `"aMap"`) are left in place and continue to be served by the
  overlay read. They were never the growth problem.
- A `drained` flag then skips the overlay read entirely.

This needs `@EnableScheduling` in the caching service, which has none today — put it on
`InfinispanConfig`, which is already conditional on `caching.storage.mode=infinispan`. Guard the job
against running before `LazyCacheManager` has initialised the caches.

Keep the old cache **defined** this release so the persisted store stays readable; delete the
definition and the migrator in the next one.

The admin `/access-token/evict` endpoints become the manual "finish the migration now" button:
force a drain ignoring the grace period, purge residual legacy entries, sweep the new cache.

**Rolling-restart caveat.** Component versions are lock-step (decided above), but nodes still restart
one at a time, and a node running the old code never joins the new cache — so revocations written
through a new node are invisible to a not-yet-restarted old node until it comes up. The reverse
direction *is* covered by the dual-read plus the drain. Note it in the release notes. Because of the
lock-step decision this plan deliberately does **not** carry the two compatibility shims that would
otherwise be needed: a legacy dual-write during the window, and a sticky
`POST /cache-query` → 404 → `readAllMaps()` fallback in `CachingServiceClient`. Both become necessary
if that decision is revisited.

### 2.5 Retire the lock; keep the evict endpoints
The `ClusteredLock` exists only to serialise the whole-map read-modify-write. With per-key entries
nothing needs it: `storeMapItem` is one atomic `put`, expiry is the reaper, and removals become CAS
`remove(key, expectedValue)`. Drop `lockSupplier` from `InfinispanStorage` along with `completeJoin`,
and remove `LOCK_ZOWE_INVALIDATED`, `zoweInvalidatedTokenLock` and `lock(CacheContainer)` from
`InfinispanConfig` (`:68,125,275-291`). This also eliminates the Phase 1.1 silent-failure bug by
construction. Keep the `infinispan-clustered-lock` dependency for one more release — `defineLock`
created an internal replicated `org.infinispan.LOCKS` cache that older nodes still use.

`removeNonRelevantTokens` / `removeNonRelevantRules` and the admin `/access-token/evict` endpoints
stay for API compatibility, reimplemented as drain-then-sweep. With TTL they are a safety net rather
than the primary mechanism, and the Phase 1 scheduled job can then be dropped — after Phase 2 there
is nothing periodic left for ZAAS to do.

---

## Risks and things to verify during implementation

1. **SoftIndexFileStore disk reclamation on expiry is unverified.** Infinispan honours `lifespan` and
   the reaper removes entries from memory and store, but whether compaction actually reclaims disk
   for 10^5–10^6 expiring entries needs a soak test. `maxCount` plus a periodic
   `getExpirationManager().processExpiration()` nudge are the fallbacks.
2. **`transient` + Jackson** (2.2) — prove with a serialization test, don't assume.
3. **`AdvancedCache.getAll` on a REPL_SYNC cache should be node-local** (no RPC) — confirm in an
   embedded-Infinispan test, since the whole hot-path win rests on it.
4. **`getAllMaps` gets slower**, not faster: O(N) per-entry keys instead of a 3-entry scan. Only
   legacy, test and manual paths hit it, but say so.
5. **Adding `@EnableScheduling` to the caching service** activates a scheduler in a service that
   never had one — check thread-pool and shutdown behaviour against `LazyCacheManager`'s
   `ApplicationReadyEvent` initialisation.
6. **`AccessTokenServiceTest:284` asserts `body("content", not(containsString(...)))`, but `content`
   is not a key in the `/cache-list` response** — that assertion is already vacuous today. Do not
   treat it as protection; strengthen it while you are in there.
7. **Register the new cache in `InfinispanConfig`, or it silently becomes REPL_SYNC + persisted.**
   `DefaultCacheManager.getCache(name)` creates an unknown cache from the *default* configuration,
   which `getCacheManagerConfig` sets to REPL_SYNC with a soft-index file store. Forgetting the
   registration would replicate and persist to disk without any error. Add a unit assertion that the
   name is present in the map handed to `LazyCacheManager`, and that its config is the expiring one.
8. **Adding `@EnableScheduling` to ZAAS activates a scheduler in a service that never had one.**
   `ScheduledAnnotationBeanPostProcessor` inspects every bean, including third-party ones on the
   classpath. A grep found no dormant `@Scheduled` methods, but confirm with a startup smoke test
   that nothing unexpected begins firing.

---

## Files to change

**Phase 1**
- `caching-service/src/main/java/org/zowe/apiml/caching/service/infinispan/storage/InfinispanStorage.java` — lock-failure fix, size warning
- `caching-service/.../infinispan/config/InfinispanConfig.java` — lock timeout + size threshold properties
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java` — salt memoization, `expiresAt` check, resilient evict
- new `AccessTokenEvictionJob` in `zaas-service/.../security/service/token/` (pattern: `gateway-service/.../scheduled/GatewayScanJob.java`)
- `zaas-service/src/main/resources/zaas-log-messages.yml` — `ZWEAZ602` store-too-large warning
- `zaas-service/src/main/resources/application.yml`, `apiml/src/main/resources/application.yml` — eviction interval, scheduler pool size

**Phase 2**
- `common-service-core/src/main/java/org/zowe/apiml/cache/Storage.java` — batch lookup method
- `common-service-core/src/main/java/org/zowe/apiml/caching/model/KeyValue.java` — `expiresAt`
- `caching-service/.../infinispan/storage/InfinispanStorage.java` — per-item layout, TTL, migration
- `caching-service/.../infinispan/config/InfinispanConfig.java` — new cache definition
- `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java` — batch endpoint
- `caching-service/.../inmemory/InMemoryStorage.java`, `.../redis/RedisStorage.java`, `.../vsam/VsamStorage.java` — `INCOMPATIBLE_STORAGE_METHOD` for the new method
- `zaas-service/.../cache/{CachingClient,CachingServiceClient,LocalCachingClient}.java` — batch lookup
- `zaas-service/.../security/service/token/ApimlAccessTokenProvider.java` — rewritten `isInvalidated`
- `caching-service/README.md` — document the new endpoint

## Verification

- Unit: `caching-service/src/test/java/.../infinispan/storage/InfinispanStorageTest.java` (nested
  `WhenStoreToken`, `WhenRetrieveInvalidTokensAndRules`, `WhenEvictNonRelevantTokensAndRules` all
  need updating for the new layout); add cases for lock-timeout → `StorageException`, TTL expiry,
  negative-TTL skip, and migration from the old layout. Its `createCache()` helper already mocks
  `org.infinispan.Cache` but only stubs the two-arg `put`, so it needs a stub for the
  `put(k, v, lifespan, unit)` overload; assert TTL via `verify` rather than by waiting for real
  expiry.
- Unit: `zaas-service/src/test/java/.../token/ApimlAccessTokenProviderTest.java` (incl. `SaltInitialization`
  and `WhenCallingEviction`) — assert the salt is read once within the refresh window and re-read
  after it; that a failed refresh serves the stale salt and the next call retries; that a failure is
  never memoized; and that `isInvalidated` issues a batch lookup rather than `readAllMaps`.
  For 1.4, the sharp case is an expired token record **plus** a matching `invalidUsers` rule — it must
  still return `true`. That is the regression guard for `Optional.empty()` vs `Optional.of(false)`.
- Unit: a `WhenLockCannotBeAcquired` nested class in `InfinispanStorageTest` — `storeMapItem` throws
  `CACHE_NOT_AVAILABLE`/503 and never writes; `removeNonRelevant*` do not throw; `unlock()` is not
  called when the lock was never taken, but *is* called when the body throws. Plus a
  `CachingControllerTest` case asserting the 503 reaches the HTTP layer.
- Unit: `AccessTokenEvictionJobTest` — designated-runner selection, fail-open on every Eureka failure
  path, DOWN instances excluded, and that a thrown `CachingServiceClientException` does not kill the
  schedule.
- Unit: `caching-service/src/test/java/.../api/CachingControllerTest.java`, and
  `apiml/src/test/java/org/zowe/apiml/controller/ReactivePATControllerTest.java`.
- Integration: `integration-tests/src/test/java/org/zowe/apiml/integration/authentication/pat/AccessTokenServiceTest.java`
  must keep passing unchanged — it encodes the revoke/validate/evict semantics, including the
  old-timestamp rule case. Also `.../pat/PATWithAllSchemesTest.java` and
  `integration-tests/.../ha/CachingServiceTests.java`, which exercises `GET /cache-list` and
  `GET /cache-list/{mapKey}` with a non-PAT map key (`"aMap"`) holding a non-JSON, non-numeric value —
  the best available proof that the REST contract survives Phase 2. Make sure it runs in the HA lane.
- Integration (multi-instance): revoke on instance A, then assert instance B rejects the token on its
  very next request. With no local caching this must be immediate; it is the test that pins the
  no-staleness decision.
- Manual end-to-end (`npm run api-layer` with `apiml.security.personalAccessToken.enabled=true`):
  generate a PAT, use it against a routed service, revoke it, confirm the very next request is
  rejected (no local caching means this must be immediate), then confirm the entry disappears on its
  own once past `expiresAt`.
- Performance sanity: revoke a few thousand tokens, then measure PAT request latency and the
  caching-service response size — should be flat with respect to the number of revocations after
  Phase 2, versus linear today.
