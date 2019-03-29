package com.sif.core.utils;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sif.model.Log;
import com.sif.model.Usuario;
import com.sif.repository.UsuarioRepository;

@Service
public class SifLogUtil {

	private String url;
	private String entidade;
	
	private Usuario usuarioSessao;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	private HttpServletRequest request;
	
	public SifLogUtil(HttpServletRequest request) {
		this.request = request;
	}
	
	public SifLogUtil withURL(String url) {
		this.url = url;
		return this;
	}
	
	public SifLogUtil withEntidade(String entidade) {
		this.entidade = entidade;
		return this;
	}
	
	public Log fromValues(
			String campo, String oldValue, String newValue, Date datAlteracao) {
		if(oldValue == null || oldValue.isEmpty())
			oldValue = "_";
		if(newValue == null || newValue.isEmpty())
			newValue = "_";
		if(oldValue.equals(newValue))
			return null;
		
		Usuario usuario;
		if(usuarioSessao != null) {
			usuario = usuarioSessao;
		}
		else {
			usuario = funcoes.getLoggedUser();
		}
		
		Log log = new Log();
		log.setUrl(url);
		log.setEntidade(entidade);
		log.setCampoMudado(campo);
		log.setUsuario(usuario); //TODO Buscar idUsuario da sessao
		log.setIp(request.getRemoteAddr()); //TODO Buscar ip da sessao
		log.setValorAntigo(oldValue);
		log.setValorNovo(newValue);
		log.setDataLog(datAlteracao);
		return log;
	}
	
	public Log fromValues(
			String campo, String oldValue, String newValue, Date datAlteracao, Long idUsuario) {
		if(oldValue == null || oldValue.isEmpty())
			oldValue = "_";
		if(newValue == null || newValue.isEmpty())
			newValue = "_";
		if(oldValue.equals(newValue))
			return null;
		
		Usuario usuario = usuarioRepository.findById(idUsuario).get();
		
		Log log = new Log();
		log.setUrl(url);
		log.setEntidade(entidade);
		log.setCampoMudado(campo);
		log.setUsuario(usuario); //TODO Buscar idUsuario da sessao
		log.setIp(request.getRemoteAddr()); //TODO Buscar ip da sessao
		log.setValorAntigo(oldValue);
		log.setValorNovo(newValue);
		log.setDataLog(datAlteracao);
		return log;
	}
	
	public void setUsuarioSessao(Usuario usuario) {
		usuarioSessao = usuario;
	}
}
