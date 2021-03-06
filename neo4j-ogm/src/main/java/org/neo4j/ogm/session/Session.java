package org.neo4j.ogm.session;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.transaction.Transaction;

import java.util.Collection;
import java.util.Map;

public interface Session {

    <T> T load(Class<T> type, Long id);

    <T> T load(Class<T> type, Long id, int depth);

    <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids);

    <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth);

    <T> Collection<T> loadAll(Class<T> type);

    <T> Collection<T> loadAll(Class<T> type, int depth);

    <T> Collection<T> loadAll(Collection<T> objects);

    <T> Collection<T> loadAll(Collection<T> objects, int depth);

    <T> Collection<T> loadByProperty(Class<T> type, Property<String, Object> property);

    <T> Collection<T> loadByProperty(Class<T> type, Property<String, Object> property, int depth);


    void execute(String jsonStatements);

    void purge();


    <T> void save(T object);

    <T> void save(T object, int depth);

    <T> void delete(T object);

    <T> void deleteAll(Class<T> type);


    Transaction beginTransaction();

    /**
     * Given a non modifying cypher statement this method will return a domain object that is hydrated to the
     * level specified in the given cypher query or a scalar (depending on the parametrized type).
     *
     * @param objectType The type that should be returned from the query.
     * @param cypher The parametrizable cypher to execute.
     * @param parameters Any scalar parameters to attach to the cypher.
     *
     * @param <T> A domain object or scalar.
     *
     * @return An instance of the objectType that matches the given cypher and parameters. Null if no object
     * is matched
     *
     * @throws java.lang.RuntimeException If more than one object is found.
     */
    <T> T queryForObject(Class<T> objectType, String cypher,  Map<String, Object> parameters);

    /**
     * Given a non modifying cypher statement this method will return a collection of domain objects that is hydrated to
     * the level specified in the given cypher query or a collection of scalars (depending on the parametrized type).
     *
     * @param objectType The type that should be returned from the query.
     * @param cypher The parametrizable cypher to execute.
     * @param parameters Any parameters to attach to the cypher.
     *
     * @param <T> A domain object or scalar.
     *
     * @return A collection of domain objects or scalars as prescribed by the parametrized type.
     */
    <T> Collection<T> query(Class<T> objectType, String cypher, Map<String, Object> parameters);

    /**
     * This method allows a cypher statement with a modification statement to be executed.
     *
     * <p>Parameters may be scalars or domain objects themselves.</p>
     *
     * @param cypher The parametrizable cypher to execute.
     * @param parameters Any parameters to attach to the cypher. These may be domain objects or scalars. Note that
     *                   if a complex domain object is provided only the properties of that object will be set.
     *                   If relationships of a provided object also need to be set then the cypher should reflect this
     *                   and further domain object parameters provided.
     *
     */
    void execute(String cypher, Map<String, Object> parameters);

    //    <T> T queryForObject(QueryResultMapper<T> objectType, String cypher,  Map<String, Object> parameters);

    //<T> Query<T> createQuery(T type, String cypher, Map<String, Object> parameters);

}
