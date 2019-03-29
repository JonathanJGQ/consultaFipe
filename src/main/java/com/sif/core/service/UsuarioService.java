package com.sif.core.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sif.core.exception.GenericException;
import com.sif.core.security.TokenAuthenticationService;
import com.sif.core.utils.ConfiguracaoHelper;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Administracao;
import com.sif.model.Averbadora;
import com.sif.model.Configuracao;
import com.sif.model.Consignataria;
import com.sif.model.Endereco;
import com.sif.model.Funcionalidade;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Orgao;
import com.sif.model.PasswordResetToken;
import com.sif.model.Perfil;
import com.sif.model.PerfilFuncionalidade;
import com.sif.model.TipoUsuario;
import com.sif.model.Usuario;
import com.sif.model.UsuarioPermissao;
import com.sif.model.custom.EnderecoCustomDTO;
import com.sif.model.custom.PasswordTokenDTO;
import com.sif.model.custom.PermissoesFuncionalidade;
import com.sif.model.custom.UsuarioCustomDTO;
import com.sif.model.list.UsuarioDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.CidadeRepository;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.FuncionalidadeRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.PasswordResetTokenRepository;
import com.sif.repository.PerfilFuncionalidadeRepository;
import com.sif.repository.PerfilRepository;
import com.sif.repository.TipoUsuarioRepository;
import com.sif.repository.UsuarioPermissaoRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.specification.UsuarioSpecification;

import io.jsonwebtoken.Jwts;

@Service
public class UsuarioService {

	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	CidadeRepository cidadeRepository;
	
	@Autowired
	PerfilRepository perfilRepository;
	
	@Autowired
	TipoUsuarioRepository tipoUsuarioRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	AdministracaoService administracaoService;
	
	@Autowired
	OrgaoService orgaoService;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	@Autowired
	PasswordResetTokenRepository passwordResetTokenRepository;
	
	@Autowired
	ServletContext servletContext;
	
	@Autowired
	PerfilFuncionalidadeRepository perfilFuncionalidadeRepository;
	
	@Autowired
    public JavaMailSender emailSender;
	
	@Autowired
	TicketService ticketService;
	
	@Autowired
	FuncionalidadeRepository funcionalidadeRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	UserLoginHistoryService userLoginHistoryService;
	
	@Autowired
	UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	@Autowired
	@Qualifier("sessionRegistry")
	private SessionRegistry sessionRegistry;
	
	public Page<UsuarioDTO> getAll(Pageable pageable, Usuario usuario) {
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		UsuarioSpecification spec = new UsuarioSpecification();
		
		List<UsuarioDTO> listaRetorno = new ArrayList<UsuarioDTO>();
		List<Usuario> lista = new ArrayList<Usuario>();
		
		if(usuarioLogado.getPerfil().getId() == TipoPerfil.SUPREMO
			|| usuarioLogado.getPerfil().getId() == TipoPerfil.ADMINISTRADOR) {
			lista = usuarioRepository.findAll(Specification.where(
					spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
				.and(spec.nomeLike(usuario.getNome()))
				.and(spec.emailLike(usuario.getEmail()))
				.and(spec.documentoLike(usuario.getDocumento()))
				.and(spec.findByStatus())
				.and(spec.orderByIdDESC()));
		} else {
			
			if(usuarioLogado.getPerfil().getId() == TipoPerfil.ORGAO) {
				List<Averbadora> averbadoras = averbadoraService.findAverbadoraByOrgao(usuarioLogado.getEntidade());
				List<Usuario> listaUsuariosAverbadoras = new ArrayList<Usuario>();
				
				List<Consignataria> consignatarias = new ArrayList<Consignataria>();
				List<Usuario> listaUsuariosConsignatarias = new ArrayList<Usuario>();
				
				for(Averbadora averbadora : averbadoras) {
					
					listaUsuariosAverbadoras.addAll(usuarioRepository.findAll(Specification.where(
							spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
						.and(spec.nomeLike(usuario.getNome()))
						.and(spec.emailLike(usuario.getEmail()))
						.and(spec.documentoLike(usuario.getDocumento()))
						.and(spec.perfilEntidadeEqual(TipoPerfil.AVERBADORA, averbadora.getId()))
						.and(spec.findByStatus())
						.and(spec.orderByIdDESC())));
					
					consignatarias.addAll(consignatariaService.findConsignatariaByAverbadora(averbadora.getId()));
				}
				
				for(Consignataria consignataria : consignatarias) {
					listaUsuariosConsignatarias.addAll(usuarioRepository.findAll(Specification.where(
							spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
						.and(spec.nomeLike(usuario.getNome()))
						.and(spec.emailLike(usuario.getEmail()))
						.and(spec.documentoLike(usuario.getDocumento()))
						.and(spec.perfilEntidadeEqual(TipoPerfil.CONSIGNATARIA, consignataria.getId()))
						.and(spec.findByStatus())
						.and(spec.orderByIdDESC())));
				}
				
				lista = usuarioRepository.findAll(Specification.where(
						spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
					.and(spec.nomeLike(usuario.getNome()))
					.and(spec.emailLike(usuario.getEmail()))
					.and(spec.documentoLike(usuario.getDocumento()))
					.and(spec.perfilEntidadeEqual(usuarioLogado.getPerfil().getId(), usuarioLogado.getEntidade()))
					.and(spec.findByStatus())
					.and(spec.orderByIdDESC()));
				
				lista.addAll(listaUsuariosAverbadoras);
				lista.addAll(listaUsuariosConsignatarias);
				
			} else if(usuarioLogado.getPerfil().getId() == TipoPerfil.AVERBADORA) {
				
				List<Consignataria> consignatarias = consignatariaService.findConsignatariaByAverbadora(usuarioLogado.getEntidade());
				List<Usuario> listaUsuariosConsignatarias = new ArrayList<Usuario>();
				
				for(Consignataria consignataria : consignatarias) {
					listaUsuariosConsignatarias.addAll(usuarioRepository.findAll(Specification.where(
							spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
						.and(spec.nomeLike(usuario.getNome()))
						.and(spec.emailLike(usuario.getEmail()))
						.and(spec.documentoLike(usuario.getDocumento()))
						.and(spec.perfilEntidadeEqual(TipoPerfil.CONSIGNATARIA, consignataria.getId()))
						.and(spec.findByStatus())
						.and(spec.orderByIdDESC())));
				}
				
				lista = usuarioRepository.findAll(Specification.where(
						spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
					.and(spec.nomeLike(usuario.getNome()))
					.and(spec.emailLike(usuario.getEmail()))
					.and(spec.documentoLike(usuario.getDocumento()))
					.and(spec.perfilEntidadeEqual(usuarioLogado.getPerfil().getId(), usuarioLogado.getEntidade()))
					.and(spec.findByStatus())
					.and(spec.orderByIdDESC()));
				
				lista.addAll(listaUsuariosConsignatarias);
				
			} else {
				
				lista = usuarioRepository.findAll(Specification.where(
						spec.idEquals(usuario.getId() != null ? usuario.getId().toString() : null))
					.and(spec.nomeLike(usuario.getNome()))
					.and(spec.emailLike(usuario.getEmail()))
					.and(spec.documentoLike(usuario.getDocumento()))
					.and(spec.perfilEntidadeEqual(usuarioLogado.getPerfil().getId(), usuarioLogado.getEntidade()))
					.and(spec.findByStatus())
					.and(spec.orderByIdDESC()));
				
			}
			
		}
		
		for(Usuario objeto : lista) {
			
//			if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.ORGAO)){
//				if(!objeto.getPerfil().getId().equals(PerfilHelper.ORGAO)
//					|| !objeto.getPerfil().getId().equals(PerfilHelper.AVERBADORA)
//					|| !objeto.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
//					continue;
//				}
//			}
//			if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.AVERBADORA)){
//				if(!objeto.getPerfil().getId().equals(PerfilHelper.AVERBADORA)
//					|| !objeto.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
//					continue;
//				}
//			}
//			if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)){
//				if(!objeto.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
//					continue;
//				}
//			}
			
			UsuarioDTO entity = new UsuarioDTO();
			entity.setId(objeto.getId());
			entity.setNome(objeto.getNome());
			entity.setEmail(objeto.getEmail());
			entity.setDocumento(objeto.getDocumento());
			entity.setLoggedIn(usuarioLogado(objeto));
			entity.setBloqueado(objeto.getStatus() == 2 ? true : false);
			entity.setStatus(objeto.getStatus());
			
			listaRetorno.add(entity);
		}
		
		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return new PageImpl<UsuarioDTO>(listaRetorno.subList(start, end), pageable, listaRetorno.size());
	}
	
	public ResponseEntity<UsuarioCustomDTO> findById(Long id) {
		
		Usuario usuarioSalvo = usuarioRepository.findOne(Specification.where(new UsuarioSpecification().idLongEquals(id))).orElse(null);
		
		if(usuarioSalvo == null) {
			throw new GenericException("Erro", "Usuário não encontrado");
		}
		
		UsuarioCustomDTO usuarioCustom = new UsuarioCustomDTO();
		EnderecoCustomDTO enderecoCustom = new EnderecoCustomDTO();
		
		enderecoCustom = funcoes.enderecoToDTO(usuarioSalvo.getEndereco());
		
		usuarioCustom.setId(usuarioSalvo.getId());
		usuarioCustom.setTipo_usuario(usuarioSalvo.getTipoUsuario().getId());
		usuarioCustom.setPerfil(usuarioSalvo.getPerfil().getId());
		usuarioCustom.setEntidade(usuarioSalvo.getEntidade());
		usuarioCustom.setNome(usuarioSalvo.getNome());
		usuarioCustom.setDocumento(usuarioSalvo.getDocumento());
		usuarioCustom.setMatricula(usuarioSalvo.getMatricula());
		usuarioCustom.setSexo(usuarioSalvo.getSexo());
		usuarioCustom.setDataNascimento(usuarioSalvo.getDataNascimento());
		usuarioCustom.setDdd(usuarioSalvo.getDdd());
		usuarioCustom.setCelular(usuarioSalvo.getCelular());
		usuarioCustom.setStatus(usuarioSalvo.getStatus());
		usuarioCustom.setEndereco(Arrays.asList(enderecoCustom));
		usuarioCustom.setSenha(usuarioSalvo.getSenha());
		usuarioCustom.setEmail(usuarioSalvo.getEmail());
		
		return Optional
				.ofNullable(usuarioCustom)
				.map(usuario -> ResponseEntity.ok().body(usuario))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Usuario findUsuarioById(Long id) {
		
		UsuarioSpecification usuarioSpecification = new UsuarioSpecification();
		
		Optional<Usuario> optional = usuarioRepository.findOne(Specification.where(
				usuarioSpecification.idLongEquals(id)
			).and(usuarioSpecification.findByStatus())
		);
		
		if(optional.isPresent()) {
			return optional.get();
		}
		
		return null;
	}
	
	@Transactional
	public ResponseEntity<Usuario> create(UsuarioCustomDTO usuarioDto) {
		
		Usuario usuario = dtoToUsuario(usuarioDto);
		usuario.setErrosUsuario(0);
		
		//usuario.setEndereco(enderecoRepository.save(usuario.getEndereco()));
		
		validarUsuario(usuario);
		validarEmailCreate(usuario);
		validarDocUsuarioCreate(usuario);
		
		Usuario usuarioExiste = usuarioRepository.findDeletedUser(usuario.getDocumento());
		if(usuarioExiste != null) {
			usuario.setId(usuarioExiste.getId());
		}
		
		enderecoRepository.save(usuario.getEndereco());
		
		if(usuario.getSenha() == null || usuario.getSenha().isEmpty()) {
			throw new GenericException("Erro","Senha não pode ser nulo");
		}
		
		Usuario usuarioSaved = usuarioRepository.save(usuario);
		ResponseEntity<Usuario> responseEntity = Optional
				.ofNullable(usuarioSaved)
				.map(usuarioAux -> ResponseEntity.ok().body(usuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			ticketService.createUsuario(usuarioSaved);
			 LogAcao logAcao = funcoes.logAcao(usuarioSaved.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logUsuario(null, usuario, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return responseEntity;
		
	}
	
	public ResponseEntity<Usuario> update(UsuarioCustomDTO usuarioDto,Long id) {
		
		Usuario usuario = dtoToUsuario(usuarioDto);
		
		//Mesmo usuario que esta editando
		if(funcoes.getLoggedUser().getId() == id) {
			if(!usuario.getDocumento().equals(funcoes.getLoggedUser().getDocumento())) {
				throw new GenericException("Erro", "O usuario não pode alterar o próprio CPF.");
			}
		}
		
		validarUsuario(usuario);
		validarPerfilUpdate(usuario);
		validarTipoUsuarioUpdate(usuario);
		
		Usuario usuarioSave = usuarioRepository.findById(id).get();
		if(usuarioSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		usuario.setLastFingerPrint(usuarioSave.getLastFingerPrint());
		usuario.setErrosUsuario(usuarioSave.getErrosUsuario());
		usuario.setToken(usuarioSave.getToken());
		usuario.setItsmID(usuarioSave.getItsmID());
		
		Long idUsuario = usuarioSave.getId();
		usuario.setId(usuarioSave.getId());
		validarEmailEdit(usuario);
		
		if(usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
			try {
				LogAcao logAcao = funcoes.logAcao(usuarioSave.getId(), getDescricaoAlterarSenha(), funcoes.getLoggedUser());
				logUsuarioSenha(usuarioSave, usuario, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			usuario.setSenha(usuarioSave.getSenha());
		}
		
		usuario.setToken(usuarioSave.getToken());
		
		try {
			LogAcao logAcao = funcoes.logAcao(usuarioSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logUsuario(usuarioSave, usuario, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(usuario, usuarioSave);
		usuarioSave.setId(idUsuario);
		
		return Optional
				.ofNullable(usuarioRepository.save(usuarioSave))
				.map(usuarioAux -> ResponseEntity.ok().body(usuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	//Validando se o perfil é o mesmo do momento da criacao
	//OBS: apenas usado na edicao;
	private void validarPerfilUpdate(Usuario usuario) {
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		//Novo Perfil
		Perfil novoPerfil = usuario.getPerfil();
		
		//Antigo Perfil
		Perfil perfilAntigo = usuarioLogado.getPerfil();
		
		//Se for o mesmo usuario
		if(usuario.getId() == usuarioLogado.getId()) {
			//Comparando
			if(!novoPerfil.getId().equals(perfilAntigo.getId())) {
				throw new GenericException("Erro", "Você não pode alterar o perfil do usuário");
			}
		} else {
			if(!usuarioLogado.getPerfil().getId().equals(TipoPerfil.SUPREMO)) {
				if(usuarioLogado.getPerfil().getId().equals(TipoPerfil.ADMINISTRADOR)) {
					if(novoPerfil.getId().equals(TipoPerfil.SUPREMO)) {
						throw new GenericException("Erro", "Um administrador não pode alterar o perfil do usuário para Supremo!");
					}
				} else {
					throw new GenericException("Erro", "Você não pode alterar o perfil do usuário");
				}
			}
		}
		
	}
	
	//Validando se o tipo de usuario é o mesmo do momento da criacao
	//OBS: apenas usado na edicao;
	private void validarTipoUsuarioUpdate(Usuario usuario) {
		
		//Novo Tipo
		TipoUsuario novoTipo = usuario.getTipoUsuario();
		
		//Antigo Tipo
		TipoUsuario tipoAntigo = funcoes.getLoggedUser().getTipoUsuario();
		
		if(!novoTipo.getId().equals(tipoAntigo.getId())) {
			throw new GenericException("Erro", "Você não pode alterar o tipo do usuário");
		}
	}
	
	public ResponseEntity<Usuario> delete(Long id) {
		
		Usuario usuarioSave = usuarioRepository.findById(id).get();
		if(usuarioSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		if(usuarioSave.getId().equals(funcoes.getLoggedUser().getId())) {
			throw new GenericException("Erro","Usuário não pode se remover!");
		}
		
		Usuario usuario = new Usuario();
		BeanUtils.copyProperties(usuarioSave, usuario);
		usuario.setStatus(usuarioSave.getStatus());
	
		usuarioSave.setStatus(0);
		
		ResponseEntity<Usuario> responseEntity = Optional
				.ofNullable(usuarioRepository.save(usuarioSave))
				.map(usuarioAux -> ResponseEntity.ok().body(usuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());

		try {
			LogAcao logAcao = funcoes.logAcao(responseEntity.getBody().getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logUsuario(usuario, usuarioSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			ticketService.deleteUsuario(usuarioSave);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	public ResponseEntity<Usuario> edit(Usuario usuario) {
		
		if(usuario == null) {
			return ResponseEntity.notFound().build();
		}
		
		return Optional
				.ofNullable(usuarioRepository.save(usuario))
				.map(usuarioAux -> ResponseEntity.ok().body(usuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Usuario findByDocumento(String documento) {
		
		Usuario usuarioSave = usuarioRepository.findByDocumento(documento);
		if(usuarioSave == null) {
			return null;
		}
		
		return usuarioSave;
	}
	
	public List<Usuario> findByPerfilEntidade(Long idPerfil, Long idEntidade) {
		
		UsuarioSpecification usuarioSpecification = new UsuarioSpecification();
		
		return usuarioRepository.findAll(Specification.where(
				usuarioSpecification.perfilEntidadeEqual(idPerfil, idEntidade)
			));		
	}
	
	public List<Usuario> findByPerfil(Long idPerfil) {
		
		UsuarioSpecification usuarioSpecification = new UsuarioSpecification();
		
		return usuarioRepository.findAll(Specification.where(
				usuarioSpecification.perfilEqual(idPerfil)
			));		
	}
	
	public List<Usuario> findAll() {
		
		UsuarioSpecification usuarioSpecification = new UsuarioSpecification();
		
		return usuarioRepository.findAll(Specification.where(
				usuarioSpecification.findByStatus()
			));		
	}
	
	public Usuario findByITSMID(String itsmID) {
		
		UsuarioSpecification userSpec = new UsuarioSpecification();
		
		Optional<Usuario> optional = usuarioRepository.findOne(Specification.where(
			userSpec.itsmIDEqual(itsmID)
		));
		
		if(optional.isPresent()) {
			return optional.get();
		}
		
		return null;
	}
	
	private Usuario dtoToUsuario(UsuarioCustomDTO usuarioDto) {
		Usuario usuario = new Usuario();
		
		usuario.setCelular(usuarioDto.getCelular());
		usuario.setDataNascimento(usuarioDto.getDataNascimento());
		usuario.setDdd(usuarioDto.getDdd());
		usuario.setDocumento(usuarioDto.getDocumento());
		usuario.setEmail(usuarioDto.getEmail());
		
		Endereco endereco = new Endereco();
		endereco = funcoes.dtoToEndereco(usuarioDto.getEndereco().get(0));
		
		usuario.setEndereco(endereco);
		usuario.setEntidade(usuarioDto.getEntidade());
		usuario.setId(usuarioDto.getId());
		usuario.setMatricula(usuarioDto.getMatricula());
		usuario.setNome(usuarioDto.getNome());
		usuario.setPerfil(perfilRepository.findById(usuarioDto.getPerfil()).get());
		
		if(usuario.getId() == null) {
			BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
			usuario.setSenha(encoder.encode(usuarioDto.getSenha()));
		}
		else {
			if(usuarioDto.getSenha() != null && !usuarioDto.getSenha().isEmpty()) {
				BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
				usuario.setSenha(encoder.encode(usuarioDto.getSenha()));
			}
			else {
				usuario.setSenha(usuarioDto.getSenha());
			}
		}
		usuario.setSexo(usuarioDto.getSexo());
		usuario.setStatus(1);
		usuario.setTipoUsuario(tipoUsuarioRepository.findById(usuarioDto.getTipoUsuario()).get());
		//usuario.token
		
		return usuario;
	}
	
	private void validarUsuario(Usuario usuario) throws GenericException {
		
		if(usuario.getEndereco() == null) {
			throw new GenericException("Erro","Endereço não pode ser nulo");
		}
		if(usuario.getTipoUsuario() == null) {
			throw new GenericException("Erro","Tipo Usuário não pode ser nulo");
		}
		if(usuario.getPerfil() == null) {
			throw new GenericException("Erro","Perfil não pode ser nulo");
		}
		if(usuario.getPerfil().getId() != 1L && usuario.getEntidade() == null) {
			
			if(usuario.getPerfil().getId() == TipoPerfil.ADMINISTRADOR) {
				Administracao administracao = administracaoService.findAdministracaoById(usuario.getEntidade());			
				if(administracao == null) {
					throw new GenericException("Erro","Administração não pode ser nulo ou não existe");
				}
			}
			
			if(usuario.getPerfil().getId() == TipoPerfil.ORGAO) {
				Orgao orgao = orgaoService.findOrgaoById(usuario.getEntidade());			
				if(orgao == null) {
					throw new GenericException("Erro","Orgão não pode ser nulo ou não existe");
				}
			}
			
			if(usuario.getPerfil().getId() == TipoPerfil.CONSIGNATARIA) {
				Consignataria consignataria = consignatariaService.findConsignatariaById(usuario.getEntidade());			
				if(consignataria == null) {
					throw new GenericException("Erro","Consignatária não pode ser nulo ou não existe");
				}
			}
			
			if(usuario.getPerfil().getId() == TipoPerfil.AVERBADORA) {
				Averbadora averbadora = averbadoraService.findAverbadoraById(usuario.getEntidade());			
				if(averbadora == null) { 
					throw new GenericException("Erro","Averbadora não pode ser nulo ou não existe");
				}
			}
			
			throw new GenericException("Erro","Entidade não pode ser nulo");
		}
		
		if(usuario.getNome() == null || usuario.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		if(usuario.getDocumento() == null || usuario.getDocumento().isEmpty()) {
			
			throw new GenericException("Erro","Documento não pode ser nulo");
		}
		if(usuario.getDataNascimento() == null) {
			throw new GenericException("Erro","Data do Nascimento não pode ser nulo");
		}
		if(usuario.getEmail() == null || usuario.getEmail().isEmpty()) {
			throw new GenericException("Erro","Email não pode ser vazio");
		}
		
		
	}
	
	private void validarDocUsuarioCreate(Usuario usuario) throws GenericException {
		Usuario usuarioByDoc = findByDocumento(usuario.getDocumento());
		if(usuarioByDoc != null) {
			throw new GenericException("Erro","Já existe um usuário com este documento!");
		}
	}
	
	private void validarEmailEdit(Usuario usuario) throws GenericException {
		
		
		UsuarioSpecification spec = new UsuarioSpecification();
		Usuario usuarioByEmail = usuarioRepository.findOne(Specification.where(
				spec.emailEquals(usuario.getEmail()))).orElse(null);
		
		if(usuarioByEmail == null) {
			return;
		}
		
		if(usuario.getId() != usuarioByEmail.getId()) {
			throw new GenericException("Erro","Já existe um usuário com este email");
		}
	}
	
	private void validarEmailCreate(Usuario usuario) throws GenericException {
		
		UsuarioSpecification spec = new UsuarioSpecification();
		Usuario usuarioByEmail = usuarioRepository.findOne(Specification.where(
				spec.emailEquals(usuario.getEmail()))).orElse(null);
		
		if(usuarioByEmail != null) {
			throw new GenericException("Erro","Já existe um usuário com este email");
		}
	}
	
	@Transactional
	public ResponseEntity<String> blockDisblock(Long id) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		
		String message = "";
		String title = "";
		
		Integer status = 500;
		
		Usuario usuario = usuarioRepository.findById(id).get();
		
		if(funcoes.getLoggedUser().getId().equals(id)) {
			
			status = 206;
			
		} else if(usuario.getStatus() == 1) {
			status = 204;
			
			Usuario usuarioSave = new Usuario();
			BeanUtils.copyProperties(usuario, usuarioSave);
			usuarioSave.setStatus(usuario.getStatus());
			usuario.setStatus(2);
			usuario.setToken(null);
			usuarioRepository.save(usuario);
			
			try {
				LogAcao logAcao = funcoes.logAcao(usuarioSave.getId(), getDescricaoBloquear(), funcoes.getLoggedUser());
				logUsuario(usuarioSave, usuario, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		} else if(usuario.getStatus() == 2) {
			//Se não, desbloqueie
			status = 205;
			
			Usuario usuarioSave = new Usuario();
			BeanUtils.copyProperties(usuario, usuarioSave);
			usuarioSave.setStatus(usuario.getStatus());
			usuario.setStatus(1);
			usuarioRepository.save(usuario);
			
			try {
				LogAcao logAcao = funcoes.logAcao(usuarioSave.getId(), getDescricaoDesbloquear(), funcoes.getLoggedUser());
				logUsuario(usuarioSave, usuario, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(status == 500) {
			title = "Operação não realizada!";
			message = "Não foi possível realizar esta operação.";
		} else if (status == 204) {
			title = "Operação realizada com sucesso!";
			message = "Bloqueado com sucesso.";
			status = 200;

		} else if(status == 205) {
			title = "Operação realizada com sucesso!";
			message = "Desbloqueado com sucesso.";
			status = 200;
		} else if (status == 206) {
			title = "Operação não realizada!";
			message = "Você não pode bloquear a sí mesmo!";
		}
		
		jsonObject.put("title", title);
		jsonObject.put("message", message);
		jsonObject.put("status", status);
		
		final Integer statusFinal = status;
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(usuarioPermissaoAux -> ResponseEntity.status(statusFinal).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
				
				
	}
	
	public ResponseEntity<String> validatePasswordResetToken(long id, String token) {
		
		boolean status = true;
		
		PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
		try {
			
		    if ((resetToken == null) || (resetToken.getUsuario()
		        .getId() != id)) {
		        status = false;
		    }
		 
		    Calendar cal = Calendar.getInstance();
		    if ((resetToken.getExpiryDate()
		        .getTime() - cal.getTime()
		        .getTime()) <= 0) {
		        status = false;
		    }
	    
		}catch(Exception ex) {
			ex.printStackTrace();
			status = false;
		}
		
		if(!status) {
			
			return Optional
					.ofNullable(Funcoes.jsonMessage("Erro", "Erro ao validar Token", "500"))
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
					
		}
	    
		return Optional
				.ofNullable(Funcoes.jsonMessage("Sucesso", "Token válido", "200"))
				.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
				
	}
	
	private boolean usuarioLogado(Usuario usuario) {
		
		String token = usuario.getToken();
		
		if(token != null && !token.isEmpty()) {
			try {
				String user = Jwts.parser()
					.setSigningKey(TokenAuthenticationService.SECRET)
					.parseClaimsJws(token)
					.getBody()
					.getSubject();
				
				if(user != null) {
					return true;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	public ResponseEntity<String> logout(Long id, String fingerprint) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		
		Usuario usuario = usuarioRepository.findById(id).get();
		usuario.setToken(null);
		usuarioRepository.save(usuario);
		
		userLoginHistoryService.saveNewLogoutHistory(usuario, fingerprint);
		
		jsonObject.put("title", "Logout");
		jsonObject.put("message", "Usuario deslogado com sucesso");
		jsonObject.put("status", 200);
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(usuarioPermissaoAux -> ResponseEntity.ok().body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	public ResponseEntity<String> sendRecoverEmail(String email, HttpServletRequest request) {
		SimpleMailMessage message = new SimpleMailMessage();
		
		UsuarioSpecification spec = new UsuarioSpecification();
		
		Usuario usuarioByEmail = usuarioRepository.findOne(Specification.where(
				spec.emailEquals(email))).orElse(null);
		
		JSONObject jsonObject = new JSONObject();
		
		String title = null;
		String mensagem = null;
		Integer status = null;
		
		if(!Funcoes.validarEmail(email)) {
			title = "Erro";
			mensagem = "Usuário não encontrado";
			status = 404;
			
			jsonObject.put("title", title);
			jsonObject.put("message", mensagem);
			jsonObject.put("status", status);
			
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
			
		}	
		if(usuarioByEmail != null) {
			if(usuarioByEmail.getEmail() != null
				|| !usuarioByEmail.getEmail().equals("")) {
				
				Configuracao enderecoFront = configuracaoRepository.findById(ConfiguracaoHelper.ENDERECO_FRONTEND).get();
				
				String baseUrl = enderecoFront.getValor();
				
				String token = UUID.randomUUID().toString();
				PasswordResetToken myToken
					= createPasswordResetTokenForUser(usuarioByEmail, token);
				
				Configuracao parametroToken = configuracaoRepository.findById(ConfiguracaoHelper.VALIDADE_TOKEN_SENHA).get();
				
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.MINUTE, Integer.parseInt(parametroToken.getValor()));
				myToken.setExpiryDate(calendar.getTime());
				passwordResetTokenRepository.save(myToken);
				
				
				String url = baseUrl + "/login/change?id=" + 
						usuarioByEmail.getId() + "&token=" + token;
				
				message.setSubject("SIF - Mensagem Importante");
				message.setText(
						"Prezado,"+ "\r\n" +
						"Recebemos uma requisição para alterar a sua senha de acesso do SIF. "
						+ "Acesse o link abaixo para redefinir sua senha:"+"\r\n" +
						"\r\n" +
						url+
						"\r\n" +
						"\r\n" +
						"Caso não tenha sido você, basta ignorar esta mensagem."+ "\r\n" +
						"Por favor não responda este email."+ "\r\n" +
						"Att,"+ "\r\n" +
						"Sistema SIF."
				);
				
				Configuracao emailLogin = configuracaoRepository.findById(ConfiguracaoHelper.EMAIL_LOGIN).get();
				
		        message.setTo(usuarioByEmail.getEmail());
		        message.setFrom(emailLogin.getValor());
		      
		        
		        try {
		            emailSender.send(message);
		            
		            title = "Email Enviado";
					mensagem = "Enviado com sucesso";
					status = 200;
					
					try {
						LogAcao logAcao = funcoes.logAcao(usuarioByEmail.getId(), getDescricaoAlterarSenha(), usuarioByEmail);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
		        } catch (Exception e) {
		            e.printStackTrace();
		           
		            title = "Erro";
					mensagem = "Erro ao enviar email";
					status = 500;
		        }
				
			}
		}
		else {
			title = "Erro";
			mensagem = "Usuário não encontrado";
			status = 404;
		}
	
		jsonObject.put("title", title);
		jsonObject.put("message", mensagem);
		jsonObject.put("status", status);
		
		final Integer statusFinal = status;
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(usuarioPermissaoAux -> ResponseEntity.status(statusFinal).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public PasswordResetToken createPasswordResetTokenForUser(Usuario user, String token) {
	    PasswordResetToken myToken = new PasswordResetToken(token, user);
	    Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.MINUTE, 15);
	    myToken.setExpiryDate(calendar.getTime());
	    return myToken;
	}
	
	private void deleteTokens(Usuario usuario) {
		 List<PasswordResetToken> tokensToDelete
	    	= passwordResetTokenRepository.getTokensByUsuario(usuario.getId());
	    //Deletando tokens daquele usuário
	    if(!tokensToDelete.isEmpty()) {
	    	for(int i = 0; i < tokensToDelete.size(); i++) {
	    		passwordResetTokenRepository.delete(tokensToDelete.get(i));
	    	}
	    }
	}
	
	public ResponseEntity<String> savePassword(PasswordTokenDTO passwordTokenDTO) {
	    PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(passwordTokenDTO.getToken());
	    Usuario usuario = new Usuario();
	    
	    if(passwordResetToken != null) {
	    	
	    	if(!passwordTokenDTO.getSenha().equals(passwordTokenDTO.getConfirmSenha())) {
	    		return Optional
	    				.ofNullable(Funcoes.jsonMessage("Erro", "Senha de confirmação diferente da nova senha", "500"))
	    				.map(usuarioPermissaoAux -> ResponseEntity.status(500).body(usuarioPermissaoAux))
	    				.orElseGet(() -> ResponseEntity.notFound().build());
	    	}
	    	
	    	usuario = passwordResetToken.getUsuario();
	    	
	    	Usuario usuarioSave = new Usuario();
			BeanUtils.copyProperties(usuario, usuarioSave);
	    	
	    	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
			usuario.setSenha(encoder.encode(passwordTokenDTO.getSenha()));
	    	
		    usuarioRepository.save(usuario);
		    

		    deleteTokens(usuario);
		    
		    try {
				LogAcao logAcao = funcoes.logAcao(usuarioSave.getId(), getDescricaoRecuperarSenha(), usuario);
				logUsuario(usuarioSave, usuario, logAcao);
		    } catch (Exception e) {
				e.printStackTrace();
			}
		    
		    return Optional
    				.ofNullable(Funcoes.jsonMessage("Sucesso", "Senha alterada", "200"))
    				.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
    				.orElseGet(() -> ResponseEntity.notFound().build());
		    		
		    
	    }
	    
	    return Optional
				.ofNullable(Funcoes.jsonMessage("Erro", "Erro ao alterar senha", "500"))
				.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	    		
	    
	}
	
	public ResponseEntity<PermissoesFuncionalidade> getPermissoesPerfil(Long id) {
		
		PermissoesFuncionalidade permissoesFuncionalidade = new PermissoesFuncionalidade();
		
		Usuario usuario = usuarioRepository.findById(id).get();
		UsuarioCustomDTO usuarioCustom = new UsuarioCustomDTO();
		EnderecoCustomDTO enderecoCustom = new EnderecoCustomDTO();
		
		BeanUtils.copyProperties(usuario, usuarioCustom);
		enderecoCustom = funcoes.enderecoToDTO(usuario.getEndereco());
		usuarioCustom.setEndereco(Arrays.asList(enderecoCustom));
		
		Perfil perfilUsuario = usuario.getPerfil();
		String nomeEntidade = "Sem Entidade";
		if(perfilUsuario.getId().equals(TipoPerfil.ADMINISTRADOR)) {
			Administracao admin = administracaoService.findAdministracaoById(usuario.getEntidade());
			nomeEntidade = admin.getNome();
		} else if(perfilUsuario.getId().equals(TipoPerfil.ORGAO)) {
			Orgao orgao = orgaoService.findOrgaoById(usuario.getEntidade());
			nomeEntidade = orgao.getNome();
		} else if(perfilUsuario.getId().equals(TipoPerfil.AVERBADORA)) {
			Averbadora averbadora = averbadoraService.findAverbadoraById(usuario.getEntidade());
			nomeEntidade = averbadora.getNome();
		}  else if(perfilUsuario.getId().equals(TipoPerfil.CONSIGNATARIA)) {
			Consignataria consignataria = consignatariaService.findConsignatariaById(usuario.getEntidade());
			nomeEntidade = consignataria.getNome();
		}
		usuarioCustom.setNomeEntidade(nomeEntidade);
		
		
		List<Funcionalidade> listaFuncionalidade = new ArrayList<Funcionalidade>(); 
		
		List<PerfilFuncionalidade> listaPerfilFuncionalidade = perfilFuncionalidadeRepository.findByPerfil(usuario.getPerfil());
		
		for(PerfilFuncionalidade perfilFuncionalidade : listaPerfilFuncionalidade) {
			if(perfilFuncionalidade.getFuncionalidade().isStatus()) {
				listaFuncionalidade.add(funcionalidadeRepository.findById(perfilFuncionalidade.getFuncionalidade().getId()).get());
			}
		}
		
		List<Funcionalidade> listaFuncionalidadeUsuario = new ArrayList<Funcionalidade>(); 
		
		List<UsuarioPermissao> listaUsuarioPermissao = usuarioPermissaoRepository.findByUsuario(usuario);
		for(UsuarioPermissao permissao : listaUsuarioPermissao) {
			listaFuncionalidadeUsuario.add(permissao.getFuncionalidade());
		}
		
		List<Long> idsFuncionalidade = new ArrayList<Long>();
		for(Funcionalidade funcionalidade : listaFuncionalidadeUsuario) {
			idsFuncionalidade.add(funcionalidade.getId());
		}
		
		for(Funcionalidade funcionalidade : listaFuncionalidade) {
			if(idsFuncionalidade.contains(funcionalidade.getId())) {
					funcionalidade.setSelecionado(true);
				
			}
			else {
				funcionalidade.setSelecionado(false);
			}
		}
		
		permissoesFuncionalidade.setUsuario(usuarioCustom);
		permissoesFuncionalidade.setPermissoes(listaFuncionalidade);
		
		return Optional
				.ofNullable(permissoesFuncionalidade)
				.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
	}
	
	public ResponseEntity<String> salvarPermissoesPerfil(Long id, List<Funcionalidade> permissoes) {
		
		Usuario usuario = usuarioRepository.findById(id).get();
		
 		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		if(usuarioLogado.getId() == usuario.getId()) {
			return Optional
					.ofNullable(Funcoes.jsonMessage("Erro", "Não é permitido alterar as próprias permissões", "400"))
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		List<UsuarioPermissao> listaUsuarioPermissao = usuarioPermissaoRepository.findByUsuario(usuario);
		
		for(Funcionalidade permissao : permissoes) {
			boolean temPermissao = false;
			for(UsuarioPermissao usuarioPermissao : listaUsuarioPermissao) {
				if(permissao.getId().equals(usuarioPermissao.getFuncionalidade().getId())) {
					temPermissao = true;
					if(!permissao.isSelecionado()) {
						usuarioPermissaoRepository.delete(usuarioPermissao);
					}
				}
			}
			if(!temPermissao) {
				if(permissao.isSelecionado()) {
					UsuarioPermissao usuarioPermissao = new UsuarioPermissao();
					usuarioPermissao.setData(new Date());
					usuarioPermissao.setFuncionalidade(permissao);
					usuarioPermissao.setUsuario(usuario);
					usuarioPermissaoRepository.save(usuarioPermissao);
				}
			}
		}
		
		return Optional
				.ofNullable(Funcoes.jsonMessage("Sucesso", "Permissões salvas com sucesso", "200"))
				.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
				
	}
	
	
	public ResponseEntity<String> uploadFoto(Long id, MultipartFile file) {
		
		JSONObject jsonObject = new JSONObject();
		
		try {
			Usuario usuario = usuarioRepository.findById(id).get();
			
			Configuracao configuracaoPhoto = configuracaoRepository.findById(ConfiguracaoHelper.PATH_PHOTO_USUARIO).get();
			
			File diretorioFoto = new File(configuracaoPhoto.getValor()+File.separator+usuario.getId());
			
			if(!diretorioFoto.exists()) {
				diretorioFoto.mkdirs();
			}
			
			String[] nome = file.getOriginalFilename().split("\\.");
			File photoUsuario = new File(configuracaoPhoto.getValor()+File.separator+usuario.getId()+File.separator+usuario.getId()+"."+nome[nome.length - 1]);
			
			FileUtils.writeByteArrayToFile(photoUsuario, file.getBytes());
			
			Usuario usuarioSave = new Usuario();
			BeanUtils.copyProperties(usuario, usuarioSave);
			
			usuario.setPathFoto(photoUsuario.getAbsolutePath());
			
			jsonObject.put("title", "Salvo com sucesso");
			jsonObject.put("message", "Imagem do perfil salva com sucesso");
			jsonObject.put("Status", "200");
			
			try {
				LogAcao logAcao = funcoes.logAcao(usuario.getId(), getDescricaoInserirFoto(), funcoes.getLoggedUser());
				logUsuario(usuarioSave, usuario, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			usuarioRepository.save(usuario);
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
					
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		jsonObject.put("title", "Erro ao salvar");
		jsonObject.put("message", "Imagem do perfil não foi salva");
		jsonObject.put("Status", "500");
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}	
	
	public ResponseEntity<String> deleteFoto(String id) {
		
		JSONObject jsonObject = new JSONObject();
		
		if(id == null || id.isEmpty()) {
			jsonObject.put("title", "Foto não removida");
			jsonObject.put("message", "Id inválido");
			jsonObject.put("status", "500");
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
					
		}
		
		Usuario usuario = usuarioRepository.findById(Long.parseLong(id)).get();
		
		if(usuario == null) {
			jsonObject.put("title", "Foto não removida");
			jsonObject.put("message", "Usario não encontrato");
			jsonObject.put("status", "500");
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		if(!usuario.getId().equals(funcoes.getLoggedUser().getId())) {
			jsonObject.put("title", "Foto não removida");
			jsonObject.put("message", "Você não pode remover esta foto.");
			jsonObject.put("status", "500");
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		Usuario usuarioSave = new Usuario();
		BeanUtils.copyProperties(usuario, usuarioSave);
		
		usuario.setPathFoto(null);
		
		try {
			LogAcao logAcao = funcoes.logAcao(usuario.getId(), getDescricaoRemoverFoto(), funcoes.getLoggedUser());
			logUsuario(usuarioSave, usuario, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Usuario usuarioSaved = usuarioRepository.save(usuario);
		if(usuarioSaved == null) {
			jsonObject.put("title", "Foto não removida");
			jsonObject.put("message", "Não foi possível remover a foto do perfil");
			jsonObject.put("Status", "500");
			
			return Optional
					.ofNullable(jsonObject.toString())
					.map(usuarioPermissaoAux -> ResponseEntity.status(400).body(usuarioPermissaoAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		jsonObject.put("title", "Foto removida com sucesso");
		jsonObject.put("message", "Imagem do perfil foi removida com sucesso");
		jsonObject.put("Status", "200");
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(usuarioPermissaoAux -> ResponseEntity.status(200).body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_USUARIO;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_USUARIO;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_USUARIO;
	}
	
	public Long getDescricaoBloquear() {
		return DescricaoLogAcaoHelper.BLOQUEAR_USUARIO;
	}
	
	public Long getDescricaoDesbloquear() {
		return DescricaoLogAcaoHelper.DESBLOQUEAR_USUARIO;
	}
	
	public Long getDescricaoAlterarSenha() {
		return DescricaoLogAcaoHelper.ALTERAR_SENHA_DO_USUARIO;
	}
	
	public Long getDescricaoRecuperarSenha() {
		return DescricaoLogAcaoHelper.RECUPERACAO_DE_SENHA;
	}
	
	public Long getDescricaoInserirFoto() {
		return DescricaoLogAcaoHelper.INSERIR_FOTO_DO_PERFIL;
	}
	
	public Long getDescricaoRemoverFoto() {
		return DescricaoLogAcaoHelper.REMOVER_FOTO_DO_PERFIL;
	}
	
	public void logUsuarioSenha(Usuario previous, Usuario current, LogAcao logAcao){
		if(previous == null || previous.getId() == null) {
			previous = new Usuario();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		Date datAlteracao = new Date();
		
		logUtil.withEntidade("Usuário");
		
		logs.add(logUtil.fromValues("senha_usuario", 
				previous.getSenha() == null ? "-" : previous.getSenha(), 
				current.getSenha() == null ? "-" : current.getSenha(),
				datAlteracao
		));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log != null) {
				log.setIdRow(current.getId());
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
		}
	}
	
	public void logUsuario(Usuario previous, Usuario current, LogAcao logAcao){
		
		if(previous == null || previous.getId() == null) {
			previous = new Usuario();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		Date datAlteracao = new Date();
		
		logUtil.withEntidade("Usuário");
		
		logs.add(logUtil.fromValues("endereco_usuario", 
				previous.getEndereco() == null ? "-" : previous.getEndereco().getId().toString(), 
				current.getEndereco() == null || current.getEndereco().getId() == null ? "-" : current.getEndereco().getId().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("tipo_usuario", 
				previous.getTipoUsuario() == null ? "-" : previous.getTipoUsuario().getCodigo(), 
				current.getTipoUsuario() == null || current.getTipoUsuario().getCodigo() == null ? "-" : current.getTipoUsuario().getCodigo(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("perfil_usuario", 
				previous.getPerfil() == null ? "-" : previous.getPerfil().getId().toString(), 
				current.getPerfil() == null ? "-" : current.getPerfil().getId().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("entidade_usuario", 
				previous.getEntidade() == null ? "-" : previous.getEntidade().toString(), 
				current.getEntidade() == null ? "-" : current.getEntidade().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("nome_usuario", 
				previous.getNome() == null ? "-" : previous.getNome(), 
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("documento_usuario", 
				previous.getDocumento() == null ? "-" : previous.getDocumento(), 
				current.getDocumento() == null ? "-" : current.getDocumento(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("email_usuario", 
				previous.getEmail() == null ? "-" : previous.getEmail(), 
				current.getEmail() == null ? "-" : current.getEmail(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("matricula_usuario", 
				previous.getMatricula() == null ? "-" : previous.getMatricula(), 
				current.getMatricula() == null ? "-" : current.getMatricula(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("sexo_usuario", 
				previous.getSexo() == null ? "-" : previous.getSexo(), 
				current.getSexo() == null ? "-" : current.getSexo(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("data_nascimento_usuario", 
				previous.getDataNascimento() == null ? "-" : sdf.format(previous.getDataNascimento()), 
				current.getDataNascimento() == null ? "-" : sdf.format(current.getDataNascimento()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("ddd_usuario", 
				previous.getDdd() == null ? "-" : previous.getDdd().toString(), 
				current.getDdd() == null ? "-" : current.getDdd().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("token_usuario", 
				previous.getToken() == null ? "-" : previous.getToken(), 
				current.getToken() == null ? "-" : current.getToken(),
				datAlteracao
		));

		logs.add(logUtil.fromValues("erros_usuario_usuario", 
				previous.getErrosUsuario() == null ? "-" : previous.getErrosUsuario().toString(), 
				current.getErrosUsuario() == null ? "-" : current.getErrosUsuario().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("celular_usuario", 
				previous.getCelular() == null ? "-" : previous.getCelular(), 
				current.getCelular() == null ? "-" : current.getCelular(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_usuario", 
				previous.getStatus() == null ? "-" : previous.getStatus().toString(), 
				current.getStatus() == null ? "-" : current.getStatus().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("itsm_id", 
				previous.getItsmID() == null ? "-" : previous.getItsmID(), 
				current.getItsmID() == null ? "-" : current.getItsmID(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("path_foto_usuario", 
				previous.getPathFoto() == null ? "-" : previous.getPathFoto(), 
				current.getPathFoto() == null ? "-" : current.getPathFoto(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("last_fingerprint_usuario", 
				previous.getLastFingerPrint() == null ? "-" : previous.getLastFingerPrint(), 
				current.getLastFingerPrint() == null ? "-" : current.getLastFingerPrint(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("token_fingerprint_usuario", 
				previous.getTokenFingerPrint() == null ? "-" : previous.getTokenFingerPrint(), 
				current.getTokenFingerPrint() == null ? "-" : current.getTokenFingerPrint(),
				datAlteracao
		));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log != null) {
				log.setIdRow(current.getId());
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
		}
	}
}
