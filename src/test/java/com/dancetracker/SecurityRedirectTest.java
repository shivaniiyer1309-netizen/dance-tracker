package com.dancetracker;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.Test;

class SecurityRedirectTest {

    // Base URL of your DanceTracker project
    private static final String BASE_URL =
            "http://localhost:8080/DanceTracker";

    @Test
    void protectedPagesShouldRedirectToLogin() throws Exception {

        /*
         * Put every page here that should NOT be accessible
         * unless the user is logged in.
         */
        String[] protectedPages = {

                "/DanceServlet?action=view_students",

                "/DanceServlet?action=view_student_progress",

                "/DanceTracker?action=view_makeup_counter"

        };

        for (String page : protectedPages) {

            System.out.println();
            System.out.println("====================================");
            System.out.println("Testing page: " + page);

            URL url = new URL(BASE_URL + page);

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            /*
             * VERY IMPORTANT:
             *
             * Normally Java would automatically follow the redirect
             * from the protected page to index.html.
             *
             * We turn that OFF so JUnit can actually see
             * whether the server returned a redirect.
             */
            connection.setInstanceFollowRedirects(false);

            connection.setRequestMethod("GET");

            // Get HTTP response
            int responseCode =
                    connection.getResponseCode();

            // Find where the page is redirecting
            String redirectLocation =
                    connection.getHeaderField("Location");

            System.out.println(
                    "Response code: " + responseCode
            );

            System.out.println(
                    "Redirect location: " + redirectLocation
            );

            /*
             * TEST 1:
             *
             * A redirect should normally return HTTP 302.
             */
            assertEquals(
                    302,
                    responseCode,
                    "SECURITY FAILURE: "
                            + page
                            + " did not redirect."
            );

            /*
             * TEST 2:
             *
             * Make sure a redirect location actually exists.
             */
            assertNotNull(
                    redirectLocation,
                    "SECURITY FAILURE: No redirect location for "
                            + page
            );

            /*
             * TEST 3:
             *
             * Make sure the user is being redirected
             * specifically to the login/index page.
             */
            assertTrue(
                    redirectLocation.contains("login.html"),
                    "SECURITY FAILURE: "
                            + page
                            + " did not redirect to login.html."
            );

            System.out.println(
                    "PASS: Page correctly redirected to login."
            );

            connection.disconnect();
        }
    }
}