# Connectly Frontend Changes Log

This log file tracks all code, configuration, and architectural modifications made to the Connectly frontend workspace, including precise timestamps, root causes, and summaries.

---

### [2026-11-24 09:12:49 IST] - Fixed Blank Screen Issue (`global is not defined` Runtime Error)

- **Issue**: Opening `http://localhost:4200` resulted in a blank screen.
- **Root Cause**:
  1. The application uses `@stomp/stompjs` and `sockjs-client` for real-time WebSocket communication.
  2. In modern Angular versions (Angular 17+ with the Vite/esbuild application builder), Node.js global variables like `global` are not natively available in browser environments.
  3. `sockjs-client` relies on `global` (e.g. `global.crypto`, `global.addEventListener`), resulting in an uncaught `ReferenceError: global is not defined` when initializing before `<app-root>` can bootstrap.
- **Modifications**:
  1. **`src/index.html`**:
     - Added inline script before scripts run:
       ```html
       <script>
         // Polyfill global for sockjs-client & node libraries in browser
         window.global = window;
       </script>
       ```
  2. **`src/polyfills.ts`**:
     - Created explicit polyfill file defining:
       ```typescript
       (window as any).global = window;
       ```
  3. **`angular.json`**:
     - Updated `projects.frontend.architect.build.options.polyfills` to include `"src/polyfills.ts"` alongside `"zone.js"`.
  4. **`tsconfig.app.json`**:
     - Added `"src/polyfills.ts"` into the compilation `"files"` list to ensure proper TypeScript compilation.
- **Verification**:
  - `ng build` succeeded cleanly with exit code 0 (`polyfills-UX465Y6G.js`, `main-RW3K7GD4.js`, `styles-YEVOR6VH.css`).
  - Tested bundle evaluation in Node VM environment with simulated browser globals: confirmed zero runtime reference errors.

---
