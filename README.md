# pgEasyReplication

pgEasyReplicaton is a Java library to read data objects and their changes (INSERT/UPDATE/DELETE) via PostgreSQL Replication API. For this, the library uses a replication slot to reserve WAL logs on the server and the output plugin to decode the WAL logs.
The data changes are returned by the library in JSON format.
