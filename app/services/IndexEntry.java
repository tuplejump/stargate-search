package services;

import com.datastax.driver.core.*;
import com.google.common.hash.Hashing;
import models.Entry;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * User: satya
 */
public class IndexEntry {
    Session session;

    public IndexEntry(Session session) {
        this.session = session;
    }

    public ResultSet select(PreparedStatement ps, String query, int numShards) {
        BoundStatement stmt = ps.bind(query, 0, numShards);
        stmt.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
        return session.execute(stmt);
    }

    public void add(PreparedStatement ps, Entry entry, int numShards) {
        long time = new Date().getTime();
        int partitionKey = Hashing.consistentHash(Hashing.md5().hashString(entry.id().toString(), StandardCharsets.UTF_8), numShards);
        session.execute(ps.bind(partitionKey, entry.id(), time, entry.json()));
    }
}
