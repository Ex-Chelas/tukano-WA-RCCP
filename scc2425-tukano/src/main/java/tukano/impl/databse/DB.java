package tukano.impl.databse;

import tukano.api.Result;

import java.util.List;

/**
 * Abstraction class for database operations
 * Singleton class aka kotlin equivalent of an object
 *
 * @See DBService
 */
public class DB {

    private static DBService instance;

    public static void configureInstance(DBService db) {
        instance = db;
    }

    public static <T, R> Result<List<R>> sql(String query, Class<R> returnClazz, Class<T> clazz) {
        return instance.sql(query, returnClazz, clazz);
    }

    public static <T, R> Result<List<R>> sql(Class<R> returnClazz, Class<T> clazz, String fmt, Object... args) {
        return instance.sql(String.format(fmt, args), returnClazz, clazz);
    }

    public static <T> Result<T> getOne(String id, Class<T> clazz) {
        return instance.getOne(id, clazz);
    }

    public static <T> Result<T> deleteOne(T obj) {
        return instance.deleteOne(obj);
    }

    public static <T> Result<T> updateOne(T obj) {
        return instance.updateOne(obj);
    }

    public static <T> Result<T> insertOne(T obj) {
        return Result.errorOrValue(instance.insertOne(obj), obj);
    }

//    public static <T> Result<T> transaction(Consumer<Session> c) {
//        return instance.transaction(c);
//    }
//
//    public static <T> Result<T> transaction(Function<Session, Result<T>> func) {
//        return instance.transaction(func);
//    }
}