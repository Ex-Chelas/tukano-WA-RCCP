package tukano.impl.databse;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Hibernate implementation of the DBService
 * Uses PostgresSQL as the database
 *
 * @See DBService
 */
public class Hibernate implements DBService {
    private static final Logger Log = Logger.getLogger(Hibernate.class.getName());

    private static final String HIBERNATE_CFG_FILE = "scc2425-tukano/src/main/webapp/WEB-INF/classes/hibernate.cfg.xml";
    private static Hibernate instance;
    private final SessionFactory sessionFactory;

    private Hibernate(String connectionString) {
        try {
            sessionFactory = new Configuration().configure(new File(HIBERNATE_CFG_FILE)).buildSessionFactory();
        } catch (Exception e) {
            Log.severe("Error in Hibernate constructor: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the Hibernate instance, initializing if necessary. Requires a
     * configuration file (hibernate.cfg.xml) in the root directory.
     */
    synchronized public static Hibernate getInstance(String connectionString, String primaryRegion) {
        if (instance == null)
            instance = new Hibernate(connectionString);
        return instance;
    }

    public Result<Void> persistOne(Object obj) {
        return execute((hibernate) -> {
            hibernate.persist(obj);
        });
    }

    @Override
    public <T> Result<T> updateOne(T obj) {
        return execute(hibernate -> {
            var res = hibernate.merge(obj);
            if (res == null)
                return Result.error(ErrorCode.NOT_FOUND);

            return Result.ok(res);
        });
    }

    @Override
    public <T> Result<T> insertOne(T obj) {
        return execute(hibernate -> {
            hibernate.persist(obj);
            return Result.ok(obj);
        });
    }

    @Override
    public <T, R> Result<List<R>> sql(String query, Class<R> returnClazz, Class<T> clazz) {
        return execute(hibernate -> {
            var res = hibernate.createNativeQuery(query, returnClazz).list();
            return Result.ok(res);
        });
    }

    @Override
    public <T> Result<T> getOne(String id, Class<T> clazz) {
        return execute(hibernate -> {
            var res = hibernate.find(clazz, id);
            if (res == null)
                return Result.error(ErrorCode.NOT_FOUND);
            else
                return Result.ok(res);
        });
    }

    @Override
    public <T> Result<T> deleteOne(T obj) {
        return execute(hibernate -> {
            hibernate.remove(obj);
            return Result.ok(obj);
        });
    }

    //@Override
    public <T> Result<T> getOne(Object id, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            var res = session.find(clazz, id);
            if (res == null)
                return Result.error(ErrorCode.NOT_FOUND);
            else
                return Result.ok(res);
        } catch (Exception e) {
            Log.severe("Error in Hibernate.getOne: " + e.getMessage());
            throw e;
        }
    }

    //@Override
    public <T> List<T> sql(String sqlStatement, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createNativeQuery(sqlStatement, clazz);
            return query.list();
        } catch (Exception e) {
            Log.severe("Error in Hibernate.sql: " + e.getMessage());
            throw e;
        }
    }

    public <T> Result<T> execute(Consumer<Session> proc) {
        return execute((hibernate) -> {
            proc.accept(hibernate);
            return Result.ok();
        });
    }

    public <T> Result<T> execute(Function<Session, Result<T>> func) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            var res = func.apply(session);
            session.flush();
            tx.commit();
            return res;
        } catch (ConstraintViolationException __) {
            return Result.error(ErrorCode.CONFLICT);
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();

            Log.severe("Error in Hibernate.execute: " + e.getMessage());
            throw e;
        }
    }
}
