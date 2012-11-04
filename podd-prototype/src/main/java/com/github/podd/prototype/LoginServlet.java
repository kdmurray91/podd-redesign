package com.github.podd.prototype;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet to handle login to PODD prototype web service
 * 
 * @author kutila
 * @created 2012/10/26
 */
public class LoginServlet extends PoddBaseServlet
{
    
    // Check whether this behaves as expected
    private static Properties passwords;
    private static long passwordsLoadedAt = -1;
    
    /**
     */
    public LoginServlet()
    {
        super();
    }
    
    @Override
    protected void processRequest(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException
    {
        final PrintWriter out = response.getWriter();
        
        final String httpMethod = request.getMethod();
        String username = null;
        
        if(PoddBaseServlet.HTTP_POST.equals(httpMethod))
        {
            username = request.getParameter("username");
            final String password = request.getParameter("password");
            if(!this.checkCredentials(username, password))
            {
                this.log.info("Failed login attempt for " + username + "/" + password);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login failed");
                return;
            }
            // create session and send SUCCESS response
            final HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(180); // invalidate session after 3 minutes
            session.setAttribute("user", username);
            out.write(username + " login successful\r\n");
            out.flush();
            out.close();
            return;
        }
        
    }
    
    /**
     * The prototype will use a file based authentication mechanism. All userid/password pairs are
     * periodically read from a Properties file.
     * 
     * @param userid
     * @param password
     * @return
     */
    private boolean checkCredentials(final String userid, final String password)
    {
        if(userid == null || password == null || userid.trim().length() < 1 || password.trim().length() < 1)
        {
            return false;
        }
        this.loadPasswordFile();
        if(LoginServlet.passwords.containsKey(userid))
        {
            final String expectedPassword = LoginServlet.passwords.getProperty(userid);
            if(expectedPassword.equals(password))
            {
                return true;
            }
        }
        
        return false;
    }
    
    private void loadPasswordFile()
    {
        if((System.currentTimeMillis() - LoginServlet.passwordsLoadedAt) < 60000)
        {
            return;
        }
        
        final String passwordFile =
                (String)this.getServletContext().getAttribute(PoddServletContextListener.PODD_PASSWORD_FILE);
        
        LoginServlet.passwords = new Properties();
        try
        {
            LoginServlet.passwords.load(new FileInputStream(passwordFile));
        }
        catch(final Exception e)
        {
            this.log.error("Failed to load passwords", e);
        }
        LoginServlet.passwordsLoadedAt = System.currentTimeMillis();
    }
    
}