# Plan: Upgrade react-router-dom from v5.3.4 to v7.x

**Status:** Draft  
**Author:** Mistral Vibe  
**Date:** 2026-05-19  
**Related PR:** #4534 (Dependencies upgrade before releasing 2.18.5)

> **Revision note (2026-05-19):** Target upgraded from v6 to v7 to align with the `api-layer` v3.x.x codebase, which already runs `react-router@7.15.1`. React Router v7 also resolves the `path-to-regexp` conflict without removing the security override (see §3.2).

---

## Objective

Upgrade `react-router-dom` from version `5.3.4` to the latest stable v7.x to enable the security upgrade of `path-to-regexp` to version `8.4.0`. This resolves the compatibility conflict where the global override of `path-to-regexp@8.4.0` breaks `react-router-dom@5.x` which expects `path-to-regexp@1.x` API.

Targeting v7 (over v6) aligns with `api-layer` v3.x.x — which already uses `react-router@7.15.1` — and consolidates the team to a single router version across both codebases.

---

## Problem Statement

PR #4534 introduces a security upgrade for `path-to-regexp` from `1.9.0` to `8.4.0` via npm overrides. However, this causes the following error:

```
TypeError: pathToRegexp is not a function
    at pathToRegexp (node_modules/react-router/modules/matchPath.js:14:18)
```

**Root Cause:**
- `react-router-dom@5.3.4` bundles `path-to-regexp@1.8.0` and expects its CommonJS API
- `path-to-regexp@8.4.0` uses ES Modules with a different export structure
- The global override forces v8.4.0, breaking react-router's internal usage

**Why v7 solves this:** React Router v7 (and v6) does not depend on `path-to-regexp` at all — it handles path matching internally. The override can remain in place for other packages that still transitively depend on older, vulnerable versions.

---

## Scope

This upgrade affects:
- `api-catalog-ui/frontend` — Uses react-router-dom extensively for API catalog navigation
- `metrics-service-ui/frontend` — Uses react-router-dom for metrics UI navigation
- All components using routing features (Routes, Links, navigation, etc.)

---

## Timeline

| Phase | Duration | Start Date | End Date | Status |
|-------|----------|------------|----------|--------|
| Phase 1: Research & Audit | 1-2 days | TBD | TBD | Not Started |
| Phase 2: Branch Setup | 1 day | TBD | TBD | Not Started |
| Phase 3: Dependency Update | 1 day | TBD | TBD | Not Started |
| Phase 4: Code Migration | 3-5 days | TBD | TBD | Not Started |
| Phase 5: Testing | 2-3 days | TBD | TBD | Not Started |
| Phase 6: Integration with PR #4534 | 1 day | TBD | TBD | Not Started |
| **Total** | **1-2 weeks** | | | |

---

## Phase 1: Research & Preparation

### 1.1 Identify Security Requirements
- [x] Document the specific CVE(s) in path-to-regexp@1.x that require upgrading to 8.4.0
- [x] Confirm react-router@7.x has no dependency on path-to-regexp (verify with `npm list path-to-regexp` after install)

**CVEs in path-to-regexp@1.x:**
| CVE | Description | Fixed Version |
|-----|-------------|---------------|
| CVE-2024-45296 | ReDoS via backtracking regular expressions when two parameters exist in a single path segment (e.g., `/:a-:b`) | 8.4.0 |
| CVE-2024-52798 | Incomplete fix for CVE-2024-45296, ReDoS vulnerability | 8.4.0 |
| CVE-2026-4923 | ReDoS via multiple wildcards — **unverified, check NVD/GitHub advisory DB before citing** | 8.4.0 |
| CVE-2026-4926 | DoS via sequential optional groups — **unverified, check NVD/GitHub advisory DB before citing** | 8.4.0 |

**Reference:**
- [GHSA-27v5-c462-wpq7](https://github.com/pillarjs/path-to-regexp/security/advisories/GHSA-27v5-c462-wpq7)
- CVE-2026-4923 / CVE-2026-4926 — links TBD, verify against NVD and GitHub advisory DB before sharing externally

**Confirmation:** React Router v7 (and v6) removed the `path-to-regexp` dependency entirely. The library now handles path matching internally, eliminating the version conflict. Source: [React Router FAQ](https://reactrouter.com/en/main/start/faq)

### 1.2 Audit Current Usage
- [x] Search codebase for all react-router imports
- [x] Catalog all used components and hooks

**api-catalog-ui/frontend:**

| Component/Hook | Files | Count | v7 Migration Notes |
|----------------|-------|-------|-------------------|
| `HashRouter` | `index.js:20` | 1 | Replace with `HashRouter` from `'react-router'` |
| `Router` | `App.jsx:11`, `DetailPage.jsx:13` | 2 | v7 no longer accepts `history` prop; use `HashRouter`/`BrowserRouter` |
| `MemoryRouter` | `App.test.jsx:12` | 1 | Import from `'react-router'` |
| `Switch` | `App.jsx:11`, `DetailPage.jsx:13` | 2 | Replace with `Routes` |
| `Route` | `App.jsx`, `DetailPage.jsx`, `ServicesNavigationBar/ServicesNavigationBar.jsx` | 3+ | Replace `component={X}` with `element={<X />}`; remove `exact` prop |
| `Redirect` | `App.jsx:11`, `DetailPage.jsx:13` | 2 | Replace with `Navigate` component |
| `Link` (as RouterLink) | `ServicesNavigationBar/ServicesNavigationBar.jsx:13` | 1 | Import from `'react-router'` |
| `withRouter` HOC | `AppContainer.jsx:10`, `ServiceTabContainer.jsx:11`, `LoginContainer.jsx:11` | 3 | Remove HOC, convert to functional components with hooks |
| `history` package | `helpers/history.jsx:11`, `actions/user-actions.jsx:13` | 2 | Singleton imported by Redux actions (`login`, `logout`, `authenticationFailure`); same pattern as metrics-service-ui — needs refactor (see §1.4b) |

**metrics-service-ui/frontend:**

| Component/Hook | Files | Count | v7 Migration Notes |
|----------------|-------|-------|-------------------|
| `HashRouter` | `index.js:14` | 1 | Replace with `HashRouter` from `'react-router'` |
| `Router` | `App.jsx:12` | 1 | v7 no longer accepts `history` prop; use `HashRouter`/`BrowserRouter` |
| `Switch` | `App.jsx:12` | 1 | Replace with `Routes` |
| `Route` | `App.jsx:12`, `AuthRoute.jsx:12` | 2 | Replace `component={X}` with `element={<X />}`; `render={fn}` with `element={fn()}` |
| `Redirect` | `App.jsx:12`, `AuthRoute.jsx:12` | 2 | Replace with `Navigate` component |
| `withRouter` HOC | `AppContainer.jsx:11` | 1 | Remove HOC; App.jsx is a class component — needs conversion |
| `history` package | `helpers/history.jsx:11`, `actions/user-actions.jsx:13` | 2 | Direct `history.push()` in Redux actions; needs refactor (see §1.4b) |

**Summary of Breaking Changes Found:**
- **12 files** in api-catalog-ui import from `react-router-dom`
- **7 files** in metrics-service-ui import from `react-router-dom`
- **No hooks currently used** (useHistory, useLocation, useParams, useRouteMatch) — all navigation uses class components or direct history package
- **Critical:** 2 class components wrapped with `withRouter` that use `Router` with explicit `history` prop (App.jsx in both UIs)
- **Critical:** Direct `history.push()` calls in Redux actions in **both** UIs — `api-catalog-ui/src/actions/user-actions.jsx` (`login`, `logout`, `authenticationFailure`) and `metrics-service-ui/src/actions/user-actions.jsx` (`login`, `logout`, `authenticationFailure`); additionally `DetailPage.jsx` calls `this.props.history.push()` via injected prop
- **Note:** `NavLink` is NOT used in either codebase; only `Link` (RouterLink) in api-catalog-ui

### 1.3 Review Breaking Changes
- [x] Study the [official v5 to v6 migration guide](https://reactrouter.com/en/main/upgrading/v5) and the [v6 to v7 upgrade guide](https://reactrouter.com/upgrading/v6)
- [x] Document all breaking changes that affect this codebase

**v5 → v7 Breaking Changes Impacting This Codebase:**

| v5 | v7 | Impact | Files Affected |
|----|----|--------|----------------|
| `<Switch>` | `<Routes>` | **HIGH** | App.jsx (both UIs), DetailPage.jsx |
| `<Route component={X} />` | `<Route element={<X />} />` | **HIGH** | App.jsx, AuthRoute.jsx, DetailPage.jsx |
| `<Route render={fn} />` | `<Route element={fn()} />` | **HIGH** | App.jsx (both UIs), AuthRoute.jsx |
| `<Route children={fn} />` | `<Route element={fn()} />` | **MEDIUM** | ServicesNavigationBar.jsx (uses `children` for Link) |
| `useHistory()` | `useNavigate()` | **LOW** | Not currently used in codebase |
| `useRouteMatch()` | `useMatch()` | **LOW** | Not currently used in codebase |
| `Redirect` | `Navigate` | **HIGH** | App.jsx (both UIs), AuthRoute.jsx, DetailPage.jsx |
| `activeClassName` on `NavLink` | `className` function prop | **NONE** | `NavLink` not used in either UI |
| `withRouter` HOC | Use hooks directly | **HIGH** | AppContainer.jsx (both UIs), ServiceTabContainer.jsx, LoginContainer.jsx — ServiceTab.jsx and App.jsx are class components requiring conversion, not just a hook swap |
| `import from 'react-router-dom'` | `import from 'react-router'` | **HIGH** | All 19 files importing react-router |
| `<Router history={h}>` | `<HashRouter>` / `<BrowserRouter>` | **CRITICAL** | App.jsx (both UIs), DetailPage.jsx |
| `exact` prop on `<Route>` | Removed (routes match exactly by default) | **MEDIUM** | App.jsx (both UIs), DetailPage.jsx |

**v6 → v7 Additional Changes:**
| v6 | v7 | Impact | Notes |
|----|----|--------|-------|
| `react-router-dom` package | `react-router` package | **HIGH** | Canonical import is now `react-router`; `react-router-dom` is a shim |
| `history` peer dependency optional | `history@5.x` still compatible | **MEDIUM** | Direct `history` usage needs migration strategy (see §1.4) |
| `React.StrictMode` edge cases | Improved handling | **LOW** | Usually transparent; no action needed |

**Priority Assessment for This Codebase:**
1. **CRITICAL:** `<Router history={h}>` pattern in class components (App.jsx both UIs, DetailPage.jsx) — v7 no longer supports explicit history prop
2. **CRITICAL:** Direct `history.push()` in Redux action creators in **both** UIs (`user-actions.jsx`) — must be replaced before removing the `history` singleton
3. **HIGH:** Import path changes from `'react-router-dom'` to `'react-router'` (19 files)
4. **HIGH:** `withRouter` HOC removal (4 files: AppContainer both UIs, ServiceTabContainer, LoginContainer) — ServiceTab and App are class components requiring conversion
5. **HIGH:** `BigShield.jsx` receives `history` via prop drilling from App and DetailPage — needs hooks conversion and call-site cleanup
6. **HIGH:** `<Switch>` → `<Routes>` (3 files)
7. **HIGH:** `Redirect` → `Navigate` (4 files)
8. **HIGH:** `<Route component={X}>` → `<Route element={<X />}>` (multiple files)
9. **MEDIUM:** `exact` prop removal (routes match exactly by default in v7)
10. **MEDIUM:** `history@4.10.1` → `history@^5.3.0` upgrade (or removal if Option A/C chosen)

### 1.4 Identify Class Components and Imperative Navigation
- [x] Identify all class components using react-router features
- [x] Identify all imperative navigation patterns (direct history usage)

**Known issues requiring special handling in this codebase:**

**a) Class Components with `withRouter` and `<Router history={...}>` pattern**

| File | Issue | Component Type | Props Used |
|------|-------|----------------|------------|
| `metrics-service-ui/src/components/App/App.jsx` | Class component, wraps `<Router history={this.props.history}>` | Class | `history` prop from `withRouter` |
| `metrics-service-ui/src/components/App/AppContainer.jsx` | Wraps App with `withRouter` HOC | HOC | - |
| `api-catalog-ui/src/components/App/App.jsx` | Class component, wraps `<Router history={this.props.history}>` | Class | `history` prop from `withRouter` |
| `api-catalog-ui/src/components/App/AppContainer.jsx` | Wraps App with `withRouter` HOC | HOC | - |
| `api-catalog-ui/src/components/DetailPage/DetailPage.jsx` | Class component; uses `this.props.history.push('/dashboard')` in `handleGoBack()` and `this.props.history.location.pathname` in `render()` | Class | `history` prop from `withRouter` via `DetailPageContainer.jsx` |
| `api-catalog-ui/src/components/ErrorBoundary/BigShield/BigShield.jsx` | Class component; receives `history` as a prop passed down from `App.jsx` and `DetailPage.jsx`; calls `history.push('/dashboard')` and reads `history.location.pathname` | Class | `history` prop passed explicitly (not via `withRouter`) |
| `api-catalog-ui/src/components/ServiceTab/ServiceTabContainer.jsx` | Redux-connected **class** component (`ServiceTab extends Component`) wrapped with `withRouter(connect(...)(ServiceTab))` | Class (via connect) | `match`, `location`, `history` |
| `api-catalog-ui/src/components/Login/LoginContainer.jsx` | `withRouter(connect(...)(Login))` — check `Login.jsx` to confirm if class or functional | TBD | `history` |

**Migration Strategy for Class Components:**
- **Option A (preferred):** Convert to functional components and use React Router v7 hooks (`useNavigate`, `useLocation`, `useMatch`)
- **Option B (shim):** Use `unstable_HistoryRouter` with shared history instance (preserves class components)

**Recommendation:** Use **Option A** for App.jsx in both UIs and for LoginContainer. `DetailPage.jsx`, `BigShield.jsx`, and `ServiceTab.jsx` are larger class components — Option A is still preferred but requires more careful refactoring. Note that `BigShield.jsx` receives `history` via direct prop drilling (not `withRouter`), so its parent callers must also be updated to stop passing `history` and instead let `BigShield` call `useNavigate()` directly. See specific examples in Phase 4.

**b) Imperative `history.push()` in Redux actions**

| File | Usage | Location |
|------|-------|----------|
| `metrics-service-ui/src/actions/user-actions.jsx` | `history.push('/dashboard')` | `login()` on success |
| `metrics-service-ui/src/actions/user-actions.jsx` | `history.push('/login')` | `logout()` on success |
| `metrics-service-ui/src/actions/user-actions.jsx` | `history.push('/login')` | `authenticationFailure()` |
| `api-catalog-ui/src/actions/user-actions.jsx` | `history.push('/dashboard')` | `login()` on success |
| `api-catalog-ui/src/actions/user-actions.jsx` | `history.push('/login')` | `logout()` on success |
| `api-catalog-ui/src/actions/user-actions.jsx` | `history.push('/login')` | `authenticationFailure()` |
| `api-catalog-ui/src/components/DetailPage/DetailPage.jsx` | `this.props.history.push('/dashboard')` | `handleGoBack()` method — uses injected prop, not singleton |
| `api-catalog-ui/src/components/DetailPage/DetailPage.jsx` | `this.props.history.location.pathname` | `render()` path parsing — uses injected prop, not singleton |
| `api-catalog-ui/src/components/ErrorBoundary/BigShield/BigShield.jsx` | `this.props.history.push('/dashboard')` | error recovery "go back" button |
| `api-catalog-ui/src/components/ErrorBoundary/BigShield/BigShield.jsx` | `this.props.history.location.pathname` | checks current path before redirecting |

**Migration Options for Imperative Navigation:**

- **Option A (recommended):** Pass `navigate` function as parameter to action creators:
  ```js
  // metrics-service-ui/src/actions/user-actions.jsx
  export const login = (credentials, navigate) => (dispatch) => {
    userService.login(credentials).then(
      (token) => {
        dispatch(success(token));
        navigate('/dashboard');  // Use injected navigate
      },
      (error) => dispatch(failure(error))
    );
  };

  // In component (e.g., LoginContainer.jsx after converting to functional):
  const navigate = useNavigate();
  dispatch(login(credentials, navigate));
  ```

- **Option B:** Use `unstable_HistoryRouter` with shared history instance:
  ```js
  // metrics-service-ui/src/helpers/history.jsx
  import { createHashHistory } from 'history';
  export const history = createHashHistory();

  // metrics-service-ui/src/index.js
  import { unstable_HistoryRouter as HistoryRouter } from 'react-router';
  import { history } from './helpers/history';
  root.render(
    <HashRouter>
      <HistoryRouter history={history}>
        <App />
      </HistoryRouter>
    </HashRouter>
  );

  // metrics-service-ui/src/actions/user-actions.jsx - unchanged
  import history from '../helpers/history';
  history.push('/dashboard');
  ```
  **Note:** `unstable_` prefix indicates this API may change in future minor releases.

- **Option C:** Move navigation to component layer via Redux state + useEffect:
  ```js
  // Action just dispatches auth state change
  export const login = (credentials) => (dispatch) => {
    userService.login(credentials).then(
      (token) => dispatch(success(token)),
      (error) => dispatch(failure(error))
    );
  };

  // Component watches auth state and navigates
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector(state => state.user);
  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard');
  }, [isAuthenticated, navigate]);
  ```

**Recommendation for This Codebase:**
- **metrics-service-ui:** Use **Option A** (pass navigate as parameter) — requires converting LoginContainer to functional component
- **api-catalog-ui DetailPage.jsx:** Use **Option A** — convert class component to functional, use `useNavigate()`
- Avoid Option B if possible due to `unstable_` API status
- Option C provides cleaner separation of concerns but requires more refactoring

**Decision Required Before Phase 4:** Choose and document the strategy for each file above.

### 1.5 Create Migration Cheatsheet
- [x] Document before/after examples for each breaking change found in the codebase
- [x] Document chosen strategy for class component and imperative navigation (§1.4)

**Deliverable:** Migration cheatsheet with project-specific examples

---

#### **Migration Cheatsheet for api-layer-v2 (v5.3.4 → v7.x)**

**Chosen Strategies:**
1. **Class Components:** Convert to functional components with hooks (Option A from §1.4)
2. **Imperative Navigation:** Pass `navigate` as parameter to action creators (Option A from §1.4)
3. **Package:** Use `react-router@^7.0.0` with imports from `'react-router'`
4. **History:** Remove direct `history` package usage where possible; upgrade to `history@^5.3.0` if still needed

---

##### **1. Import Path Changes**

| Before (v5) | After (v7) | Files Affected |
|--------------|-------------|----------------|
| `import { X } from 'react-router-dom'` | `import { X } from 'react-router'` | All 19 files |

**Example:**
```jsx
// Before
import { HashRouter, Route, Switch, Redirect } from 'react-router-dom';

// After
import { HashRouter, Routes, Route, Navigate } from 'react-router';
```

---

##### **2. `<Switch>` → `<Routes>`**

| Before (v5) | After (v7) | Files: App.jsx (both), DetailPage.jsx |
|--------------|-------------|---------------------------------------|

**Example from metrics-service-ui/App.jsx:**
```jsx
// Before
<Switch>
  <Route path="/login" exact render={null} />
  <Route component={HeaderContainer} />
</Switch>
<Switch>
  <AuthRoute path="/" exact render={() => <Redirect replace to="/dashboard" />} />
  <Route path="/login" exact render={(props, state) => <AsyncLoginContainer {...props} {...state} />} />
  <AuthRoute path="/dashboard" component={DashboardContainer} />
</Switch>

// After
<Routes>
  <Route path="/login" element={null} />
  <Route element={<HeaderContainer />} />
</Routes>
<Routes>
  <Route path="/" element={<Navigate replace to="/dashboard" />} />
  <Route path="/login" element={<AsyncLoginContainer />} />
  <Route path="/dashboard" element={<AuthRoute><DashboardContainer /></AuthRoute>} />
</Routes>
```

**Key Changes:**
- `Switch` → `Routes`
- Nested `AuthRoute` wraps the element (AuthRoute must be adapted to work as a wrapper)

---

##### **3. `<Route component={X} />` → `<Route element={<X />} />`**

| Pattern | Before | After |
|---------|--------|-------|
| component prop | `<Route path="/dashboard" component={DashboardContainer} />` | `<Route path="/dashboard" element={<DashboardContainer />} />` |
| render prop with function | `<Route path="/login" render={(props) => <Login {...props} />} />` | `<Route path="/login" element={<Login />} />` |
| render prop with null | `<Route path="/login" exact render={null} />` | `<Route path="/login" element={null} />` |
| children prop | `<Route path="/users">{() => <Users />}</Route>` | `<Route path="/users" element={<Users />} />` |

**Example from api-catalog-ui/App.jsx:**
```jsx
// Before
<Route
  exact
  path={dashboardPath}
  render={(props, state) => (
    <BigShield>
      <AsyncDashboardContainer {...props} {...state} />
    </BigShield>
  )}
/>

// After
<Route
  path={dashboardPath}
  element={(
    <BigShield>
      <AsyncDashboardContainer />
    </BigShield>
  )}
/>
```

**Note:** The `exact` prop is removed in v7 (routes match exactly by default).

---

##### **4. `<Redirect>` → `<Navigate>`**

| Before | After | Files: App.jsx (both), AuthRoute.jsx, DetailPage.jsx |
|--------|-------|-----------------------------------------------------|

**Example from AuthRoute.jsx:**
```jsx
// Before
if (!isAuthenticated) {
  return <Redirect replace to="/login" />;
}

// After
if (!isAuthenticated) {
  return <Navigate replace to="/login" />;
}
```

**Example from App.jsx (root redirect):**
```jsx
// Before
<Route path="/" exact render={() => <Redirect replace to={dashboardPath} />} />

// After
<Route path="/" element={<Navigate replace to={dashboardPath} />} />
```

---

##### **5. `withRouter` HOC → Hooks**

| Before | After | Files: AppContainer.jsx (both), ServiceTabContainer.jsx, LoginContainer.jsx |

**Example - Functional Component (ServiceTabContainer.jsx):**
```jsx
// Before
import { withRouter } from 'react-router-dom';

function ServiceTabContainer(props) {
  const { match, location, history } = props;
  // ... use match.params, location.pathname, history.push()
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(ServiceTabContainer));

// After
import { useMatch, useLocation, useNavigate } from 'react-router';

function ServiceTabContainer(props) {
  const match = useMatch({ path: location.pathname });
  const location = useLocation();
  const navigate = useNavigate();
  // ... use match.params, location.pathname, navigate()
}

export default connect(mapStateToProps, mapDispatchToProps)(ServiceTabContainer);
```

**Example - Class Component (App.jsx metrics-service-ui):**
```jsx
// Before (AppContainer.jsx)
import { withRouter } from 'react-router-dom';
import App from './App';
export default withRouter(App);

// Before (App.jsx)
class App extends Component {
  render() {
    const { history } = this.props;
    return <Router history={history}>...</Router>;
  }
}

// After (AppContainer.jsx) - DELETE THIS FILE, move router to index.js
// After (index.js)
import { HashRouter } from 'react-router';
root.render(
  <HashRouter>
    <App />
  </HashRouter>
);

// After (App.jsx) - Convert to functional
function App() {
  return (
    <div className="App">
      <ThemeProvider theme={theme}>
        <Suspense fallback={<Spinner isLoading={true} />}>
          <Routes>
            {/* ... routes ... */}
          </Routes>
        </Suspense>
      </ThemeProvider>
    </div>
  );
}
```

---

##### **6. `<Router history={history}>` → `<HashRouter>` / `<BrowserRouter>`**

| Before | After | Files: App.jsx (both), DetailPage.jsx |

**Example from metrics-service-ui/App.jsx:**
```jsx
// Before
import { Router } from 'react-router-dom';

class App extends Component {
  render() {
    const { history } = this.props;
    return (
      <Router history={history}>
        <Switch>...</Switch>
      </Router>
    );
  }
}

// After - Move router to entry point (index.js)
import { HashRouter } from 'react-router';

function App() {
  return (
    <div className="App">
      <ThemeProvider theme={theme}>
        <Suspense fallback={<Spinner isLoading={true} />}>
          <Routes>...</Routes>
        </Suspense>
      </ThemeProvider>
    </div>
  );
}

// In index.js:
import { HashRouter } from 'react-router';
root.render(
  <HashRouter>
    <App />
  </HashRouter>
);
```

**Note for DetailPage.jsx:** This file has a nested `<Router history={history}>`. In v7, remove the nested Router entirely and use the parent Router from index.js. Convert DetailPage to functional component and use hooks for navigation.

---

##### **7. Direct `history` Package Usage**

| Before | After | Files: helpers/history.jsx (both), user-actions.jsx, DetailPage.jsx |

**Option A - Recommended (Remove direct history usage):**

**Example - Redux Actions (metrics-service-ui/actions/user-actions.jsx):**
```jsx
// Before
import history from '../helpers/history';

export function login(credentials) {
  return (dispatch) => {
    userService.login(credentials).then(
      (token) => {
        dispatch(success(token));
        history.push('/dashboard');
      },
      (error) => dispatch(failure(error))
    );
  };
}

// After
// Remove: import history from '../helpers/history';

export function login(credentials, navigate) {
  return (dispatch) => {
    userService.login(credentials).then(
      (token) => {
        dispatch(success(token));
        navigate('/dashboard');
      },
      (error) => dispatch(failure(error))
    );
  };
}

// In component (LoginContainer.jsx after removing withRouter):
import { useNavigate } from 'react-router';

function LoginContainer(props) {
  const navigate = useNavigate();
  const { login } = props;
  
  const handleSubmit = (credentials) => {
    login(credentials, navigate);
  };
  // ...
}
```

**Example - Class Component (api-catalog-ui/DetailPage.jsx):**
```jsx
// Before
handleGoBack = () => {
  const { history } = this.props;
  history.push('/dashboard');
};

// After (convert to functional component)
import { useNavigate } from 'react-router';

function DetailPage(props) {
  const navigate = useNavigate();
  
  const handleGoBack = () => {
    navigate('/dashboard');
  };
  
  // Also replace history.location.pathname with useLocation()
  const location = useLocation();
  // use location.pathname instead of history.location.pathname
  // ...
}
```

**Option B - If keeping history package (not recommended):**
```jsx
// helpers/history.jsx - keep as is
import { createHashHistory } from 'history';
const history = createHashHistory();
export default history;

// index.js
import { unstable_HistoryRouter as HistoryRouter } from 'react-router';
import { HashRouter } from 'react-router';
import history from './helpers/history';

root.render(
  <HashRouter>
    <HistoryRouter history={history}>
      <App />
    </HistoryRouter>
  </HashRouter>
);

// actions/user-actions.jsx - unchanged
import history from '../helpers/history';
history.push('/dashboard');
```

---

##### **8. `exact` Prop Removal**

| Before | After | Files: App.jsx (both), DetailPage.jsx |

**Example:**
```jsx
// Before
<Route exact path="/dashboard" component={Dashboard} />

// After
<Route path="/dashboard" element={<Dashboard />} />
```

**Note:** In v7, routes match exactly by default. To match child routes, the parent path must end with `/*`:
```jsx
<Route path="/service/*" element={<ServiceLayout />}>
  <Route path="details" element={<ServiceDetails />} />
</Route>
```

---

##### **9. Nested Routes**

| Before (v5) | After (v7) | Files: DetailPage.jsx |

**Example from api-catalog-ui/DetailPage.jsx:**
```jsx
// Before
<Router history={history}>
  <Switch>
    <Route
      exact
      path={`${match.path}`}
      render={() => <Redirect replace to={`${match.url}/${tiles[0].services[0].serviceId}`} />}
    />
    <Route
      exact
      path={`${match.path}/:serviceId`}
      render={() => <ServiceTabContainer ... />}
    />
  </Switch>
</Router>

// After
// Remove nested Router entirely - use parent Router from index.js
<Routes>
  <Route
    index
    element={<Navigate replace to={`${match.path}/${tiles[0].services[0].serviceId}`} />}
  />
  <Route
    path=":serviceId"
    element={<ServiceTabContainer ... />}
  />
</Routes>
```

**Key Changes:**
- Remove nested `<Router>`
- Remove `exact` prop
- Change `path={`${match.path}/:serviceId`}` to `path=":serviceId"` (relative to parent)
- Change root redirect to use `index` prop instead of `exact path`

---

##### **10. `Link` Component**

| Before | After | Files: ServicesNavigationBar.jsx |

**Example from api-catalog-ui/ServicesNavigationBar.jsx:**
```jsx
// Before
import { Link as RouterLink } from 'react-router-dom';

<Link component={RouterLink} to={somePath} />

// After
import { Link as RouterLink } from 'react-router';

<Link component={RouterLink} to={somePath} />
```

---

##### **11. Index Routes**

| Before (v5) | After (v7) |

**Example:**
```jsx
// Before
<Route path="/users" exact component={UserList} />
<Route path="/users/:id" component={UserProfile} />

// After
<Route path="/users">
  <Route index element={<UserList />} />
  <Route path=":id" element={<UserProfile />} />
</Route>
```

---

##### **12. Package.json Changes**

**api-catalog-ui/frontend/package.json:**
```json
{
  "dependencies": {
    "react-router": "^7.0.0",
    "history": "^5.3.0"
  },
  "overrides": {
    "path-to-regexp": "8.4.0",
    "axios": "1.13.6",
    "ajv": "8.18.0",
    "dompurify": "3.3.3"
  }
}
```

**metrics-service-ui/frontend/package.json:**
```json
{
  "dependencies": {
    "react-router": "^7.0.0",
    "history": "^5.3.0"
  },
  "overrides": {
    "path-to-regexp": "8.4.0",
    "follow-redirects": "1.15.11",
    "minimatch": "7.4.9",
    "picomatch": "2.3.2",
    "immutable-js": "3.8.3"
  }
}
```

**Action:** Remove `react-router-dom` from dependencies in both files.

---

##### **Summary: File-by-File Migration Checklist**

| File | Changes Required |
|------|------------------|
| **api-catalog-ui** |
| `package.json` | Replace `react-router-dom` with `react-router@^7.0.0`, add `history@^5.3.0` |
| `index.js` | Change import from `'react-router-dom'` to `'react-router'` |
| `src/components/App/App.jsx` | Convert to functional, remove `withRouter`, replace `Router` with fragment, `Switch`→`Routes`, update `Route` props |
| `src/components/App/AppContainer.jsx` | **DELETE** - no longer needed |
| `src/components/DetailPage/DetailPage.jsx` | Convert to functional, replace `Router` with fragment, `Switch`→`Routes`, `Redirect`→`Navigate`, replace `history.push` with `useNavigate` |
| `src/components/ServicesNavigationBar/ServicesNavigationBar.jsx` | Change import to `'react-router'` |
| `src/components/ServiceTab/ServiceTabContainer.jsx` | Remove `withRouter`, add hooks (`useNavigate`, `useLocation`, `useMatch`) |
| `src/components/Login/LoginContainer.jsx` | Remove `withRouter`, add `useNavigate`, pass to actions |
| `src/helpers/history.jsx` | Keep (needed for DetailPage during transition) or remove if Option A used |
| Test files | Change imports to `'react-router'` |

| File | Changes Required |
|------|------------------|
| **metrics-service-ui** |
| `package.json` | Replace `react-router-dom` with `react-router@^7.0.0`, add `history@^5.3.0` |
| `index.js` | Change import from `'react-router-dom'` to `'react-router'` |
| `src/components/App/App.jsx` | Convert to functional, remove `withRouter`, replace `Router` with fragment, `Switch`→`Routes`, update `Route` props |
| `src/components/App/AppContainer.jsx` | **DELETE** - no longer needed |
| `src/components/AuthRoute/AuthRoute.jsx` | Change import, `Redirect`→`Navigate` |
| `src/actions/user-actions.jsx` | Add `navigate` parameter to actions, replace `history.push` |
| `src/helpers/history.jsx` | Keep (needed for actions during transition) or remove if Option A used |
| Test files | Change imports to `'react-router'` |

---

## Phase 2: Branch Setup

### 2.1 Confirm Base Branch
Verify the correct base branch before creating the feature branch:
```bash
git branch -a | grep v2
```

The current checked-out branch is `reboot/dependency-upgrade-2-18-5`. Confirm whether `v2.x.x` exists and is the intended base, or whether the feature branch should be cut from the current branch.

### 2.2 Create Feature Branch
```bash
# From v2.x.x (or whichever base branch is confirmed in 2.1)
git checkout v2.x.x
git pull origin v2.x.x
git checkout -b chore/react-router-v7-upgrade
```

### 2.3 Update Branch Protection
- [ ] Ensure branch has same protection rules as the base branch
- [ ] Configure required status checks

---

## Phase 3: Dependency Update

### 3.1 Update package.json Files

In v7 the package is `react-router` (not `react-router-dom`). The `react-router-dom` package still exists as a re-export shim, but the canonical import path is `react-router`. Align with the v3.x.x codebase which already uses `react-router`.

**api-catalog-ui/frontend/package.json:**
```json
{
  "dependencies": {
    "react-router": "^7.0.0"
  }
}
```
Remove `react-router-dom` from dependencies if present.

**metrics-service-ui/frontend/package.json:**
```json
{
  "dependencies": {
    "react-router": "^7.0.0"
  }
}
```
Remove `react-router-dom` from dependencies if present.

### 3.2 Keep the `path-to-regexp` Override

**Do not remove the `path-to-regexp` override.** React Router v7 does not depend on `path-to-regexp`, so the override no longer conflicts — but it must remain to keep other packages from pulling in vulnerable older versions.

**api-catalog-ui/frontend/package.json** — keep as-is:
```json
{
  "overrides": {
    "path-to-regexp": "8.4.0",
    "axios": "1.13.6",
    "ajv": "8.18.0",
    "dompurify": "3.3.3"
    // ... all other overrides from PR #4534
  }
}
```

**metrics-service-ui/frontend/package.json** — keep as-is:
```json
{
  "overrides": {
    "path-to-regexp": "8.4.0",
    "follow-redirects": "1.15.11",
    "minimatch": "7.4.9",
    "picomatch": "2.3.2",
    "immutable-js": "3.8.3"
    // ... all other overrides from PR #4534
  }
}
```

Verify the full list of overrides currently in each file before editing — do not drop any.

### 3.3 Update `history` Package (if used directly)
- [ ] Check if any code imports `history` directly:
  ```bash
  grep -r "from 'history'" --include="*.js" --include="*.jsx" .
  ```
- `metrics-service-ui/src/helpers/history.jsx` and `src/actions/user-actions.jsx` use `history` directly. Handling depends on the strategy chosen in §1.4b.
- If keeping the `history` package: upgrade from `4.10.1` to `^5.3.0` (required by react-router v7's peer deps).
- If removing the direct `history` usage (Option A or C from §1.4b): remove the `history` dependency entirely.

### 3.4 Clean Install
```bash
# In each UI directory:
rm -rf node_modules package-lock.json
npm install
```

After install, verify path-to-regexp is not pulled in transitively at a vulnerable version:
```bash
npm list path-to-regexp
```

---

## Phase 4: Code Migration

### 4.1 Update All Imports

**Before:**
```js
import { BrowserRouter, Switch, Route, Link } from 'react-router-dom';
```

**After:**
```js
import { BrowserRouter, Routes, Route, Link } from 'react-router';
```

All imports from `'react-router-dom'` must become `'react-router'`. Find every instance:
```bash
grep -r "from 'react-router-dom'" --include="*.jsx" --include="*.js" --include="*.ts" --include="*.tsx" .
```

---

### 4.2 Replace `<Switch>` with `<Routes>`

**Pattern to find:**
```bash
grep -r "<Switch>" --include="*.jsx" --include="*.js" .
```

**Before:**
```jsx
import { Switch, Route } from 'react-router-dom';

function App() {
  return (
    <Switch>
      <Route path="/about" component={About} />
      <Route path="/users" component={Users} />
      <Route path="/" component={Home} />
    </Switch>
  );
}
```

**After:**
```jsx
import { Routes, Route } from 'react-router';

function App() {
  return (
    <Routes>
      <Route path="/about" element={<About />} />
      <Route path="/users" element={<Users />} />
      <Route path="/" element={<Home />} />
    </Routes>
  );
}
```

**Key Changes:**
- `Switch` → `Routes`
- `component={Component}` → `element={<Component />}`
- `render={fn}` → `element={fn()}`
- `children={fn}` → `element={fn()}`

---

### 4.3 Replace `useHistory` with `useNavigate`

**Pattern to find:**
```bash
grep -r "useHistory" --include="*.jsx" --include="*.js" .
```

**Before:**
```jsx
import { useHistory } from 'react-router-dom';

function MyComponent() {
  const history = useHistory();
  const goBack = () => { history.goBack(); };
  const goToUser = (userId) => { history.push(`/users/${userId}`); };
}
```

**After:**
```jsx
import { useNavigate } from 'react-router';

function MyComponent() {
  const navigate = useNavigate();
  const goBack = () => { navigate(-1); };
  const goToUser = (userId) => { navigate(`/users/${userId}`); };
}
```

**API Changes:**
| v5 | v7 |
|----|----|
| `history.push(path)` | `navigate(path)` |
| `history.replace(path)` | `navigate(path, { replace: true })` |
| `history.go(n)` | `navigate(n)` |
| `history.goBack()` | `navigate(-1)` |
| `history.goForward()` | `navigate(1)` |
| `history.listen(callback)` | use `useEffect` + `useLocation` |

---

### 4.4 Replace `useRouteMatch` with `useMatch`

**Pattern to find:**
```bash
grep -r "useRouteMatch" --include="*.jsx" --include="*.js" .
```

**Before:**
```jsx
import { useRouteMatch } from 'react-router-dom';

function UserPage() {
  const match = useRouteMatch();
  const { path, url } = match;
  const userMatch = useRouteMatch('/users/:userId');
  const { params } = userMatch;
}
```

**After:**
```jsx
import { useMatch, useLocation, matchPath } from 'react-router';

function UserPage() {
  // For a specific pattern:
  const userMatch = useMatch('/users/:userId');
  const { params } = userMatch || {};

  // For current route info without a pattern, use useLocation:
  const location = useLocation();
  const match = matchPath('/', location.pathname);
}
```

---

### 4.5 Replace `Redirect` with `Navigate`

**Pattern to find:**
```bash
grep -r "Redirect" --include="*.jsx" --include="*.js" . | grep import
```

**Before:**
```jsx
import { Redirect } from 'react-router-dom';

function ProtectedRoute({ user, children }) {
  if (!user) return <Redirect to="/login" />;
  return children;
}
```

**After:**
```jsx
import { Navigate } from 'react-router';

function ProtectedRoute({ user, children }) {
  if (!user) return <Navigate to="/login" replace />;
  return children;
}
```

---

### 4.6 Update `NavLink` activeClassName

**Pattern to find:**
```bash
grep -r "activeClassName" --include="*.jsx" --include="*.js" .
```

**Before:**
```jsx
<NavLink to="/users" activeClassName="active" className="nav-link">
  Users
</NavLink>
```

**After:**
```jsx
<NavLink to="/users" className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
  Users
</NavLink>
```

---

### 4.7 Replace `withRouter` HOC

**Pattern to find:**
```bash
grep -r "withRouter" --include="*.jsx" --include="*.js" .
```

#### Case A — Functional component (straightforward)

**Before:**
```jsx
import { withRouter } from 'react-router-dom';

function MyComponent({ match, location, history }) { /* ... */ }
export default withRouter(MyComponent);
```

**After:**
```jsx
import { useLocation, useNavigate, useMatch } from 'react-router';

function MyComponent() {
  const location = useLocation();
  const navigate = useNavigate();
  const match = useMatch({ path: location.pathname });
}
export default MyComponent;
```

#### Case B — Class component (`metrics-service-ui/App.jsx`)

Class components cannot use hooks. Convert `App.jsx` to a functional component following the pattern above, or use a thin functional wrapper (see §1.4a for trade-offs).

If converting to a functional component, the `<Router history={this.props.history}>` pattern also needs to change — replace it with `<HashRouter>` (or `<BrowserRouter>`) directly, since v7 no longer accepts an explicit `history` prop on `<Router>`:

**Before:**
```jsx
// AppContainer.jsx
import { withRouter } from 'react-router-dom';
import App from './App';
export default withRouter(App);

// App.jsx (class component)
class App extends React.Component {
  render() {
    return <Router history={this.props.history}>...</Router>;
  }
}
```

**After:**
```jsx
// AppContainer.jsx — wrapper no longer needed, remove it
// index.js — wrap at the entry point instead
import { HashRouter } from 'react-router';
root.render(<HashRouter><App /></HashRouter>);

// App.jsx (functional component)
function App() {
  return <Routes>...</Routes>;
}
export default App;
```

---

### 4.8 Handle Imperative Navigation in Redux Actions

`metrics-service-ui/src/actions/user-actions.jsx` calls `history.push()` outside React. Follow the strategy chosen in §1.4b.

**Option A — Pass `navigate` as a parameter (recommended):**

```jsx
// user-actions.jsx
export const loginSuccess = (navigate) => (dispatch) => {
  dispatch({ type: LOGIN_SUCCESS });
  navigate('/dashboard');
};

// LoginContainer.jsx
const navigate = useNavigate();
dispatch(loginSuccess(navigate));
```

**Option B — Keep a shared history object via `unstable_HistoryRouter`:**

```jsx
// helpers/history.js
import { createHashHistory } from 'history';
export const history = createHashHistory();

// index.js
import { unstable_HistoryRouter as HistoryRouter } from 'react-router';
import { history } from './helpers/history';
root.render(<HistoryRouter history={history}><App /></HistoryRouter>);

// user-actions.jsx — unchanged
import { history } from '../helpers/history';
history.push('/dashboard');
```

Note: `unstable_HistoryRouter` is available in v7 but carries an `unstable_` prefix — it may change in a future minor release.

---

### 4.9 Update Route Nesting

**Before (v5):**
```jsx
<Route path="/app" component={App}>
  <Route path="/users" component={Users} />
</Route>
```

**After (v7):**
```jsx
<Route path="/app" element={<App />}>
  <Route path="users" element={<Users />} />
</Route>
```

In v7, nested routes are relative to the parent. Child paths must **not** repeat the parent prefix.

---

### 4.10 Update Index Routes

**Before (v5):**
```jsx
<Route path="/users" component={Users}>
  <Route path="/users/:id" component={UserProfile} />
  <Route exact path="/users" component={UserList} />
</Route>
```

**After (v7):**
```jsx
<Route path="/users" element={<Users />}>
  <Route path=":id" element={<UserProfile />} />
  <Route index element={<UserList />} />
</Route>
```

---

### 4.11 Remove `exact` Prop

**Before (v5):**
```jsx
<Route exact path="/users" component={Users} />
```

**After (v7):**
```jsx
<Route path="/users" element={<Users />} />
```

Routes match exactly by default in v7. To match children, the path must end with `/*`.

---

## Phase 5: Testing

### 5.1 Unit Tests
- [ ] Run existing unit tests in api-catalog-ui
  ```bash
  cd api-catalog-ui/frontend
  npm test
  ```
- [ ] Run existing unit tests in metrics-service-ui
  ```bash
  cd metrics-service-ui/frontend
  npm test
  ```

Update test files that import from `'react-router-dom'` to use `'react-router'`.

**Expected:** All tests should pass after migration. The failing test from PR #4534 (`DetailPageContainer.test.jsx`) should now pass.

### 5.2 Manual Testing Checklist

**Navigation:**
- [ ] Click all navigation links
- [ ] Test browser back/forward
- [ ] Test direct URL entry
- [ ] Test 404 handling
- [ ] Test protected routes
- [ ] Test nested routes

**API Catalog UI Specific:**
- [ ] Test API listing navigation
- [ ] Test API detail page navigation
- [ ] Test service detail page navigation
- [ ] Test search/filter navigation
- [ ] Test tiles/grid view navigation

**Metrics Service UI Specific:**
- [ ] Test login → redirect to dashboard
- [ ] Test logout → redirect to login
- [ ] Test metrics dashboard navigation
- [ ] Test time range selection
- [ ] Test metric category navigation

### 5.3 Integration Testing
- [ ] Verify both UIs build successfully
  ```bash
  cd api-catalog-ui/frontend && npm run build
  cd metrics-service-ui/frontend && npm run build
  ```
- [ ] Verify JIB container builds work
- [ ] Run end-to-end tests (if available)
- [ ] Confirm `path-to-regexp` is not downgraded in either build:
  ```bash
  npm list path-to-regexp
  ```

---

## Phase 6: Integration with PR #4534

### 6.1 Rebase Strategy

First confirm the base branch (see §2.1), then:

```bash
git checkout chore/react-router-v7-upgrade
git fetch origin
git rebase origin/v2.x.x

# Merge PR #4534 changes into this branch
git merge origin/reboot/dependency-upgrade-2-18-5 --no-ff

# Resolve any conflicts
# Commit the resolution
```

### 6.2 Verify All Changes
- [ ] Confirm `path-to-regexp` override is at 8.4.0 and present in both package.json files
- [ ] Confirm `react-router` is at 7.x (not `react-router-dom`)
- [ ] Confirm all other dependency upgrades from PR #4534 are present
- [ ] Run full build and test suite

### 6.3 Update CHANGELOG
Add entry for:
- react-router-dom → react-router upgrade from 5.3.4 to 7.x
- path-to-regexp override kept at 8.4.0 (security fix)
- Any other breaking changes

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking changes in routing | High | High | Thorough code audit and testing |
| Class component rewrite in metrics-service-ui | High | Medium | Decide strategy in §1.4 before coding |
| Imperative navigation in Redux actions | High | High | Choose and implement §1.4b strategy first |
| Test failures | Medium | Medium | Update all test imports and router wrappers |
| Build failures | Low | Medium | Verify clean install |
| Runtime errors | Medium | High | Manual testing of all navigation flows |
| `path-to-regexp` security regression | Low | High | Verify override present after install |
| `unstable_HistoryRouter` API change (if used) | Low | Medium | Monitor react-router changelog |

---

## Resources

- [React Router v7 Documentation](https://reactrouter.com/en/main)
- [v5 to v6 Migration Guide](https://reactrouter.com/en/main/upgrading/v5)
- [v6 to v7 Upgrade Guide](https://reactrouter.com/upgrading/v6)
- [v7 API Reference](https://reactrouter.com/en/main/route/route)
- [GitHub Issue: path-to-regexp CVE](TBD — fill in actual CVE number)

---

## Appendices

### Appendix A: Files to Modify

**api-catalog-ui/frontend:**
- [ ] `package.json` (replace `react-router-dom` with `react-router@^7.0.0`)
- [ ] `package-lock.json` (regenerated)
- [ ] All files importing from `'react-router-dom'` → `'react-router'`
- [ ] Test files (especially `DetailPageContainer.test.jsx`)

**metrics-service-ui/frontend:**
- [ ] `package.json` (replace `react-router-dom` with `react-router@^7.0.0`)
- [ ] `package-lock.json` (regenerated)
- [ ] `src/components/App/App.jsx` — class component refactor (see §4.7 Case B)
- [ ] `src/components/App/AppContainer.jsx` — remove `withRouter` wrapper
- [ ] `src/actions/user-actions.jsx` — replace `history.push()` (see §4.8)
- [ ] `src/helpers/history.jsx` — update or remove depending on §1.4b strategy
- [ ] `src/components/AuthRoute/AuthRoute.jsx` — `Redirect` → `Navigate`
- [ ] All files importing from `'react-router-dom'` → `'react-router'`

### Appendix B: Verification Commands

```bash
# Check react-router version
npm list react-router

# Confirm react-router-dom is removed (should show nothing or the shim at 7.x)
npm list react-router-dom

# Confirm path-to-regexp override is in effect
npm list path-to-regexp

# Run tests
npm test

# Build project
npm run build
```

---

## Acceptance Criteria

- [ ] `react-router@7.x` is used in both UIs (imported from `'react-router'`, not `'react-router-dom'`)
- [ ] `react-router-dom` dependency is removed from both `package.json` files
- [ ] `path-to-regexp@8.4.0` override is present and enforced in both packages
- [ ] All existing functionality works as before
- [ ] All unit tests pass
- [ ] Build succeeds for both UIs
- [ ] No runtime errors related to routing or navigation
- [ ] PR #4534 can be merged without conflicts

---

**Approvals Required:**
- [ ] Code Review
- [ ] QA Testing
- [ ] Security Review (if applicable)
