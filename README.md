# pgEasyReplication

pgEasyReplicaton is a Java library to capture data changes (INSERT/UPDATE/DELETE) in PostgreSQL tables via Logical Replication. Also, this library provides snapshots of all published tables.

All the data are returned in JSON format.

Nerd informations
=================

pgEasyReplicaton uses PostgreSQL Replication API to create a replication slot that reserve WAL logs on the server and a stream replication with output plugin to read and decode the WAL logs.

Requirements
============

* Java 8+
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

PostgreSQL
----------

First, you need a Publication for the tables that you want to capture data changes:

```
CREATE PUBLICATION cidade_pub FOR TABLE cidade;
```

**OBS**: A published table must have a “replica identity” configured in order to be able to replicate UPDATE and DELETE operations, so that appropriate rows to update or delete can be identified on the subscriber side. By default, this is the primary key, if there is one. Another unique index (with certain additional requirements) can also be set to be the replica identity. If the table does not have any suitable key, then it can be set to replica identity “full”, which means the entire row becomes the key. This, however, is very inefficient and should only be used as a fallback if no other solution is possible.

More details:
https://www.postgresql.org/docs/10/logical-replication-publication.html

Java
----

In your Java code, import the pgEasyReplicaton library.

Then, instantiate the class PGEasyReplication with PostgreSQL server connection parameters and publication name:

```
// Parameters
			
String pgServer = "192.168.32.51";
String pgPort = "5432";
String pgDatabase = "test";
String pgSSL = "false";
String pgUser = "postgres";
String pgPassword = "";
String pgPublication = "cidade_pub";
boolean messagePretty = true; 		// Default is true. Set false to return details like xid, xCommitTime, xCommitTime, numColumns, TupleType, etc.

// Instantiate pgEasyReplication class	

PGEasyReplication pgEasyReplication = new PGEasyReplication(pgServer, pgPort, pgDatabase, pgSSL, pgUser, pgPassword, pgPublication, messagePretty);
```

To get a snapshot of the published tables:

```
LinkedList<String> snapshots = pgEasyReplication.getSnapshot();

System.out.println("TEST: Printing snapshot ...");

// Printing to console

for (String snapshot : snapshots) {
  System.out.println(snapshot);
}
```

To capture data changes:

```
// Initialize Logical Replication

pgEasyReplication.initializeLogicalReplication();

// Reading and decode Logical Replication Slot
	
LinkedList<String> changes = pgEasyReplication.readLogicalReplicationSlot();

// Printing to console

System.out.println("TEST: Printing data changes ...");

for (String change : changes) {
	System.out.println(change);
}
```

License
=======

> Copyright (c) 2018-2020, Davy Alvarenga Machado
> All rights reserved.

> Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

> Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

> Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

> Neither the name of the Davy Alvarenga Machado nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
