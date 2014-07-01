package net.steveperkins.fitnessjiffy.etl.writer;

import net.steveperkins.fitnessjiffy.etl.model.Datastore;
import net.steveperkins.fitnessjiffy.etl.model.Exercise;
import net.steveperkins.fitnessjiffy.etl.model.ExercisePerformed;
import net.steveperkins.fitnessjiffy.etl.model.Food;
import net.steveperkins.fitnessjiffy.etl.model.FoodEaten;
import net.steveperkins.fitnessjiffy.etl.model.User;
import net.steveperkins.fitnessjiffy.etl.model.Weight;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.TABLES;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.EXERCISE;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.FOOD;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.USER;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.WEIGHT;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.FOOD_EATEN;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader.EXERCISE_PERFORMED;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class LegacySQLiteWriter extends JDBCWriter {

    private final Map<UUID, Integer> userIds = new HashMap<>();
    private final Map<UUID, Integer> exerciseIds = new HashMap<>();
    private final Map<UUID, Integer> foodIds = new HashMap<>();
    private final Map<UUID, Integer> weightIds = new HashMap<>();
    private final Map<UUID, Integer> foodEatenIds = new HashMap<>();
    private final Map<UUID, Integer> exercisePerformedIds = new HashMap<>();

    private final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    public LegacySQLiteWriter(@Nonnull Connection connection, @Nonnull Datastore datastore) {
        super(connection, datastore);
    }

    @Override
    protected void writeSchema() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS [EXERCISES] (\n" +
                "  [ID] INTEGER PRIMARY KEY, \n" +
                "  [NAME] [VARCHAR(50)] NOT NULL UNIQUE, \n" +
                "  [CALORIES_PER_HOUR] INTEGER NOT NULL, \n" +
                "  [HIDDEN] BOOLEAN NOT NULL DEFAULT 'FALSE');\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS [EXERCISES_PERFORMED] (\n" +
                "  [USER_ID] INTEGER NOT NULL CONSTRAINT [user_foreignkey] REFERENCES [USERS]([ID]) ON DELETE NO ACTION ON UPDATE CASCADE, \n" +
                "  [EXERCISE_ID] INTEGER NOT NULL CONSTRAINT [exercise_foreignkey] REFERENCES [EXERCISES]([ID]) ON DELETE NO ACTION ON UPDATE CASCADE, \n" +
                "  [DATE] DATE NOT NULL, \n" +
                "  [MINUTES] INTEGER NOT NULL, \n" +
                "  [ID] INTEGER NOT NULL PRIMARY KEY);\n" +
                "\n" +
                "CREATE INDEX IF NOT EXISTS [exercised_performed_unique] ON [EXERCISES_PERFORMED] ([USER_ID], [EXERCISE_ID], [DATE]);\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS [FOODS] (\n" +
                "  [ID] INTEGER PRIMARY KEY, \n" +
                "  [NAME] [VARCHAR(50)] NOT NULL UNIQUE, \n" +
                "  [DEFAULT_SERVING_TYPE] [VARCHAR(20)], \n" +
                "  [SERVING_TYPE_QTY] [FLOAT(0, 2)] DEFAULT 1, \n" +
                "  [CALORIES] INTEGER NOT NULL, \n" +
                "  [FAT] [FLOAT(0, 2)], \n" +
                "  [SATURATED_FAT] [FLOAT(0, 2)], \n" +
                "  [CARBS] [FLOAT(0, 2)], \n" +
                "  [FIBER] [FLOAT(0, 2)], \n" +
                "  [SUGAR] [FLOAT(0, 2)], \n" +
                "  [PROTEIN] [FLOAT(0, 2)], \n" +
                "  [SODIUM] [FLOAT(0, 2)], \n" +
                "  [USER_ID] INTEGER CONSTRAINT [food_owner_foreignkey] REFERENCES [USERS]([ID]) ON DELETE NO ACTION ON UPDATE CASCADE);\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS [FOODS_EATEN] (\n" +
                "  [USER_ID] INTEGER NOT NULL CONSTRAINT [user_foreignkey] REFERENCES [USERS]([ID]) ON DELETE NO ACTION ON UPDATE CASCADE, \n" +
                "  [FOOD_ID] INTEGER NOT NULL CONSTRAINT [food_foreignkey] REFERENCES [FOODS]([ID]) ON DELETE NO ACTION ON UPDATE CASCADE, \n" +
                "  [DATE] DATE NOT NULL, \n" +
                "  [SERVING_TYPE] [VARCHAR(20)] NOT NULL, \n" +
                "  [SERVING_QTY] [FLOAT(0, 2)] NOT NULL DEFAULT 1, \n" +
                "  [ID] INTEGER NOT NULL PRIMARY KEY);\n" +
                "\n" +
                "CREATE UNIQUE INDEX IF NOT EXISTS [foods_eaten_unique] ON [FOODS_EATEN] ([USER_ID], [FOOD_ID], [DATE]);\n" +
                "\n" +
                "\n" +
//                "CREATE TABLE IF NOT EXISTS [SERVING_TYPES] (\n" +
//                "  [NAME] [VARCHAR(20)], \n" +
//                "  [NUM_OF_OUNCES] [FLOAT(0, 5)], \n" +
//                "  [ID] INTEGER NOT NULL PRIMARY KEY);\n" +
//                "\n" +
//                "CREATE INDEX IF NOT EXISTS [serving_types_unique] ON [SERVING_TYPES] ([NAME]);\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS [USERS] (\n" +
                "  [ID] INTEGER PRIMARY KEY, \n" +
                "  [GENDER] [VARCHAR(7)] NOT NULL, \n" +
                "  [AGE] INTEGER NOT NULL, \n" +
                "  [HEIGHT_IN_INCHES] [FLOAT(0, 2)] NOT NULL, \n" +
                "  [ACTIVITY_LEVEL] [FLOAT(0, 2)] NOT NULL, \n" +
                "  [USERNAME] [VARCHAR(65)] NOT NULL UNIQUE, \n" +
                "  [PASSWORD] CLOB NOT NULL, \n" +
                "  [FIRST_NAME] [VARCHAR(50)] NOT NULL, \n" +
                "  [LAST_NAME] [VARCHAR(50)] NOT NULL, \n" +
                "  [ACTIVE] [CHAR(1)] NOT NULL DEFAULT 'Y');\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS [WEIGHT] (\n" +
                "  [USER_ID] INTEGER NOT NULL CONSTRAINT [user_foreignkey] REFERENCES [USERS]([ID]) ON DELETE NO ACTION ON UPDATE CASCADE, \n" +
                "  [DATE] DATE NOT NULL, \n" +
                "  [POUNDS] [FLOAT(0, 2)] NOT NULL, \n" +
                "  [ID] INTEGER NOT NULL PRIMARY KEY);\n" +
                "\n" +
                "CREATE INDEX IF NOT EXISTS [weight_unique] ON [WEIGHT] ([USER_ID], [DATE]);";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
        }
    }

    @Override
    protected void writeExercises() throws SQLException {
        List<String> descriptionsWritten = new ArrayList<>();

        for(Exercise exercise : datastore.getExercises()) {
            if(!descriptionsWritten.contains(exercise.getDescription())) {
                descriptionsWritten.add(exercise.getDescription());

                int exerciseId = getNextAvailableId(exerciseIds.values());
                exerciseIds.put(exercise.getId(), exerciseId);

                String sql = "INSERT INTO "+TABLES.EXERCISE+" ("+ EXERCISE.ID+", "+EXERCISE.NAME+", "+EXERCISE.CALORIES_PER_HOUR
                        +", "+EXERCISE.HIDDEN+") VALUES (?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, exerciseId);
                    statement.setString(2, exercise.getDescription());
                    int caloriesBurnedPerHour = (int) (exercise.getMetabolicEquivalent() * (300 / 2.2));
                    statement.setInt(3, caloriesBurnedPerHour);
                    statement.setString(4, "FALSE");
                    statement.executeUpdate();
                }
            }
        }
    }

    @Override
    protected void writeUsers() throws Exception {
        for(User user : datastore.getUsers()) {
            int userId = getNextAvailableId(userIds.values());
            userIds.put(user.getId(), userId);

            Calendar birthDate = new GregorianCalendar();
            birthDate.setTimeInMillis(user.getBirthdate().getTime());
            Calendar currentDate = new GregorianCalendar();
            int age = currentDate.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);


            String userSql = "INSERT INTO "+ TABLES.USER+" ("+USER.ID+", "+ USER.GENDER+", "+USER.AGE+", "+USER.HEIGHT_IN_INCHES
                    +", "+USER.ACTIVITY_LEVEL+", "+USER.USERNAME+", "+USER.PASSWORD+", "+USER.FIRST_NAME+", "
                    +USER.LAST_NAME+", "+USER.IS_ACTIVE+") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                statement.setInt(1, userId);
                statement.setString(2, user.getGender().toString());
                statement.setInt(3, age);
                statement.setFloat(4, (float) user.getHeightInInches());
                statement.setFloat(5, (float) user.getActivityLevel().getValue());
                statement.setString(6, user.getEmail());

                // The old legacy SQLite database was used in versions of the application that lacked true user-level authentication.  The
                // Perl version had a global username and password configured for basic auth at the Apache level, which the application code
                // never even knew about.  The Python version relied on the Django admin login system.  In both cases, the application
                // model was such that if you knew the global admin credentials, then you could do anything with any user.
                // Never versions of the application, and supporting databases, are built around true user-level authentication.  However,
                // we can hardcode a password here because there's no need for backporting all of that to the older versions.
                statement.setString(7, "password");
                statement.setString(8, user.getFirstName());
                statement.setString(9, user.getLastName());

                // The concept of an "active" or "inactive" user is dropped (for now) in the newer versions of the application.  It was
                // never really used in the legacy SQLite versions either, so it can be hardcoded to "yes".
                statement.setString(10, "Y");
                statement.executeUpdate();
            }

            for(Weight weight : user.getWeights()) {
                int weightId = getNextAvailableId(weightIds.values());
                weightIds.put(weight.getId(), weightId);

                String sql = "INSERT INTO "+TABLES.WEIGHT+" ("+ WEIGHT.ID+", "+WEIGHT.USER_ID+", "+WEIGHT.DATE+", "
                        +WEIGHT.POUNDS+") VALUES (?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, weightId);
                    statement.setInt(2, userId);
                    statement.setString(3, dateFormatter.format(weight.getDate()));
                    statement.setFloat(4, weight.getPounds().floatValue());
                    statement.executeUpdate();
                }
            }

            for(Food food : user.getFoods()) {
                writeFood(food, user.getId());
            }

            for(FoodEaten foodEaten : user.getFoodsEaten()) {
                if(foodIds.get(foodEaten.getFoodId()) == null) {
                    throw new Exception("Found no food matching FOOD_EATEN with ID: " + foodEaten.getId());
                }

                int foodEatenId = getNextAvailableId(foodEatenIds.values());
                foodEatenIds.put(foodEaten.getId(), foodEatenId);

                String sql = "INSERT INTO "+TABLES.FOOD_EATEN+" ("+FOOD_EATEN.ID+", "+FOOD_EATEN.USER_ID+", "
                        +FOOD_EATEN.FOOD_ID+", "+FOOD_EATEN.DATE+", "+FOOD_EATEN.SERVING_TYPE+", "
                        +FOOD_EATEN.SERVING_QTY+") VALUES (?, ?, ?, ?, ? ,?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    String servingType = foodEaten.getServingType().toString();
                    if(!servingType.equals("CUSTOM")) {
                        servingType = servingType.toLowerCase();
                    }

                    statement.setInt(1, foodEatenId);
                    statement.setInt(2, userId);
                    statement.setInt(3, foodIds.get(foodEaten.getFoodId()));
                    statement.setString(4, dateFormatter.format(foodEaten.getDate()));
                    statement.setString(5, servingType);
                    statement.setFloat(6, foodEaten.getServingQty().floatValue());
                    statement.executeUpdate();
                }
            }

            for(ExercisePerformed exercisePerformed : user.getExercisesPerformed()) {
                if(exerciseIds.get(exercisePerformed.getExerciseId()) == null) {
                    throw new Exception("Found no exercise matching EXERCISES_PERFORMED with ID: " + exercisePerformed.getId());
                }

                int exercisePerfomedId = getNextAvailableId(exercisePerformedIds.values());
                exercisePerformedIds.put(exercisePerformed.getId(), exercisePerfomedId);

                String sql = "INSERT INTO "+TABLES.EXERCISE_PERFORMED+" ("+EXERCISE_PERFORMED.ID+", "
                        +EXERCISE_PERFORMED.USER_ID+", "+EXERCISE_PERFORMED.EXERCISE_ID+", "
                        +EXERCISE_PERFORMED.DATE+", "+EXERCISE_PERFORMED.MINUTES+") VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, exercisePerfomedId);
                    statement.setInt(2, userId);
                    statement.setInt(3, exerciseIds.get(exercisePerformed.getExerciseId()));
                    statement.setString(4, dateFormatter.format(exercisePerformed.getDate()));
                    statement.setInt(5, exercisePerformed.getMinutes());
                    statement.executeUpdate();
                }
            }
        }
    }

    @Override
    protected void writeFood(@Nonnull Food food, @Nullable UUID ownerId) throws SQLException {
        int foodId = getNextAvailableId(foodIds.values());
        foodIds.put(food.getId(), foodId);

        String sql = "INSERT INTO "+TABLES.FOOD+" ("+FOOD.ID+", "+FOOD.NAME+", "+FOOD.DEFAULT_SERVING_TYPE+", "
                +FOOD.SERVING_TYPE_QTY+", "+FOOD.CALORIES+", "+FOOD.FAT+", "+FOOD.SATURATED_FAT+", "
                +FOOD.CARBS+", "+FOOD.FIBER+", "+FOOD.SUGAR+", "+FOOD.PROTEIN+", "+FOOD.SODIUM;
        sql += (ownerId != null && userIds.get(ownerId) != null)
                ? ", "+FOOD.USER_ID+") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            String defaultServingType = food.getDefaultServingType().toString();
            if(!defaultServingType.equals("CUSTOM")) {
                defaultServingType = defaultServingType.toLowerCase();
            }

            statement.setInt(1, foodId);
            statement.setString(2, food.getName());
            statement.setString(3, defaultServingType);
            statement.setFloat(4, food.getServingTypeQty().floatValue());
            statement.setInt(5, food.getCalories());
            statement.setFloat(6, food.getFat().floatValue());
            statement.setFloat(7, food.getSaturatedFat().floatValue());
            statement.setFloat(8, food.getCarbs().floatValue());
            statement.setFloat(9, food.getFiber().floatValue());
            statement.setFloat(10, food.getSugar().floatValue());
            statement.setFloat(11, food.getProtein().floatValue());
            statement.setFloat(12, food.getSodium().floatValue());
            if(ownerId != null && userIds.get(ownerId) != null) {
                statement.setInt(13, userIds.get(ownerId));
            }
            statement.executeUpdate();
        }
    }

    private int getNextAvailableId(@Nonnull Collection<Integer> ids) {
        if(ids == null || ids.isEmpty()) {
            return 1;
        }
        List<Integer> sortedIds = new ArrayList<>(ids);
        Collections.sort(sortedIds);
        int currentMax = sortedIds.get(sortedIds.size() - 1);
        return currentMax + 1;
    }

}
