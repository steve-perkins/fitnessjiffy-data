package net.steveperkins.fitnessjiffy.etl.test;

import net.steveperkins.fitnessjiffy.etl.model.Datastore;
import net.steveperkins.fitnessjiffy.etl.model.ReportData;
import net.steveperkins.fitnessjiffy.etl.model.User;
import net.steveperkins.fitnessjiffy.etl.reader.LegacySQLiteReader;
import net.steveperkins.fitnessjiffy.etl.writer.LegacySQLiteWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;

import static junit.framework.TestCase.assertEquals;

public final class LegacySQLiteTest extends AbstractTest {

    protected static final int EXPECTED_JSON_STRING_LENGTH = 3475955;
    protected static final int EXPECTED_JSON_FILE_LENGTH = 3476102;

    @Before
    public void before() throws Exception {
        Class.forName("org.sqlite.JDBC");
        cleanFileInWorkingDirectory("sqlite-temp.db");
        cleanFileInWorkingDirectory("h2-temp.h2.db");
    }

    @Test
    public void canReadTest() throws Exception {
        final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + CURRENT_WORKING_DIRECTORY + "sqlite.db");

        // The the number of report data entries that were generated
        final Datastore datastore = new LegacySQLiteReader(connection).read();
        final User user = datastore.getUsers().iterator().next();
        assertEquals(EXPECTED_NUMBER_OF_REPORT_DATA_RECORDS, user.getReportData().size());

        // Remove the report data entries, so that the rest of the data will have a predictable size
        // (size can't be predicted with the report data entries in place, because different UUID values are generated for them with each run)
        user.setReportData(new HashSet<ReportData>());

        // Test conversion to JSON string
        final String jsonString = datastore.toJSONString();
        assertEquals(EXPECTED_JSON_STRING_LENGTH, jsonString.length());

        // Test output to JSON file
        final File jsonFile = new File(CURRENT_WORKING_DIRECTORY + "output.json");
        datastore.toJSONFile(jsonFile);
        assertEquals(EXPECTED_JSON_FILE_LENGTH, jsonFile.length());

        connection.close();
    }

    @Test
    public void canWriteTest() throws Exception {
        // Read the existing database
        final Connection readConnection = DriverManager.getConnection("jdbc:sqlite:" + CURRENT_WORKING_DIRECTORY + "sqlite.db");
        final Datastore datastore = new LegacySQLiteReader(readConnection).read();
        readConnection.close();

        // Write its contents to a new database
        final Connection writeConnection = DriverManager.getConnection("jdbc:sqlite:" + CURRENT_WORKING_DIRECTORY + "sqlite-temp.db");
        new LegacySQLiteWriter(writeConnection, datastore).write();
        writeConnection.close();

        // Do a round-trip read of the new database, and confirm its etl has the same expected size
        final Connection confirmationConnection = DriverManager.getConnection("jdbc:sqlite:" + CURRENT_WORKING_DIRECTORY + "sqlite-temp.db");
        final Datastore newDatastore = new LegacySQLiteReader(confirmationConnection).read();
        confirmationConnection.close();
        assertEquals(newDatastore.getExercises().size(), datastore.getExercises().size());
        assertEquals(newDatastore.getGlobalFoods().size(), datastore.getGlobalFoods().size());
        assertEquals(newDatastore.getUsers().size(), datastore.getUsers().size());
        assertEquals(newDatastore.getUsers().iterator().next().getWeights().size(), datastore.getUsers().iterator().next().getWeights().size());
        assertEquals(newDatastore.getUsers().iterator().next().getFoods().size(), datastore.getUsers().iterator().next().getFoods().size());
        assertEquals(newDatastore.getUsers().iterator().next().getFoodsEaten().size(), datastore.getUsers().iterator().next().getFoodsEaten().size());
        assertEquals(newDatastore.getUsers().iterator().next().getExercisesPerformed().size(), datastore.getUsers().iterator().next().getExercisesPerformed().size());
    }

}
