package edu.montana.pika.query;

record OrderBy(String col, SortOrder direction) {

    public String toString() {
        return col + " " + (direction == null ? "" : direction.name());
    }
}
