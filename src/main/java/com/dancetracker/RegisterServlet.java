package com.dancetracker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;

import db.DB;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Validate fields
        if (email == null
                || email.trim().isEmpty()
                || password == null
                || password.isEmpty()) {

            response.getWriter().println(
                    "<h3>Please enter an email and password.</h3>"
            );

            return;
        }

        email = email.trim().toLowerCase();

        // Basic password requirement
        if (password.length() < 8) {

            response.getWriter().println(
                    "<h3>Password must be at least 8 characters long.</h3>"
            );

            return;
        }

        // Hash password before storing it
        String passwordHash =
                BCrypt.hashpw(
                        password,
                        BCrypt.gensalt(12)
                );

        String sql =
                "INSERT INTO users "
                + "(email, password_hash) "
                + "VALUES (?, ?)";

        try (Connection con = DB.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, passwordHash);

            ps.executeUpdate();

            // Registration successful
            // Send user directly to login page
            response.sendRedirect(
                    request.getContextPath()
                    + "/login.html?registered=true"
            );

        } catch (SQLIntegrityConstraintViolationException e) {

            // Usually means email already exists
            response.getWriter().println(
                    "<h3>An account with this email already exists.</h3>"
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new ServletException(
                    "Unable to create account.",
                    e
            );
        }
    }
}
