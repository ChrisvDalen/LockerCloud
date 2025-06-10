# Traceability Matrix

| Requirement ID | Beschrijving | Implementatieklasse(s) | Testklasse(n) |
|----------------|-------------|------------------------|---------------|
| **RQ-01** | Uploaden van bestanden via WebSocket | `WebSocketFileServer.onMessage` (upload) en client `WebSocketClientApp` gebruiken `FileManagerService.saveStream` | `FileManagerServiceTest`, `NetworkFaultIntegrationTest` |
| **RQ-02** | Chunked transfer >4GB | `FileManagerService.saveLargeFile` | `FileManagerServiceTest` |
| **RQ-03** | Downloaden van bestanden | `WebSocketFileServer.onMessage` (download) en `WebSocketClientApp` | `FileManagerServiceTest` |
| **RQ-04** | Verwijderen van bestanden | `WebSocketFileServer.onMessage` (delete) en `WebSocketClientApp` | `FileManagerServiceTest` |
| **RQ-05** | Lijst van beschikbare bestanden opvragen | `WebSocketFileServer.onMessage` (list) en `WebSocketClientApp` | `FileManagerServiceTest` |
| **RQ-06** | Synchronisatie tussen client en server | `WebSocketFileServer.onMessage` (future `/sync`) en `FileManagerService.syncFiles` | `FileManagerServiceTest` |
| **RQ-07** | Herstel bij netwerkfouten (retries) | Retry annotations in `FileManagerService` | `NetworkFaultIntegrationTest` |
| **RQ-08** | Meerdere gelijktijdige clients | `SslSyncServer` uses `ExecutorService` | `ConcurrencyFileAccessTest`, `SslSyncServerTest` |
| **RQ-09** | Beveiligde communicatie via SSL/TLS | `WebSocketFileServer` and `SslSyncServer` create `SSLContext` | `SslSyncServerTest` |
