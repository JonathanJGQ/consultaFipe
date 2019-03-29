package com.sif.core.security;

import java.util.Properties;

import javax.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sif.core.config.CustomAuthenticationProvider;
import com.sif.core.service.UserLoginHistoryService;
import com.sif.core.utils.ConfiguracaoHelper;
import com.sif.core.utils.Funcoes;
import com.sif.model.Configuracao;
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

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{
	
	private final UserDetailsService userDetailsService;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	ModuloRepository moduloRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	AdministracaoRepository administracaoRepository;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	UserLoginHistoryRepository userLoginHistoryRepository;
	
	@Autowired
	InternalUserDetailsService internalUserDetailsService;
	
	@Autowired
	CustomAuthenticationProvider customAuth;
	
	@Autowired
	UserLoginHistoryService userLoginHistoryService;
	
	@Autowired
	FingerprintTokenRepository fingerprintTokenRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	public WebSecurityConfig(UserDetailsService userDetailsService) {
	    super();
	    this.userDetailsService = userDetailsService;
	}
	
	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.csrf().disable()
		
			// filtra requisições de login
			.addFilterBefore(new JWTLoginFilter("/login", authenticationManager(), 
					usuarioRepository, moduloRepository, orgaoRepository, consignatariaRepository,
					averbadoraRepository, administracaoRepository, usuarioPermissaoRepository, configuracaoRepository, userLoginHistoryRepository, internalUserDetailsService, customAuth, funcoes, fingerprintTokenRepository, getJavaMailSender()),
	                UsernamePasswordAuthenticationFilter.class)
						
			// filtra outras requisições para verificar a presença do JWT no header
			.addFilterBefore(new JWTAuthenticationFilter(),
	                UsernamePasswordAuthenticationFilter.class);
						
		
		httpSecurity
			.authorizeRequests()
			.antMatchers(HttpMethod.POST, "/login").permitAll()
			.antMatchers("/v2/api-docs",
					"/swagger-ui.html",
					"/webjars/**",
					"/configuration/ui",
					"/configuration/security/**",
					"/v2/swagger.json",
					"/swagger-resources/**",
        			"/user/sendRecoverEmail",
        			"/user/checkPasswordToken",
        			"/user/changePassword",
        			"/user/token",
        			"/user/incrementErro").permitAll()
			.anyRequest().authenticated();
		
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService);
	}
	
	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
	
	@Bean
    SessionRegistry sessionRegistry() {			
        return new SessionRegistryImpl();
    }
	
	@Bean
	public SimpleMailMessage simpleMailMessage() {
		return new SimpleMailMessage();
	}
	
	@Override
    public void configure(WebSecurity web) throws Exception {
    	
    	web.ignoring().antMatchers("/css/**", "/assets/**", "/libs/**",
    			"/v2/api-docs", "/v2/swagger.json", "/configuration/ui", "/configuration/security/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**","/user/sendRecoverEmail", "/user/checkPasswordToken",
    			"/user/changePassword", "/user/fpauth", "/user/token", "/user/incrementErro");
    }
	 
	@Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        Configuracao emailLogin = configuracaoRepository.findById(ConfiguracaoHelper.EMAIL_LOGIN).get();
        Configuracao passwordEmailLogin = configuracaoRepository.findById(ConfiguracaoHelper.PASSWORD_EMAIL_LOGIN).get();
        Configuracao hostEmailLogin = configuracaoRepository.findById(ConfiguracaoHelper.HOST_EMAIL_LOGIN).get();
        Configuracao portaEmailLogin = configuracaoRepository.findById(ConfiguracaoHelper.PORTA_EMAIL_LOGIN).get();
        
        if(emailLogin == null || emailLogin.getValor() == null) {
        	 mailSender.setUsername("security@auttis.com.br");
        } else {
        	mailSender.setUsername(emailLogin.getValor());
        }
        
        if(passwordEmailLogin == null || passwordEmailLogin.getValor() == null) {
       	 	mailSender.setPassword("X8znDLxeGZ59V90z7ZmG");
        } else {
       		mailSender.setPassword(passwordEmailLogin.getValor());
        }
        
        if(hostEmailLogin == null || hostEmailLogin.getValor() == null) {
       	 	mailSender.setHost("email-ssl.com.br");
        } else {
       		mailSender.setHost(hostEmailLogin.getValor());
        }
        
        if(portaEmailLogin == null || portaEmailLogin.getValor() == null) {
       	 	mailSender.setPort(587);
        } else {
        	mailSender.setPort(Integer.parseInt(portaEmailLogin.getValor()));
        }
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.timeout", "5000");    
        props.put("mail.smtp.connectiontimeout", "5000");  
        
        return mailSender;
    }
	
	@Bean
	public MultipartConfigElement multipartConfigElement() {
	     MultipartConfigFactory factory = new MultipartConfigFactory();
	     factory.setMaxFileSize("100MB");
	     factory.setMaxRequestSize("100MB");
	     return factory.createMultipartConfig();

	}
	
}
