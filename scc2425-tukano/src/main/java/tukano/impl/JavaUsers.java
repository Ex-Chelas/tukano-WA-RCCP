package tukano.impl;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import tukano.impl.databse.DB;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.*;

public class JavaUsers implements Users {
    private static final Logger Log = Logger.getLogger(JavaUsers.class.getName());
    private static final User systemUser = new User("0", "DEOXIS", "system@killteam.xpto", "Tukano Recommends");
    private static boolean isFirstUser = true;
    private static Users instance;

    private JavaUsers() {
    }

    synchronized public static Users getInstance() {
        if (instance == null)
            instance = new JavaUsers();
        return instance;
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info(() -> format("createUser : %s\n", user));
        if (isFirstUser) {
            DB.insertOne(systemUser);
            isFirstUser = false;
        }

        if (badUserInfo(user)) {
            return error(BAD_REQUEST);
        }
        var createUserResponse = errorOrValue(DB.insertOne(user), user.getId());
        JavaShorts.getInstance().follow(user.getId(), systemUser.getId(), true, user.pwd());
        return createUserResponse;
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {
        Log.info(() -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));

        if (userId == null)
            return error(BAD_REQUEST);

        return validatedUserOrError(DB.getOne(userId, User.class), pwd);
    }

    @Override
    public Result<User> updateUser(String userId, String pwd, User other) {
        Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

        if (badUpdateUserInfo(userId, pwd, other))
            return error(BAD_REQUEST);

        return errorOrResult(
                validatedUserOrError(DB.getOne(userId, User.class), pwd), user ->
                        DB.updateOne(user.updateFrom(other))
        );
    }

    @Override
    public Result<User> deleteUser(String userId, String pwd) {
        Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

        if (userId == null || pwd == null)
            return error(BAD_REQUEST);

        return errorOrResult(
                validatedUserOrError(DB.getOne(userId, User.class), pwd), user -> {
                    // Delete user shorts and related info asynchronously in a separate thread
                    Executors.defaultThreadFactory().newThread(() -> {
                        JavaShorts.getInstance().deleteAllShorts(userId, pwd);
                        JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
                    }).start();

                    return DB.deleteOne(user);
                }
        );
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info(() -> format("searchUsers : patterns = %s\n", pattern));

        var query = format("SELECT * FROM users u WHERE UPPER(u.id) LIKE '%%%s%%'", pattern.toUpperCase());
        var hits = DB.sql(query, User.class, User.class)
                .value()
                .stream()
                .map(User::copyWithoutPassword)
                .toList();

        return ok(hits);
    }


    private Result<User> validatedUserOrError(Result<User> res, String pwd) {
        if (res.isOK())
            return res.value().getPwd().equals(pwd) ? res : error(FORBIDDEN);
        else
            return res;
    }

    private boolean badUserInfo(User user) {
        return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
    }

    private boolean badUpdateUserInfo(String userId, String pwd, User info) {
        return (userId == null || pwd == null || info.getId() != null && !userId.equals(info.getId()));
    }
}
