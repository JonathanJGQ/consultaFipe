package com.sif.core.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sif.core.exception.GenericException;
import com.sif.model.Usuario;
import com.sif.model.UsuarioPermissao;
import com.sif.repository.UsuarioPermissaoRepository;
import com.sif.repository.UsuarioRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
@Order(1)
public class TransactionFilter implements Filter{
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	static final long REMAINING_TIME = 1200000;
//	static final long REMAINING_TIME = 3570000;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
	  
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
        
		if(SecurityContextHolder.getContext().getAuthentication() == null) {
			chain.doFilter(req, res);
			return;
		}
		
		req.getInputStream();
		
		Usuario usuario = usuarioRepository.findByDocumento(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
		
		String token = req.getHeader("Authorization").replace("Bearer ", ""); 
		
		if(usuario.getToken() == null || usuario.getToken().isEmpty()) {
			
			chain.doFilter(req, res);
			return;
		}
		
		if(!token.equals(usuario.getToken())) {
//			throw new GenericException("Erro", "Token expirou");
			chain.doFilter(req, res);
			return;
		}
		
		SecurityContextHolder.getContext().setAuthentication(
		        new UsernamePasswordAuthenticationToken(
		                SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
		                SecurityContextHolder.getContext().getAuthentication().getCredentials(),
		                getAuthorities(usuario)
		));
		
		Claims jwsMap = Jwts.parser()
			       .setSigningKey(DatatypeConverter.parseBase64Binary(TokenAuthenticationService.SECRET))
			       .parseClaimsJws(token)
			       .getBody();
		Date dateExpiration = jwsMap.getExpiration();
		Date dateNow = new Date();
		
		long difference = dateExpiration.getTime() - dateNow.getTime();
		
		//Se faltar 20 minutos para expirar envia um novo token
		if(difference <= REMAINING_TIME) {
			String jws = Jwts.builder()
			  .setSubject(jwsMap.getSubject())
			  .setExpiration(new Date(System.currentTimeMillis() + TokenAuthenticationService.EXPIRATION_TIME))
			  .signWith(
			    SignatureAlgorithm.HS512,
			    TokenAuthenticationService.SECRET)
			  .compact();
			res.setHeader("refresh_token", jws);
			usuario.setToken(jws);
			usuarioRepository.save(usuario);
		}
		
        chain.doFilter(req, res);
     
	}
	
	private List<GrantedAuthority> getAuthorities(Usuario usuario) {
		
		List<UsuarioPermissao> usuarioPermissaoList = usuarioPermissaoRepository.findByUsuario(usuario);
		
		List<String> roles = new ArrayList<String>();
		for(UsuarioPermissao usuarioPermissao : usuarioPermissaoList) {
			roles.add(usuarioPermissao.getFuncionalidade().getRota());
		}
		
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(r))
                .collect(Collectors.toList());
    }

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
