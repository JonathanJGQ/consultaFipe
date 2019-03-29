package com.sif.core.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lacunasoftware.restpki.RestException;
import com.lacunasoftware.restpki.SecurityContext;
import com.sif.core.config.CustomAuthenticationProvider;
import com.sif.core.exception.GenericException;
import com.sif.core.security.AccountCredentials;
import com.sif.core.security.InternalUserDetailsService;
import com.sif.core.security.TokenAuthenticationService;
import com.sif.core.service.UserLoginHistoryService;
import com.sif.core.service.UsuarioService;
import com.sif.core.utils.ConfiguracaoHelper;
import com.sif.core.utils.Funcoes;
import com.sif.model.Configuracao;
import com.sif.model.FingerprintToken;
import com.sif.model.Funcionalidade;
import com.sif.model.LogAcao;
import com.sif.model.Usuario;
import com.sif.model.custom.PasswordTokenDTO;
import com.sif.model.custom.PermissoesFuncionalidade;
import com.sif.model.custom.UsuarioCustomDTO;
import com.sif.model.list.UsuarioDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.FingerprintTokenRepository;
import com.sif.repository.UsuarioRepository;

@RestController
@RequestMapping("/user")
public class UsuarioController {

	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	@Autowired
	FingerprintTokenRepository fingerprintTokenRepository;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	InternalUserDetailsService userDetailsService;
	
	@Autowired
	CustomAuthenticationProvider customAuth;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	UserLoginHistoryService userLoginHistoryService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/user/list')")
	public Page<UsuarioDTO> getAll(Pageable pageable, Usuario usuario) {
		return usuarioService.getAll(pageable, usuario);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/user/list')")
	public ResponseEntity<UsuarioCustomDTO> findById(@PathVariable() Long id) {
		return usuarioService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/user/new')")
	public ResponseEntity<Usuario> create(@RequestBody UsuarioCustomDTO usuario) {
		return usuarioService.create(usuario);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/user/edit')")
	public ResponseEntity<Usuario> update(@RequestBody UsuarioCustomDTO usuario, @PathVariable() Long id) {
		return usuarioService.update(usuario, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/user/delete')")
	public ResponseEntity<Usuario> delete(@PathVariable() Long id) {
		return usuarioService.delete(id);
	}
	
	@PostMapping("/blockDisblock/{id}")
	@PreAuthorize("hasAuthority('/user/block')")
	public ResponseEntity<String> blockDisblock(@PathVariable("id") Long id) {
		return usuarioService.blockDisblock(id);
	}
	
	@PostMapping("/logout/{id}")
	public ResponseEntity<String> logout(@PathVariable("id") Long id, HttpServletRequest request) {
		
		return usuarioService.logout(id, request.getHeader("fingerprint"));
	}
	
	@PostMapping("/sendRecoverEmail")
	public ResponseEntity<String> sendRecoverEmail(@RequestBody String email, HttpServletRequest request) {

		JSONObject jsonObject = new JSONObject(email);
		return usuarioService.sendRecoverEmail(jsonObject.getString("email"), request);
	}
	
	@PostMapping(value="/fpauth")
	public void fpauth(@RequestBody String jsonResponse, HttpServletRequest req, HttpServletResponse response) {
		
		AccountCredentials credentials;
		try {
		
			JSONObject jsonObject = new JSONObject(jsonResponse);
			String tokenFP = jsonObject.getString("code");
			
			boolean remember = false;
			try {
				remember = jsonObject.getBoolean("remember");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			String result = validadeTokenFP(1L, tokenFP);
			
			if(result != null) {
				throw new GenericException("Erro", "Código inválido");
			}
			
			FingerprintToken resetToken = fingerprintTokenRepository.findByToken(tokenFP);
			Usuario usuario = resetToken.getUsuario();
			
			if(!usuario.getTokenFingerPrint().equals(resetToken.getToken())) {
				throw new GenericException("Erro", "Código inválido");
			}
			
			UserDetails user = userDetailsService.loadUserByUsername(usuario.getDocumento());
			UsernamePasswordAuthenticationToken authReq
			 = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
			org.springframework.security.core.Authentication authUser = customAuth.authenticate(authReq);
			org.springframework.security.core.context.SecurityContext sc = SecurityContextHolder.getContext();
			sc.setAuthentication(authUser);
			
			usuario.setLastFingerPrint(resetToken.getFingerPrint());
			
			deleteTokens(resetToken.getUsuario());
			userLoginHistoryService.saveNewLoginHistory(usuario, resetToken.getFingerPrint());
			
			try {
				LogAcao logAcao = funcoes.logAcao(usuario.getId(), DescricaoLogAcaoHelper.LOGIN_COM_VALIDACAO, usuario);
			} catch (Exception e) {
				e.printStackTrace();
			}
			

			Configuracao configRemember = new Configuracao();
			if(remember) {
				configRemember = configuracaoRepository.findById(ConfiguracaoHelper.REMEMBER_ME).get();
			}
			
			TokenAuthenticationService.addAuthentication(response, usuario, authReq.getAuthorities(), funcoes.buildMenu(usuario), funcoes.getUsuarioEntidade(usuario), usuarioRepository, configRemember.getValor());
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
	}
	
	private void deleteTokens(Usuario usuario) {
		 List<FingerprintToken> tokensToDelete
	    	= fingerprintTokenRepository.getTokensByUsuario(usuario.getId());
	    
	    
	    //Deletando tokens daquele usuário
	    if(!tokensToDelete.isEmpty()) {
	    	for(int i = 0; i < tokensToDelete.size(); i++) {
	    		fingerprintTokenRepository.delete(tokensToDelete.get(i));
	    	}
	    }
	}
	
	public String validadeTokenFP(long id, String token) {
		try {
			FingerprintToken resetToken = fingerprintTokenRepository.findByToken(token);
			
			if(resetToken == null){
				
				return "invalid";
			}
			
		    Calendar cal = Calendar.getInstance();
		    if ((resetToken.getExpiryDate()
		        .getTime() - cal.getTime()
		        .getTime()) <= 0) {
		        return "expired";
		    }
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	@PostMapping("/checkPasswordToken")
	public ResponseEntity<String> checkPasswordToken(@RequestBody() String jsonBody) {
		JSONObject jsonObject = new JSONObject(jsonBody);
		return usuarioService.validatePasswordResetToken(Long.parseLong(jsonObject.getString("id")), jsonObject.getString("token"));
	}
	
	@PostMapping("/changePassword")
	public ResponseEntity<String> checkPasswordToken(@RequestBody PasswordTokenDTO tokenDto) {
		return usuarioService.savePassword(tokenDto);
	}
	
	@GetMapping("/perfilImage/{id}")
    public void image(@PathVariable("id") Long id, HttpServletResponse response) {
        
		Usuario usuario = usuarioService.findUsuarioById(id);
		
        response.setContentType("image/png");
        
        try {
	        File file;
	        
	        if(usuario.getPathFoto() == null || usuario.getPathFoto().isEmpty()) {
	        	file = resourceLoader.getResource("classpath:/default.png").getFile();
	        } else {
	        	file = new File(usuario.getPathFoto());
	        }
        
        	InputStream is = new FileInputStream(file);
            BufferedImage bi = ImageIO.read(is);
            OutputStream os = response.getOutputStream();
            ImageIO.write(bi, "png", os);
        }
        catch(Exception e) {}
        
    }
	
	@DeleteMapping("/deleteFoto/{id}")
	@PreAuthorize("hasAuthority('/user/delete')")
	public ResponseEntity<String> deleteFoto(@PathVariable("id") String id) {
		return usuarioService.deleteFoto(id);
	}
	
	@PostMapping("/uploadFoto")
	@PreAuthorize("hasAuthority('/user/edit')")
	public ResponseEntity<String> uploadFoto(@RequestParam("id") Long id, @RequestParam("file") MultipartFile file) {
		return usuarioService.uploadFoto(id, file);
	}

	@GetMapping("/getPermissions/{id}")
	@PreAuthorize("hasAuthority('/user/list')")
	public ResponseEntity<PermissoesFuncionalidade> getUserPermissions(@PathVariable("id") Long id) {
		return usuarioService.getPermissoesPerfil(id);
	}
	
	@PutMapping("/savePermissions/{id}")
	@PreAuthorize("hasAuthority('/user/permissions')")
	public ResponseEntity<String> saveUserPermissions(@PathVariable("id") Long id
			, @RequestBody() List<Funcionalidade> permissoes) {
		return usuarioService.salvarPermissoesPerfil(id, permissoes);
	}
	
	@PostMapping("/incrementErro")
	public boolean incrementError(@RequestBody String documento) {
		
		Configuracao configuracao = configuracaoRepository.findById(ConfiguracaoHelper.MAX_ERRO_LOGIN).get();
		Integer parametroErro = Integer.parseInt(configuracao.getValor());
		
		JSONObject json = new JSONObject(documento);
		
		if(!json.has("documento")) {
			return false;
		}
		
		Usuario usuario = usuarioService.findByDocumento(json.get("documento").toString());
		
		if(usuario != null) {
			if(usuario.getErrosUsuario() == null) {
				usuario.setErrosUsuario(1);
			} else if(usuario.getErrosUsuario() >= parametroErro) {
				return false;
			} else {
				usuario.setErrosUsuario(usuario.getErrosUsuario() + 1);
			}
			
			if(usuario.getErrosUsuario() >= 3) {
				usuario.setStatus(2);
				usuarioService.edit(usuario);
				return true;
			}
			
			usuarioService.edit(usuario);
			
			return false;
		}
		
		return false;
	}
	
	@GetMapping("/token")
	public ResponseEntity<String> getToken() {
		com.lacunasoftware.restpki.Authentication 
		auth = new com.lacunasoftware.restpki.Authentication(Funcoes.getRestPkiClient());
		
		String token = "";
		//String token = auth.startWithWebPki(SecurityContext.pkiBrazil);
		try {
			token = auth.startWithWebPki(new SecurityContext("803517ad-3bbc-4169-b085-60053a8f6dbf"));
		} catch (RestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("token", token);
		
		return ResponseEntity.ok().body(jsonObject.toString());
	}
	
	
}
