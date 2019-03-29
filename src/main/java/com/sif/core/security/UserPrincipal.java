package com.sif.core.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.sif.model.Usuario;



public class UserPrincipal extends User{
	
	private final Usuario user;
	
	public UserPrincipal(Usuario user, Collection<? extends GrantedAuthority> authorities) {
        super(user.getDocumento(), user.getSenha(), authorities);
        this.user = user;
    }
	
	public UserPrincipal(Usuario user, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(user.getDocumento(), user.getSenha(),
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.user = user;
    }
	
	public Usuario getUser() {
        return this.user;
    }
}
