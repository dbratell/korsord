package sarasas;

import java.sql.SQLException;

public class Util
{
    private Util()
    {
    }

    public static void closeConnection(java.sql.Connection conn)
    {
        if (conn != null)
        {
            try
            {
                conn.close();
            }
            catch (SQLException sqle)
            {
                // Don't care. Can't do anything anyway.
            }
        }
    }
                
    
    public static void closeStatement(java.sql.Statement statement)
    {
        if (statement != null)
        {
            try
            {
                statement.close();
            }
            catch (SQLException sqle)
            {
                // Don't care. Can't do anything anyway.
            }
        }
    }

    public static void closeResultSet(java.sql.ResultSet resultSet)
    {
        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (SQLException sqle)
            {
                // Don't care. Can't do anything anyway.
            }
        }
    }
    
}
