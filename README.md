# pgEasyReplication

pgEasyReplicaton is a Java library to read data changes (INSERT/UPDATE/DELETE) in PostgreSQL tables via Logical Replication.

Also, the library provides snapshots of all tables covered by a Publication.

All the data are returned in JSON format.

Nerd informations
=================

pgEasyReplicaton uses PostgreSQL Replication API to create a replication slot that reserve WAL logs on the server and a stream replication with output plugin to read and decode the WAL logs.

Requirements
============

* PostgreSQL 10+

Configuration
=============

postgresql.conf
---------------

Your database should be configured to enable logical replication.

* Property **max_wal_senders** should be at least equal to the number of replication consumers.
* Property **wal_keep_segments** should contain count wal segments that can't be removed from database.
* Property **wal_level** for logical replication should be equal to logical.
* Property **max_replication_slots** should be greater than zero for logical replication, because logical replication can't work without replication slot.

Example:

```
max_wal_senders = 4             # max number of walsender processes
wal_keep_segments = 4           # in logfile segments, 16MB each; 0 disables
wal_level = logical             # minimal, replica, or logical
max_replication_slots = 4       # max number of replication slots
```

After configurations, restart/reload PostgreSQL service.

pg_hba.conf
-----------

Enable connect user with replication privileges to replication stream.

Example:
  
```
local   replication   all                   trust
host    replication   all   192.168.32.0/24 md5
host    replication   all   ::1/128         md5
```

After configurations, restart/reload PostgreSQL service.

How to use
==========

SQL
---

First, you need a Publication for the tables that you want to capture data changes.

```
CREATE PUBLICATION cidade_pub FOR TABLE cidade;
```

OBS: A published table must have a “replica identity” configured in order to be able to replicate UPDATE and DELETE operations, so that appropriate rows to update or delete can be identified on the subscriber side. By default, this is the primary key, if there is one. Another unique index (with certain additional requirements) can also be set to be the replica identity. If the table does not have any suitable key, then it can be set to replica identity “full”, which means the entire row becomes the key. This, however, is very inefficient and should only be used as a fallback if no other solution is possible.

More details:
https://www.postgresql.org/docs/10/logical-replication-publication.html

Java
----


```
// Parameters
			
String pgServer = "192.168.32.51";
String pgPort = "5432";
String pgDatabase = "test";
String pgSSL = "false";
String pgUser = "postgres";
String pgPassword = "";
String pgPublication = "cidade_pub";
boolean messagePretty = false;


// Initialize Logical Replication		

PGEasyReplication pgEasyReplication = new PGEasyReplication(pgServer, pgPort, pgDatabase, pgSSL, pgUser, pgPassword, pgPublication, messagePretty);
			
pgEasyReplication.initializeLogicalReplication();
```			
	
```
// Print snapshot

System.out.println("TEST: Printing snapshot ...");

LinkedList<String> snapshots = pgEasyReplication.getSnapshot();

for (String snapshot : snapshots) {
  System.out.println(snapshot);
}
```


