package sarasas.korsord;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.*;
import sarasas.Util;


public class ListaOrd
{
    final static String DATABASE_URL = KorsordServlet.DATABASE_URL;
    final static String TABLE_NAME_PREFIX = KorsordServlet.TABLE_NAME_PREFIX;
    final static String COLUMN_NAME = KorsordServlet.COLUMN_NAME;
    final static int MAX_WORD_LENGTH = KorsordServlet.MAX_WORD_LENGTH;
    private final static char SQL_WILD_CHAR = '_';

    public ListaOrd()
        throws ClassNotFoundException
    {
        // Have to have it in memory. 
        Class.forName ("sun.jdbc.odbc.JdbcOdbcDriver");
    }
    
    public static void main(String args[])
        throws Exception
    {
        new ListaOrd().listFiles(args[0]);
    }

    public void listFiles(String file)
        throws Exception
    {
        PrintWriter out = new PrintWriter(new FileWriter(file));

        for (int wordLength = 1; wordLength <= MAX_WORD_LENGTH; wordLength++)
        {
            String tableName = TABLE_NAME_PREFIX+wordLength;

            System.out.println("Tabell: "+tableName);

            Connection conn =
                DriverManager.getConnection(DATABASE_URL, "", "");       

            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try
            {
                // Allocate and use a connection from the pool          
                //  ... use this connection to access the database ...
                String sqlQuery =
                    "SELECT "+COLUMN_NAME+" FROM "+ tableName;
                statement = conn.prepareStatement(sqlQuery);
                resultSet = statement.executeQuery();

                while(resultSet.next())
                {
                    String word = resultSet.getString(1);
                    out.println(word);
                }
            }
            finally
            {
                Util.closeResultSet(resultSet);
                Util.closeStatement(statement);
            }
        }
        out.close();
    }
}
