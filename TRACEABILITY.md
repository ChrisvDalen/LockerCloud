# Traceability Matrix

| Requirement ID | Beschrijving | Implementatieklasse(s) | Testklasse(n) |
|----------------|-------------|------------------------|---------------|
| **RQ-01** | Uploaden van bestanden via socket | `SSLFileServer.handlePost` and client `SocketClient.upload` use `FileManagerService.saveStream` | `FileManagerServiceTest`, `NetworkFaultIntegrationTest` |
| **RQ-02** | Chunked transfer >4GB | `FileManagerService.saveLargeFile` | `FileManagerServiceTest` |
| **RQ-03** | Downloaden van bestanden | `SSLFileServer.handleGet`, `SocketClient.download` and `FileManagerService.getFile` | `FileManagerServiceTest` |
| **RQ-04** | Verwijderen van bestanden | `SSLFileServer.handleDelete`, `SocketClient.delete` and `FileManagerService.deleteFile` | `FileManagerServiceTest` |
| **RQ-05** | Lijst van beschikbare bestanden opvragen | `SSLFileServer.handlePost` (`/listFiles`), `SocketClient.listFiles` and `FileManagerService.listFiles` | `FileManagerServiceTest` |
| **RQ-06** | Synchronisatie tussen client en server | `SSLFileServer.handlePost` (`/sync`) and `FileManagerService.syncFiles` | `FileManagerServiceTest` |
| **RQ-07** | Herstel bij netwerkfouten (retries) | Retry annotations in `FileManagerService` | `NetworkFaultIntegrationTest` |
| **RQ-08** | Meerdere gelijktijdige clients | `SslSyncServer` uses `ExecutorService` | `ConcurrencyFileAccessTest`, `SslSyncServerTest` |
| **RQ-09** | Beveiligde communicatie via SSL/TLS | `SSLFileServer` and `SslSyncServer` create `SSLContext` | `SslSyncServerTest` |
