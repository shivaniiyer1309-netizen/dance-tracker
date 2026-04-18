package com.dancetracker;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/DanceServlet")
public class DanceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_HOST = System.getenv("MYSQLHOST");
    private static final String DB_PORT = System.getenv("MYSQLPORT");
    private static final String DB_NAME = System.getenv("MYSQLDATABASE");
    private static final String DB_USER = System.getenv("MYSQLUSER");
    private static final String DB_PASS = System.getenv("MYSQLPASSWORD");

    private static String getDbUrl() {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("MySQL Driver error: " + e.getMessage());
            return;
        }

        try (Connection con = DriverManager.getConnection(getDbUrl(), DB_USER, DB_PASS)) {        	/* ================= VIEW ACADEMY INCOME (LIST) ================= */
        	if ("view_academy_income".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql =
        	        "SELECT class_date, day_name, " +
        	        "MAX(CASE WHEN slot_no=1 THEN TIME_FORMAT(from_time, '%h:%i %p') END) AS from1, " +
        	        "MAX(CASE WHEN slot_no=1 THEN TIME_FORMAT(to_time,   '%h:%i %p') END) AS to1, " +
        	        "MAX(CASE WHEN slot_no=2 THEN TIME_FORMAT(from_time, '%h:%i %p') END) AS from2, " +
        	        "MAX(CASE WHEN slot_no=2 THEN TIME_FORMAT(to_time,   '%h:%i %p') END) AS to2, " +
        	        "SUM(hours) AS total_hours, " +
        	        "MAX(rate) AS rate, " +
        	        "SUM(line_total) AS total_amount " +
        	        "FROM academy_income " +
        	        "GROUP BY class_date, day_name " +
        	        "ORDER BY class_date DESC, day_name";

        	    try (PreparedStatement ps = con.prepareStatement(sql);
        	         ResultSet rs = ps.executeQuery()) {

        	        out.println("<!DOCTYPE html><html><head><title>Academy Income</title></head><body>");
        	        out.println("<h2>Academy Income - View List</h2>");
        	        out.println("<a href='display.html'><button type='button'>Back</button></a><br><br>");

        	        out.println("<table border='1' cellpadding='8'>");
        	        out.println("<tr><th>Date</th><th>Day</th><th>Slot 1</th><th>Slot 2</th><th>Total Hours</th><th>Rate</th><th>Total Amount</th></tr>");

        	        while (rs.next()) {
        	            String slot1 = "";
        	            if (rs.getString("from1") != null) {
        	                slot1 = rs.getString("from1") + " - " + rs.getString("to1");
        	            }

        	            String slot2 = "";
        	            if (rs.getString("from2") != null) {
        	                slot2 = rs.getString("from2") + " - " + rs.getString("to2");
        	            }

        	            out.println("<tr>");
        	            out.println("<td>" + rs.getDate("class_date") + "</td>");
        	            out.println("<td>" + rs.getString("day_name") + "</td>");
        	            out.println("<td>" + slot1 + "</td>");
        	            out.println("<td>" + slot2 + "</td>");
        	            out.println("<td>" + rs.getBigDecimal("total_hours") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("rate") + "</td>");
        	            out.println("<td><b>$" + rs.getBigDecimal("total_amount") + "</b></td>");
        	            out.println("</tr>");
        	        }

        	        out.println("</table>");
        	        out.println("</body></html>");
        	    }
        	    return;
        	}
        	if ("getAllCourses".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql = "SELECT course_id, course_name FROM Course ORDER BY course_id";

        	    try (PreparedStatement stmt = con.prepareStatement(sql);
        	         ResultSet rs = stmt.executeQuery()) {

        	        out.print("[");
        	        boolean first = true;

        	        while (rs.next()) {
        	            if (!first) out.print(",");
        	            first = false;

        	            out.print("{");
        	            out.print("\"course_id\":" + rs.getInt("course_id") + ",");
        	            out.print("\"course_name\":\"" + escapeJson(rs.getString("course_name")) + "\"");
        	            out.print("}");
        	        }
        	        out.print("]");
        	    }
        	    return;
        	}
        	/* ================= VIEW STUDENT PROGRESS ================= */
        	if ("view_student_progress".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql =
        	        "SELECT sp.progress_date, c.course_name, s.name AS student_name, sp.status, sp.comments " +
        	        "FROM student_progress sp " +
        	        "JOIN Course c ON sp.course_id = c.course_id " +
        	        "JOIN Student s ON sp.student_id = s.student_id " +
        	        "ORDER BY sp.progress_date DESC, c.course_name, s.name";

        	    try (PreparedStatement ps = con.prepareStatement(sql);
        	         ResultSet rs = ps.executeQuery()) {

        	        out.println("<!DOCTYPE html><html><head><title>Student Progress</title></head><body>");
        	        out.println("<h2>Student Progress List</h2>");
        	        out.println("<a href='display.html'><button type='button'>Back</button></a><br><br>");

        	        out.println("<table border='1' cellpadding='8'>");
        	        out.println("<tr><th>Date</th><th>Course</th><th>Student Name</th><th>Status</th><th>Comments</th></tr>");

        	        while (rs.next()) {
        	            out.println("<tr>");
        	            out.println("<td>" + rs.getDate("progress_date") + "</td>");
        	            out.println("<td>" + escapeHtml(rs.getString("course_name")) + "</td>");
        	            out.println("<td>" + escapeHtml(rs.getString("student_name")) + "</td>");
        	            out.println("<td>" + (rs.getString("status") == null ? "" : escapeHtml(rs.getString("status"))) + "</td>");
        	            out.println("<td>" + (rs.getString("comments") == null ? "" : escapeHtml(rs.getString("comments"))) + "</td>");
        	            out.println("</tr>");
        	        }

        	        out.println("</table>");
        	        out.println("</body></html>");
        	    }
        	    return;
        	}
        	/* ================= VIEW EXPENSES ================= */
        	if ("view_expenses".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql =
        	        "SELECT expense_id, expense_date, expense_name, hours, rounded_income, deduction_amount, final_income, created_at " +
        	        "FROM expenses ORDER BY expense_date DESC, expense_id DESC";

        	    try (PreparedStatement ps = con.prepareStatement(sql);
        	         ResultSet rs = ps.executeQuery()) {

        	        out.println("<!DOCTYPE html><html><head><title>Expenses</title></head><body>");
        	        out.println("<h2>Expenses - View List</h2>");
        	        out.println("<a href='display.html'><button type='button'>Back</button></a><br><br>");

        	        out.println("<table border='1' cellpadding='8'>");
        	        out.println("<tr>" +
        	                "<th>ID</th>" +
        	                "<th>Date</th>" +
        	                "<th>Name</th>" +
        	                "<th>Hours</th>" +
        	                "<th>Rounded Income</th>" +
        	                "<th>Deduction</th>" +
        	                "<th>Final Income</th>" +
        	                "<th>Created</th>" +
        	                "</tr>");

        	        while (rs.next()) {
        	            out.println("<tr>");
        	            out.println("<td>" + rs.getInt("expense_id") + "</td>");
        	            out.println("<td>" + rs.getDate("expense_date") + "</td>");
        	            out.println("<td>" + escapeHtml(rs.getString("expense_name")) + "</td>");

        	            java.math.BigDecimal hrs = rs.getBigDecimal("hours");
        	            out.println("<td>" + (hrs == null ? "" : hrs) + "</td>");

        	            out.println("<td>$" + rs.getBigDecimal("rounded_income") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("deduction_amount") + "</td>");
        	            out.println("<td><b>$" + rs.getBigDecimal("final_income") + "</b></td>");
        	            out.println("<td>" + rs.getTimestamp("created_at") + "</td>");
        	            out.println("</tr>");
        	        }

        	        out.println("</table>");
        	        out.println("</body></html>");
        	    }
        	    return;
        	}
        	
        	/* ================= VIEW PROVISION LIST ================= */
        	if ("view_provision".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql =
        	        "SELECT start_date, end_date, grand_total " +
        	        "FROM month_end_summary " +
        	        "ORDER BY summary_id DESC";

        	    try (PreparedStatement ps = con.prepareStatement(sql);
        	         ResultSet rs = ps.executeQuery()) {

        	        out.println("<!DOCTYPE html><html><head><title>Provision List</title></head><body>");
        	        out.println("<h2>Provision - View List</h2>");
        	        out.println("<a href='display.html'><button type='button'>Back</button></a><br><br>");

        	        out.println("<table border='1' cellpadding='8'>");
        	        out.println("<tr>" +
        	                "<th>Start Date</th>" +
        	                "<th>End Date</th>" +
        	                "<th>Grand Total</th>" +
        	                "<th>Rounded Income</th>" +
        	                "<th>Provision</th>" +
        	                "</tr>");

        	        java.math.BigDecimal thousand = new java.math.BigDecimal("1000");

        	        while (rs.next()) {
        	            java.sql.Date startDate = rs.getDate("start_date");
        	            java.sql.Date endDate = rs.getDate("end_date");
        	            java.math.BigDecimal grandTotal = rs.getBigDecimal("grand_total");

        	            if (grandTotal == null) {
        	                grandTotal = java.math.BigDecimal.ZERO;
        	            }

        	            java.math.BigDecimal roundedAmount =
        	                grandTotal.divide(thousand, 0, java.math.RoundingMode.DOWN)
        	                          .multiply(thousand)
        	                          .setScale(2, java.math.RoundingMode.HALF_UP);

        	            java.math.BigDecimal provisionAmount =
        	                grandTotal.subtract(roundedAmount)
        	                          .setScale(2, java.math.RoundingMode.HALF_UP);

        	            out.println("<tr>");
        	            out.println("<td>" + startDate + "</td>");
        	            out.println("<td>" + endDate + "</td>");
        	            out.println("<td>$" + grandTotal + "</td>");
        	            out.println("<td>$" + roundedAmount + "</td>");
        	            out.println("<td><b>$" + provisionAmount + "</b></td>");
        	            out.println("</tr>");
        	        }

        	        out.println("</table>");
        	        out.println("</body></html>");
        	    }
        	    return;
        	}
        	/* ================= VIEW ONLINE INCOME (LIST) ================= */
        	if ("view_online_income".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql =
        	        "SELECT class_date, day_name, " +
        	        "MAX(CASE WHEN slot_no=1 THEN TIME_FORMAT(from_time, '%h:%i %p') END) AS from1, " +
        	        "MAX(CASE WHEN slot_no=1 THEN TIME_FORMAT(to_time,   '%h:%i %p') END) AS to1, " +
        	        "MAX(CASE WHEN slot_no=2 THEN TIME_FORMAT(from_time, '%h:%i %p') END) AS from2, " +
        	        "MAX(CASE WHEN slot_no=2 THEN TIME_FORMAT(to_time,   '%h:%i %p') END) AS to2, " +
        	        "SUM(hours) AS total_hours, " +
        	        "MAX(rate) AS rate, " +
        	        "SUM(line_total) AS total_amount " +
        	        "FROM online_income " +
        	        "GROUP BY class_date, day_name " +
        	        "ORDER BY class_date DESC, day_name";

        	    try (PreparedStatement ps = con.prepareStatement(sql);
        	         ResultSet rs = ps.executeQuery()) {

        	        out.println("<!DOCTYPE html><html><head><title>Online Income</title></head><body>");
        	        out.println("<h2>Online Income - View List</h2>");
        	        out.println("<a href='display.html'><button type='button'>Back</button></a><br><br>");

        	        out.println("<table border='1' cellpadding='8'>");
        	        out.println("<tr><th>Date</th><th>Day</th><th>Slot 1</th><th>Slot 2</th><th>Total Hours</th><th>Rate</th><th>Total Amount</th></tr>");

        	        while (rs.next()) {
        	            String slot1 = "";
        	            if (rs.getString("from1") != null) slot1 = rs.getString("from1") + " - " + rs.getString("to1");

        	            String slot2 = "";
        	            if (rs.getString("from2") != null) slot2 = rs.getString("from2") + " - " + rs.getString("to2");

        	            out.println("<tr>");
        	            out.println("<td>" + rs.getDate("class_date") + "</td>");
        	            out.println("<td>" + rs.getString("day_name") + "</td>");
        	            out.println("<td>" + slot1 + "</td>");
        	            out.println("<td>" + slot2 + "</td>");
        	            out.println("<td>" + rs.getBigDecimal("total_hours") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("rate") + "</td>");
        	            out.println("<td><b>$" + rs.getBigDecimal("total_amount") + "</b></td>");
        	            out.println("</tr>");
        	        }

        	        out.println("</table>");
        	        out.println("</body></html>");
        	    }
        	    return;
        	}
        	/* ================= GET Students by course ================= */
        	if ("getStudentsByCourse".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    int courseId = Integer.parseInt(request.getParameter("course_id"));

        	    String sql = "SELECT student_id, name FROM Student WHERE course_id=? ORDER BY name";

        	    try (PreparedStatement stmt = con.prepareStatement(sql)) {
        	        stmt.setInt(1, courseId);

        	        try (ResultSet rs = stmt.executeQuery()) {
        	            out.print("[");
        	            boolean first = true;

        	            while (rs.next()) {
        	                if (!first) out.print(",");
        	                first = false;

        	                out.print("{");
        	                out.print("\"student_id\":" + rs.getInt("student_id") + ",");
        	                out.print("\"name\":\"" + escapeJson(rs.getString("name")) + "\"");
        	                out.print("}");
        	            }
        	            out.print("]");
        	        }
        	    }
        	    return;
        	}
        	/* ================= AJAX: GET TOPICS (JSON) ================= */
        	if ("getTopics".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    int courseId = Integer.parseInt(request.getParameter("course_id"));

        	    // Map course_id to your topic table
        	    String table;
        	    switch (courseId) {
        	        case 1: table = "parichay"; break;
        	        case 2: table = "prarambhik"; break;
        	        case 3: table = "elementary"; break;
        	        case 4: table = "intermediate"; break;
        	        default: table = "parichay"; break;
        	    }

        	    String sql =
        	        "SELECT topic_id, topic_name, topic_order " +
        	        "FROM " + table + " ORDER BY topic_order";

        	    try (PreparedStatement stmt = con.prepareStatement(sql);
        	         ResultSet rs = stmt.executeQuery()) {

        	        out.print("[");
        	        boolean first = true;

        	        while (rs.next()) {
        	            if (!first) out.print(",");
        	            first = false;

        	            out.print("{");
        	            out.print("\"topic_id\":" + rs.getInt("topic_id") + ",");
        	            out.print("\"topic_name\":\"" + escapeJson(rs.getString("topic_name")) + "\",");
        	            out.print("\"topic_order\":" + rs.getInt("topic_order"));
        	            out.print("}");
        	        }
        	        out.print("]");
        	    }
        	    return;
        	}
        	/* ================= TEMPLE INCOME ================= */

        	if ("get_temple_income".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String dateStr = request.getParameter("class_date");
        	    if (dateStr == null || dateStr.isEmpty()) {
        	        out.print("{\"found\":false}");
        	        return;
        	    }

        	    PreparedStatement ps = con.prepareStatement(
        	        "SELECT class_date, present_count, rate, total_amount " +
        	        "FROM temple_income WHERE class_date=?"
        	    );
        	    ps.setDate(1, java.sql.Date.valueOf(dateStr));

        	    ResultSet rs = ps.executeQuery();

        	    if (rs.next()) {
        	        out.print("{");
        	        out.print("\"found\":true,");
        	        out.print("\"class_date\":\"" + rs.getDate("class_date") + "\",");
        	        out.print("\"present_count\":" + rs.getInt("present_count") + ",");
        	        out.print("\"rate\":" + rs.getBigDecimal("rate") + ",");
        	        out.print("\"total_amount\":" + rs.getBigDecimal("total_amount"));
        	        out.print("}");
        	    } else {
        	        out.print("{\"found\":false}");
        	    }

        	    rs.close();
        	    ps.close();
        	    return;
        	}
        	/* ================= VIEW MONTH END LIST ================= */
        	if ("view_month_end".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String sql =
        	        "SELECT summary_id, start_date, end_date, temple_total, home_total, academy_total, online_total, grand_total, created_at " +
        	        "FROM month_end_summary ORDER BY summary_id DESC";

        	    try (PreparedStatement ps = con.prepareStatement(sql);
        	         ResultSet rs = ps.executeQuery()) {

        	        out.println("<!DOCTYPE html><html><head><title>Month End Summary</title></head><body>");
        	        out.println("<h2>Month End Summary - View List</h2>");
        	        out.println("<a href='display.html'><button type='button'>Back</button></a><br><br>");

        	        out.println("<table border='1' cellpadding='8'>");
        	        out.println("<tr>" +
        	            "<th>ID</th><th>Start</th><th>End</th>" +
        	            "<th>Temple</th><th>Home</th><th>Academy</th><th>Online</th>" +
        	            "<th>Grand Total</th><th>Created</th>" +
        	            "</tr>");

        	        while (rs.next()) {
        	            out.println("<tr>");
        	            out.println("<td>" + rs.getInt("summary_id") + "</td>");
        	            out.println("<td>" + rs.getDate("start_date") + "</td>");
        	            out.println("<td>" + rs.getDate("end_date") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("temple_total") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("home_total") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("academy_total") + "</td>");
        	            out.println("<td>$" + rs.getBigDecimal("online_total") + "</td>");
        	            out.println("<td><b>$" + rs.getBigDecimal("grand_total") + "</b></td>");
        	            out.println("<td>" + rs.getTimestamp("created_at") + "</td>");
        	            out.println("</tr>");
        	        }

        	        out.println("</table>");
        	        out.println("</body></html>");
        	    }
        	    return;
        	}
        	/* ================= HOME INCOME ================= */
        	if ("get_home_income".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String dateStr = request.getParameter("class_date");
        	    if (dateStr == null || dateStr.isEmpty()) {
        	        out.print("{\"found\":false}");
        	        return;
        	    }

        	    PreparedStatement ps = con.prepareStatement(
        	        "SELECT class_date, present_count, rate, total_amount " +
        	        "FROM home_income WHERE class_date=?"
        	    );
        	    ps.setDate(1, java.sql.Date.valueOf(dateStr));

        	    ResultSet rs = ps.executeQuery();

        	    if (rs.next()) {
        	        out.print("{");
        	        out.print("\"found\":true,");
        	        out.print("\"class_date\":\"" + rs.getDate("class_date") + "\",");
        	        out.print("\"present_count\":" + rs.getInt("present_count") + ",");
        	        out.print("\"rate\":" + rs.getBigDecimal("rate") + ",");
        	        out.print("\"total_amount\":" + rs.getBigDecimal("total_amount"));
        	        out.print("}");
        	    } else {
        	        out.print("{\"found\":false}");
        	    }

        	    rs.close();
        	    ps.close();
        	    return;
        	}
        	/* ================= VIEW RATES ================= */
        	if ("view_rates".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    PreparedStatement stmt = con.prepareStatement(
        	        "SELECT br.branch_id, b.name AS branch_name, br.rate " +
        	        "FROM branch_rate br " +
        	        "JOIN Branch b ON b.branch_id = br.branch_id " +
        	        "ORDER BY br.branch_id"
        	    );

        	    ResultSet rs = stmt.executeQuery();

        	    out.println("<!DOCTYPE html><html><head><title>Rates</title></head><body>");
        	    out.println("<h2>Branch Rates</h2>");

        	    out.println("<form action='DanceTracker' method='post'>");
        	    out.println("<input type='hidden' name='action' value='update_rates'>");

        	    out.println("<table border='1' cellpadding='8'>");
        	    out.println("<tr><th>Branch</th><th>Rate</th></tr>");

        	    while (rs.next()) {
        	        int bid = rs.getInt("branch_id");
        	        String bname = rs.getString("branch_name");
        	        double rate = rs.getDouble("rate");

        	        out.println("<tr>");
        	        out.println("<td>" + escapeHtml(bname) + "</td>");
        	        out.println("<td>");
        	        out.println("<input type='number' step='0.01' name='rate_" + bid + "' value='" + rate + "' required>");
        	        out.println("</td>");
        	        out.println("</tr>");
        	    }

        	    out.println("</table><br>");
        	    out.println("<input type='submit' value='Save Rates'>");
        	    out.println("</form>");

        	    out.println("<br><a href='display.html'>Back</a>");
        	    out.println("</body></html>");

        	    rs.close();
        	    stmt.close();
        	    return;
        	}
        	

        	/* ================= AJAX: GET SUBTOPICS (JSON) ================= */
        	if ("getSubtopics".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    String topicIdStr = request.getParameter("topic_id");
        	    String courseIdStr = request.getParameter("course_id");

        	    if (topicIdStr == null || courseIdStr == null) {
        	        // return empty JSON instead of crashing
        	        out.print("[]");
        	        return;
        	    }

        	    int topicId = Integer.parseInt(topicIdStr);
        	    int courseId = Integer.parseInt(courseIdStr);

        	    String table;
        	    switch (courseId) {
        	        case 1: table = "parichay_subtopic"; break;
        	        case 2: table = "prarambhik_subtopic"; break;
        	        case 3: table = "elementary_subtopic"; break;
        	        case 4: table = "intermediate_subtopic"; break;
        	        default: table = "parichay_subtopic"; break;
        	    }

        	    String sql =
        	        "SELECT subtopic_id, subtopic_name " +
        	        		"FROM " + table + " WHERE topic_id=? ORDER BY subtopic_order";
        	    try (PreparedStatement stmt = con.prepareStatement(sql)) {
        	        stmt.setInt(1, topicId);

        	        try (ResultSet rs = stmt.executeQuery()) {
        	            out.print("[");
        	            boolean first = true;

        	            while (rs.next()) {
        	                if (!first) out.print(",");
        	                first = false;

        	                out.print("{");
        	                out.print("\"subtopic_id\":" + rs.getInt("subtopic_id") + ",");
        	                out.print("\"subtopic_name\":\"" + escapeJson(rs.getString("subtopic_name")) + "\"");
        	                out.print("}");
        	            }
        	            out.print("]");
        	        }
        	    }
        	    return;
        	}
        	/* ================= VIEW MAKEUP COUNTER LIST ================= */
        	if ("view_makeup_counter".equals(action)) {
        	    response.setContentType("text/html;charset=UTF-8");
        	    PrintWriter out = response.getWriter();
        	    
               

        	    PreparedStatement stmt = con.prepareStatement(
        	    	    "SELECT s.name AS student_name, " +
        	    	    "       c.course_name, " +
        	    	    "       b.name AS branch_name, " +
        	    	    "       mc.absent_count, " +
        	    	    "       mc.last_absent_date " +
        	    	    "FROM makeup_counter mc " +
        	    	    "JOIN Student s ON s.student_id = mc.student_id " +
        	    	    "JOIN Course  c ON c.course_id  = mc.course_id " +
        	    	    "JOIN Branch  b ON b.branch_id  = mc.branch_id " +
        	    	    "ORDER BY mc.absent_count DESC, s.name"
        	    	);

        	    ResultSet rs = stmt.executeQuery();

        	    out.println("<!DOCTYPE html>");
        	    out.println("<html><head><title>Makeup Classes</title></head><body>");
        	    out.println("<h2>Makeup Classes (Absence Counter)</h2>");

        	    out.println("<table border='1'>");
        	    out.println("<tr>" +
        	    	    "<th>Name</th>" +
        	    	    "<th>Course</th>" +
        	    	    "<th>Branch</th>" +
        	    	    "<th>Absent Count</th>" +
        	    	    "<th>Last Absent Date</th>" +
        	    	    "</tr>");


        	    while (rs.next()) {
        	        out.println("<tr>");
        	        out.println("<td>" + rs.getString("student_name") + "</td>");
        	        out.println("<td>" + rs.getString("course_name") + "</td>");
        	        out.println("<td>" + rs.getString("branch_name") + "</td>");
        	        out.println("<td>" + rs.getInt("absent_count") + "</td>");

        	        java.sql.Date d = rs.getDate("last_absent_date");
        	        out.println("<td>" + (d == null ? "" : d.toString()) + "</td>");

        	        out.println("</tr>");
        	    }


        	    out.println("</table>");

        	    out.println("<br><a href='display.html'>Back</a>");
        	    out.println("</body></html>");

        	    rs.close();
        	    stmt.close();
        	    return;
        	}

        	/* ================= AJAX: GET STUDENTS FOR ATTENDANCE (JSON) ================= */
        	if ("getStudentsForAttendance".equals(action)) {
        	    response.setContentType("application/json;charset=UTF-8");
        	    PrintWriter out = response.getWriter();

        	    int branchId = Integer.parseInt(request.getParameter("branch_id"));
        	    int courseId = Integer.parseInt(request.getParameter("course_id"));
        	    String day = request.getParameter("day"); // Mon, Tue, etc

        	    // Detect if Student table has course_id (so we can filter)
        	    boolean hasCourseId = false;
        	    try (PreparedStatement chk = con.prepareStatement(
        	            "SELECT COUNT(*) AS c " +
        	            "FROM INFORMATION_SCHEMA.COLUMNS " +
        	            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='Student' AND COLUMN_NAME='course_id'"
        	    )) {
        	        try (ResultSet rschk = chk.executeQuery()) {
        	            if (rschk.next()) hasCourseId = (rschk.getInt("c") > 0);
        	        }
        	    }

        	    PreparedStatement stmt;

        	    if (hasCourseId) {
        	        if (courseId == 6) {
        	            // Elementary - Choreography -> load Elementary + Elementary - Choreography
        	            stmt = con.prepareStatement(
        	                "SELECT student_id, name, days " +
        	                "FROM Student " +
        	                "WHERE branch_id=? AND course_id IN (3,6) AND days LIKE ? " +
        	                "ORDER BY name"
        	            );
        	            stmt.setInt(1, branchId);
        	            stmt.setString(2, "%" + day + "%");

        	        } else if (courseId == 9) {
        	            // Intermediate - Choreography -> load Intermediate + Intermediate - Choreography
        	            stmt = con.prepareStatement(
        	                "SELECT student_id, name, days " +
        	                "FROM Student " +
        	                "WHERE branch_id=? AND course_id IN (4,9) AND days LIKE ? " +
        	                "ORDER BY name"
        	            );
        	            stmt.setInt(1, branchId);
        	            stmt.setString(2, "%" + day + "%");

        	        } else {
        	            stmt = con.prepareStatement(
        	                "SELECT student_id, name, days " +
        	                "FROM Student " +
        	                "WHERE branch_id=? AND course_id=? AND days LIKE ? " +
        	                "ORDER BY name"
        	            );
        	            stmt.setInt(1, branchId);
        	            stmt.setInt(2, courseId);
        	            stmt.setString(3, "%" + day + "%");
        	        }
        	    } else {
        	        stmt = con.prepareStatement(
        	            "SELECT student_id, name, days " +
        	            "FROM Student " +
        	            "WHERE branch_id=? AND days LIKE ? " +
        	            "ORDER BY name"
        	        );
        	        stmt.setInt(1, branchId);
        	        stmt.setString(2, "%" + day + "%");
        	    }

        	    try (ResultSet rs = stmt.executeQuery()) {
        	        out.print("[");
        	        boolean first = true;
        	        while (rs.next()) {
        	            if (!first) out.print(",");
        	            first = false;

        	            out.print("{");
        	            out.print("\"student_id\":" + rs.getInt("student_id") + ",");
        	            out.print("\"name\":\"" + escapeJson(rs.getString("name")) + "\",");
        	            out.print("\"days\":\"" + escapeJson(rs.getString("days")) + "\"");
        	            out.print("}");
        	        }
        	        out.print("]");
        	    }

        	    stmt.close();
        	    return;
        	}
        	
            /* ================= VIEW NOTES (HTML) ================= */
            if ("view_notes".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                String msg = request.getParameter("msg");

                PreparedStatement stmt = con.prepareStatement(
                    "SELECT note_id, note_text, DATE(created_at) AS created_date " +
                    "FROM notes ORDER BY note_id DESC"
                );
                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Notes</title></head><body>");
                out.println("<h2>Notes</h2>");

                // Add note box
                out.println("<h3>Add Note</h3>");
                if ("empty".equals(msg)) {
                    out.println("<p style='color:red;'>Note cannot be empty.</p>");
                }
                out.println("<form action='DanceTracker' method='post'>");
                out.println("<input type='hidden' name='action' value='add_note'>");
                out.println("<textarea name='note_text' rows='4' cols='60' placeholder='Type a note...' required></textarea><br>");
                out.println("<input type='submit' value='Add Note'>");
                out.println("</form>");

                out.println("<hr>");

                // Table
                out.println("<table border='1' cellpadding='8'>");
                out.println("<tr><th>ID</th><th>Note</th><th>Created</th><th>Action</th></tr>");

                while (rs.next()) {
                    int id = rs.getInt("note_id");
                    String text = rs.getString("note_text");
                    String createdDate = rs.getString("created_date"); // date only

                    out.println("<tr>");
                    out.println("<td>" + id + "</td>");
                    out.println("<td>" + escapeHtml(text) + "</td>");
                    out.println("<td>" + createdDate + "</td>");
                    out.println("<td>");

                    // Update button -> opens edit form page
                    out.println("<a href='DanceTracker?action=edit_note&id=" + id + "'>");
                    out.println("<button type='button'>Update</button></a> ");

                    // Delete button
                    out.println("<a href='DanceTracker?action=delete_note&id=" + id + "' " +
                            "onclick=\"return confirm('Delete this note?');\">");
                    out.println("<button type='button'>Delete</button></a>");

                    out.println("</td>");
                    out.println("</tr>");
                }

                out.println("</table>");
                out.println("<br><a href='display.html'>Back</a>");
                out.println("</body></html>");

                rs.close();
                stmt.close();
                return;
            }
            

            /* ================= VIEW ATTENDANCE LIST (NEW) ================= */
            if ("view_attendance".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                PreparedStatement stmt = con.prepareStatement(
                    "SELECT a.attendance_id, s.name AS student_name, c.course_name, b.name AS branch_name, " +
                    "t.name AS teacher_name, a.class_date, a.day, a.status, a.comment " +
                    "FROM Attendance a " +
                    "JOIN Student s ON a.student_id = s.student_id " +
                    "JOIN Course c ON a.course_id = c.course_id " +
                    "JOIN Branch b ON a.branch_id = b.branch_id " +
                    "JOIN Teacher t ON a.teacher_id = t.teacher_id " +
                    "ORDER BY a.class_date DESC, c.course_name, b.name, s.name"
                );

                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Attendance List</title></head><body>");
                out.println("<h2>Attendance List</h2>");

                out.println("<a href='DanceTracker?action=add_attendance_form'><button type='button'>Add Attendance</button></a>");
                out.println("<br><br>");

                out.println("<table border='1'>");
                out.println("<tr>" +
                    "<th>Name</th>" +
                    "<th>Course</th>" +
                    "<th>Branch</th>" +
                    "<th>Teacher</th>" +
                    "<th>Class Date</th>" +
                    "<th>Day</th>" +
                    "<th>Status</th>" +
                    "<th>Comment</th>" +
                    "<th>Actions</th>" +
                    "</tr>"
                );

                while (rs.next()) {
                    int aid = rs.getInt("attendance_id");

                    out.println("<tr>");
                    out.println("<td>" + rs.getString("student_name") + "</td>");
                    out.println("<td>" + rs.getString("course_name") + "</td>");
                    out.println("<td>" + rs.getString("branch_name") + "</td>");
                    out.println("<td>" + rs.getString("teacher_name") + "</td>");
                    out.println("<td>" + rs.getDate("class_date") + "</td>");
                    out.println("<td>" + rs.getString("day") + "</td>");
                    out.println("<td>" + rs.getString("status") + "</td>");
                    out.println("<td>" + (rs.getString("comment") == null ? "" : rs.getString("comment")) + "</td>");

                    out.println("<td>");
                    out.println("<a href='DanceTracker?action=edit_attendance&id=" + aid + "'><button type='button'>Update</button></a> ");
                    out.println("<a href='DanceTracker?action=delete_attendance&id=" + aid + "' onclick=\"return confirm('Delete this attendance record?');\"><button type='button'>Delete</button></a>");
                    out.println("</td>");

                    out.println("</tr>");
                }

                out.println("</table>");
                out.println("<br><a href='display.html'>Back</a>");
                out.println("</body></html>");

                rs.close();
                stmt.close();
                return;
            }

            /* ================= EDIT ATTENDANCE (SHOW FORM) (NEW) ================= */
            if ("edit_attendance".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement stmt = con.prepareStatement(
                    "SELECT attendance_id, class_date, day, status, comment FROM Attendance WHERE attendance_id=?"
                );
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Edit Attendance</title></head><body>");
                out.println("<h2>Edit Attendance</h2>");

                if (rs.next()) {
                    out.println("<form action='DanceTracker' method='post'>");
                    out.println("<input type='hidden' name='action' value='update_attendance'>");
                    out.println("<input type='hidden' name='attendance_id' value='" + rs.getInt("attendance_id") + "'>");

                    out.println("Class Date: <input type='date' name='class_date' value='" + rs.getDate("class_date") + "' required><br><br>");
                    out.println("Day: <input type='text' name='day' value='" + rs.getString("day") + "' required><br><br>");

                    String st = rs.getString("status");
                    out.println("Status: <select name='status'>");
                    out.println("<option value='P'" + ("P".equals(st) ? " selected" : "") + ">P</option>");
                    out.println("<option value='A'" + ("A".equals(st) ? " selected" : "") + ">A</option>");
                    out.println("</select><br><br>");

                    String cmt = rs.getString("comment");
                    out.println("Comment: <input type='text' name='comment' value='" + (cmt == null ? "" : cmt) + "'><br><br>");

                    out.println("<input type='submit' value='Update Attendance'>");
                    out.println("</form>");
                } else {
                    out.println("<h3>No attendance record found.</h3>");
                }

                out.println("<br><a href='DanceTracker?action=view_attendance'>Back to Attendance List</a>");
                out.println("</body></html>");

                rs.close();
                stmt.close();
                return;
            }

            /* ================= DELETE ATTENDANCE (NEW) ================= */
            if ("delete_attendance".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement del = con.prepareStatement("DELETE FROM Attendance WHERE attendance_id=?");
                del.setInt(1, id);
                del.executeUpdate();
                del.close();

                response.sendRedirect("DanceTracker?action=view_attendance");
                return;
            }
            /* ================= DELETE NOTE ================= */
            if ("delete_note".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement del = con.prepareStatement("DELETE FROM notes WHERE note_id=?");
                del.setInt(1, id);
                del.executeUpdate();
                del.close();

                response.sendRedirect("DanceTracker?action=view_notes");
                return;
            }

            /* ================= ADD ATTENDANCE (SHOW FORM) (NEW) ================= */
            if ("add_attendance_form".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Add Attendance</title></head><body>");
                out.println("<h2>Add Attendance</h2>");

                out.println("<form action='DanceTracker' method='post'>");
                out.println("<input type='hidden' name='action' value='add_attendance'>");

                // ✅ CHANGED ONLY THIS PART: removed IDs, using names instead
             // Student stays as text (no change)
                out.println("Student Name: <input type='text' name='student_name' required><br><br>");

                // ✅ Course dropdown from course table
                out.println("Course: <select name='course_name' required>");
                out.println("<option value=''>-- Select Course --</option>");
                try (PreparedStatement stCourse = con.prepareStatement(
                        "SELECT course_name FROM course ORDER BY course_name");
                     ResultSet rsCourse = stCourse.executeQuery()) {

                    while (rsCourse.next()) {
                        String cname = rsCourse.getString("course_name");
                        out.println("<option value='" + cname + "'>" + cname + "</option>");
                    }
                }
                out.println("</select><br><br>");

                // ✅ Branch dropdown from branch table
                out.println("Branch: <select name='branch_name' required>");
                out.println("<option value=''>-- Select Branch --</option>");
                try (PreparedStatement stBranch = con.prepareStatement(
                        "SELECT name FROM branch ORDER BY name");
                     ResultSet rsBranch = stBranch.executeQuery()) {

                    while (rsBranch.next()) {
                        String bname = rsBranch.getString("name");
                        out.println("<option value='" + bname + "'>" + bname + "</option>");
                    }
                }
                out.println("</select><br><br>");


                out.println("Class Date: <input type='date' name='class_date' required><br><br>");
                out.println("Day: <input type='text' name='day' required><br><br>");

                out.println("Status: <select name='status'>");
                out.println("<option value='P'>P</option>");
                out.println("<option value='A'>A</option>");
                out.println("</select><br><br>");

                out.println("Comment: <input type='text' name='comment'><br><br>");

                out.println("<input type='submit' value='Add Attendance'>");
                out.println("</form>");

                out.println("<br><a href='DanceTracker?action=view_attendance'>Back to Attendance List</a>");
                out.println("</body></html>");
                return;
            }

            /* ================= EDIT STUDENT (SHOW FORM) ================= */
            if ("edit_student".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement stmt = con.prepareStatement("SELECT * FROM Student WHERE student_id=?");
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Edit Student</title></head><body>");
                out.println("<h2>Edit Student</h2>");

                if (rs.next()) {
                    out.println("<form action='DanceTracker' method='post'>");
                    out.println("<input type='hidden' name='action' value='update_student'>");
                    out.println("<input type='hidden' name='student_id' value='" + rs.getInt("student_id") + "'>");

                    out.println("Name: <input type='text' name='name' value='" + rs.getString("name") + "' required><br><br>");
                    out.println("DOB: <input type='date' name='dob' value='" + rs.getDate("dob") + "' required><br><br>");
                    out.println("Joined: <input type='date' name='date_joined' value='" + rs.getDate("date_joined") + "' required><br><br>");
                    out.println("Days: <input type='text' name='days' value='" + rs.getString("days") + "' required><br><br>");

                    out.println("Mode: <select name='class_mode'>");
                    out.println("<option value='physical'" + ("physical".equals(rs.getString("class_mode")) ? " selected" : "") + ">In-person</option>");
                    out.println("<option value='online'" + ("online".equals(rs.getString("class_mode")) ? " selected" : "") + ">Online</option>");
                    out.println("</select><br><br>");

                    out.println("<input type='submit' value='Update Student'>");
                    out.println("</form>");
                } else {
                    out.println("<h3>No student found.</h3>");
                }

                out.println("<br><a href='DanceTracker?action=view_students'>Back to Student List</a>");
                out.println("</body></html>");

                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                return;
            }

            /* ================= DELETE STUDENT ================= */
            if ("delete_student".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement delAtt = con.prepareStatement("DELETE FROM Attendance WHERE student_id=?");
                delAtt.setInt(1, id);
                delAtt.executeUpdate();
                delAtt.close();

                PreparedStatement del = con.prepareStatement("DELETE FROM Student WHERE student_id=?");
                del.setInt(1, id);
                del.executeUpdate();
                del.close();

                response.sendRedirect("DanceTracker?action=view_students");
                return;
            }

            /* ================= EDIT BRANCH (SHOW FORM) ================= */
            if ("edit_branch".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement stmt = con.prepareStatement("SELECT * FROM Branch WHERE branch_id=?");
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Edit Branch</title></head><body>");
                out.println("<h2>Edit Branch</h2>");

                if (rs.next()) {
                    out.println("<form action='DanceTracker' method='post'>");
                    out.println("<input type='hidden' name='action' value='update_branch'>");
                    out.println("<input type='hidden' name='branch_id' value='" + rs.getInt("branch_id") + "'>");

                    out.println("Name: <input type='text' name='name' value='" + rs.getString("name") + "' required><br><br>");
                    out.println("Location: <input type='text' name='location' value='" + rs.getString("location") + "' required><br><br>");
                    out.println("Address: <input type='text' name='address' value='" + rs.getString("address") + "' required><br><br>");
                    out.println("Phone: <input type='text' name='phone' value='" + rs.getString("phone") + "' required><br><br>");

                    out.println("<input type='submit' value='Update Branch'>");
                    out.println("</form>");
                } else {
                    out.println("<h3>No branch found.</h3>");
                }

                out.println("<br><a href='DanceTracker?action=view_branches'>Back to Branch List</a>");
                out.println("</body></html>");

                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                return;
            }

            /* ================= DELETE BRANCH ================= */
            if ("delete_branch".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement del = con.prepareStatement("DELETE FROM Branch WHERE branch_id=?");
                del.setInt(1, id);
                del.executeUpdate();
                del.close();

                response.sendRedirect("DanceTracker?action=view_branches");
                return;
            }

            /* ================= EDIT TEACHER (SHOW FORM) ================= */
            if ("edit_teacher".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement stmt = con.prepareStatement("SELECT * FROM Teacher WHERE teacher_id=?");
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Edit Teacher</title></head><body>");
                out.println("<h2>Edit Teacher</h2>");

                if (rs.next()) {
                    out.println("<form action='DanceTracker' method='post'>");
                    out.println("<input type='hidden' name='action' value='update_teacher'>");
                    out.println("<input type='hidden' name='teacher_id' value='" + rs.getInt("teacher_id") + "'>");

                    out.println("Name: <input type='text' name='name' value='" + rs.getString("name") + "' required><br><br>");
                    out.println("Address: <input type='text' name='address' value='" + rs.getString("address") + "' required><br><br>");
                    out.println("Type: <input type='text' name='type' value='" + rs.getString("type") + "' required><br><br>");

                    out.println("<input type='submit' value='Update Teacher'>");
                    out.println("</form>");
                } else {
                    out.println("<h3>No teacher found.</h3>");
                }

                out.println("<br><a href='DanceTracker?action=view_teachers'>Back to Teacher List</a>");
                out.println("</body></html>");

                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                return;
            }

            /* ================= DELETE TEACHER ================= */
            if ("delete_teacher".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement del = con.prepareStatement("DELETE FROM Teacher WHERE teacher_id=?");
                del.setInt(1, id);
                del.executeUpdate();
                del.close();

                response.sendRedirect("DanceTracker?action=view_teachers");
                return;
            }

            /* ================= EDIT COURSE (SHOW FORM) ================= */
            if ("edit_course".equals(action)) {
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement stmt = con.prepareStatement("SELECT * FROM Course WHERE course_id=?");
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Edit Course</title></head><body>");
                out.println("<h2>Edit Course</h2>");

                if (rs.next()) {
                    out.println("<form action='DanceTracker' method='post'>");
                    out.println("<input type='hidden' name='action' value='update_course'>");
                    out.println("<input type='hidden' name='course_id' value='" + rs.getInt("course_id") + "'>");

                    out.println("Course Name: <input type='text' name='course_name' value='" + rs.getString("course_name") + "' required><br><br>");

                    out.println("<input type='submit' value='Update Course'>");
                    out.println("</form>");
                } else {
                    out.println("<h3>No course found.</h3>");
                }

                out.println("<br><a href='DanceTracker?action=view_courses'>Back to Course List</a>");
                out.println("</body></html>");

                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                return;
            }

            /* ================= DELETE COURSE ================= */
            if ("delete_course".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement del = con.prepareStatement("DELETE FROM Course WHERE course_id=?");
                del.setInt(1, id);
                del.executeUpdate();
                del.close();

                response.sendRedirect("DanceTracker?action=view_courses");
                return;
            }

            /* ================= YOUR EXISTING "VIEW LIST" HTML OUTPUT ================= */
            if ("view_students".equals(action)) action = "student";
            if ("view_branches".equals(action)) action = "branch";
            if ("view_teachers".equals(action)) action = "teacher";
            if ("view_courses".equals(action)) action = "course";

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();

            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>View List</title></head><body>");

            PreparedStatement stmt = null;
            ResultSet rs = null;

            if ("student".equals(action)) {
            	stmt = con.prepareStatement(
            		    "SELECT s.student_id, s.name, s.dob, s.date_joined, s.days, s.class_mode, c.course_name " +
            		    "FROM Student s " +
            		    "LEFT JOIN Course c ON s.course_id = c.course_id " +
            		    "ORDER BY s.student_id"            		);
            	rs = stmt.executeQuery();

                out.println("<h2>Student List</h2>");
                out.println("<table border='1'>");
                out.println("<tr><th>ID</th><th>Name</th><th>DOB</th><th>Joined</th><th>Days</th><th>Mode</th><th>Course</th><th>Actions</th></tr>");
                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("student_id") + "</td>");
                    out.println("<td>" + rs.getString("name") + "</td>");
                    out.println("<td>" + rs.getDate("dob") + "</td>");
                    out.println("<td>" + rs.getDate("date_joined") + "</td>");
                    out.println("<td>" + rs.getString("days") + "</td>");
                    out.println("<td>" + rs.getString("class_mode") + "</td>");
                    out.println("<td>" + (rs.getString("course_name") == null ? "" : rs.getString("course_name")) + "</td>");
                    int sid = rs.getInt("student_id");
                    out.println("<td>");
                    out.println("<a href='DanceTracker?action=edit_student&id=" + sid + "'>");
                    out.println("<button type='button'>Update</button>");
                    out.println("</a> ");

                    out.println("<a href='DanceTracker?action=delete_student&id=" + sid + "' onclick=\"return confirm('Delete student ID " + sid + "?');\">");
                    out.println("<button type='button'>Delete</button>");
                    out.println("</a>");
                    out.println("</td>");

                    out.println("</tr>");
                }
                out.println("</table>");
            }

            else if ("branch".equals(action)) {
                stmt = con.prepareStatement("SELECT * FROM Branch");
                rs = stmt.executeQuery();

                out.println("<h2>Branch List</h2>");
                out.println("<table border='1'>");
                out.println("<tr><th>ID</th><th>Name</th><th>Location</th><th>Address</th><th>Phone</th><th>Actions</th></tr>");

                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("branch_id") + "</td>");
                    out.println("<td>" + rs.getString("name") + "</td>");
                    out.println("<td>" + rs.getString("location") + "</td>");
                    out.println("<td>" + rs.getString("address") + "</td>");
                    out.println("<td>" + rs.getString("phone") + "</td>");

                    int bid = rs.getInt("branch_id");
                    out.println("<td>");
                    out.println("<a href='DanceTracker?action=edit_branch&id=" + bid + "'>");
                    out.println("<button type='button'>Update</button>");
                    out.println("</a> ");

                    out.println("<a href='DanceTracker?action=delete_branch&id=" + bid + "' onclick=\"return confirm('Delete branch ID " + bid + "?');\">");
                    out.println("<button type='button'>Delete</button>");
                    out.println("</a>");
                    out.println("</td>");

                    out.println("</tr>");
                }
                out.println("</table>");
            }

            else if ("teacher".equals(action)) {
                stmt = con.prepareStatement("SELECT * FROM Teacher");
                rs = stmt.executeQuery();

                out.println("<h2>Teacher List</h2>");
                out.println("<table border='1'>");
                out.println("<tr><th>ID</th><th>Name</th><th>Address</th><th>Type</th><th>Actions</th></tr>");

                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("teacher_id") + "</td>");
                    out.println("<td>" + rs.getString("name") + "</td>");
                    out.println("<td>" + rs.getString("address") + "</td>");
                    out.println("<td>" + rs.getString("type") + "</td>");

                    int tid = rs.getInt("teacher_id");
                    out.println("<td>");
                    out.println("<a href='DanceTracker?action=edit_teacher&id=" + tid + "'>");
                    out.println("<button type='button'>Update</button>");
                    out.println("</a> ");

                    out.println("<a href='DanceTracker?action=delete_teacher&id=" + tid + "' onclick=\"return confirm('Delete teacher ID " + tid + "?');\">");
                    out.println("<button type='button'>Delete</button>");
                    out.println("</a>");
                    out.println("</td>");

                    out.println("</tr>");
                }
                out.println("</table>");
            }

            else if ("course".equals(action)) {
                stmt = con.prepareStatement("SELECT * FROM Course");
                rs = stmt.executeQuery();

                out.println("<h2>Course List</h2>");
                out.println("<table border='1'>");
                out.println("<tr><th>ID</th><th>Course Name</th><th>Actions</th></tr>");

                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("course_id") + "</td>");
                    out.println("<td>" + rs.getString("course_name") + "</td>");

                    int cid = rs.getInt("course_id");
                    out.println("<td>");
                    out.println("<a href='DanceTracker?action=edit_course&id=" + cid + "'>");
                    out.println("<button type='button'>Update</button>");
                    out.println("</a> ");

                    out.println("<a href='DanceTracker?action=delete_course&id=" + cid + "' onclick=\"return confirm('Delete course ID " + cid + "?');\">");
                    out.println("<button type='button'>Delete</button>");
                    out.println("</a>");
                    out.println("</td>");

                    out.println("</tr>");
                }
                out.println("</table>");
            }

            else {
                out.println("<h3>No action provided.</h3>");
                out.println("<p>Try: <code>?action=student</code> or <code>?action=branch</code></p>");
            }

            out.println("<br><a href='display.html'>Back</a>");
            out.println("</body></html>");

            if (rs != null) rs.close();
            if (stmt != null) stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("Error: " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(getDbUrl(), DB_USER, DB_PASS);
            PreparedStatement stmt = null;
            
            /* ================= SAVE ACADEMY INCOME ================= */
            if ("save_academy_income".equals(action)) {

                String dateStr = request.getParameter("class_date");
                String dayName = request.getParameter("day_name");
                String fromStr = request.getParameter("from_time");
                String toStr   = request.getParameter("to_time");

                if (dateStr == null || dayName == null || fromStr == null || toStr == null ||
                    dateStr.isEmpty() || dayName.isEmpty() || fromStr.isEmpty() || toStr.isEmpty()) {

                    out.println("<h3>❌ Missing fields.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                java.sql.Date classDate = java.sql.Date.valueOf(dateStr);
                java.time.LocalTime fromT = java.time.LocalTime.parse(fromStr);
                java.time.LocalTime toT   = java.time.LocalTime.parse(toStr);

                if (!toT.isAfter(fromT)) {
                    out.println("<h3>❌ To time must be after From time.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                // get academy rate (branch_id=4)
                java.math.BigDecimal rate;
                try (PreparedStatement pr = con.prepareStatement("SELECT rate FROM branch_rate WHERE branch_id=4");
                     ResultSet rr = pr.executeQuery()) {

                    if (!rr.next()) {
                        out.println("<h3>❌ No academy rate found (branch_id=4).</h3>");
                        out.println("<a href='display.html'>Go Back</a>");
                        con.close();
                        return;
                    }
                    rate = rr.getBigDecimal("rate");
                }

                long minutes = java.time.Duration.between(fromT, toT).toMinutes();
                java.math.BigDecimal hours = new java.math.BigDecimal(minutes)
                        .divide(new java.math.BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);

                java.math.BigDecimal lineTotal = hours.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);

                // slot_no = next slot for that date/day
                int slotNo = 1;
                try (PreparedStatement psSlot = con.prepareStatement(
                        "SELECT COALESCE(MAX(slot_no),0)+1 AS next_slot FROM academy_income WHERE class_date=? AND day_name=?")) {
                    psSlot.setDate(1, classDate);
                    psSlot.setString(2, dayName);
                    try (ResultSet rsSlot = psSlot.executeQuery()) {
                        if (rsSlot.next()) slotNo = rsSlot.getInt("next_slot");
                    }
                }

                String insertSql =
                    "INSERT INTO academy_income (class_date, day_name, slot_no, from_time, to_time, hours, rate, line_total) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setDate(1, classDate);
                    ps.setString(2, dayName);
                    ps.setInt(3, slotNo);
                    ps.setTime(4, java.sql.Time.valueOf(fromT));
                    ps.setTime(5, java.sql.Time.valueOf(toT));
                    ps.setBigDecimal(6, hours);
                    ps.setBigDecimal(7, rate);
                    ps.setBigDecimal(8, lineTotal);
                    ps.executeUpdate();
                }

                response.sendRedirect("DanceServlet?action=view_academy_income");
                con.close();
                return;
            }
            /* ================= PROCESS MONTH END ================= */
            if ("process_month_end".equals(action)) {

                String startStr = request.getParameter("start_date");
                String endStr   = request.getParameter("end_date");

                if (startStr == null || endStr == null || startStr.isEmpty() || endStr.isEmpty()) {
                    out.println("<h3>❌ Missing dates.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                java.sql.Date startDate = java.sql.Date.valueOf(startStr);
                java.sql.Date endDate   = java.sql.Date.valueOf(endStr);

                if (endDate.before(startDate)) {
                    out.println("<h3>❌ End date must be after Start date.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                // Helper: returns SUM(total_amount) or SUM(line_total) depending on table structure
                java.math.BigDecimal templeTotal  = java.math.BigDecimal.ZERO;
                java.math.BigDecimal homeTotal    = java.math.BigDecimal.ZERO;
                java.math.BigDecimal academyTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal onlineTotal  = java.math.BigDecimal.ZERO;

                // TEMPLE: temple_income has total_amount
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) AS total " +
                    "FROM temple_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) templeTotal = rs.getBigDecimal("total");
                    }
                }

                // HOME: home_income has total_amount
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) AS total " +
                    "FROM home_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) homeTotal = rs.getBigDecimal("total");
                    }
                }

                // ACADEMY: if you store line_total per row, sum it
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(line_total),0) AS total " +
                    "FROM academy_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) academyTotal = rs.getBigDecimal("total");
                    }
                }

                // ONLINE: sum line_total (same pattern as academy)
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(line_total),0) AS total " +
                    "FROM online_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) onlineTotal = rs.getBigDecimal("total");
                    }
                }

                java.math.BigDecimal grandTotal =
                    templeTotal.add(homeTotal).add(academyTotal).add(onlineTotal)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

                // Save into month_end_summary
                try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO month_end_summary " +
                    "(start_date, end_date, temple_total, home_total, academy_total, online_total, grand_total) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"
                )) {
                    ins.setDate(1, startDate);
                    ins.setDate(2, endDate);
                    ins.setBigDecimal(3, templeTotal);
                    ins.setBigDecimal(4, homeTotal);
                    ins.setBigDecimal(5, academyTotal);
                    ins.setBigDecimal(6, onlineTotal);
                    ins.setBigDecimal(7, grandTotal);
                    ins.executeUpdate();
                }

                response.sendRedirect("DanceServlet?action=view_month_end");
                con.close();
                return;
            }
            /* ================= SAVE STUDENT PROGERESS ================= */
            else if ("save_student_progress".equals(action)) {
                String progressDate = request.getParameter("progress_date");
                int courseId = Integer.parseInt(request.getParameter("course_id"));
                int studentId = Integer.parseInt(request.getParameter("student_id"));
                String status = request.getParameter("status");
                String comments = request.getParameter("comments");

                if (status == null) status = "";
                if (comments == null) comments = "";

                String sql = "INSERT INTO student_progress (progress_date, course_id, student_id, status, comments) VALUES (?, ?, ?, ?, ?)";
                stmt = con.prepareStatement(sql);
                stmt.setDate(1, java.sql.Date.valueOf(progressDate));
                stmt.setInt(2, courseId);
                stmt.setInt(3, studentId);

                if (status.trim().isEmpty()) {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    stmt.setString(4, status);
                }

                if (comments.trim().isEmpty()) {
                    stmt.setNull(5, java.sql.Types.VARCHAR);
                } else {
                    stmt.setString(5, comments);
                }
            }
            /* ================= SAVE EXPENSE ================= */
            else if ("save_expense".equals(action)) {

                String expenseDate = request.getParameter("expense_date");
                String expenseName = request.getParameter("expense_name");
                String hoursStr = request.getParameter("hours");

                if (expenseDate == null || expenseDate.isEmpty() ||
                    expenseName == null || expenseName.trim().isEmpty()) {
                    out.println("<h3>❌ Date and Name are required.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                java.math.BigDecimal hours = null;
                if (hoursStr != null && !hoursStr.trim().isEmpty()) {
                    hours = new java.math.BigDecimal(hoursStr).setScale(2, java.math.RoundingMode.HALF_UP);
                }

                // take latest rounded income from provision table
                java.math.BigDecimal roundedIncome = java.math.BigDecimal.ZERO;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT rounded_amount FROM provision ORDER BY provision_id DESC LIMIT 1");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getBigDecimal("rounded_amount") != null) {
                        roundedIncome = rs.getBigDecimal("rounded_amount");
                    }
                }

                // deduct 50 per hour only for MA class
                java.math.BigDecimal deductionAmount = java.math.BigDecimal.ZERO;
                if (expenseName.toLowerCase().contains("ma class")) {
                    java.math.BigDecimal safeHours = (hours == null) ? java.math.BigDecimal.ZERO : hours;
                    deductionAmount = safeHours.multiply(new java.math.BigDecimal("50"))
                                               .setScale(2, java.math.RoundingMode.HALF_UP);
                }

                java.math.BigDecimal finalIncome =
                    roundedIncome.subtract(deductionAmount).setScale(2, java.math.RoundingMode.HALF_UP);

                PreparedStatement psInsert = con.prepareStatement(
                    "INSERT INTO expenses (expense_date, expense_name, hours, rounded_income, deduction_amount, final_income) " +
                    "VALUES (?, ?, ?, ?, ?, ?)"
                );
                psInsert.setDate(1, java.sql.Date.valueOf(expenseDate));
                psInsert.setString(2, expenseName);

                if (hours != null) {
                    psInsert.setBigDecimal(3, hours);
                } else {
                    psInsert.setNull(3, java.sql.Types.DECIMAL);
                }

                psInsert.setBigDecimal(4, roundedIncome);
                psInsert.setBigDecimal(5, deductionAmount);
                psInsert.setBigDecimal(6, finalIncome);

                psInsert.executeUpdate();
                psInsert.close();

                response.sendRedirect("DanceServlet?action=view_expenses");
                con.close();
                return;
            }
            /* ================= PROCESS PROVISION ================= */
            if ("process_provision".equals(action)) {

                String startStr = request.getParameter("start_date");
                String endStr   = request.getParameter("end_date");

                if (startStr == null || endStr == null || startStr.isEmpty() || endStr.isEmpty()) {
                    out.println("<h3>❌ Missing dates.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                java.sql.Date startDate = java.sql.Date.valueOf(startStr);
                java.sql.Date endDate   = java.sql.Date.valueOf(endStr);

                if (endDate.before(startDate)) {
                    out.println("<h3>❌ End date must be after Start date.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                java.math.BigDecimal templeTotal  = java.math.BigDecimal.ZERO;
                java.math.BigDecimal homeTotal    = java.math.BigDecimal.ZERO;
                java.math.BigDecimal academyTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal onlineTotal  = java.math.BigDecimal.ZERO;

                // Temple
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) AS total " +
                    "FROM temple_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) templeTotal = rs.getBigDecimal("total");
                    }
                }

                // Home
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount),0) AS total " +
                    "FROM home_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) homeTotal = rs.getBigDecimal("total");
                    }
                }

                // Academy
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(line_total),0) AS total " +
                    "FROM academy_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) academyTotal = rs.getBigDecimal("total");
                    }
                }

                // Online
                try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(SUM(line_total),0) AS total " +
                    "FROM online_income WHERE class_date BETWEEN ? AND ?"
                )) {
                    ps.setDate(1, startDate);
                    ps.setDate(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) onlineTotal = rs.getBigDecimal("total");
                    }
                }

                java.math.BigDecimal grandTotal =
                    templeTotal.add(homeTotal).add(academyTotal).add(onlineTotal)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

                java.math.BigDecimal thousand = new java.math.BigDecimal("1000");

                java.math.BigDecimal roundedAmount =
                    grandTotal.divide(thousand, 0, java.math.RoundingMode.DOWN).multiply(thousand)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

                java.math.BigDecimal provisionAmount =
                    grandTotal.subtract(roundedAmount).setScale(2, java.math.RoundingMode.HALF_UP);

                try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO provision " +
                    "(start_date, end_date, total_month_end_amount, rounded_amount, provision_amount) " +
                    "VALUES (?, ?, ?, ?, ?)"
                )) {
                    ins.setDate(1, startDate);
                    ins.setDate(2, endDate);
                    ins.setBigDecimal(3, grandTotal);
                    ins.setBigDecimal(4, roundedAmount);
                    ins.setBigDecimal(5, provisionAmount);
                    ins.executeUpdate();
                }

                response.sendRedirect("DanceServlet?action=view_provision");
                con.close();
                return;
            }
            /* ================= SAVE ONLINE INCOME ================= */
            if ("save_online_income".equals(action)) {

                String dateStr = request.getParameter("class_date");
                String dayName = request.getParameter("day_name");
                String fromStr = request.getParameter("from_time");
                String toStr   = request.getParameter("to_time");
                String hoursStr = request.getParameter("hours");

                if (dateStr == null || dayName == null || dateStr.isEmpty() || dayName.isEmpty()) {
                    out.println("<h3>❌ Missing fields.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                java.sql.Date classDate = java.sql.Date.valueOf(dateStr);

                java.math.BigDecimal hours = null;
                java.sql.Time fromTimeSql = null;
                java.sql.Time toTimeSql = null;

                boolean hasFrom = fromStr != null && !fromStr.isEmpty();
                boolean hasTo = toStr != null && !toStr.isEmpty();
                boolean hasHours = hoursStr != null && !hoursStr.trim().isEmpty();

                if (hasFrom && hasTo) {
                    java.time.LocalTime fromT = java.time.LocalTime.parse(fromStr);
                    java.time.LocalTime toT   = java.time.LocalTime.parse(toStr);

                    if (!toT.isAfter(fromT)) {
                        out.println("<h3>❌ To time must be after From time.</h3>");
                        out.println("<a href='display.html'>Go Back</a>");
                        con.close();
                        return;
                    }

                    long minutes = java.time.Duration.between(fromT, toT).toMinutes();
                    hours = new java.math.BigDecimal(minutes)
                            .divide(new java.math.BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);

                    fromTimeSql = java.sql.Time.valueOf(fromT);
                    toTimeSql = java.sql.Time.valueOf(toT);

                } else if (hasHours) {
                    hours = new java.math.BigDecimal(hoursStr).setScale(2, java.math.RoundingMode.HALF_UP);

                    if (hours.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        out.println("<h3>❌ Hours must be greater than 0.</h3>");
                        out.println("<a href='display.html'>Go Back</a>");
                        con.close();
                        return;
                    }
                } else {
                    out.println("<h3>❌ Enter either From/To time OR Hours.</h3>");
                    out.println("<a href='display.html'>Go Back</a>");
                    con.close();
                    return;
                }

                // ✅ Online rate (branch_id = 2)
                java.math.BigDecimal rate;
                try (PreparedStatement pr = con.prepareStatement("SELECT rate FROM branch_rate WHERE branch_id=2");
                     ResultSet rr = pr.executeQuery()) {

                    if (!rr.next()) {
                        out.println("<h3>❌ No online rate found (branch_id=2).</h3>");
                        out.println("<a href='display.html'>Go Back</a>");
                        con.close();
                        return;
                    }
                    rate = rr.getBigDecimal("rate");
                }

                java.math.BigDecimal lineTotal = hours.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);

                // slot_no = next slot for that date/day
                int slotNo = 1;
                try (PreparedStatement psSlot = con.prepareStatement(
                        "SELECT COALESCE(MAX(slot_no),0)+1 AS next_slot FROM online_income WHERE class_date=? AND day_name=?")) {
                    psSlot.setDate(1, classDate);
                    psSlot.setString(2, dayName);
                    try (ResultSet rsSlot = psSlot.executeQuery()) {
                        if (rsSlot.next()) slotNo = rsSlot.getInt("next_slot");
                    }
                }

                String insertSql =
                    "INSERT INTO online_income (class_date, day_name, slot_no, from_time, to_time, hours, rate, line_total) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setDate(1, classDate);
                    ps.setString(2, dayName);
                    ps.setInt(3, slotNo);

                    if (fromTimeSql != null) ps.setTime(4, fromTimeSql);
                    else ps.setNull(4, java.sql.Types.TIME);

                    if (toTimeSql != null) ps.setTime(5, toTimeSql);
                    else ps.setNull(5, java.sql.Types.TIME);

                    ps.setBigDecimal(6, hours);
                    ps.setBigDecimal(7, rate);
                    ps.setBigDecimal(8, lineTotal);
                    ps.executeUpdate();
                }

                response.sendRedirect("DanceServlet?action=view_online_income");
                con.close();
                return;
            }

            /* ================= STUDENT ================= */
            if ("student".equals(action)) {
                String name = request.getParameter("name");
                String dob = request.getParameter("dob");
                String dateJoined = request.getParameter("date_joined");
                String days = request.getParameter("days");
                String classMode = request.getParameter("class_mode");

                // ✅ NEW: read selected branch and course from the form
                int branchId = Integer.parseInt(request.getParameter("branch"));      // HTML uses name="branch"
                int courseId = Integer.parseInt(request.getParameter("course_id"));  // HTML uses name="course_id"

                // ✅ FIX: insert branch_id and course_id instead of hardcoding 1 / default 1
                String sql =
                    "INSERT INTO Student (student_id, name, dob, date_joined, branch_id, days, class_mode, course_id) " +
                    "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setDate(2, java.sql.Date.valueOf(dob));
                stmt.setDate(3, java.sql.Date.valueOf(dateJoined));
                stmt.setInt(4, branchId);
                stmt.setString(5, days);
                stmt.setString(6, classMode);
                stmt.setInt(7, courseId);
            }

            /* ================= UPDATE STUDENT ================= */
            else if ("update_student".equals(action)) {
                int studentId = Integer.parseInt(request.getParameter("student_id"));

                String name = request.getParameter("name");
                String dob = request.getParameter("dob");
                String dateJoined = request.getParameter("date_joined");
                String days = request.getParameter("days");
                String classMode = request.getParameter("class_mode");

                String sql =
                    "UPDATE Student SET name=?, dob=?, date_joined=?, days=?, class_mode=? WHERE student_id=?";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setDate(2, java.sql.Date.valueOf(dob));
                stmt.setDate(3, java.sql.Date.valueOf(dateJoined));
                stmt.setString(4, days);
                stmt.setString(5, classMode);
                stmt.setInt(6, studentId);
            }
            /* ================= UPDATE RATES ================= */
            else if ("update_rates".equals(action)) {

                // Update all branches (1..4). If you have more branches later, we can query them dynamically.
                PreparedStatement ps = con.prepareStatement(
                    "UPDATE branch_rate SET rate=? WHERE branch_id=?"
                );

                for (int bid = 1; bid <= 4; bid++) {
                    String val = request.getParameter("rate_" + bid);
                    if (val == null) continue;

                    ps.setBigDecimal(1, new java.math.BigDecimal(val));
                    ps.setInt(2, bid);
                    ps.addBatch();
                }

                ps.executeBatch();
                ps.close();

                response.sendRedirect("DanceTracker?action=view_rates");
                con.close();
                return;
            }

            /* ================= BRANCH ================= */
            else if ("branch".equals(action)) {
                String name = request.getParameter("name");
                String location = request.getParameter("location");
                String address = request.getParameter("address");
                String phone = request.getParameter("phone");

                String sql =
                    "INSERT INTO Branch (branch_id, name, location, address, phone) " +
                    "VALUES (NULL, ?, ?, ?, ?)";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setString(2, location);
                stmt.setString(3, address);
                stmt.setString(4, phone);
            }

            /* ================= UPDATE BRANCH ================= */
            else if ("update_branch".equals(action)) {
                int branchId = Integer.parseInt(request.getParameter("branch_id"));

                String name = request.getParameter("name");
                String location = request.getParameter("location");
                String address = request.getParameter("address");
                String phone = request.getParameter("phone");

                String sql =
                    "UPDATE Branch SET name=?, location=?, address=?, phone=? WHERE branch_id=?";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setString(2, location);
                stmt.setString(3, address);
                stmt.setString(4, phone);
                stmt.setInt(5, branchId);
            }

            /* ================= TEACHER ================= */
            else if ("teacher".equals(action)) {
                String name = request.getParameter("name");
                String address = request.getParameter("address");
                String type = request.getParameter("teacher_id");

                String sql =
                    "INSERT INTO Teacher (teacher_id, name, address, type) " +
                    "VALUES (NULL, ?, ?, ?)";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setString(2, address);
                stmt.setString(3, type);
            }

            /* ================= UPDATE TEACHER ================= */
            else if ("update_teacher".equals(action)) {
                int teacherId = Integer.parseInt(request.getParameter("teacher_id"));

                String name = request.getParameter("name");
                String address = request.getParameter("address");
                String type = request.getParameter("type");

                String sql =
                    "UPDATE Teacher SET name=?, address=?, type=? WHERE teacher_id=?";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setString(2, address);
                stmt.setString(3, type);
                stmt.setInt(4, teacherId);
            }

            /* ================= COURSE ================= */
            else if ("course".equals(action)) {
                String courseName = request.getParameter("course_name");

                String sql = "INSERT INTO Course (course_id, course_name) VALUES (NULL, ?)";
                stmt = con.prepareStatement(sql);
                stmt.setString(1, courseName);
            }

            /* ================= UPDATE COURSE ================= */
            else if ("update_course".equals(action)) {
                int courseId = Integer.parseInt(request.getParameter("course_id"));
                String courseName = request.getParameter("course_name");

                String sql =
                    "UPDATE Course SET course_name=? WHERE course_id=?";

                stmt = con.prepareStatement(sql);
                stmt.setString(1, courseName);
                stmt.setInt(2, courseId);
            }
            /* ================= ADD TOPIC ================= */
            else if ("add_topic".equals(action)) {
                int courseId = Integer.parseInt(request.getParameter("course_id"));
                String topicName = request.getParameter("topic_name");
                int topicOrder = Integer.parseInt(request.getParameter("topic_order"));

                String table;
                switch (courseId) {
                    case 1: table = "parichay"; break;
                    case 2: table = "prarambhik"; break;
                    case 3: table = "elementary"; break;
                    case 4: table = "intermediate"; break;
                    default:
                        out.println("<h3>❌ Invalid course.</h3>");
                        out.println("<a href='display.html'>Go Back</a>");
                        con.close();
                        return;
                }

                String sql = "INSERT INTO " + table + " (topic_name, topic_order) VALUES (?, ?)";
                stmt = con.prepareStatement(sql);
                stmt.setString(1, topicName);
                stmt.setInt(2, topicOrder);
            }
            /* ================= ADD SUBTOPIC ================= */
            else if ("add_subtopic".equals(action)) {
                int courseId = Integer.parseInt(request.getParameter("course_id"));
                int topicId = Integer.parseInt(request.getParameter("topic_id"));
                String subtopicName = request.getParameter("subtopic_name");
                int subtopicOrder = Integer.parseInt(request.getParameter("subtopic_order"));

                String table;
                switch (courseId) {
                    case 1: table = "parichay_subtopic"; break;
                    case 2: table = "prarambhik_subtopic"; break;
                    case 3: table = "elementary_subtopic"; break;
                    case 4: table = "intermediate_subtopic"; break;
                    default:
                        out.println("<h3>❌ Invalid course.</h3>");
                        out.println("<a href='display.html'>Go Back</a>");
                        con.close();
                        return;
                }

                String sql = "INSERT INTO " + table + " (topic_id, subtopic_name, subtopic_order) VALUES (?, ?, ?)";
                stmt = con.prepareStatement(sql);
                stmt.setInt(1, topicId);
                stmt.setString(2, subtopicName);
                stmt.setInt(3, subtopicOrder);
            }
            /* ================= SAVE ATTENDANCE ================= */
            else if ("saveAttendance".equals(action)) {

                int branchId = Integer.parseInt(request.getParameter("branch_id"));
                int courseId = Integer.parseInt(request.getParameter("course_id"));
                String day = request.getParameter("day"); // store selected day

                int teacherId = 1; // always same teacher

                String classDateStr = request.getParameter("class_date");
                java.sql.Date today;

                if (classDateStr != null && !classDateStr.trim().isEmpty()) {
                    today = java.sql.Date.valueOf(classDateStr);
                } else {
                    today = new java.sql.Date(System.currentTimeMillis());
                }

                // ALL students shown in the table
                String[] allIds = request.getParameterValues("all_student_id");
                if (allIds == null) allIds = new String[0];

                // Only checked students
                java.util.HashSet<String> presentSet = new java.util.HashSet<>();
                String[] presentIds = request.getParameterValues("present_student_id");
                if (presentIds != null) {
                    for (String s : presentIds) presentSet.add(s);
                }

                PreparedStatement psMakeup = con.prepareStatement(
                        "INSERT INTO makeup_counter (student_id, course_id, branch_id, absent_count, last_absent_date) " +
                        "VALUES (?, ?, ?, 1, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "absent_count = absent_count + 1, " +
                        "last_absent_date = VALUES(last_absent_date)"
                );

                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Attendance (student_id, course_id, branch_id, teacher_id, class_date, day, status, comment) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE status=VALUES(status), day=VALUES(day), comment=VALUES(comment)"
                );

                for (String sid : allIds) {
                    ps.setInt(1, Integer.parseInt(sid));
                    ps.setInt(2, courseId);
                    ps.setInt(3, branchId);
                    ps.setInt(4, teacherId);
                    ps.setDate(5, today);
                    ps.setString(6, day);

                    String st = presentSet.contains(sid) ? "P" : "A";
                    ps.setString(7, st);
                    if ("A".equals(st)) {
                        psMakeup.setInt(1, Integer.parseInt(sid));
                        psMakeup.setInt(2, courseId);
                        psMakeup.setInt(3, branchId);
                        psMakeup.setDate(4, today);
                        psMakeup.addBatch();
                    }

                    String comment = request.getParameter("comment_" + sid);
                    if (comment == null) comment = "";
                    ps.setString(8, comment);

                    ps.addBatch();
                }

                ps.executeBatch();

                // After saving Attendance rows:
                String incomeTable = null;
                if (branchId == 1) incomeTable = "home_income";
                else if (branchId == 3) incomeTable = "temple_income";

                if (incomeTable != null) {
                    PreparedStatement psIncome = con.prepareStatement(
                        "INSERT INTO " + incomeTable + " (class_date, course_id, present_count, rate, total_amount) " +
                        "SELECT a.class_date, a.course_id, " +
                        "       SUM(a.status='P') AS present_count, " +
                        "       r.rate, " +
                        "       SUM(a.status='P') * r.rate AS total_amount " +
                        "FROM Attendance a " +
                        "JOIN branch_rate r ON r.branch_id = a.branch_id " +
                        "WHERE a.branch_id = ? AND a.class_date = ? AND a.course_id = ? " +
                        "GROUP BY a.class_date, a.course_id, r.rate " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  present_count = VALUES(present_count), " +
                        "  rate = VALUES(rate), " +
                        "  total_amount = VALUES(total_amount)"
                    );

                    psIncome.setInt(1, branchId);
                    psIncome.setDate(2, today);
                    psIncome.setInt(3, courseId);

                    psIncome.executeUpdate();
                    psIncome.close();
                }

                psMakeup.executeBatch();
                psMakeup.close();
                ps.close();

                out.println("<h3>✅ Attendance saved for " + allIds.length + " students (" + today + ")</h3>");
                out.println("<a href='display.html'>Go Back</a>");
                con.close();
                return;
            }

            /* ================= UPDATE ATTENDANCE (NEW) ================= */
            else if ("update_attendance".equals(action)) {
                int attendanceId = Integer.parseInt(request.getParameter("attendance_id"));
                String classDate = request.getParameter("class_date");
                String day = request.getParameter("day");
                String status = request.getParameter("status");
                String comment = request.getParameter("comment");
                if (comment == null) comment = "";

                String sql =
                    "UPDATE Attendance SET class_date=?, day=?, status=?, comment=? WHERE attendance_id=?";

                stmt = con.prepareStatement(sql);
                stmt.setDate(1, java.sql.Date.valueOf(classDate));
                stmt.setString(2, day);
                stmt.setString(3, status);
                stmt.setString(4, comment);
                stmt.setInt(5, attendanceId);
            }

            /* ================= ADD ATTENDANCE (NEW) ================= */
            else if ("add_attendance".equals(action)) {

                String studentName = request.getParameter("student_name");
                String courseName  = request.getParameter("course_name");
                String branchName  = request.getParameter("branch_name");

                String classDate = request.getParameter("class_date");
                String day = request.getParameter("day");
                String status = request.getParameter("status");
                String comment = request.getParameter("comment");
                if (comment == null) comment = "";

                // find student_id by student name
                int studentId = -1;
                PreparedStatement s1 = con.prepareStatement(
                    "SELECT student_id FROM Student WHERE name=? LIMIT 1"
                );
                s1.setString(1, studentName);
                ResultSet r1 = s1.executeQuery();
                if (r1.next()) studentId = r1.getInt("student_id");
                r1.close();
                s1.close();

                // find course_id by course name
                int courseId = -1;
                PreparedStatement s2 = con.prepareStatement(
                    "SELECT course_id FROM Course WHERE course_name=? LIMIT 1"
                );
                s2.setString(1, courseName);
                ResultSet r2 = s2.executeQuery();
                if (r2.next()) courseId = r2.getInt("course_id");
                r2.close();
                s2.close();

                // find branch_id by branch name
                int branchId = -1;
                PreparedStatement s3 = con.prepareStatement(
                    "SELECT branch_id FROM Branch WHERE name=? LIMIT 1"
                );
                s3.setString(1, branchName);
                ResultSet r3 = s3.executeQuery();
                if (r3.next()) branchId = r3.getInt("branch_id");
                r3.close();
                s3.close();

                // keep teacher consistent with your existing attendance system
                int teacherId = 1;

                if (studentId == -1 || courseId == -1 || branchId == -1) {
                    out.println("<h3>❌ Error: Name not found.</h3>");
                    if (studentId == -1) out.println("<p>Student not found: " + studentName + "</p>");
                    if (courseId == -1) out.println("<p>Course not found: " + courseName + "</p>");
                    if (branchId == -1) out.println("<p>Branch not found: " + branchName + "</p>");
                    out.println("<a href='DanceTracker?action=add_attendance_form'>Go Back</a>");
                    con.close();
                    return;
                }

                String sql =
                    "INSERT INTO Attendance (student_id, course_id, branch_id, teacher_id, class_date, day, status, comment) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                stmt = con.prepareStatement(sql);
                stmt.setInt(1, studentId);
                stmt.setInt(2, courseId);
                stmt.setInt(3, branchId);
                stmt.setInt(4, teacherId);
                stmt.setDate(5, java.sql.Date.valueOf(classDate));
                stmt.setString(6, day);
                stmt.setString(7, status);
                stmt.setString(8, comment);
            }
            /* ================= SAVE NOTE (AJAX) ================= */
            else if ("save_note".equals(action)) {

                response.setContentType("application/json;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");

                String noteText = request.getParameter("note_text");
                if (noteText == null) noteText = "";
                noteText = noteText.trim();

                if (noteText.isEmpty()) {
                    out.print("{\"ok\":false,\"error\":\"Note cannot be empty\"}");
                    con.close();
                    return;
                }

                String sql = "INSERT INTO notes (note_text) VALUES (?)";
                stmt = con.prepareStatement(sql);
                stmt.setString(1, noteText);

                int rows = stmt.executeUpdate();
                stmt.close();

                out.print("{\"ok\":true,\"rows\":" + rows + "}");
                con.close();
                return;
            }
            else if ("add_note".equals(action)) {
                String noteText = request.getParameter("note_text");
                if (noteText == null) noteText = "";
                noteText = noteText.trim();

                if (noteText.isEmpty()) {
                    response.sendRedirect("DanceTracker?action=view_notes&msg=empty");
                    con.close();
                    return;
                }

                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO notes (note_text) VALUES (?)"
                );
                ps.setString(1, noteText);
                ps.executeUpdate();
                ps.close();

                response.sendRedirect("DanceTracker?action=view_notes");
                con.close();
                return;
            }
            else if ("update_note".equals(action)) {
                int noteId = Integer.parseInt(request.getParameter("note_id"));
                String noteText = request.getParameter("note_text");
                if (noteText == null) noteText = "";
                noteText = noteText.trim();

                if (noteText.isEmpty()) {
                    response.sendRedirect("DanceTracker?action=view_notes&msg=empty");
                    con.close();
                    return;
                }

                PreparedStatement ps = con.prepareStatement(
                    "UPDATE notes SET note_text=? WHERE note_id=?"
                );
                ps.setString(1, noteText);
                ps.setInt(2, noteId);
                ps.executeUpdate();
                ps.close();

                response.sendRedirect("DanceTracker?action=view_notes");
                con.close();
                return;
            }

            /* ================= EXECUTE ================= */
            if (stmt != null) {
                int rows = stmt.executeUpdate();
                stmt.close();

                out.println("<h3>✅ Data Submitted Successfully! (" + rows + " row)</h3>");
                out.println("<a href='display.html'>Go Back</a>");
            } else {
                out.println("<h3>❌ Unknown action: " + action + "</h3>");
                out.println("<a href='display.html'>Go Back</a>");
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<h3>❌ Error: " + e.getMessage() + "</h3>");
            out.println("<a href='display.html'>Go Back</a>");
        }
    }
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    private String escapeJson1(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
