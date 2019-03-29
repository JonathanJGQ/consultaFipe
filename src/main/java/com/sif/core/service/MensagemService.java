package com.sif.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Mensagem;
import com.sif.model.MensagemDestino;
import com.sif.model.Usuario;
import com.sif.model.custom.EntidadeMensagemCustom;
import com.sif.model.custom.MensagemCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.model.utils.TipoPerfil;
import com.sif.repository.LogRepository;
import com.sif.repository.MensagemRepository;
import com.sif.repository.specification.MensagemSpecification;

@Service
public class MensagemService {

	@Autowired
	MensagemRepository mensagemRepository;
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	MensagemDestinoService mensagemDestinoService;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	@PersistenceContext
    EntityManager entityManager;
	
	public List<Mensagem> getMessagesByDestino(Long id, Usuario usuarioLogado){
		
		if(usuarioLogado != null) {
			Usuario usuario = usuarioService.findUsuarioById(id);
			
			if(usuario == null) {
				return null;
			}
			
			if(usuarioLogado.getId() != usuario.getId()) {
				return null;
			}
			
			return mensagemDestinoService.getMessagesByDestino(usuario);
		}
		
		return null;
	}
	
	public Page<MensagemDestino> getPageMessagesByDestino(Pageable pageable, Long id, Usuario usuarioLogado, MensagemCustomDTO mensagemCustomDTO){
		
		if(usuarioLogado != null) {
			Usuario usuario = usuarioService.findUsuarioById(id);
			
			if(usuario == null) {
				return null;
			}
			
			if(usuarioLogado.getId() != usuario.getId()) {
				return null;
			}
			
			return mensagemDestinoService.getPageMessagesByDestino(pageable, usuario, mensagemCustomDTO);
		}
		
		return null;
	}
	
	public Page<Mensagem> getAll(Pageable pageable, MensagemCustomDTO mensagemCustomDTO){
		
		Usuario usuario = funcoes.getLoggedUser();
		
		MensagemSpecification mensagemSpec = new MensagemSpecification();
		
		Page<Mensagem> mensagens = null;
		
		if(mensagemCustomDTO.getDataFinal() != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(mensagemCustomDTO.getDataFinal());
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			mensagemCustomDTO.setDataFinal(calendar.getTime());
		}
		
		if(usuario.getPerfil().getId() == TipoPerfil.ADMINISTRADOR ||
				usuario.getPerfil().getId() == TipoPerfil.SUPREMO) {
			mensagens = mensagemRepository.findAll(
					Specification.where(
						mensagemSpec.findByTitulo(mensagemCustomDTO.getTitulo())
					).and(mensagemSpec.afterDate(mensagemCustomDTO.getDataInicial()))
					.and(mensagemSpec.beforeDate(mensagemCustomDTO.getDataFinal()))
					.and(mensagemSpec.afterFinalDate(new Date()))
					.and(mensagemSpec.findByRemetente(mensagemCustomDTO.getRemetente()))
					.and(mensagemSpec.findByStatus())
					.and(mensagemSpec.orderByIdDESC()),pageable
				);
		} else {
			mensagens = mensagemRepository.findAll(
					Specification.where(
						mensagemSpec.findByTitulo(mensagemCustomDTO.getTitulo())
					).and(mensagemSpec.afterDate(mensagemCustomDTO.getDataInicial()))
					.and(mensagemSpec.beforeDate(mensagemCustomDTO.getDataFinal()))
					.and(mensagemSpec.findByRemetente(usuario))
					.and(mensagemSpec.findByStatus())
					.and(mensagemSpec.orderByIdDESC()),pageable
				);
		}
		
		
		for(Mensagem mensagem : mensagens) {
			MensagemDestino mensagemAux = mensagemDestinoService.getMessagesDestinoByMensagemEDestino(mensagem, funcoes.getLoggedUser());
			
			if(mensagemAux != null) {
				mensagem.setSeenByUser(mensagemAux.getLida());
			} else {
				mensagem.setSeenByUser(false);
			}
			
		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > mensagensCustomDTO.size() ? mensagensCustomDTO.size() : (start + pageable.getPageSize());
		return mensagens;
	}
	
	public Mensagem getMessageById(Long id, Usuario usuarioLogado){
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		if(usuarioLogado == null) {
			return null;
		}
		
		MensagemSpecification mensagemSpecification = new MensagemSpecification();
		
		Optional<Mensagem> optional = mensagemRepository.findOne(Specification.where(
				mensagemSpecification.findById(id)
			).and(mensagemSpecification.findByStatus())
		);
		
		if(optional.isPresent()) {
			Mensagem mensagem = optional.get();
			
			Mensagem mensagemRetorno = mensagemDestinoService.getMessagesByMensagemEDestino(mensagem, usuarioLogado);
		
			//Vendo se é destinatario
			if(mensagemRetorno != null) {
				return mensagemRetorno;
			}
			
			//Vendo se é remetente
			if(mensagem.getRemetente().getId() == usuarioLogado.getId()) {
				return mensagem;
			}
			
			return null;
		}
		
		return null;
	}
	
	public Mensagem getMessageByIdERemetente(Long id, Usuario usuarioLogado){
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		if(usuarioLogado == null) {
			return null;
		}
		
		MensagemSpecification mensagemSpecification = new MensagemSpecification();
		
		Optional<Mensagem> optional = mensagemRepository.findOne(Specification.where(
				mensagemSpecification.findById(id)
			).and(mensagemSpecification.findByStatus())
			.and(mensagemSpecification.findByRemetente(usuarioLogado.getId()))
		);
		
		if(optional.isPresent()) {
			Mensagem mensagem = optional.get();
			
			if(mensagem.getRemetente().getId() != usuarioLogado.getId()) {
				return null;
			}
			
			return mensagem;
		}
		
		return null;
	}
	
	public List<Mensagem> getMessagesByRemetente(Long id, Usuario usuarioLogado){
		
		MensagemSpecification mensagemSpecification = new MensagemSpecification();
		
		if(usuarioLogado != null) {
			Usuario usuario = usuarioService.findUsuarioById(id);
			
			if(usuarioLogado.getId() != usuario.getId()) {
				return null;
			}
			
			List<Mensagem> mensagens = mensagemRepository.findAll(Specification.where(
					mensagemSpecification.findByRemetente(usuario)
				).and(mensagemSpecification.findByStatus())
			);
			
			return mensagens;
		}
		
		return null;
	}
	
	public MensagemCustomDTO mensagemToDTO(Mensagem mensagem) {
		MensagemCustomDTO mensagemDTO = new MensagemCustomDTO();
		
		mensagemDTO.setId(mensagem.getId());
		mensagemDTO.setTitulo(mensagem.getTitulo());
		mensagemDTO.setCorpo(mensagem.getCorpo());
		mensagemDTO.setDataRegistro(mensagem.getDataRegistro());
		mensagemDTO.setDataInicial(mensagem.getDataInicial());
		mensagemDTO.setDataFinal(mensagem.getDataFinal());
		mensagemDTO.setRemetente(mensagem.getRemetente().getId());
		mensagemDTO.setRemetenteNome(mensagem.getRemetente().getNome());
		mensagemDTO.setFiltro(mensagem.getFiltro());
		
		List<MensagemDestino> mensagensDestino = mensagemDestinoService.getMensagensDestinoByMensagem(mensagem); 
	
		if(mensagensDestino != null) {
			mensagemDTO.setDestinatarios(new ArrayList<Long>());
			for(MensagemDestino mensagemDestino : mensagensDestino) {
				mensagemDTO.setLida(mensagemDestino.getLida());
				mensagemDTO.getDestinatarios().add(mensagemDestino.getDestino().getId());
			
			}
		}
		
		EntidadeMensagemCustom entidadeMensagemCustom = new EntidadeMensagemCustom();
		List<Long> administracoes = new ArrayList<Long>();
		List<Long> orgaos = new ArrayList<Long>();
		List<Long> averbadoras = new ArrayList<Long>();
		List<Long> consignatarias = new ArrayList<Long>();
		
		if(mensagem.getFiltro() != null && mensagem.getFiltro() == 1) {
			
			for(MensagemDestino mensagemDestino : mensagensDestino) {
				if(mensagemDestino.getDestino().getPerfil().getId().equals(TipoPerfil.ORGAO)) {
					
					if(!orgaos.contains(mensagemDestino.getDestino().getEntidade())) {
						orgaos.add(mensagemDestino.getDestino().getEntidade());
					}
					
				} else if (mensagemDestino.getDestino().getPerfil().getId().equals(TipoPerfil.AVERBADORA)) {
					
					if(!averbadoras.contains(mensagemDestino.getDestino().getEntidade())) {
						averbadoras.add(mensagemDestino.getDestino().getEntidade());
					}
					
				} else if(mensagemDestino.getDestino().getPerfil().getId().equals(TipoPerfil.CONSIGNATARIA)) {
				
					if(!consignatarias.contains(mensagemDestino.getDestino().getEntidade())) {
						consignatarias.add(mensagemDestino.getDestino().getEntidade());
					}
					
				} else if(mensagemDestino.getDestino().getPerfil().getId().equals(TipoPerfil.ADMINISTRADOR)) {
					
					if(!administracoes.contains(mensagemDestino.getDestino().getEntidade())) {
						administracoes.add(mensagemDestino.getDestino().getEntidade());
					}
				}
			}
			
			
		}
		
		entidadeMensagemCustom.setAdministracoes(administracoes);
		entidadeMensagemCustom.setConsignatarias(consignatarias);
		entidadeMensagemCustom.setAverbadoras(averbadoras);
		entidadeMensagemCustom.setOrgaos(orgaos);
		
		mensagemDTO.setEntidade(entidadeMensagemCustom);
		
		return mensagemDTO;
	}
	
	public Mensagem dtoToMensagem(MensagemCustomDTO mensagemDTO) {
		
		MensagemSpecification mensagemSpecification = new MensagemSpecification();
		
		Optional<Mensagem> optional = mensagemRepository.findOne(Specification.where(
				mensagemSpecification.findById(mensagemDTO.getId())
			).and(mensagemSpecification.findByStatus())	
		);
		
		if(optional.isPresent()) {
			return optional.get();
		}
		
		
		return null;
	}
	
	public Mensagem dtoToEditMensagem(MensagemCustomDTO mensagemDTO) {
		
		MensagemSpecification mensagemSpecification = new MensagemSpecification();
		
		Optional<Mensagem> optional = mensagemRepository.findOne(Specification.where(
				mensagemSpecification.findById(mensagemDTO.getId())
			).and(mensagemSpecification.findByStatus())	
		);
		
		if(optional.isPresent()) {
			Mensagem mensagem = optional.get();
			
			mensagem.setTitulo(mensagemDTO.getTitulo());
			mensagem.setCorpo(mensagemDTO.getCorpo());
			
			Calendar calendar = Calendar.getInstance();
			mensagem.setDataRegistro(calendar.getTime());
			
			mensagem.setDataInicial(mensagemDTO.getDataInicial());
			mensagem.setDataFinal(mensagemDTO.getDataFinal());
			
			Usuario usuarioRemetente = usuarioService.findUsuarioById(mensagemDTO.getRemetente());
			mensagem.setRemetente(usuarioRemetente);
			
			return mensagem;
			
		}
		
		
		return null;
	}
	
	public Mensagem dtoToNewMensagem(MensagemCustomDTO mensagemDTO) {
		
		Mensagem mensagem = new Mensagem();
		
		mensagem.setTitulo(mensagemDTO.getTitulo());
		mensagem.setCorpo(mensagemDTO.getCorpo());
		mensagem.setDataRegistro(mensagemDTO.getDataRegistro());
		mensagem.setDataInicial(mensagemDTO.getDataInicial());
		mensagem.setDataFinal(mensagemDTO.getDataFinal());
		
		Usuario usuarioRemetente = usuarioService.findUsuarioById(mensagemDTO.getRemetente());
		mensagem.setRemetente(usuarioRemetente);
		
		return mensagem;
	}
	
	public Mensagem saveNewMensagem(Mensagem mensagem, List<Long> destinatarios) {
		
		Mensagem mensagemSalva = mensagemRepository.save(mensagem);
		
		try {
			LogAcao logAcao = funcoes.logAcao(mensagemSalva.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logMensagem(null, mensagemSalva, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		List<Usuario> usuariosDestinatarios = new ArrayList<Usuario>();
		
		for(Long id : destinatarios) {
			Usuario destinatario = usuarioService.findUsuarioById(id);
			
			if(destinatario == null) {
				continue;
			}
			
			mensagemDestinoService.saveNew(mensagemSalva, destinatario);
		}
		
		return mensagem;
		
	}
	
	public Mensagem editMensagem(Mensagem mensagem) {
		
		//Tirando mensagem do contexto
		entityManager.detach(mensagem);
		
		Mensagem mensagemSalva = mensagemRepository.findById(mensagem.getId()).get();
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		mensagem.setDataRegistro(today);
		
		
		try {
			LogAcao logAcao = funcoes.logAcao(mensagemSalva.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logMensagem(mensagemSalva, mensagem, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mensagemSalva = mensagemRepository.save(mensagem);
		
		return mensagemSalva;
	}
	
	public Mensagem deleteMensagem(Mensagem mensagem) {
		
		Mensagem mensagemSave = new Mensagem();
		BeanUtils.copyProperties(mensagem, mensagemSave);
		
		mensagem.setStatus(false);
		
		try {
			LogAcao logAcao = funcoes.logAcao(mensagem.getId(), getDescricaoCancelar(), funcoes.getLoggedUser());
			logMensagem(mensagemSave, mensagem, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		List<MensagemDestino> mensagensDestino
			= mensagemDestinoService.getMensagensDestinoByMensagem(mensagem);
		
		for(MensagemDestino mensagemDestino : mensagensDestino) {
			mensagemDestinoService.delete(mensagemDestino);
		}
		
		Mensagem mensagemSalva = mensagemRepository.save(mensagem);
		
		return mensagemSalva;
		
	}
	
	public void validarMensagem(Mensagem mensagem) {
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date today = calendar.getTime();
		calendar.add(Calendar.DATE, 1);
		Date tomorow = calendar.getTime();
		
		if(mensagem.getTitulo() == null || mensagem.getTitulo().isEmpty()) {
			throw new GenericException("Erro", "O título da mensagem não pode ser vazio");
		}
		
		if(mensagem.getCorpo() == null || mensagem.getCorpo().isEmpty()) {
			throw new GenericException("Erro", "O corpo da mensagem não pode ser vazio");
		}
		
		if(mensagem.getDataRegistro() == null) {
			throw new GenericException("Erro", "A data de registro não pode ser vazia");
		}
		
		if(mensagem.getDataRegistro().before(today)) {
			throw new GenericException("Erro", "A data de registro não pode ser anterior a data de hoje");
		}
		
		if(mensagem.getDataRegistro().after(tomorow)) {
			throw new GenericException("Erro", "A data de registro não pode ser posterior a data de hoje");
		}
		
		if(mensagem.getDataInicial() == null) {
			throw new GenericException("Erro", "A data inicial não pode ser vazia");
		}
		
		if(mensagem.getDataInicial().before(today)) {
			throw new GenericException("Erro", "A data inicial não pode ser anterior a data de hoje");
		}
		
		if(mensagem.getDataFinal() == null) {
			throw new GenericException("Erro", "A data final não pode ser vazia");
		}
		
		if(mensagem.getDataFinal().before(today)) {
			throw new GenericException("Erro", "A data final não pode ser anterior a data de hoje");
		}
		
		if(mensagem.getDataFinal().before(mensagem.getDataInicial())) {
			throw new GenericException("Erro", "A data final não pode ser anterior a data inicial");
		}
		
		if(mensagem.getRemetente() == null) {
			throw new GenericException("Erro", "O remetente deve ser especificado");
		}
		
		if(mensagem.getRemetente().getId() == null) {
			throw new GenericException("Erro", "O id do remetente não deve estar vazio");
		}
		
	}
	
	public Mensagem getLastNotSeenMessage(Long idUsuario, Date dateParam) {
		return mensagemRepository.getLastNotSeenMessage(idUsuario, dateParam);
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_MENSAGEM;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_MENSAGEM;
	}

	public Long getDescricaoCancelar() {
		return DescricaoLogAcaoHelper.CANCELAR_MENSAGEM;
	}
	
	public void logMensagem(Mensagem previous, Mensagem current, LogAcao logAcao){
		
		if(previous == null || previous.getId() == null) {
			previous = new Mensagem();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date datAlteracao = new Date();

		logUtil.withEntidade("Mensagem");
		
		logs.add(logUtil.fromValues("titulo_mensagem", 
				previous.getTitulo() == null ? "-" : previous.getTitulo(), 
				current.getTitulo() == null ? "-" : current.getTitulo(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("corpo_mensagem", 
				previous.getCorpo() == null ? "-" : previous.getCorpo(), 
				current.getCorpo() == null ? "-" : current.getCorpo(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("data_inicial_mensagem", 
				previous.getDataInicial() == null ? "-" : sdf.format(previous.getDataInicial()), 
				current.getDataInicial() == null ? "-" : sdf.format(current.getDataInicial()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("data_final_mensagem", 
				previous.getDataFinal() == null ? "-" : sdf.format(previous.getDataFinal()), 
				current.getDataFinal() == null ? "-" : sdf.format(current.getDataFinal()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("data_registro_mensagem", 
				previous.getDataRegistro() == null ? "-" : sdf.format(previous.getDataRegistro()), 
				current.getDataRegistro() == null ? "-" : sdf.format(current.getDataRegistro()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_mensagem", 
				Boolean.toString(previous.isStatus()), 
				Boolean.toString(current.isStatus()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("remetente_mensagem", 
				previous.getRemetente() == null ? "-" : previous.getRemetente().getId().toString(), 
				current.getRemetente() == null ? "-" : current.getRemetente().getId().toString(),
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
