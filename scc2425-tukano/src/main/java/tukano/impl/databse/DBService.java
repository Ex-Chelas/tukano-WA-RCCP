package tukano.impl.databse;

import tukano.api.Result;

import java.util.List;

/**
 * Interface for database service for CRUD and Query operations
 */
public interface DBService {

    /**
     * Execute a SQL query and returns the result
     *
     * @param query       SQL query
     * @param returnClazz Class of the return type
     * @param clazz       Class of the object to perform the query on
     * @param <T>         Operation class
     * @param <R>         Result class
     * @return Result containing the list of objects
     */
    <T, R> Result<List<R>> sql(String query, Class<R> returnClazz, Class<T> clazz);

    /**
     * Gets a single object from the database by its id/key
     *
     * @param id    Object's id/key
     * @param clazz Class of the object
     * @param <T>   Operation class
     * @return Result containing the object
     */
    <T> Result<T> getOne(String id, Class<T> clazz);

    /**
     * Deletes a single object from the database
     *
     * @param obj Object to delete
     * @param <T> Operation class
     * @return Result containing the object
     */
    <T> Result<T> deleteOne(T obj);

    /**
     * Updates a single object in the database
     * requires the object to have an unaltered id/key
     *
     * @param obj Object to update
     * @param <T> Operation class
     * @return Result containing the object
     */
    <T> Result<T> updateOne(T obj);

    /**
     * Inserts a single object into the database
     *
     * @param obj Object to insert
     * @param <T> Operation class
     * @return Result containing the object
     */
    <T> Result<T> insertOne(T obj);
}
