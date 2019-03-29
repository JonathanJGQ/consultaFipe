package com.sif.core.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lacunasoftware.restpki.PKCertificate;
import com.lacunasoftware.restpki.RestException;
import com.lacunasoftware.restpki.ValidationResults;
import com.sif.core.config.CustomAuthenticationProvider;
import com.sif.core.exception.GenericException;
import com.sif.core.exception.SifExceptionHandler.Erro;
import com.sif.core.service.UserLoginHistoryService;
import com.sif.core.utils.ConfiguracaoHelper;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.TipoPerfil;
import com.sif.core.utils.TipoUsuarioHelper;
import com.sif.model.Averbadora;
import com.sif.model.Configuracao;
import com.sif.model.Consignataria;
import com.sif.model.FingerprintToken;
import com.sif.model.LogAcao;
import com.sif.model.TipoUsuario;
import com.sif.model.Usuario;
import com.sif.model.UsuarioPermissao;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.AdministracaoRepository;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.FingerprintTokenRepository;
import com.sif.repository.ModuloRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.UserLoginHistoryRepository;
import com.sif.repository.UsuarioPermissaoRepository;
import com.sif.repository.UsuarioRepository;


public class JWTLoginFilter extends AbstractAuthenticationProcessingFilter {
	
	private UsuarioRepository usuarioRepository;
	
	private ModuloRepository moduloRepository;
	
	private FingerprintTokenRepository fingerPrintRepository;
	
    public JavaMailSender emailSender;
	
	private AverbadoraRepository averbadoraRepository;
	
	private AdministracaoRepository administracaoRepository;
	
	private OrgaoRepository orgaoRepository;
	
	private ConsignatariaRepository consignatariaRepository;
	
	private UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	private ConfiguracaoRepository configuracaoRepository;
	
	private UserLoginHistoryRepository userLoginHistoryRepository;
	
	private InternalUserDetailsService internalUserDetailsService;
	
	private CustomAuthenticationProvider customAuth;
	
	private AccountCredentials credentials;
	
	private Funcoes funcoes;
	
	protected JWTLoginFilter(String url, AuthenticationManager authManager, UsuarioRepository usuarioRepository
			, ModuloRepository moduloRepository, OrgaoRepository orgaoRepository, ConsignatariaRepository consignatariaRepository
			, AverbadoraRepository averbadoraRepository, AdministracaoRepository administracaoRepository
			, UsuarioPermissaoRepository usuarioPermissaoRepository, ConfiguracaoRepository configuracaoRepository
			, UserLoginHistoryRepository userLoginHistoryRepository, InternalUserDetailsService internalUserDetailsService
			, CustomAuthenticationProvider customAuth, Funcoes funcoes, FingerprintTokenRepository fingerPrintRepository
			, JavaMailSender emailSender) {
		super(new AntPathRequestMatcher(url));
		setAuthenticationManager(authManager);
		this.usuarioRepository = usuarioRepository;
		this.moduloRepository = moduloRepository;
		this.averbadoraRepository = averbadoraRepository;
		this.administracaoRepository = administracaoRepository;
		this.orgaoRepository = orgaoRepository;
		this.consignatariaRepository = consignatariaRepository;
		this.usuarioPermissaoRepository = usuarioPermissaoRepository;
		this.configuracaoRepository = configuracaoRepository;
		this.userLoginHistoryRepository = userLoginHistoryRepository;
		this.funcoes = funcoes;
		this.internalUserDetailsService = internalUserDetailsService;
		this.customAuth = customAuth;
		this.fingerPrintRepository = fingerPrintRepository;
		this.emailSender = emailSender;
	}
	
	public FingerprintToken createPasswordResetTokenForUser(Usuario user, String token) {
		FingerprintToken myToken = new FingerprintToken();
		myToken.setToken(token);
		myToken.setUsuario(user);
	    Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.MINUTE, 15);
	    myToken.setExpiryDate(calendar.getTime());
	    return myToken;
	}
	
	private boolean sendEmailTokenLogin(Usuario usuario, String fingerprint) {
		String token = UUID.randomUUID().toString();
		Configuracao configuracaoToken = configuracaoRepository.findById(ConfiguracaoHelper.TOKEN_FINGERPRINT_TIMER).get();
		
		//TODO Algoritmo de geracao de token
		String tokenGe = generateToken();
		
		usuario.setTokenFingerPrint(tokenGe);
		usuarioRepository.save(usuario);
		
		FingerprintToken myToken = new FingerprintToken();
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, Integer.parseInt(configuracaoToken.getValor()));
		myToken.setExpiryDate(calendar.getTime());
		myToken.setFingerPrint(fingerprint);
		myToken.setToken(tokenGe);
		myToken.setUsuario(usuario);
		fingerPrintRepository.save(myToken);
		
		SimpleMailMessage message = new SimpleMailMessage();
		
		message.setSubject("SIF - Mensagem Importante");
		message.setText(
				"Prezado,"+ "\r\n" +
				"Parece que alguém está tentando acessar o sistema com suas informações. "
				+ "Se foi você mesmo, aqui está seu código de acesso:"+"\r\n" +
				"\r\n" +
				tokenGe+
				"\r\n" +
				"\r\n" +
				"Caso não tenha sido você, basta ignorar esta mensagem."+ "\r\n" +
				"Por favor não responda este email."+ "\r\n" +
				"Att,"+ "\r\n" +
				"Sistema SIF."
		);
		
		Configuracao emailLogin = configuracaoRepository.findById(ConfiguracaoHelper.EMAIL_LOGIN).get();
        message.setTo(usuario.getEmail());
        message.setFrom(emailLogin.getValor());
        
        System.out.println(tokenGe);
        
        try {
            emailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
	}
	
	private String generateToken() {
		
		String sequence = "";
		Random rand = new Random();
		int indice_letter = 0;
		
		for(int i = 0; i < 6; i++) {
			sequence = sequence + Integer.toString(rand.nextInt(9));
		}
		
		return sequence;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException, GenericException {
		
		credentials = new ObjectMapper()
				.readValue(request.getInputStream(), AccountCredentials.class);
		
		Usuario usuario = usuarioRepository.findByDocumento(credentials.getUsername());
		
		if(usuario.getTipoUsuario().getId().equals(TipoUsuarioHelper.WEBSERVICE)) {
			String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Este usuário não tem acesso ao sistema pela web.\" }";
			response.getOutputStream().write(responseJson.getBytes());
			response.setContentType("application/json");
			response.setStatus(401);
			return null;
		}
		
		if(usuario == null) {
			String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Usuário incorreto\" }";
			response.getOutputStream().write(responseJson.getBytes());
			response.setContentType("application/json");
			response.setStatus(401);
			return null;
		}
		
		//checar se usuário está bloqueado
		if(usuario.getStatus() == 2) {
			String responseJson  = "{\"title\":\"Bloqueado\", \"message\":\"Usuário bloqueado\" }";
			response.getOutputStream().write(responseJson.getBytes());
			response.setContentType("application/json");
			response.setStatus(401);
			return null;
		}
		
		String fingerPrint = request.getHeader("fingerprint");
		
		
		String tokenCertificado = request.getHeader("tokencertificado");
		
		//Esse trecho de código verifica se o token é válido
		try {
			if(tokenCertificado != null) {
				if(!tokenCertificado.isEmpty()) {
					com.lacunasoftware.restpki.Authentication 
						auth = new com.lacunasoftware.restpki.Authentication(Funcoes.getRestPkiClient());
					
					ValidationResults vr = auth.completeWithWebPki(tokenCertificado);
					
					if (!vr.isValid()) {
						String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Este certificado não é válido\" }";
						response.getOutputStream().write(responseJson.getBytes());
						response.setContentType("application/json");
						response.setStatus(401);
						return null;
					} else {
						PKCertificate userCert = auth.getPKCertificate();
						String certCpf = userCert.getPkiBrazil().getCpf();
						certCpf = certCpf.replace(".", "");
						certCpf = certCpf.replace("-", "");
						
						if(!certCpf.equals(usuario.getDocumento())) {
							String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Este cpf não é o mesmo do certificado.\" }";
							response.getOutputStream().write(responseJson.getBytes());
							response.setContentType("application/json");
							response.setStatus(401);
							return null;
						} else {
							UserDetails user = internalUserDetailsService.loadUserByUsername(usuario.getDocumento());
							UsernamePasswordAuthenticationToken authReq
							 = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
						
							org.springframework.security.core.Authentication authUser = customAuth.authenticate(authReq);
							org.springframework.security.core.context.SecurityContext sc = SecurityContextHolder.getContext();
						
							sc.setAuthentication(authUser);
							
							UserLoginHistoryService serviceLoginHistory = new UserLoginHistoryService();
							serviceLoginHistory.setUserLoginHistoryRepository(userLoginHistoryRepository);
							
							if(usuario.getLastFingerPrint() != null) {
								response = validateFingerPrint(usuario, fingerPrint, response);
								
								if(response.getStatus() == 403
									|| response.getStatus() == 406
									|| response.getStatus() == 418) {
									return null;
								}
							}
							
							serviceLoginHistory.saveNewLoginHistory(usuario, fingerPrint);
							
							return sc.getAuthentication();
						
						}
					}
				}
			}
		} catch (RestException e) {
			e.printStackTrace();
			String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Este certificado não é válido\" }";
			response.getOutputStream().write(responseJson.getBytes());
			response.setContentType("application/json");
			response.setStatus(401);
			return null;
		}
		
		for (Enumeration<?> e = request.getHeaderNames(); e.hasMoreElements();) {
		    String nextHeaderName = (String) e.nextElement();
		    String headerValue = request.getHeader(nextHeaderName);
		    System.out.println(nextHeaderName + ": " + headerValue);
		}
		
		//checando se a entidade do usuário está bloqueada
		if(usuario.getEntidade() != null) {
			if(usuario.getPerfil().getId().equals(TipoPerfil.AVERBADORA)) {
				Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
				if(averbadora.getStatus() == 2) {
					String responseJson  = "{\"title\":\"Bloqueado\", \"message\":\"Averbadora bloqueada\" }";
					response.getOutputStream().write(responseJson.getBytes());
					response.setContentType("application/json");
					response.setStatus(401);
					return null;
				}
			}
			if(usuario.getPerfil().getId().equals(TipoPerfil.CONSIGNATARIA)) {
				Consignataria consignataria = consignatariaRepository.findById(usuario.getEntidade()).get();
				if(consignataria.getStatus() == 2) {
					String responseJson  = "{\"title\":\"Bloqueado\", \"message\":\"Consignatária bloqueada\" }";
					response.getOutputStream().write(responseJson.getBytes());
					response.setContentType("application/json");
					response.setStatus(401);
					return null;
				}
			}
		}
		Authentication authentication = null;
		authentication = getAuthenticationManager().authenticate(
			new UsernamePasswordAuthenticationToken(
					credentials.getUsername(), 
					credentials.getPassword(), 
					Collections.emptyList()
					)
			);
		
		if(usuario.getLastFingerPrint() != null) {
			response = validateFingerPrint(usuario, fingerPrint, response);
			
			if(response.getStatus() == 403
				|| response.getStatus() == 406
				|| response.getStatus() == 418) {
				return null;
			}
			
		}
		
		return authentication;
	}
	
	private HttpServletResponse validateFingerPrint(Usuario usuario, String fingerPrint, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException, GenericException {
		
		
		if(fingerPrint != null) {
			if(!fingerPrint.equals(usuario.getLastFingerPrint())) {
				
				
				if(sendEmailTokenLogin(usuario, fingerPrint)) {
					String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Parece que você está tentando logar de um ambiente diferente, um email foi enviado para este usuário!\" }";
					response.getOutputStream().write(responseJson.getBytes());
					response.setContentType("application/json");
					response.setStatus(418);
				} else {
					String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Parece que você está tentando logar de um ambiente diferente, contudo não conseguimos enviar um email para este usuario, tente novamente mais tarde!\" }";
					response.getOutputStream().write(responseJson.getBytes());
					response.setContentType("application/json");
					response.setStatus(406);
				}
			}
		} else {
			String responseJson  = "{\"title\":\"Oops!\", \"message\":\"Você deve me enviar o identificador do seu browser!\" }";
			response.getOutputStream().write(responseJson.getBytes());
			response.setContentType("application/json");
			response.setStatus(403);
		}
		
		return response;
	}
	
	@Override
	protected void successfulAuthentication(
			HttpServletRequest request, 
			HttpServletResponse response,
			FilterChain filterChain,
			Authentication auth) throws IOException, ServletException {
		
		Usuario usuario = usuarioRepository.findByDocumento(auth.getName());
		
		UserLoginHistoryService serviceLoginHistory = new UserLoginHistoryService();
		serviceLoginHistory.setUserLoginHistoryRepository(userLoginHistoryRepository);
		
		String fingerPrint = request.getHeader("fingerprint");
		if(fingerPrint != null) {
			serviceLoginHistory.saveNewLoginHistory(usuario, fingerPrint);
			if(!fingerPrint.equals(usuario.getLastFingerPrint())) {
				usuario.setLastFingerPrint(fingerPrint);
				
			}
		}
		
		usuario.setErrosUsuario(0);
		usuarioRepository.save(usuario);
		
		Configuracao configRemember = new Configuracao();
		if(credentials.isRemember()) {
			configRemember = configuracaoRepository.findById(ConfiguracaoHelper.REMEMBER_ME).get();
			Cookie cookie = new Cookie("remember-me","true");
			response.addCookie(cookie);
		}
		else {
			Cookie cookie = new Cookie("remember-me","false");
			response.addCookie(cookie);
		}
		
		TokenAuthenticationService.addAuthentication(response, usuario, auth.getAuthorities(), funcoes.buildMenu(usuario), funcoes.getUsuarioEntidade(usuario), usuarioRepository, configRemember.getValor());
		
	}
	
	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		
		Configuracao configuracao = configuracaoRepository.findById(ConfiguracaoHelper.MAX_ERRO_LOGIN).get();
		
		//Bloquear Usuário se tiver acessado mais de x vezes
		Usuario usuario = usuarioRepository.findByDocumento(credentials.getUsername());
		if(usuario != null) {
			if(usuario.getErrosUsuario() == null) {
				usuario.setErrosUsuario(0);
			}
			usuario.setErrosUsuario(usuario.getErrosUsuario() + 1);
			if(usuario.getErrosUsuario() >= Integer.parseInt(configuracao.getValor())) {
				usuario.setStatus(2);
				
				try {
					LogAcao logAcao = funcoes.logAcao(usuario.getId(), getDescricaoBloqueioPorErro(), usuario);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			usuarioRepository.save(usuario);
		}
		
		String stringJson = "{\"title\": \"Oops!\", \"message\":\"Houve um erro na autenticação\"}";
		
		response.setContentType("application/json");
		response.setStatus(401);
		try {
			response.getOutputStream().write(stringJson.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	@ExceptionHandler({ GenericException.class })
	public ResponseEntity<Object> handleGenericException(GenericException ex) {
		String title = ex.getTitulo();
		String message = ex.getMessage();
		return ResponseEntity.badRequest().body(new Erro(title, message));
	}
	
	public Long getDescricaoBloqueioPorErro() {
		return DescricaoLogAcaoHelper.BLOQUEIO_POR_ERRO_DE_SENHA;
	}
}