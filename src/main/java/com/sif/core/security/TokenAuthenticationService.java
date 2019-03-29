package com.sif.core.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.sif.model.Perfil;
import com.sif.model.Usuario;
import com.sif.model.utils.TipoPerfil;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.UsuarioRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class TokenAuthenticationService {
	
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	// EXPIRATION_TIME = 1 hora
	static final long EXPIRATION_TIME = 3600000;
	public static final String SECRET = "MySecret";
	static final String TOKEN_PREFIX = "Bearer";
	static final String HEADER_STRING = "Authorization";
	
	public static void addAuthentication(HttpServletResponse response, Usuario usuario, Collection<?> authorities, String menu, String entidade, UsuarioRepository usuarioRepository, String timeRemember) {
		
		String JWT = "";
		
		if(timeRemember != null && !timeRemember.isEmpty()) {
			JWT = Jwts.builder()
				.setSubject(usuario.getDocumento())
				.setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(timeRemember)))
				.signWith(SignatureAlgorithm.HS512, SECRET)
				.compact();
		}
		else {
			JWT = Jwts.builder()
				.setSubject(usuario.getDocumento())
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
				.signWith(SignatureAlgorithm.HS512, SECRET)
				.compact();
		}
		
		Claims jwsMap = Jwts.parser()
		       .setSigningKey(DatatypeConverter.parseBase64Binary(TokenAuthenticationService.SECRET))
		       .parseClaimsJws(JWT)
		       .getBody();
		
		jwsMap.setExpiration(new Date(System.currentTimeMillis()));
		
		jwsMap.values();
		
		String jsonPermissoes = "[";
		for(Object permissao : authorities) {
			jsonPermissoes = jsonPermissoes + "\"" + permissao.toString() + "\",";
		}
		if(jsonPermissoes.length() > 1) {
			jsonPermissoes = jsonPermissoes.substring(0, jsonPermissoes.length() - 1);
		}
		jsonPermissoes = jsonPermissoes + "]";
		
		String stringJson = "{\"token\": \""+ JWT +"\", "
				+ "\"usuario\":{\"id\":" + usuario.getId() + ", \"nome\":\""+ usuario.getNome() + "\","
				+ "\"entidade\":\""+ entidade + "\", "
				+ "\"supremo\":\""+ (usuario.getPerfil().getId() == TipoPerfil.SUPREMO ? "true" : "false") + "\", "
				+ "\"admin\":\""+ (usuario.getPerfil().getId() == TipoPerfil.ADMINISTRADOR ? "true" : "false") + "\", "
				+ "\"perfil\":"+ createPerfil(usuario.getPerfil()) + ", "
				+ "\"permissoes\":" + jsonPermissoes + "}, "
				+ "\"menu\":" + menu + "}";
		
		response.addHeader(HEADER_STRING, TOKEN_PREFIX + " " + JWT);
		response.setContentType("application/json");
		
		usuario.setToken(JWT);
		usuarioRepository.save(usuario);
		try {
			response.getOutputStream().write(stringJson.getBytes());
			usuario.setToken(JWT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String createPerfil(Perfil perfil) {
		
		String retorno = "{";
		retorno = retorno + "\"id\":\"" + perfil.getId() + "\", ";
		retorno = retorno + "\"nome\":\"" + perfil.getNome()+ "\", ";
		retorno = retorno + "\"descricao\":\"" + perfil.getDescricao() + "\", ";
		retorno = retorno + "\"sigla\":\"" + perfil.getSigla()+ "\"}";
		
		return retorno;
		
	}
	
	static Authentication getAuthentication(HttpServletRequest request) {
		String token = request.getHeader(HEADER_STRING);
		
		
		if (token != null) {
			// faz parse do token
			try {
				String user = Jwts.parser()
					.setSigningKey(SECRET)
					.parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
					.getBody()
					.getSubject();
				
				if (user != null) {
					return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}

		}
		return null;
	}
	
}