# pgEasyReplication

pgEasyReplicaton is a Java library to capture data changes (INSERT/UPDATE/DELETE) in PostgreSQL tables via Logical Replication. Also, this library provides snapshots of all published tables.

All the data is returned in JSON format.

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

Example
=======

PostgreSQL
----------

First, you need a Publication for the tables that you want to capture data changes:
```
CREATE PUBLICATION cidade_pub FOR TABLE cidade;
```

**OBS**: A published table must have a “replica identity” configured in order to be able to replicate UPDATE and DELETE operations, so that appropriate rows to update or delete can be identified on the subscriber side. By default, this is the primary key, if there is one. Another unique index (with certain additional requirements) can also be set to be the replica identity. If the table does not have any suitable key, then it can be set to replica identity “full”, which means the entire row becomes the key. This, however, is very inefficient and should only be used as a fallback if no other solution is possible. More details: https://www.postgresql.org/docs/10/logical-replication-publication.html

Table struct:

```
                Table "public.cidade"
  Column   |  Type   | Collation | Nullable | Default
-----------+---------+-----------+----------+---------
 codigo    | integer |           | not null |
 data_fund | date    |           | not null |
 nome      | text    |           |          |
Publications:
    "cidade_pub"
```

Java
----

In your Java code, **import the pgEasyReplicaton package**.

Then, **instantiate the PGEasyReplication class**:
```			
String server = "192.168.32.51:5432";		// PostgreSQL server (host:port)
String database = "test";			// PostgreSQL database
String user = "postgres";			// PostgreSQL username
String password = "123123";			// PostgreSQL password (123123, really? kkk)
String publication = "cidade_pub";		// PostgreSQL publication name
String slot = "slot_cidade_pub";		// PostgreSQL slot name (OPTIONAL, DEFAULT "easy_slot_" + publication name)
boolean slotDropIfExists = false;		// Drop slot if exists (OPTIONAL, DEFAULT false)

PGEasyReplication pgEasyReplication = new PGEasyReplication(server, database, user, password, publication, slot, slotDropIfExists);
```
----------
To get a **snapshot** of the published tables:
```
Event eventSnapshots = pgEasyReplication.getSnapshot();

LinkedList<String> snapshots = eventSnapshots.getData();
```

Printing snapshot:
```
for (String snapshot : snapshots) {
  System.out.println(snapshot);
}
```

Output:
```
{"snaphost":{"public.cidade":["{"codigo":1,"data_fund":"1554-01-25","nome":"SAO PAULO"}","{"codigo":2,"data_fund":"1960-04-21","nome":"BRASILIA"}","{"codigo":3,"data_fund":"1565-03-01","nome":"RIO DE JANEIRO"}"]}}
```
----------
To **capture data changes** of the published tables:
```
pgEasyReplication.initializeLogicalReplication();

boolean isSimpleEvent = true;		// Simple JSON data change (DEFAULT is true). Set false to return details like xid, xCommitTime, numColumns, TupleType, LSN, etc.
boolean withBeginCommit = true;		// Include BEGIN and COMMIT events (DEFAULT is true).
Long startLSN = null;			// Start LSN (DEFAULT is null). If null, get all the changes pending.

Event eventChanges = pgEasyReplication.readEvent(isSimpleEvent, withBeginCommit, startLSN);	// Using DEFAULT values: readEvent(), readEvent(isSimpleEvent), readEvent(isSimpleEvent, withBeginCommit)

LinkedList<String> changes = eventChanges.getData();
```

Printing data changes:
```
for (String change : changes) {
	System.out.println(change);
}
```

Output:
```
{"begin":"begin"}
{"insert":{"cidade":{"codigo":4,"nome":"UBERLANDIA","data_fund":"1929-10-19"}}}
{"commit":"commit"}
{"begin":"begin"}
{"update":{"cidade":{"codigo":20,"nome":"UBERLANDIA","data_fund":"1929-10-19"}}}
{"commit":"commit"}
```

Output with isSimpleEvent = false:
```
{"begin":{"xid":859,"xCommitTime":"2020-01-06 20:32:57 BRST -0200","xLSNFinal":24202080}}
{"relation":{"relationName":"cidade","relReplIdent":"f","columns":[{"typeSpecificData":-1,"isKey":1,"dataTypeColId":23,"columnName":"codigo"},{"typeSpecificData":-1,"isKey":1,"dataTypeColId":1082,"columnName":"data_fund"},{"typeSpecificData":-1,"isKey":1,"dataTypeColId":25,"columnName":"nome"}],"relationId":16385,"namespaceName":"public","numColumns":3}}
{"insert":{"tupleData":{"values":"(4,1929-10-19,UBERLANDIA)","numColumns":3},"relationId":16385,"tupleType":"N"}}
{"commit":{"flags":0,"xCommitTime":"2020-01-06 20:32:57 BRST -0200","commitLSN":24202080,"xLSNEnd":24202128}}
{"begin":{"xid":860,"xCommitTime":"2020-01-06 20:32:57 BRST -0200","xLSNFinal":24202240}}
{"update":{"tupleType2":"N","tupleData1":{"values":"(4,1929-10-19,UBERLANDIA)","numColumns":3},"tupleData2":{"values":"(20,1929-10-19,UBERLANDIA)","numColumns":3},"relationId":16385,"tupleType1":"O"}}
{"commit":{"flags":0,"xCommitTime":"2020-01-06 20:32:57 BRST -0200","commitLSN":24202240,"xLSNEnd":24202288}}
```
----------
In our environment, if you wish, you can adjust UnitTest.java file to run a simple unit dev test.

License
=======

> Copyright (c) 2018-2020, Davy Alvarenga Machado
> All rights reserved.

> Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

> Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

> Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

> Neither the name of the Davy Alvarenga Machado nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
