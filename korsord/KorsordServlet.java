package sarasas.korsord;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.*;

public class KorsordServlet extends HttpServlet {

    final static String WORD_LENGTH_PARAMETER_NAME = "wordlength";
    final static String CHAR_PARAMETER_NAME_PREFIX = "l";
    final static String TABLE_NAME_PREFIX = "ord";
    final static String DATABASE_URL = "jdbc:odbc:ord";
    final static int MAX_WORD_LENGTH = 20;
    final static String COLUMN_NAME = "ord";
    private final static char SQL_WILD_CHAR = '_';

    private final static String USER_AGENT_HEADER = "User-Agent";

    public void init()
        throws ServletException
    {
        try
        {
            // Have to have it in memory. 
            Class.forName ("sun.jdbc.odbc.JdbcOdbcDriver");
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new ServletException(cnfe);
        }
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        int wordLength = -1;
        
        String wordLengthStr =
            request.getParameter(WORD_LENGTH_PARAMETER_NAME);
        if (wordLengthStr != null)
        {
            try
            {
                wordLength = Integer.parseInt(wordLengthStr);
            }
            catch (NumberFormatException nfe)
            {
                // Ok, we never set wordLength to a legal number and 
                // that is enough.
            }
        }

        if (wordLength < 1 || wordLength > MAX_WORD_LENGTH)
        {
            printStandardQueryPage(request, response);
            return;
        }
        
        char[] chars = new char[wordLength]; // New nulled array
        for (int i = 0; i < wordLength; i++)
        {
            String thisChar =
                request.getParameter(CHAR_PARAMETER_NAME_PREFIX+i);
            if (thisChar != null && thisChar.length() == 1)
            {
                chars[i] = thisChar.charAt(0);
            }
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        printDocHeader(out, request.getHeader(USER_AGENT_HEADER));
        out.println("<h1>Korsordshjälpen</h1>");

        printForm(request, out, chars);
        
        out.println("<p>Du vill ha ett ord med "+wordLength+" bokstäver:<br>");

        for (int i = 0; i < wordLength; i++)
        {
            //            out.print("<li>");
            if (chars[i] == '\0')
            {
                out.print("<i>lucka</i> ");
            }
            else
            {
                out.print("<b>"+htmlEscape(chars[i])+"</b> ");
            }
            //            out.println("</li>");
        }

        //      listAllWords(out, wordLength);

        //      out.println("<p>chars = "+htmlEscape(String.valueOf(chars)));

        listMatchingWords(out, chars);
        
        out.println("<hr><address>Sidansvarig: Daniel Bratell</address>");
        printDocFooter(out);
        
    }

    private void listAllWords(PrintWriter out, int wordLength)
    {
        final String tableName = TABLE_NAME_PREFIX+wordLength;

        Connection conn = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            // Allocate and use a connection from the pool          
            conn = DriverManager.getConnection(DATABASE_URL, "", "");    
            //  ... use this connection to access the database ...
            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT "+COLUMN_NAME+" FROM "+
                                               tableName);

            out.println("<ul>");
            while (resultSet.next())
            {
                out.println("<li>"+htmlEscape(resultSet.getString(1))+"</li>");
            }
            conn.close();
            out.println("</ul>");
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            out.println("Misslyckades!<br>"+htmlPreEscape(sw.toString()));
        }
        finally
        {
            closeResultSet(resultSet);
            closeStatement(statement);
            closeConnection(conn);
        }
    }


    private void listMatchingWords(PrintWriter out, char[] pattern)
    {
        int wordLength = pattern.length;
        final String tableName = TABLE_NAME_PREFIX+wordLength;

        StringBuffer sqlPatternBuffer = new StringBuffer();
        for (int i = 0; i < wordLength; i++)
        {
            if (pattern[i] == '\0')
            {
                sqlPatternBuffer.append(SQL_WILD_CHAR);
            }
            else
            {
                sqlPatternBuffer.append(sqlEscape(Character.toLowerCase(pattern[i])));
            }
            
        }
        
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            // Allocate and use a connection from the pool          
            conn = DriverManager.getConnection(DATABASE_URL, "", "");    
            //  ... use this connection to access the database ...
            String sqlQuery =
                "SELECT "+COLUMN_NAME+" FROM "+ tableName +" WHERE "+
                COLUMN_NAME +" LIKE '"+ sqlPatternBuffer.toString() + "'";

            //      out.println("<p>Query = "+htmlEscape(sqlQuery));
            statement = conn.prepareStatement(sqlQuery);
            resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                out.println("<h2>Passande ord</h2>");
                
                int columns = 40/(wordLength+3) + 1;
                int currentColumn = 0;
                out.println("<table class=\"gles\">");
                do
                {
                    if (currentColumn == 0)
                    {
                        out.println("<tr>");
                    }
                    out.println("<td>"+
                                markAndHtmlEscapeMatch(resultSet.getString(1),
                                                       pattern)+
                                "</td>");
                    currentColumn = (currentColumn + 1)%columns;
                    if (currentColumn == 0)
                    {
                        out.println("</tr>");
                    }
                    
                }
                while (resultSet.next());

                int remainingColumns = columns - currentColumn;
                while(remainingColumns > 0)
                {
                    out.println("<td>&nbsp;</td>");
                    remainingColumns--;
                }
                
                out.println("</table>");
            }
            else
            {
                out.println("<p>Inga kända ord passade.");
            }
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            out.println("Misslyckades!<br>"+htmlPreEscape(sw.toString()));
        }
        finally
        {
            closeResultSet(resultSet);
            closeStatement(statement);
            closeConnection(conn);
        }
    }

    
    private void closeConnection(Connection conn)
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
                
    
    private void closeStatement(Statement statement)
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

    private void closeResultSet(ResultSet resultSet)
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

    private void printForm(HttpServletRequest request,
                           PrintWriter out, char[] chars)
    {
        out.println("<form method=get action=\""+
                    htmlAttributeEscape(request.getRequestURI())+"\">");
        out.print("Hur många bokstäver: " +
                    "<input id="+WORD_LENGTH_PARAMETER_NAME+
                    " name="+WORD_LENGTH_PARAMETER_NAME+
                    " type=text size=3");
        if (chars != null)
        {
            out.print(" value="+chars.length);
        }
        
        out.println(">");

        out.println("<p>");
        for (int i = 0; i < MAX_WORD_LENGTH; i++)
        {
            if (chars != null && chars.length == i && i != 0)
            {
                out.println(" - ");
            }
            char value = (chars != null && i < chars.length)?chars[i]:'\0';
            printLetterField(out, i, value);
        }
        out.println("</p>");

        out.println("<p><input type=submit value=Leta>");
        // Need to clear the fields some way. The reset button only
        // restores the initial values.
        //        out.println("<input type=reset value=\"Töm\">");
        out.println("</form>");
    }
    
    private void printStandardQueryPage(HttpServletRequest request,
                                        HttpServletResponse response)
    throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        printDocHeader(out, request.getHeader(USER_AGENT_HEADER));
        out.println("<h1>Korsordshjälpen</h1>");
        out.println("Mata in de bokstäver du har och antalet bokstäver "+
                    "i ordet och se vilka ord som passar in.");

        printForm(request, out, null);
        
        out.println("<hr><address>Sidansvarig: Daniel Bratell</address>");
        printDocFooter(out);
    }

    private void printLetterField(PrintWriter out, int number, char value)
    {
        String idAndName = CHAR_PARAMETER_NAME_PREFIX+number;
        out.print("<input class=\"box\" id=\""+idAndName+"\" "+
                    "name="+idAndName+
                    " type=text size=1 maxlength=1");
        if (value != '\0')
        {
            out.print(" value='"+htmlEscape(value)+"'");
        }
        out.println(">");
    }
    
    private void printDocHeader(PrintWriter out, String userAgent)
    {
      out.println("<!DOCTYPE HTML PUBLIC "+
                  "\"-//W3C//DTD HTML 4.01 Transitional//EN\">");
      out.println("<html><head><title>Korsordshjälp</title>");
//       out.println("<LINK href=\"mystyle.css\" "+
//                   "rel=\"style sheet\" type=\"text/css\">");
      out.println("<style>"+
                  ".gles td {padding: 0.3em 3em 0em 0em} "+
                  ".matchingchar {background: pink; color:black} "+
                  ".nomatchingchar {background: white; color:black} ");
      if (userAgent == null || !userAgent.startsWith("Mozilla/4.7"))
      {
          // This confuses Netscape 4.76 a lot. Let it look plain instead
          out.println(".box {border: 1px solid black; "+
                      "background: white; "+
                      "color: black; "+
                      "text-align:center; "+
                      "font-family: sans-serif; "+
                      "font-weight: bold; "+
                      "text-transform: uppercase} ");
      }
      out.println("</style>");
      out.println("</head>\n<body>");
    }

    private void printDocFooter(PrintWriter out)
    {
        out.println("</body>\n</html>");
    }

    private String htmlEscape(char unsafeChar)
    {
        String safeChar;
        
        switch(unsafeChar)
        {
        case '<':
            safeChar = "&lt;";
            break;
        case '>':
            safeChar = "&gt;";
            break;
        case '&':
            safeChar = "&amp;";
            break;
        default:
            safeChar = String.valueOf(unsafeChar);
        }

        return safeChar;
    }

    /**
     * Could be way faster!
     */
    private String markAndHtmlEscapeMatch(String unsafeWord, char[] pattern)
    {
        StringBuffer safeString = new StringBuffer();
        char [] chars = unsafeWord.toCharArray();

        int length = chars.length;

        String currentSpanClass = null;
        for (int i = 0; i < length; i++)
        {
            String spanClass;
            if (pattern[i] == '\0')
            {
                spanClass = "nonmatchingchar";
                //                safeString.append("<i>"+htmlEscape(chars[i])+"</i>");
                //                safeString.append(htmlEscape(chars[i]));
            }
            else
            {
                spanClass = "matchingchar";
//                 safeString.append("<span class=\"matchingchar\">"+
//                                   htmlEscape(chars[i])+"</span>");
                //                safeString.append(htmlEscape(Character.toUpperCase(chars[i])));
                //                safeString.append(htmlEscape(chars[i]));
            }
            if (!spanClass.equals(currentSpanClass))
            {
                if (currentSpanClass != null)
                {
                    safeString.append("</span>");
                }
                safeString.append("<span class=\""+spanClass+"\">");
                currentSpanClass = spanClass;
            }
            
            safeString.append(htmlEscape(chars[i]));
        }
        if (currentSpanClass != null)
        {
            safeString.append("</span>");
        }

        return safeString.toString();
    }
    
    private String htmlPreEscape(String unsafeString)
    {
        return "<pre>"+htmlEscape(unsafeString)+"</pre>";
    }
    
    private String htmlEscape(String unsafeString)
    {
        StringBuffer safeString = new StringBuffer();
        char [] chars = unsafeString.toCharArray();
        int length = chars.length;

        for (int i = 0; i < length; i++)
        {
            char unsafeChar = chars[i];
            String safeChar;
            switch(unsafeChar)
            {
            case '<':
                safeChar = "&lt;";
                break;
            case '>':
                safeChar = "&gt;";
                break;
            case '&':
                safeChar = "&amp;";
                break;
            default:
                safeChar = String.valueOf(unsafeChar);
            }

            safeString.append(safeChar);
        }

        return safeString.toString();
    }

    private String htmlAttributeEscape(String unsafeString)
    {
        StringBuffer safeString = new StringBuffer();
        char [] chars = unsafeString.toCharArray();
        int length = chars.length;

        for (int i = 0; i < length; i++)
        {
            char unsafeChar = chars[i];
            String safeChar;
            switch(unsafeChar)
            {
            case '<':
                safeChar = "&lt;";
                break;
            case '>':
                safeChar = "&gt;";
                break;
            case '&':
                safeChar = "&amp;";
                break;
            case '"':
                safeChar = "&quot;";
                break;
            default:
                safeChar = String.valueOf(unsafeChar);
            }

            safeString.append(safeChar);
        }

        return safeString.toString();
    }

    
    
    private char sqlEscape(char unsafeChar)
    {
        if (unsafeChar == '%' || unsafeChar == '\'')
        {
            return SQL_WILD_CHAR; // XXX Better than nothing
        }
        
        return unsafeChar;
    }
}
