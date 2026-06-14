# miniStaticServer — Unit Tests

## Test Classes (2 files, 15 tests)

### StaticSiteTest (9 tests)

| Test Method | What it evaluates |
|-------------|-------------------|
| `constructor_createsServer` | A `StaticSite` can be constructed with a valid port and directory |
| `constructor_nullDirectory_throws` | Null root directory throws `NullPointerException` |
| `constructor_nonExistentDirectory_throws` | Non-existent directory throws `IllegalArgumentException` |
| `constructor_nonDirectory_throws` | A file path (not a directory) throws `IllegalArgumentException` |
| `addTemplate_storesKeyValue` | Templates can be registered |
| `start_runsServer` | `start()` launches the server without error |
| `scanDirectory_addsRoutesForFiles` | Files in the directory are registered as routes |
| `stop_haltsServer` | `stop()` shuts down the server cleanly |
| `idle_blocksUntilStopped` | `idle()` blocks until `stop()` is called |

### QrStaticSiteTest (6 tests)

| Test Method | What it evaluates |
|-------------|-------------------|
| `constructor_createsServer` | A `QrStaticSite` can be constructed with a valid port and directory |
| `constructor_nullDirectory_throws` | Null root directory throws `NullPointerException` |
| `constructor_nonExistentDirectory_throws` | Non-existent directory throws `IllegalArgumentException` |
| `addQrPlaceholder_storesMapping` | QR placeholders can be registered |
| `stop_haltsServer` | `stop()` shuts down the server cleanly |
| `getLocalIp_returnsNonEmpty` | `getLocalIp()` returns a valid IP address |

## Running the Tests

```bash
cd miniStaticServer
mvn test
```
