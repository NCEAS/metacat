# Metacat Tests

This README contains information that is intended to be helpful for developers who are unfamiliar
with the metacat codebase. If you find any of this advice to be incorrect, please update this
document!

## Utility Classes

### test/edu/ucsb/nceas/LeanTestUtils

* `LeanTestUtils` is intended to be a fast and lightweight provider of common setup and runtime
  functionality that is needed in multiple tests, but without incorporating JUnit3-specific
  functionality (e.g. no "extends TestCase"), and importantly, WITHOUT REQUIRING A RUNNING INSTANCE
  OF METACAT to allow testing.
* This class should therefore be most useful for fast-running Unit tests, or integration tests that
  do *not* rely on querying metacat, solr, postgres, etc, but instead use mocks.

### test/edu/ucsb/nceas/MCTestCase

* Many of the older tests extend `MCTestCase`, which, in turn, extends JUnit
  v3's `junit.framework.TestCase`.
* Any new tests should strive **not** to extend `MCTestCase`, (thus avoiding being stuck on JUnit
  3). If `MCTestCase` utility methods are needed in newer tests, these should be refactored to allow
  public access.
* `MCTestCase` is configured by default to load its properties from the "runtime" copies of the
  properties files, by calling:
  `LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST)`

## Test Properties vs. Runtime Properties

### test/test.properties
This file contains configurable, test-specific properties that must be customized to your dev 
environment. Hints:
* `metacat.contextDir=`/your/context/dir/typically/tomcat/webapps/metacat
* `expected.internalURL=http\://localhost\:8080` - the hostname and port should match whatever 
  settings exist in `metacat.properties` for the keys `server.internalName` and 
  `server.internalPort`, respectively. This should already be true for a clean checkout.
* **The following will be used to override the properties with the same keys in 
  `metacat.properties`:**
  * `application.backupDir=`/your/backup/props/dir/typically/var/metacat/.metacat 
  * `application.context=`metacat_or_whatever
  * `application.deployDir=`/your/deploy/dir/typically/tomcat/webapps
  * `test.printdebug=true` (preferred, so you can see output)
  * `guid.doi.enabled=true` (needed for DOI tests to pass)
  * `guid.doi.password=apitest` (needed for DOI tests to pass)

### Initializing PropertyService
The `LeanTestUtils.initializePropertyService()` method can be used to make properties available
for tests, initializing `PropertyService` in one of two modes:
1. `PropertiesMode.UNIT_TEST` mode *(preferred)*: default properties from `lib/metacat.properties`
   overlaid with configurable test-specific properties from `test/test.properties`
2. `PropertiesMode.LIVE_TEST` mode (deprecated *): default properties from
   `<metacat.contextDir>/WEB-INF/metacat.properties` overlaid with configurable properties from
   `test/test.properties` (where `metacat.contextDir` is defined in the `test/test.properties`
   file). *&ast; NOTE that `PropertiesMode.LIVE_TEST` mode is provided to support legacy tests,
   and should not be used for new tests, if testing in live mode can be avoided through the use of
   mocks.*

## Test Packaging & Organization

NOTE there may currently be tests in the wrong location. Feel free to move them!

### test/edu/ucsb/nceas/metacat

This directory should contain unit and integration tests that do NOT require metacat to be running,
use the test directory.

### test/edu/ucsb/nceas/metacatnettest

This directory should contain only *tests that require a running metacat instance* (e.g. to test
networking and replication functionality)

### test/edu/ucsb/nceas/metacattest is deprecated!

**Do not add any more tests here!** All new tests (and any old ones you want to move :-) should be
located in one of the previous places.
