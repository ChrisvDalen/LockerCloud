# LockerCloud Test Infrastructure

This project contains additional tests for simulating unstable network
conditions when uploading and downloading files.  The key pieces are:

* **FaultyInputStream** – wraps another `InputStream` and throws an
  `IOException` after a configurable number of bytes to emulate dropped
  connections or timeouts.
* **FaultyMultipartFile** – implementation of `MultipartFile` used in
  tests.  It provides a fresh stream for every attempt and can be
  configured to return a `FaultyInputStream` for the first _n_ attempts.
* **FaultyHttpClient** – lightweight wrapper around `HttpClient` that
  throws an `IOException` for a configurable number of calls.  This can
  be used when testing HTTP based clients.
* **LargeInputStream** – generates a stream of arbitrary size without
  allocating the full byte array.  Useful for simulating files larger
  than the chunk threshold.

The new `NetworkFaultIntegrationTest` exercises the retry logic in
`FileManagerService` by uploading small and large files while network
errors are injected.  It also runs uploads from multiple threads to
mimic concurrent clients.  Assertions verify the number of retry
attempts, that transfers eventually succeed and that no leftover chunk
files remain.
