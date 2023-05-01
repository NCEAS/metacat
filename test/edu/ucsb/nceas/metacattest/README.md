# The use of `metacattest` is deprecated

All new tests (and any old ones you want to move :-) should be located in `metacatnettest` (for tests that need a running metacat instance, and test networking and replication functionality) or `metacat` (for unit tests or integration tests that do NOT require metacat to be running)
