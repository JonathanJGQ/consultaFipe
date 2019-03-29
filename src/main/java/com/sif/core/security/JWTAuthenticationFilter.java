package com.sif.core.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class JWTAuthenticationFilter extends GenericFilterBean {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		
		Cookie[] cookies = req.getCookies();
		
		if(cookies != null) {
			for(int i = 0; i < cookies.length; i++) {
				System.out.println(cookies[i].getName() + " : " + cookies[i].getValue());
			}
		}
		
		String url[] = req.getRequestURL().toString().split("/");
		
		Authentication authentication = TokenAuthenticationService
				.getAuthentication((HttpServletRequest) request);
		
		HttpServletResponse res = ((HttpServletResponse) response);
		
		System.out.println(req.getRequestURL().toString());
		if(req.getRequestURL().toString().contains("csrf")
				|| req.getRequestURL().toString().contains("v2/api-docs")) {
			res = validateRequest(filterChain, request, response);
			return;
		}
		
		if(authentication == null) {
			String jsonString = "{\"title\":\"Erro\", \"message\":\"Token expirou!\"}";
			
			res.getOutputStream().write(jsonString.getBytes());
			res.setContentType("application/json");
			res.setStatus(401);
		} else {
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, res);
		}
		
		
	}
	
	private HttpServletResponse validateRequest(FilterChain filterChain, ServletRequest request, ServletResponse response) throws IOException, ServletException {
		
		Authentication authentication = TokenAuthenticationService
				.getAuthentication((HttpServletRequest) request);
		
		HttpServletResponse res = ((HttpServletResponse) response);
		
		if(authentication == null) {
			String jsonString = "{\"title\":\"Erro\", \"message\":\"Token expirou!\"}";
			
			res.getOutputStream().write(jsonString.getBytes());
			res.setContentType("application/json");
			res.setStatus(401);
		} else {
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, res);
		}
		
		
		return res;
	}

}