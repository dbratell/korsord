package sarasas.korsord;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.*;
import sarasas.Util;


public class TaBortOrd
{
    final static String DATABASE_URL = KorsordServlet.DATABASE_URL;
    final static String TABLE_NAME_PREFIX = KorsordServlet.TABLE_NAME_PREFIX;
    final static String COLUMN_NAME = KorsordServlet.COLUMN_NAME;
    final static int MAX_WORD_LENGTH = KorsordServlet.MAX_WORD_LENGTH;
    private final static char SQL_WILD_CHAR = '_';

    public TaBortOrd()
        throws ClassNotFoundException
    {
        // Have to have it in memory. 
        Class.forName ("sun.jdbc.odbc.JdbcOdbcDriver");
    }
    
    public static void main(String args[])
        throws Exception
    {
        new TaBortOrd().removeWords(args);
    }

    public void removeWords(String[] words)
        throws Exception
    {
        Connection conn =
            DriverManager.getConnection(DATABASE_URL, "", "");       

        try
        {
            for (int i = 0; i < words.length; i++)
            {
                String word = words[i];
                
                int wordLength = word.length();
                
                String tableName = TABLE_NAME_PREFIX+wordLength;
                
                System.out.println("Tabell: "+tableName);
                
                PreparedStatement statement = null;
                //            ResultSet resultSet = null;
                try
                {
                    // Allocate and use a connection from the pool          
                    //  ... use this connection to access the database ...
                    String sqlQuery =
                        "DELETE FROM "+ tableName +  " WHERE "+COLUMN_NAME +
                        "='"+sqlEscape(word.toLowerCase())+"'";
                    statement = conn.prepareStatement(sqlQuery);
                    int rowCount = statement.executeUpdate();
                    
                    System.out.println(word+": deleted "+rowCount+" lines.");
                }
                finally
                {
                    Util.closeStatement(statement);
                }
            }
        }
        finally
        {
            Util.closeConnection(conn);
        }
    }

    private String sqlEscape(String unsafeString)
    {
        StringBuffer safe = new StringBuffer();
        for (int i = 0; i<unsafeString.length(); i++)
        {
            safe.append(sqlEscape(unsafeString.charAt(i)));
        }
        return safe.toString();
    }
    
    private String sqlEscape(char unsafeChar)
    {
        if (unsafeChar == '\'')
        {
            return "''";
        }
        return String.valueOf(unsafeChar);
    }

}
