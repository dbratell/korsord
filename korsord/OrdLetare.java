package sarasas.korsord;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.*;
import sarasas.Util;


public class OrdLetare
{
    final static String DATABASE_URL = KorsordServlet.DATABASE_URL;
    final static String TABLE_NAME_PREFIX = KorsordServlet.TABLE_NAME_PREFIX;
    final static String COLUMN_NAME = KorsordServlet.COLUMN_NAME;
    final static int MAX_WORD_LENGTH = KorsordServlet.MAX_WORD_LENGTH;
    private final static char SQL_WILD_CHAR = '_';

    private Connection mConn = null;

    public OrdLetare()
        throws ClassNotFoundException
    {
        // Have to have it in memory. 
        Class.forName ("sun.jdbc.odbc.JdbcOdbcDriver");
    }
    
    public static void main(String args[])
        throws Exception
    {
        new OrdLetare().parseFile(args[0]);
    }

    public void parseFile(String file)
        throws Exception
    {
        Reader indata = new BufferedReader(new FileReader(file));
        
        StreamTokenizer strtok = new StreamTokenizer(indata);

        strtok.lowerCaseMode(true);
        strtok.whitespaceChars('\'', '\'');
        strtok.whitespaceChars('\"', '\"');
        strtok.whitespaceChars('.', '.');
        strtok.whitespaceChars(',', ',');
        strtok.whitespaceChars(';', ';');
        strtok.whitespaceChars(':', ':');
        strtok.whitespaceChars('»', '»');
        strtok.whitespaceChars('-', '-');

        mConn = DriverManager.getConnection(DATABASE_URL, "", "");       
        
        String lastAnswer = "";

        int skippedWordCount = 0;
        Set seenWordSet = new HashSet();
        
        BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));
        while (strtok.nextToken() != StreamTokenizer.TT_EOF)
        {
            if (strtok.ttype == StreamTokenizer.TT_WORD)
            {
                String word = strtok.sval.toLowerCase();

                //              System.out.print("-->"+word+"<--");
                if (word.length() <= MAX_WORD_LENGTH &&
                    !seenWordSet.contains(word) &&
                    !hasWord(word))
                {
                    System.out.print("("+skippedWordCount+
                                     ") '"+word+"'? ["+lastAnswer+"] ");
                    String answer = ""; // in.readLine().toLowerCase();
                    if (answer.length() == 0)
                    {
                        answer = lastAnswer;
                    }
                    
                    //              if (answer.startsWith("j") || answer.startsWith("y"))
                    {
                        insertWord(word);
                        System.out.print("OK.  ");
                    }

                    lastAnswer = answer;
                    skippedWordCount = 0;
                }
                else
                {
                    skippedWordCount++;
                }

                seenWordSet.add(word);
            }
        }
        
    }

    public boolean hasWord(String word)
    {
        int wordLength = word.length();
        final String tableName = TABLE_NAME_PREFIX+wordLength;

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
//          // Obtain our environment naming context
//          Context initCtx = new InitialContext();
//          Context envCtx = (Context) initCtx.lookup("java:comp/env");
            
//          // Look up our data source
//          DataSource ds = (DataSource)
//              envCtx.lookup(DATASOURCE_NAME);
//          conn = ds.getConnection();

            // Allocate and use a connection from the pool          
            //  ... use this connection to access the database ...
            String sqlQuery =
                "SELECT "+COLUMN_NAME+" FROM "+ tableName +" WHERE "+
                COLUMN_NAME + "='" + sqlEscape(word)+"'";
            statement = mConn.prepareStatement(sqlQuery);
            resultSet = statement.executeQuery();

            // Do we have anything?
            return resultSet.next();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        finally
        {
            Util.closeResultSet(resultSet);
            Util.closeStatement(statement);
        }
    
    }
    
    public void insertWord(String word)
    {
        
        int wordLength = word.length();
        final String tableName = TABLE_NAME_PREFIX+wordLength;

        Statement statement = null;
        try
        {
//          // Obtain our environment naming context
//          Context initCtx = new InitialContext();
//          Context envCtx = (Context) initCtx.lookup("java:comp/env");
            
//          // Look up our data source
//          DataSource ds = (DataSource)
//              envCtx.lookup(DATASOURCE_NAME);
//          conn = ds.getConnection();

            // Allocate and use a connection from the pool          
            //  ... use this connection to access the database ...
            statement = mConn.createStatement();
            String sqlCommand =
                "INSERT INTO "+tableName+
                " ("+COLUMN_NAME+") VALUES "+
                "('"+sqlEscape(word)+"')";
            //      System.out.println(sqlCommand);
            statement.executeUpdate(sqlCommand);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            Util.closeStatement(statement);
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
