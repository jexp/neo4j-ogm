package org.neo4j.cineasts.domain;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity
public class Rating {

    private static final int MAX_STARS = 5;
    private static final int MIN_STARS = 0;

    public Rating() {
    }

    public Rating(User user, Movie movie, int stars, String comment) {
        this.user = user;
        this.movie = movie;
        this.stars = stars;
        this.comment = comment;
    }

    @GraphId
    Long id;
    @StartNode
    User user;
    @EndNode
    Movie movie;

    int stars;
    String comment;

    public User getUser() {
        return user;
    }

    public Movie getMovie() {
        return movie;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Rating rate(int stars, String comment) {
        if (stars>= MIN_STARS && stars <= MAX_STARS) this.stars=stars;
        if (comment!=null && !comment.isEmpty()) this.comment = comment;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rating rating = (Rating) o;
        if (id == null) return super.equals(o);
        return id.equals(rating.id);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

}
