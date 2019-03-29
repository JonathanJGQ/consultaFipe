package com.sif.core.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sif.model.Usuario;
import com.sif.model.UsuarioPermissao;
import com.sif.repository.PerfilRepository;
import com.sif.repository.UsuarioPermissaoRepository;
import com.sif.repository.UsuarioRepository;

@Service
@Transactional
public class InternalUserDetailsService implements UserDetailsService{
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Autowired
	private PerfilRepository perfilRepository;
	
	@Autowired
	private UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	@Autowired
    public InternalUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
	
	@Override 
	public UserPrincipal loadUserByUsername(String documento) throws UsernameNotFoundException {
		documento = documento.replace(".", "");
		documento = documento.replace("-", "");
        Usuario user = usuarioRepository.loginByDocumento(documento);
        if (user == null) {
        	throw new UsernameNotFoundException("Usuário não encontrado");
        }
        boolean enabled = true;
        boolean accountNonExpired = true;
        boolean credentialsNotExpired = true;
        boolean accountNonLocked = true;
        UserPrincipal principal = new UserPrincipal(
                user,
                enabled, accountNonExpired, credentialsNotExpired, accountNonLocked,
                getAuthorities(user)
        );
        
        
        return principal;
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
	
}
