package com.dancetracker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

import db.DB;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Validate input
        if (email == null
                || password == null
                || email.trim().isEmpty()
                || password.isEmpty()) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/login.html?error=true"
            );

            return;
        }

        try (Connection con = DB.getConnection()) {

            String sql =
                    "SELECT user_id, email, password_hash "
                    + "FROM users "
                    + "WHERE email = ?";

            try (PreparedStatement ps =
                    con.prepareStatement(sql)) {

                ps.setString(
                        1,
                        email.trim()
                );

                try (ResultSet rs =
                        ps.executeQuery()) {

                    if (rs.next()) {

                        String storedHash =
                                rs.getString(
                                        "password_hash"
                                );

                        boolean passwordCorrect =
                                BCrypt.checkpw(
                                        password,
                                        storedHash
                                );

                        if (passwordCorrect) {

                            // Remove previous session
                            HttpSession oldSession =
                                    request.getSession(false);

                            if (oldSession != null) {
                                oldSession.invalidate();
                            }

                            // Create new authenticated session
                            HttpSession session =
                                    request.getSession(true);

                            session.setAttribute(
                                    "loggedInUser",
                                    rs.getString("email")
                            );

                            session.setAttribute(
                                    "userId",
                                    rs.getInt("user_id")
                            );

                            // 30 minutes inactivity timeout
                            session.setMaxInactiveInterval(
                                    30 * 60
                            );

                            // Successful login
                            response.sendRedirect(
                                    request.getContextPath()
                                    + "/login-success.html"
                            );

                            return;
                        }
                    }
                }
            }

            // Wrong email or password
            response.sendRedirect(
                    request.getContextPath()
                    + "/login.html?error=true"
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new ServletException(
                    "Login failed because of a server error.",
                    e
            );
        }
    }
}