CREATE KEYSPACE metric_offset_30000 WITH replication = {'class': 'NetworkTopologyStrategy', 'ap-northeast-2': '3'} AND durable_writes = true;

-- 'bucketSize * rollup = range of data can be stored. here bucketSize = 30000 and rollup = 60s'
CREATE TABLE metric_offset_30000.metric_60s (
  tenant text,
  path text,
  startTime bigint,
  offset smallint,
  data double,
  PRIMARY KEY ((tenant, path, startTime), offset)
) WITH CLUSTERING ORDER BY (offset ASC)
  AND bloom_filter_fp_chance = 0.01
  AND caching = {'keys': 'ALL', 'rows_per_partition': '10'}
  AND comment = ''
  AND compaction = {'class': 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy', 'sstable_size_in_mb': '640'}
  AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
  AND crc_check_chance = 1.0
  AND dclocal_read_repair_chance = 0.0
  AND default_time_to_live = 0
  AND gc_grace_seconds = 864000
  AND max_index_interval = 2048
  AND memtable_flush_period_in_ms = 0
  AND min_index_interval = 128
  AND read_repair_chance = 0.0
  AND speculative_retry = 'NONE';
