package com.dancetracker;

import java.io.IOException;
import java.net.URI;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.annotation.WebFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@WebFilter(
    urlPatterns = {
        "/*"
    }
)
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {


        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;


        /*
         * Prevent protected pages from
         * being stored in the browser cache.
         */
        httpResponse.setHeader(
                "Cache-Control",
                "no-cache, no-store, must-revalidate"
        );

        httpResponse.setHeader(
                "Pragma",
                "no-cache"
        );

        httpResponse.setDateHeader(
                "Expires",
                0
        );


        String contextPath =
                httpRequest.getContextPath();

        String requestURI =
                httpRequest.getRequestURI();


        /*
         * Get current session.
         *
         * false means:
         * DON'T create a new session.
         */
        HttpSession session =
                httpRequest.getSession(false);


        /*
         * Check whether user has successfully
         * logged in.
         */
        boolean loggedIn =
                session != null
                &&
                session.getAttribute(
                        "loggedInUser"
                ) != null;


        /*
         * Pages that must remain available
         * before login.
         */
        boolean publicPage =

                requestURI.equals(
                        contextPath + "/"
                )

                ||

                requestURI.equals(
                        contextPath + "/login.html"
                )

                ||

                requestURI.equals(
                        contextPath + "/register.html"
                )

                ||

                requestURI.equals(
                        contextPath + "/LoginServlet"
                )

                ||

                requestURI.equals(
                        contextPath + "/RegisterServlet"
                );


        /*
         * Static resources needed by
         * login/register pages.
         */
        boolean publicResource =

                requestURI.endsWith(".css")

                ||

                requestURI.endsWith(".js")

                ||

                requestURI.endsWith(".png")

                ||

                requestURI.endsWith(".jpg")

                ||

                requestURI.endsWith(".jpeg")

                ||

                requestURI.endsWith(".gif")

                ||

                requestURI.endsWith(".svg")

                ||

                requestURI.endsWith(".ico");


        /*
         * Login/register/resources are
         * always allowed.
         */
        if (publicPage || publicResource) {

            chain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * If there is no login session,
         * immediately go to login.
         */
        if (!loggedIn) {

            httpResponse.sendRedirect(
                    contextPath
                    + "/login.html"
            );

            return;
        }


        /*
         * -------------------------------------------------
         * DETECT DIRECT / COPIED / PASTED URL
         * -------------------------------------------------
         *
         * Modern browsers send:
         *
         * Sec-Fetch-Site: none
         *
         * when the user types/pastes a URL
         * directly into the address bar.
         *
         * Clicking links INSIDE RythmFlo normally
         * sends:
         *
         * Sec-Fetch-Site: same-origin
         */
        String fetchMode =
                httpRequest.getHeader(
                        "Sec-Fetch-Mode"
                );

        String fetchSite =
                httpRequest.getHeader(
                        "Sec-Fetch-Site"
                );


        boolean topLevelNavigation =
                "navigate".equalsIgnoreCase(
                        fetchMode
                );


        boolean directNavigation =
                topLevelNavigation
                &&
                "none".equalsIgnoreCase(
                        fetchSite
                );


        /*
         * Fallback for browsers that do not
         * provide Sec-Fetch headers.
         */
        if (
                topLevelNavigation
                &&
                fetchSite == null
        ) {

            String referer =
                    httpRequest.getHeader(
                            "Referer"
                    );

            boolean sameSiteReferer =
                    false;

            if (referer != null) {

                try {

                    URI ref =
                            new URI(referer);

                    String refHost =
                            ref.getHost();

                    String currentHost =
                            httpRequest.getServerName();

                    sameSiteReferer =
                            refHost != null
                            &&
                            currentHost != null
                            &&
                            refHost.equalsIgnoreCase(
                                    currentHost
                            );

                } catch (Exception e) {

                    sameSiteReferer =
                            false;
                }
            }


            directNavigation =
                    !sameSiteReferer;
        }


        /*
         * User pasted/typed/copied a protected URL.
         *
         * Force a brand-new login.
         */
        if (directNavigation) {

            /*
             * Invalidate existing login session.
             *
             * This makes sure they actually
             * have to log in again.
             */
            if (session != null) {

                session.invalidate();
            }


            httpResponse.sendRedirect(
                    contextPath
                    + "/login.html"
            );

            return;
        }


        /*
         * User is logged in AND reached
         * this page normally from inside
         * RythmFlo.
         *
         * Allow request.
         */
        chain.doFilter(
                request,
                response
        );
    }
}