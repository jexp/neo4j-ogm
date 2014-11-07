package org.neo4j.ogm.session;

import java.util.Collection;

public class CypherQuery implements Query {

    @Override
    public String findOne(Long id) {
        return String.format("MATCH p=(n)-->(m) WHERE id(n) = %d RETURN p", id);
    }

    @Override
    public String findAll(Collection<Long> ids) {
        return String.format("MATCH p=(n)-->(m) WHERE id(n) in [%s] RETURN p", idList(ids));
    }

    @Override
    public String findAll() {
        return "MATCH p=()-->() RETURN p";
    }

    @Override
    public String findByLabel(Collection<String> labels) {
        return String.format("MATCH p=(n%s)-->(m) RETURN p", labelExpression(labels));
    }

    @Override
    public String delete(Long id) {
        return String.format("MATCH (n) WHERE id(n) = %d OPTIONAL MATCH (n)-[r]-() DELETE r, n", id);
    }

    @Override
    public String deleteAll(Collection<Long> ids) {
        return String.format("MATCH (n) WHERE id(n) in [%s] OPTIONAL MATCH (n)-[r]-() DELETE r, n", idList(ids));
    }

    @Override
    public String purge() {
        return "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n";
    }

    @Override
    public String deleteByLabel(Collection<String> labels) {
        return String.format("MATCH (n%s) OPTIONAL MATCH (n)-[r]-() DELETE r, n", labelExpression(labels));
    }

    private String labelExpression(Collection<String> labels) {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            builder.append(":");
            builder.append(label);
        }
        return builder.toString();
    }

    private String idList(Collection<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            builder.append(",");
            builder.append(id);
        }
        return builder.toString().substring(1);
    }

}