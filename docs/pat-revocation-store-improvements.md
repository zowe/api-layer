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

- **One release, not phases.** Everything below ships together. "Part 1" and "Part 2" order the
  *narrative*, not the releases; nothing in Part 2 waits for Part 1 to reach the field.
- **No local caching of the revocation answer.** Revocation must take effect immediately, so the
  hot-path win must come from making the remote lookup itself O(1) — that is what Part 2 delivers.
  Salt memoization is still in scope: the salt is immutable after initialization, so caching it costs
  no freshness.
- **The upgrade invalidates no PAT.** A global epoch (`patCutoverEpoch`) is introduced, but it
  *routes* pre-cutover tokens to the legacy read path instead of rejecting them — see 2.4. This is
  what keeps the upgrade non-breaking, and it is the reason every way the epoch can be wrong or lost
  degrades to added latency rather than to an outage.
- **`patCutoverEpoch` comes from configuration**, with create-if-absent as the fallback, so the value
  is deterministic, identical fleet-wide, survives a store wipe, and doubles as an incident kill
  switch.
- **No dual-write.** Revocations go only to the new per-item cache. The legacy map is read during the
  sunset window, and nothing on this release writes to it. The cost is that a downgrade — and a
  previous-release caching-service or modulith instance mid-rollout — loses revocations made on the new
  version; see 2.6.
- **No data migration.** The two bounded rule maps (`invalidUsers`/`invalidScopes`) are not copied
  into the new cache. The residual gap that leaves is stated in 2.4.
- **No maintenance job, and disposal of the legacy store is the operator's call.** The legacy map is
  frozen and read-only after the upgrade, so nothing needs to trim it; the operator decides whether to
  keep it (pre-upgrade revocations stay enforced, at a read cost that decays to zero within 90 days) or
  delete it and start clean (flat performance, pre-upgrade revocations dropped). Documented script, not
  an automatic delete. See 1.2 and 2.7.
- **Degrade, don't fail, on version skew.** ZAAS probes `/cache-query` once at startup; against a
  caching-service that is too old it logs a catalogued error and falls back to the legacy read path
  rather than rejecting every PAT. See 2.3.

Only `InfinispanStorage` implements these map operations — `InMemoryStorage`, `RedisStorage` and
`VsamStorage` all throw `INCOMPATIBLE_STORAGE_METHOD` — so storage changes are contained to Infinispan.

---

## Part 1 — hardening the existing path

No data-format or REST contract change in this part. Section numbers are kept stable because the rest
of the document cross-references them.

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

That split matters beyond tidiness: `removeNonRelevant*` is what the admin `/access-token/evict`
endpoints call, and two operators (or two nodes) hitting it at once must not turn into a 503.

Two smaller fixes in the same change: join the `CompletableFuture` returned by `lock.unlock()`
(currently discarded, so unlock failures are invisible), and demote `storeMapItem`'s `log.info` —
it prints the full `AccessTokenContainer` JSON on every revoke. Make the lock timeout configurable
(`caching.storage.infinispan.lockTimeoutSeconds`, default 4) as the pressure valve for sites that
start seeing 503s on revocation bursts.

**Third fix, found during review: harden the maintenance filters against malformed entries.**
`removeToken` (`InfinispanStorage.java:199`, `c.getExpiresAt().isBefore(LocalDateTime.now())`) throws
an uncaught `NullPointerException` if any single entry has `expiresAt == null` — a case 1.4 explicitly
acknowledges can exist ("records written before the field was populated"). `removeNonRelevantRules`
(`:219`, `Long.parseLong(entry.getValue())`) has the same problem for a non-numeric rule value. Either
exception aborts the whole `Stream`/`Collectors.toMap` pipeline before the cleaned map is written
back, so a single poison entry permanently blocks cleanup of that *entire* map, every cycle — silently
defeating the eviction job this phase exists to enable. Treat an unparseable/incomplete entry as
*still relevant* (kept, not thrown) instead of aborting the batch, consistent with 1.4's fail-closed
intent.

**Behaviour change worth a release note:** `POST /cache-list/{mapKey}` can now return 503 where it
previously returned 201 while dropping the write. On the ZAAS side that surfaces as
`CachingServiceClientException`, which has **no `@ExceptionHandler`**, so the revoke endpoint answers
500 rather than 503. Queue a follow-up mapping.

### 1.2 No maintenance job: the legacy map is frozen, and disposal is the operator's call
An earlier draft of this document put a `@Scheduled` job in ZAAS to call
`AccessTokenProvider.evictNonRelevantTokensAndRules()` periodically. That is dropped. It is worth
recording *what* such a job would even have evicted, because the legacy layout has no TTL and the
answer is not obvious: expiry is carried **inside each record's value**, not by the cache. `removeToken`
(`InfinispanStorage:193-207`) reads the whole map, drops the entries whose `AccessTokenContainer.expiresAt`
has passed, and writes the filtered map back; `removeNonRelevantRules` (`:209-231`) does the same for
rule entries older than 90 days. So "eviction" here means a whole-map read-filter-write driven by each
record's own embedded timestamp — which is exactly why it needs the cluster-wide lock, and why running
it on a timer is expensive.

**It is not needed, because after this release the legacy map cannot grow.** Three things combine:

- Revocations are written only to the new per-item cache — there is no dual-write (see Decisions).
- The public `POST /cache-list/{mapKey}` API is re-pointed at the new per-item cache by 2.1, so even
  non-PAT callers stop writing the legacy layout.
- 1.4 stops expired entries from *matching* on read, so dead records are inert, not load-bearing.

The legacy map is therefore frozen at its upgrade-day size, read-only, consulted only by 2.4's routing
path, and consulted less and less often as pre-cutover tokens expire — reaching zero within 90 days.
The remaining cost is bytes on the wire for a decaying share of requests. Buying that back with a
net-new scheduled job that takes a cluster-wide lock, whose first and largest run would land inside the
rolling-restart window, and which would be deleted one release later, is not a good trade.

**What the operator gets told instead** — two choices, both documented, with the consequence of each
stated plainly (see 2.7 for the mechanics):

- **Keep it.** Revocations made before the upgrade stay enforced for the remaining life of the tokens
  they apply to. Cost: the legacy map crosses the wire on requests presenting a pre-cutover PAT, for up
  to 90 days.
- **Delete it and start clean.** Flat performance immediately, and the directory's disk is reclaimed.
  Cost, and it must be said in as many words: **every revocation made before the upgrade stops being
  enforced.** A pre-cutover PAT that was revoked becomes usable again until it expires on its own. For
  most sites that is either irrelevant or cheaply fixed by re-revoking what still matters — but it is a
  security decision and it belongs to the operator, not to us.

This also means neither `zaas-service` nor `caching-service` gains a scheduler, so risk 7 below
disappears, and `spring.task.scheduling.pool.size` does not need changing — which in turn leaves the
modulith's two existing `@Scheduled` methods (`ModulithConfig:243`, `GatewayScanJob:65`) serialised
exactly as they are today.

Do still make `evictNonRelevantTokensAndRules` (`ApimlAccessTokenProvider:141`) resilient: today the
first of its three calls that throws aborts the other two, which matters for the admin endpoints that
remain.

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

Never memoize an empty or failed result. On a refresh error, keep serving the last known salt — this
stays fail-closed, because `isInvalidated` goes straight on to the store lookup, which will fail too,
and `PATAuthSourceService.isValid:92` catches and denies. Return a `clone()`; `getSalt()` is public and
the array is handed to `MessageDigest.update`.

**Keep the store read off every request once a salt is known.** A due refresh is claimed by one
caller (compare-and-set on the last attempt time); every other caller is served the last known salt
at once rather than waiting behind a lock for the store. A failed attempt is retried only after a
cooldown (60s, as for the cutover epoch), so a store that is down or slow costs one read per interval,
not one per request. With no salt known yet (a cold start), callers do have to wait for the attempt in
progress, but after a failure they fail at once until the cooldown passes. The read goes through the
short-timeout lookup client (`revocationLookupTimeoutMillis`), not the shared client and its 60s
socket timeout.

**A refresh interval bounds cross-node divergence, not data loss.** If the salt is ever actually
*regenerated* (the store was wiped, not merely a memo expiring), every hash computed under the old
salt permanently stops matching anything computed under the new one — this is true whether or not the
salt is memoized at all, since `SHA-512(salt+value)` has no relationship across a salt change, and no
refresh interval can fix it. Because `invalidUsers`/`invalidScopes` have no expiry in the legacy layout, a salt
regeneration event silently and permanently un-enforces every existing revocation — the opposite of
the "self-heals" framing above, which is only true of the salt's own usability, not of the
revocations that depended on the old one. Log a loud, operator-visible warning whenever
`initializeSalt` actually creates a *new* salt rather than reading an existing one — this should read
as an incident, not a self-heal.

### 1.4 Honour `expiresAt` on read
`checkInvalidToken` (`ApimlAccessTokenProvider:111`) parses the `AccessTokenContainer` and returns
`Optional.of(c != null)` — it never looks at `expiresAt`. Treat an entry whose `expiresAt` is in the
past as non-matching. The token is expired anyway, so this changes no security outcome, but it stops
stale entries from being load-bearing and makes 2.2's TTL semantics consistent.

**Return `Optional.empty()`, not `Optional.of(false)`.** `isInvalidated` (`:94-103`) only falls
through to the user and scope rules when the token check is empty; returning `false` would let an
expired-but-listed token bypass a still-valid `invalidUsers` rule. Also keep `expiresAt == null`
meaning "still revoked" — records written before the field was populated must stay revoked.

**While in this method, also close an adjacent fail-open gap.** The existing
`catch (JsonProcessingException e)` branch falls through to `Optional.empty()` on a parse failure —
indistinguishable from "no entry at all," even though the key's mere presence under that exact token
hash already proves `invalidateToken()` was called for it. Change that branch to `Optional.of(true)`
as well: a keyed-but-unparseable entry is strictly *less* trustworthy evidence than a
missing-`expiresAt` one, so it should fail at least as closed.

### 1.5 Observability
Warn when the `invalidTokens` map exceeds a threshold, so operators see growth before it becomes an
incident. Two places: `InfinispanStorage.storeMapItem`, which already knows the size it just wrote,
and the eviction job, which can log before/after sizes.

Route the operator-facing warning through the **message catalog** (`zaas-log-messages.yml`;
`ZWEAZ602` is free), not just a Micrometer gauge — ZAAS exposes only `health,info` on actuator
(`application.yml:138`), so `/application/metrics` is unreachable by default. A gauge is still worth
adding for the sites that widen the exposure, but it cannot be the primary channel.

**Throttle the per-write warning.** `storeMapItem` sees the map on every revocation, so a naive
"warn if size > threshold" check re-fires on every single write once the threshold is crossed — worst
exactly during the revocation bursts 1.1's lock-timeout knob exists to absorb. Log it at most once per
size-doubling (or a fixed cooldown), not once per call.

### 1.6 Do not try to bound the current cache
Adding `maxCount` to `zoweInvalidatedTokenCache` looks attractive and is useless: `maxCount` counts
*cache entries*, and this cache holds about three (one per map key). Any value ≥ 3 is a no-op, and any
value that did bite would evict an entire map — silently re-validating every revoked PAT. `maxIdle`
is worse still: the three entries are only rewritten when someone revokes, so during a quiet period
the whole revocation store would silently vanish. Bound by policy and alerting here; a real bound
only becomes possible with the per-item layout (see 2.2).

Likewise, splitting `readAllMaps()` into per-map reads is **not** worth doing. The common case (not
revoked) needs all three maps, so it turns one round trip into three, and Infinispan unmarshals the
same bytes either way. Only a per-item lookup helps, and that is 2.3.

**What Part 1 does not fix on its own.** All of it is fixed by Part 2 in the same release; the list
exists so a reviewer reading only Part 1 does not mistake it for the redesign, and so the PR
description can say plainly which change does what.

- Writes stay O(n) per revocation: read, mutate, re-marshal the whole map, REPL_SYNC it to every
  member and rewrite the file store, all under a cluster-wide lock. Revoking k tokens costs O(k·n).
  1.1 only makes the failure *visible*; 2.1 removes it.
- Reads stay O(n): the full store crosses the wire on every PAT request. 2.3 removes this for every
  token issued after the cutover, and 2.4's sunset removes it for the rest within 90 days.
- The legacy store is never bounded or trimmed; it is frozen at upgrade size and then abandoned (1.2).
  2.2's TTL is what bounds the new one.
- The hard cliff: a multi-megabyte single cache value pushed synchronously through JGroups will
  eventually stall the cluster. Only 2.1 removes it.
- No per-item read and no per-item delete until 2.1/2.3.
- The 90-day rule retention in `InfinispanStorage:221` stays hardcoded in the legacy path; 2.2 derives
  it per entry in the new one.
- Time-zone skew: `invalidateToken` writes `expiresAt` via `ZoneId.systemDefault()` on ZAAS, while
  `removeToken` and the new `checkInvalidToken` compare against `LocalDateTime.now()` in *their own*
  zone. In a split deployment across zones these skew by the offset. It stays wrong for the legacy
  map for as long as that map is read; 2.2 fixes it for the new one by computing TTL on the ZAAS side.

---

## Part 2 — per-entry storage with native TTL and point lookups

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
- `getAllMaps` / `getAllMapItems` become a keyset scan grouped by `mapKey`; the REST shape stays
  byte-identical since the value type per item is unchanged. Now off the hot path — they only serve
  the public `GET /cache-list` endpoints, which should be marked deprecated. (No legacy-overlay union
  is needed here — see 2.4 for why there is no legacy data left to reconcile against.)
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
test rather than assuming it; fall back to `@JsonProperty` on the getter if it does not hold.

`equals()`/`hashCode()` need no such check: Lombok's `@EqualsAndHashCode` (part of `@Data`) already
excludes `transient` fields by construction, so they are unaffected by design — confirmed against the
pinned Lombok version, not merely assumed. **`toString()` is the one that actually changes** —
`@ToString` has no transient-exclusion behavior, so the new field will appear there.
`InfinispanStorage.update():136` already logs the whole `KeyValue` object
(`log.info("Updating record for service {} under key {}", serviceId, toUpdate)`) rather than just its
key; either exclude the new field with `@ToString.Exclude` or fix that log line to log
`toUpdate.getKey()` — otherwise this pre-existing log line silently starts printing `ttlSeconds` on
every generic `Storage.update()` call, PAT or not.

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

**Probe the endpoint once at startup and degrade, don't fail.** Zowe installs components
individually, so ZAAS on this release can be pointed at a caching-service that predates it, and
`/cache-query` then 404s. `CachingServiceClient` wraps that into `CachingServiceClientException`,
`PATAuthSourceService.isValid:92` catches it, and **every PAT is rejected fleet-wide**, diagnosable
only from a stack trace. Instead: probe `/cache-query` once at startup, and on 404/405 log a
catalogued ZAAS error that names the minimum caching-service version and fall back to the legacy
`readAllMaps()` path for all tokens. This fallback exists only because 2.4's routing design keeps
`readAllMaps` in the codebase for the sunset window — it disappears with it, so the probe and its
message are removed in the same change (2.6). Emit the catalogued error on a repeating cadence, not
just once, so a skewed deployment cannot sit silently on the slow path forever.

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

With no local caching, this blocking call becomes PAT's *sole* synchronous dependency in a split
deployment, with no fallback if the caching-service is merely slow rather than down —
`CachingServiceClient`'s shared `RestTemplate` has no circuit breaker or retry on this path today. A
caching-service slowdown can therefore tie up ZAAS request threads for every PAT user simultaneously
rather than degrading gracefully. Give this
specific call a shorter timeout and basic circuit-breaker/retry semantics rather than relying on the
shared client's general defaults.

**Bound the request size at the source, not just at the door.** The "reject requests over a
configurable total key count" guard protects the caching-service, but nothing bounds how many scopes a
PAT can be issued with in the first place (`ReactivePATController`'s `generatePat` only checks scopes
are non-empty). A legitimately-issued many-scope token whose hash count exceeds the limit becomes
permanently unauthenticatable — `PATAuthSourceService.isValid`'s fail-closed catch turns the resulting
error into "not valid" on every future request, with no way to recover short of a new PAT.

**Decision: cap the scope count at issuance.** Derive both the issuance cap and the batch-lookup
limit from one constant so they cannot drift apart, with the lookup limit at least `cap + 2` for the
token and user hashes. Reject an over-cap `generatePat` with a 400 that names the limit.

*Residual risk, and it needs a number chosen deliberately:* an issuance cap does nothing for tokens
that were **already issued** with more scopes than the cap. Such a token starts failing the batch
lookup the moment this release lands and cannot be recovered except by issuing a new PAT — the one
place in this design where an existing PAT can break. Set the constant comfortably above anything a
site could plausibly have issued (scopes are service ids, so the practical ceiling is "how many
services one token was scoped to"), and treat it as an operator-visible, schema-documented limit
rather than an internal constant. If a real deployment is found near the limit, chunk the lookup
instead of rejecting.

### 2.4 Cutover: route pre-cutover tokens to the legacy read path
`patCutoverEpoch` is a single scalar (millis since epoch) that splits the token population in two.
A PAT issued **after** it is answered entirely from the new per-item cache — one small batch lookup,
O(1). A PAT issued **before** it additionally consults the legacy whole-map store, exactly as today.
No PAT is invalidated, nothing is migrated, and the legacy path extinguishes itself.

```
long threshold = patCutoverEpoch + skewAllowanceMillis;
if (now < patCutoverEpoch + SUNSET_MILLIS                 // inert after the sunset
    && parsedToken.getCreation().getTime() <= threshold) {
    return legacyIsInvalidated(parsedToken, hashes)        // readAllMaps(), O(n)
        || batchLookup(hashes);                            // new cache, O(1)
}
return batchLookup(hashes);
```

**Why route rather than reject.** An earlier draft of this section rejected every pre-cutover PAT
outright, on the argument that "dead either way" is a superset of anything the legacy data could prove.
That argument is sound, but it makes a single scalar load-bearing for availability, and the scalar is
not as stable as it looks. Routing keeps the identical mechanism and the identical end state while
changing what a wrong value costs:

| if the epoch is… | reject | route |
|---|---|---|
| too high (issuing node's clock lags the minting node's) | fresh PATs rejected forever; only remedy is re-issue | those PATs take the legacy path — slower, correct |
| lost and re-minted (store wipe, the documented `ZWECS138` remedy) | every outstanding PAT dies, silently, again | old tokens take the legacy path for ≤ 90 days |
| divergent across nodes (split cluster, stale memo) | the auth answer depends on which node serves the request | same answer everywhere, different latency |
| too low | pre-cutover revocations silently lost | pre-cutover revocations silently lost |

Only the last row is equal, and it is the one that deserves engineering attention: a *too low* epoch is
a security bug in both designs. That is what the skew allowance is for, and why it is added to the
comparison rather than baked into the stored value — the stored epoch stays audit-meaningful while the
tolerance is tunable. Keep the allowance small (minutes, not days): under routing a high threshold is
free in correctness terms but not in latency, and it sends post-cutover tokens down the legacy path at
exactly the moment the legacy map is at its largest.

**Where the value comes from.** In resolution order:

1. `apiml.security.personalAccessToken.cutoverEpoch`, if set. This is the primary path — one value,
   identical on every node, unaffected by anything that happens to the cache, and settable again later
   as a real kill switch (see below).
2. Otherwise the value stored under a well-known key in the same generic `zoweCache` the salt already
   lives in, via `CachingServiceClient.create`/`read` — no new `Storage` or client method needed.
3. Otherwise mint `System.currentTimeMillis()`, store it with `create()` and fall back to `read()` on
   `isKeyCollision()`, i.e. `initializeSalt`'s exact idiom.

Two properties of step 3 are worth writing down because they are not obvious. `create` maps to
`putIfAbsent` (`InfinispanStorage:65`) and a collision is `HttpStatus.CONFLICT` (`Messages:20`), which
`isKeyCollision()` matches — so a *spurious* "absent" cannot overwrite a good value. That matters
because `CachingServiceClient.read` reports a Gateway 404 (e.g. `cachingservice` not yet registered at
ZAAS startup) indistinguishably from a genuinely missing key. And a re-mint therefore happens only
when the value is really gone: a store wipe, an `ZWE_haInstance_id` change on a standalone node, or a
node that mints in isolation because it could not join the JGroups cluster — the same failure the salt
warning at `ApimlAccessTokenProvider:231` already exists for. `LazyCacheManager.resetCorruptedGlobalState`
is *not* such a path: it moves only `___global.state`/`___global.lck` and deliberately preserves
per-cache data.

Log the effective epoch, its source (config / stored / minted) and the node's own clock at startup, so
divergence is diffable from a support bundle, and log a mint through the message catalog at
incident level — the same treatment 1.3 gives a salt regeneration.

**Do not store the epoch inside the salt record.** Coupling them was considered, so that they could
only be lost together; it was rejected. The salt record's value is a bare base64 string and
`initializeSalt` still migrates a legacy raw form (`isBase64EncodedSalt`, `:155-177`), so an older node
reading a combined value mid-rollout would "migrate" it by re-encoding the whole thing. Under routing
the failure that coupling was meant to prevent — salt intact, epoch re-minted — costs latency rather
than an outage, so the risk is not worth taking against the record every PAT hash depends on.

**Provably self-terminating.** The branch is unreachable once `now > patCutoverEpoch + 90 days`, and
that is a property of the code rather than a plan:

- `isInvalidated` calls `parseJwtWithSignature` (`ApimlAccessTokenProvider:83`) before touching any
  store, and that throws `ExpiredJWTException` for an expired token (`AuthenticationService:337-345`).
  An expired PAT therefore never reaches the branch.
- PAT lifetime is capped at 90 days (`ApimlAccessTokenProvider:196`), and `git log -S` shows that cap
  has held that value since the original PAT commit — so no PAT has ever been issuable with a longer
  life, and a later change to the cap cannot extend the window, because pre-cutover tokens already have
  their `exp` baked in.

Write the sunset into the condition, as above, so the branch goes inert on its own and deleting it
later is a no-op refactor rather than a behaviour change. A longer sunset costs only later deletion, so
err long if the historical cap is ever in doubt. Put a throttled counter or catalogued message on the
branch: its rate should decay to zero over 90 days, and a rate that does *not* decay is the signal that
the epoch failed to resolve — the one way this design could quietly become permanent, since a
never-resolving epoch produces answers that are correct and merely slow.

**Residual gap: future-dated legacy rules.** `invalidateAllTokensForUser`/`ForService` accept an
arbitrary `timestamp` — only `0` is replaced with now (`ApimlAccessTokenProvider:63-70`) — and neither
controller bounds it (`AuthController:192-196`, `ReactivePATController:283-288`). A rule written before
the cutover with a *future* timestamp legitimately governs tokens issued after it, and those tokens
consult only the new cache, where the rule does not exist. We are deliberately not migrating the two
rule maps, so this stays open. Bound `timestamp` at both controllers going forward, which prevents new
instances but not pre-existing ones. If it is ever judged worth closing without a migration, the cheap
form is a one-time startup read of the two (bounded) rule maps to take
`threshold = max(threshold, highestFutureRuleTimestamp)` — a read to pick a safe threshold, not a data
move.

**The resulting `timestamp` contract.** Only the upper end is bounded; there is deliberately no lower
bound, because a rule dated in the past is exactly how an operator revokes retrospectively. What makes
the far past harmless is 2.2's TTL derivation rather than a check in the controller.

| `timestamp` | response | effect |
|---|---|---|
| `0`, or body omitted | 204 | replaced with `System.currentTimeMillis()` — the "revoke everything now" case |
| within 90 days past, up to `now + skewAllowance` | 204 | stored, expires at `timestamp + 90d` |
| further than 90 days in the past | 204 | accepted, then **discarded on write** |
| beyond `now + skewAllowance` | 400 | rejected |

The third row is the non-obvious one. A rule's lifespan is `timestamp + 90d - now`, and `storeMapItem`
removes rather than stores when that is non-positive — otherwise Infinispan would read the negative value
as *never expire* and the oldest rules would be the only permanent ones (the trap in 2.2). Nothing is lost
by it: a PAT lives at most 90 days, so every token a rule that old could govern has already expired and
`parseJwtWithSignature` rejects it before any store is consulted. Only the timing of the cleanup changes —
previously such an entry was written and swept later by `removeNonRelevantRules`. `AccessTokenServiceTest`'s
old-timestamp case (`1582239600000`) exercises this and keeps passing unchanged.

What that row costs is honesty in the response: a 204 for a write that had no effect. Making the guard
symmetric — rejecting an already-elapsed retention with a 400, so that "accepted" always means "stored" —
would change the status code that integration test asserts, so it is a deliberate call rather than a
tidy-up, and it is not taken here.

**The skew allowance is a client-clock tolerance, not the cutover one.** It absorbs the difference between
the caller's clock and the node's, for a client that computes "now" itself and sends it; without it an
entirely ordinary revocation from a slightly fast client answers 400. It is unrelated to
`cutoverSkewAllowanceSeconds`, which covers skew *between ZAAS nodes* placing a token either side of the
epoch, and where being too small silently drops pre-cutover revocations — hence the guidance there to err
generous. Here erring generous buys nothing, so the value only has to stay negligible against the 90-day
retention: a rule accepted 60 seconds early expires 60 seconds before its nominal authority ends, about
10^-5 of the retention, against the year-long gap that an unbounded future timestamp would open. A minute
also matches the leeway conventionally allowed for JWT `nbf`/`exp`. Unlike the cutover allowance this is a
constant rather than a knob, which is the one place a site with unsynchronised clocks has no remedy other
than fixing its time source.

**Kill switch, now real.** Setting `cutoverEpoch` to the present instant invalidates nothing by itself
under routing — it sends every outstanding PAT to the legacy path. If an actual "invalidate every PAT
now" control is wanted (key compromise, say), it needs to be its own explicit switch rather than a side
effect of this value; note it as a follow-up rather than claiming it here.

### 2.5 Take live writes off the lock; retire the lock itself with the legacy path
The `ClusteredLock` exists only to serialise the whole-map read-modify-write. Per-key entries need
none of it: `storeMapItem` against the new cache is one atomic `put`, expiry is the reaper, and
removals become CAS `remove(key, expectedValue)`. **Every live revocation stops taking the lock in this
release** — which is the win, and it also eliminates 1.1's silent-failure shape by construction for the
path that matters.

**And the lock object goes with them, in this release.** Because 1.2 drops the scheduled job and
nothing — not revocations, not the public `POST /cache-list/{mapKey}` API, not eviction — performs a
whole-map read-modify-write any more, no caller needs the lock at all. Drop `lockSupplier` and
`completeJoin` from `InfinispanStorage`, and `LOCK_ZOWE_INVALIDATED`, `zoweInvalidatedTokenLock` and
`lock(CacheContainer)` from `InfinispanConfig` (`:68,125,275-291`). Keep the
`infinispan-clustered-lock` dependency for one more release — `defineLock` created an internal
replicated `org.infinispan.LOCKS` cache that older nodes still use.

The legacy cache becomes strictly **read-only** the moment this release starts: consulted by 2.4's
routing path and by nothing else, never written, never evicted, never locked.

`removeNonRelevantTokens` / `removeNonRelevantRules` and the admin `/access-token/evict` endpoints stay
for API compatibility, reimplemented as plain per-item removals against the new cache. They do *not*
touch the legacy layout — an operator who wants the legacy data gone deletes the directory (2.7). With
TTL they are a safety net rather than the primary mechanism.

### 2.6 Upgrade, rollback and mixed-version windows
Everything in this table is a consequence of the decisions above, not new machinery. It exists so the
PR description, the release note and support all say the same thing.

| Situation | Behaviour | Action required |
|---|---|---|
| PAT issued before the upgrade | keeps working; consults the legacy store as well as the new one | none — this is the point of 2.4 |
| Revocation made before the upgrade | still enforced, via the legacy read path, until the token expires | none |
| Revocation made after the upgrade | written only to the new cache; enforced immediately once every caching-service instance (or every modulith instance, in HA) runs this release | none |
| ZAAS peer not yet upgraded, caching-service already upgraded (split deployment) | writes through `POST /cache-list/{mapKey}`, which lands in the new cache, and reads through `GET /cache-list`, which the upgraded caching-service answers from both layouts, so it misses nothing | none |
| Caching-service cluster, or HA modulith, running two releases (rolling upgrade) | a revocation made through a previous-release instance goes into the legacy layout. Upgraded instances see the write (`ZWECS705`), set a marker in the revocation store, and from then on also check the legacy layout on every `cache-query`, until 90 days after the last such write, so the revocation is enforced. The reverse cannot be fixed: previous-release instances cannot read revocations made through upgraded ones | upgrade the instances back to back; once all run this release, repeat any revocation made while they did not. Release-note it |
| **Downgrade to the previous release** | revocations made while on this release are in the new cache, which the old code cannot read — those PATs become valid again | re-run the revocations after a downgrade. This is the accepted cost of not dual-writing |
| caching-service older than ZAAS | startup probe fails, catalogued error, ZAAS falls back to `readAllMaps()` for all tokens (2.3) | upgrade the caching-service; the log names the minimum version |
| Store wiped / epoch re-minted | pre-cutover tokens revert to the legacy path; the wipe itself has already destroyed the salt and every revocation | investigate the wipe; PAT auth keeps working |
| ZAAS node clocks disagree | tokens near the boundary take the legacy path | none, within the skew allowance |

**The sunset release.** One release after this one, and no earlier than
`patCutoverEpoch + 90 days`, remove in a single change: the 2.4 branch and the stored epoch key, the
config property, `legacyIsInvalidated`, `CachingServiceClient.readAllMaps`,
`InfinispanStorage.getAllMaps`/`getAllMapItems` and their endpoints, the 2.3 startup probe and its
message, the dedicated legacy-cache read described below, and the
`CACHE_ZOWE_INVALIDATED_TOKEN` definition. By then the branch is already inert, so this is deletion
rather than behaviour change.

**Note what the legacy read is and is not.** 2.1 re-points the public `GET/POST /cache-list` endpoints
at the new per-item cache, so `readAllMaps`/`getAllMaps` cannot simply be "kept as they are" to serve
2.4 — they would return the new layout. The routing path needs its own narrow, legacy-only read of
`CACHE_ZOWE_INVALIDATED_TOKEN` (`Storage`, `InfinispanStorage`, one client method, one endpoint),
clearly named as such. That is a small amount of net-new code whose entire purpose is to be deleted,
which is the right shape: the public contract stays on the new layout from day one, and the thing being
retired is isolated behind one method rather than tangled into the endpoints that survive. Verify no `getCache(CACHE_ZOWE_INVALIDATED_TOKEN)` call
site survives: `DefaultCacheManager.getCache` on an undefined name silently recreates it from the
*default* configuration — REPL_SYNC plus a soft-index file store — which is risk item 6 in reverse.

### 2.7 Retiring the legacy store on disk
Removing the cache definition does not remove its data. Each persistent cache is a directory
`<workspace>/caching-service/<ZWE_haInstance_id>/<cacheName>/{data,index}`, and with the default 256
segments an *empty* one still costs ~1 MB across 258 index files. Nothing in the product has ever
cleaned one up: a local workspace still carries `validationJwtToken/` from a cache renamed to
`validatedJwtTokens`, plus directories for caches that have since become non-persistent simple caches.

**Disposal is the operator's decision, not ours.** Ship a documented script rather than deleting
anything automatically — some sites will want to keep the directory for forensics, and a revocation
store is not something to remove out from under someone by surprise.

**Two moments to run it, with different consequences**, and the documentation has to distinguish them
because the script cannot:

- **Any time after the upgrade.** Reclaims the disk immediately and makes every PAT request take the
  fast path at once. Cost: every revocation made *before* the upgrade stops being enforced, so a
  pre-upgrade PAT that was revoked works again until it expires. Sites that want this should re-revoke
  anything that still matters. This is the "start clean" option in 1.2.
- **After the sunset release**, or at any point more than 90 days after the cutover. No consequence at
  all: nothing reads the directory any more and every pre-cutover token has expired regardless.

Requirements:

- Refuse to run while the service is up; the directory must not be live when it is moved.
- Default to renaming (`<cacheName>.retired-<timestamp>`) rather than deleting, and report reclaimed
  size, so the destructive step is a second, explicit choice.
- Touch only named cache subdirectories. Never `___global.state`/`___global.lck` — those are shared,
  and `LazyCacheManager` has its own recovery path for them — and never `zoweCache`, which holds the
  salt.
- Take the cache name as an argument and list retirable candidates, so the same script also reclaims
  the pre-existing orphans above. That is worth doing independently of this design.
- Document it in `caching-service/README.md` alongside the note that the directory is safe to keep,
  and state that keeping it costs disk only — nothing reads it after the sunset release.



---

## Risks and things to verify during implementation

1. **SoftIndexFileStore disk reclamation on expiry is unverified.** Infinispan honours `lifespan` and
   the reaper removes entries from memory and store, but whether compaction actually reclaims disk
   for 10^5–10^6 expiring entries needs a soak test. `maxCount` plus a periodic
   `getExpirationManager().processExpiration()` nudge are the fallbacks.
2. **`transient` + Jackson** (2.2) — prove with a serialization test, don't assume. (`equals`/
   `hashCode` need no such test — Lombok excludes `transient` fields from both by construction;
   `toString` does need one, see 2.2.) The upgrade-critical assertion is a *cross-version* one, not a
   Jackson one: `KeyValue` is the value type of `zoweCache`, which is REPL_SYNC and persisted and holds
   the salt, so round-trip it in **both** directions through the JBoss marshaller between the old and
   new class shapes. Note that 2.2's stated fallback — dropping `transient` in favour of
   `@JsonProperty` on the getter — makes the serialized form no longer byte-identical, so if it is ever
   taken it needs that test to pass first, not after.
3. **`AdvancedCache.getAll` on a REPL_SYNC cache should be node-local** (no RPC) — confirm in an
   embedded-Infinispan test, since the whole hot-path win rests on it.
4. **`getAllMaps` gets slower**, not faster: O(N) per-entry keys instead of a 3-entry scan. Only
   legacy, test and manual paths hit it, but say so.
5. **`AccessTokenServiceTest:284` asserts `body("content", not(containsString(...)))`, but `content`
   is not a key in the `/cache-list` response** — that assertion is already vacuous today. Do not
   treat it as protection; strengthen it while you are in there.
6. **Register the new cache in `InfinispanConfig`, or it silently becomes REPL_SYNC + persisted.**
   `DefaultCacheManager.getCache(name)` creates an unknown cache from the *default* configuration,
   which `getCacheManagerConfig` sets to REPL_SYNC with a soft-index file store. Forgetting the
   registration would replicate and persist to disk without any error. Add a unit assertion that the
   name is present in the map handed to `LazyCacheManager`, and that its config is the expiring one.
7. **No longer applicable — kept as a record of why.** An earlier draft added `@EnableScheduling` to
   ZAAS, which would have activated a scheduler in a service that never had one and made
   `ScheduledAnnotationBeanPostProcessor` inspect every bean on the classpath. 1.2 drops the job, so no
   scheduler is introduced anywhere and `spring.task.scheduling.pool.size` is untouched.
8. **No leader election is needed anywhere (see 1.2).** If a periodic job is ever revisited, note that
   `AuthenticationService.invalidateTokenOnAnotherInstance` is not reusable prior art — it fans a
   DELETE out to every *other* known instance, the opposite of "elect one and skip the rest."
9. **The `patCutoverEpoch` create-if-absent race (2.4)** needs the same test coverage
   `SaltInitialization` already has for the analogous salt race: two nodes hitting an absent epoch at
   once must converge on the same value, not each mint their own.
10. **The legacy map is at its maximum size exactly when the routing path starts reading it, and
    nothing shrinks it.** On upgrade `invalidTokens` holds everything accumulated since the site
    enabled PAT, and 2.4 puts that map on the read path for every pre-cutover token. 1.4's `expiresAt`
    check stops expired entries from *matching*, but they still cross the wire. Measure that payload on
    a realistic dataset: it is the number that tells an operator whether to keep the legacy directory
    or delete it (1.2), and it is the worst case for the whole sunset window rather than a transient.
11. **An already-issued PAT with more scopes than the new issuance cap cannot be recovered** (2.3).
    This is the only way an existing PAT can break in this design. Choose the constant against field
    data, not intuition.
12. **Every new operator-facing knob needs three coordinated edits**, none of which the code will
    complain about if missed: a `-D` in the component's `bin/start.sh`, a default in its
    `manifest.yaml` `configs:`, and a typed entry in its `schemas/*-config.json`. `distributedSyncTimeoutSecs`
    and `numSegments` are already in `InfinispanConfig` and in none of the three, so they are currently
    unreachable from `zowe.yaml` — evidence that this is missed by default rather than occasionally.
13. **PAT revocation only functions on Infinispan.** `InMemoryStorage`, `RedisStorage` and
    `VsamStorage` throw `INCOMPATIBLE_STORAGE_METHOD` for every map operation, and
    `PATAuthSourceService.isValid` fails closed, so PAT authentication cannot work at all on those
    backends. The standalone caching-service defaults to `inMemory`
    (`caching-service/src/main/resources/application.yml:6`, `manifest.yaml`, `start.sh:150`); the
    modulith defaults to `infinispan` (`apiml/src/main/resources/application.yml:267-269`). Both the
    defect and this fix are therefore scoped to Infinispan deployments — say so in the PR description
    so the blast radius is not overstated.
14. **Nothing to do, recorded so it is not re-litigated:** the new `/cache-query` endpoint is protected
    automatically. `caching-service/.../SpringSecurityConfig.java:64-94` is deny-by-default with an
    explicit ignore list, so it requires the client certificate with no extra configuration.

---

## Files to change

One release; grouped by area rather than by phase.

**Hardening the existing path (Part 1)**
- `caching-service/src/main/java/org/zowe/apiml/caching/service/infinispan/storage/InfinispanStorage.java` — lock-failure fix, size warning
- `caching-service/.../infinispan/config/InfinispanConfig.java` — lock timeout + size threshold properties
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java` — salt memoization, `expiresAt` check, resilient evict
- `zaas-service/src/main/resources/zaas-log-messages.yml` — `ZWEAZ602` store-too-large warning, plus
  the catalogued messages for an epoch mint (2.4) and a version-skew fallback (2.3)

**New storage layout (Part 2)**
- `common-service-core/src/main/java/org/zowe/apiml/cache/Storage.java` — batch lookup method
- `common-service-core/src/main/java/org/zowe/apiml/caching/model/KeyValue.java` — `ttlSeconds`
- `caching-service/.../infinispan/storage/InfinispanStorage.java` — per-item layout, TTL
- `caching-service/.../infinispan/config/InfinispanConfig.java` — new cache definition
- `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java` — batch endpoint
- `caching-service/.../inmemory/InMemoryStorage.java`, `.../redis/RedisStorage.java`, `.../vsam/VsamStorage.java` — `INCOMPATIBLE_STORAGE_METHOD` for the new method
- `zaas-service/.../cache/{CachingClient,CachingServiceClient,LocalCachingClient}.java` — batch lookup
- `zaas-service/.../security/service/token/ApimlAccessTokenProvider.java` — rewritten `isInvalidated`,
  the `patCutoverEpoch` routing branch and its resolution order (reuses the existing generic
  `read`/`create` — no new client method), scope cap at issuance
- `zaas-service/.../controllers/AuthController.java`, `apiml/.../controller/ReactivePATController.java` —
  bound the revoke `timestamp`, reject over-cap scope lists, and add the missing
  `@ExceptionHandler` for `CachingServiceClientException` → 503 (this is what makes both 1.1's 503 and
  2.3's version-skew failure diagnosable rather than a 500 with a stack trace)
- `caching-service/README.md` — document the new endpoint, and the retirement script from 2.7

**Packaging and operator-facing configuration** — required for every new knob, and missing from earlier
drafts of this list. Each knob needs all three:
- `caching-service-package/src/main/resources/bin/start.sh`, `zaas-package/.../bin/start.sh`,
  `apiml-package/.../bin/start.sh` — a `-D` mapped from the matching `ZWE_configs_*` variable
- the same packages' `manifest.yaml` — a default under `configs:`
- `caching-service-package/.../schemas/caching-service-config.json`,
  `zaas-package/.../schemas/zaas-config.json` — a typed, described entry
- knobs in scope: `caching.storage.infinispan.lockTimeoutSeconds`, the size-warning threshold, the new
  cache's `maxCount`, the batch-lookup key limit, the scope cap,
  and `apiml.security.personalAccessToken.cutoverEpoch` with its skew allowance.
  Backfill `distributedSyncTimeoutSecs` and `numSegments` while in there (see risk 12)
- a retirement script for 2.7, shipped with `caching-service-package`

## Verification

- Unit: `caching-service/src/test/java/.../infinispan/storage/InfinispanStorageTest.java` (nested
  `WhenStoreToken`, `WhenRetrieveInvalidTokensAndRules`, `WhenEvictNonRelevantTokensAndRules`,
  `WhenRetrieveToken` all need updating for the new layout); add cases for lock-timeout →
  `StorageException`, TTL expiry, and negative-TTL skip. Its `createCache()` helper already mocks
  `org.infinispan.Cache` but only stubs the two-arg `put`, so it needs a stub for the
  `put(k, v, lifespan, unit)` overload; assert TTL via `verify` rather than by waiting for real
  expiry.
- Unit: `zaas-service/src/test/java/.../token/ApimlAccessTokenProviderTest.java` (incl.
  `SaltInitialization` and `WhenCallingEviction`) — assert the salt is read once within the refresh
  window and re-read after it; that a failed refresh serves the stale salt and the next call retries;
  that a failure is never memoized; that a salt *regeneration* (not just a refresh) logs the new
  operator-visible warning; and that `isInvalidated` issues a batch lookup rather than `readAllMaps`.
  This last point isn't just a new assertion to add: `givenSameToken_returnInvalidated`,
  `givenDifferentToken_returnNotInvalidated`, `givenTokenWithUserIdMatchingRule_returnInvalidated` and
  `givenTokenWithScopeMatchingRule_returnInvalidated` all currently control `isInvalidated`'s outcome
  purely by stubbing `readAllMaps()` — each needs to be rewritten against the new batch-lookup mock
  while preserving its distinct same-token/different-token/user-rule/scope-rule coverage, not just
  patched to compile. For 1.4, the sharp case is an expired token record **plus** a matching
  `invalidUsers` rule — it must still return `true`. That is the regression guard for
  `Optional.empty()` vs `Optional.of(false)`. Also add a case for a keyed-but-unparseable
  `invalidTokens` entry returning `true` (the adjacent fail-open fix in 1.4).
- Unit: a `WhenLockCannotBeAcquired` nested class in `InfinispanStorageTest` — `storeMapItem` throws
  `CACHE_NOT_AVAILABLE`/503 and never writes; `removeNonRelevant*` do not throw; `unlock()` is not
  called when the lock was never taken, but *is* called when the body throws. Also cases where a
  single entry has `expiresAt == null` or a non-numeric rule value — `removeNonRelevant*` must skip
  that entry without throwing and without aborting cleanup of the rest of the map. Plus a
  `CachingControllerTest` case asserting the 503 reaches the HTTP layer.
- Unit: a size-threshold-warning test for `storeMapItem`/`ZWEAZ602` — simulate crossing the threshold
  across many consecutive writes and assert the log fires once (or at the intended cadence), not once
  per write.
- Unit: `patCutoverEpoch` resolution and routing — the configured property wins over a stored value
  and over minting; mirroring `SaltInitialization`, two concurrent create-if-absent calls converge on
  the same value rather than each minting their own; a *spurious* "absent" (a 404 with the key still
  present) does not overwrite it; a token whose `creation` predates the threshold consults **both** the
  legacy path and the batch lookup and is invalidated if either says so; a token after the threshold
  never touches `readAllMaps`; a token within the skew allowance takes the legacy path; and once
  `now > epoch + sunset` the branch is not taken at all even for a very old `creation`.
- Unit: the version-skew probe (2.3) — a 404/405 from `/cache-query` at startup logs the catalogued
  error and leaves ZAAS serving PATs via `readAllMaps()`, rather than rejecting them.
- Unit: the scope cap — `generatePat` rejects an over-cap scope list with a 400 naming the limit, and
  the batch-lookup limit is derived from the same constant.
- Unit: `caching-service/src/test/java/.../api/CachingControllerTest.java`, and
  `apiml/src/test/java/org/zowe/apiml/controller/ReactivePATControllerTest.java`.
- Integration: `integration-tests/src/test/java/org/zowe/apiml/integration/authentication/pat/AccessTokenServiceTest.java`
  must keep passing unchanged — it encodes the revoke/validate/evict semantics, including the
  old-timestamp rule case. Also `.../pat/PATWithAllSchemesTest.java` and
  `integration-tests/.../ha/CachingServiceTests.java`, which exercises `GET /cache-list` and
  `GET /cache-list/{mapKey}` with a non-PAT map key (`"aMap"`) holding a non-JSON, non-numeric value —
  the best available proof that the REST contract survives the new layout. Make sure it runs in the HA lane.
- Integration (multi-instance): revoke on instance A, then assert instance B rejects the token on its
  very next request. With no local caching this must be immediate; it is the test that pins the
  no-staleness decision.
- Integration (upgrade): issue a PAT on the previous build, upgrade, and assert it **still
  authenticates** — this is the test that pins 2.4's routing decision, and it is the exact inverse of
  what an earlier draft of this document called for. Then revoke it on the new build and assert the
  next request fails, proving the legacy and new stores are both consulted for a pre-cutover token.
- Integration (downgrade): on the new build, revoke a PAT; downgrade; assert the PAT is valid again and
  that the documented remedy — re-running the revocation — restores it. This pins the accepted cost of
  not dual-writing rather than leaving it as an assumption.
- Integration (version skew): new ZAAS against a previous-release caching-service — PAT authentication
  keeps working via the fallback and the catalogued error is logged.
- Manual end-to-end (`npm run api-layer` with `apiml.security.personalAccessToken.enabled=true`):
  generate a PAT, use it against a routed service, revoke it, confirm the very next request is
  rejected (no local caching means this must be immediate), then confirm the entry disappears on its
  own once past `expiresAt`.
- Performance sanity: revoke a few thousand tokens, then measure PAT request latency and the
  caching-service response size for a **post**-cutover token — flat with respect to the number of
  revocations, versus linear today. Measure a **pre**-cutover token separately and expect today's
  linear behaviour; that is the number an operator needs in order to make the keep-or-delete choice in
  1.2.
- Disk: after the sunset release, assert nothing recreates the legacy cache directory — including with
  a previous-release peer in the cluster, which is the case that tests the `getCache`-on-an-undefined-name
  resurrection path in 2.6.
