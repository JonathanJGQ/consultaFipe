package com.sif.core.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Usuario;
import com.sif.model.custom.ConsignatariaCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.specification.ConsignatariaSpecification;

@Service
public class ConsignatariaService {

	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	TicketService ticketService;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Consignataria> getAll(Pageable pageable, Consignataria consignataria) {
		ConsignatariaSpecification spec = new ConsignatariaSpecification();
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
//		List<ConsignatariaDTO> listaRetorno = new ArrayList<ConsignatariaDTO>();
		Page<Consignataria> lista = null;
		
		if(usuarioLogado.getPerfil().getId() == TipoPerfil.SUPREMO
				|| usuarioLogado.getPerfil().getId() == TipoPerfil.ADMINISTRADOR) {
			
			lista = consignatariaRepository.findAll(Specification.where(
					spec.idEquals(consignataria.getId() != null ? consignataria.getId().toString() : null))
				.and(spec.nomeLike(consignataria.getNome()))
				.and(spec.averbadoraEquals(consignataria.getAverbadora() != null ? consignataria.getAverbadora().getId() : null))
				.and(spec.emailLike(consignataria.getEmail()))
				.and(spec.cnpjLike(consignataria.getCnpj()))
				.and(spec.findByStatus())
				.and(spec.orderByNome()), pageable);
		} else {
			if(usuarioLogado.getPerfil().getId() == TipoPerfil.ORGAO) {
//				List<Averbadora> averbadoras = averbadoraService.findAverbadoraByOrgao(usuarioLogado.getEntidade());
//
//				List<Consignataria> consignatarias = new ArrayList<Consignataria>();
//				ConsignatariaSpecification consigSpec = new ConsignatariaSpecification();
//				for(Averbadora averbadora : averbadoras) {
//					
//					consignatarias.addAll(consignatariaRepository.findAll(Specification.where(
//						consigSpec.findByAverbadoraOrgao(usuarioLogado.getEntidade())
//					)));
//					
//				}
				ConsignatariaSpecification consigSpec = new ConsignatariaSpecification();
				lista = consignatariaRepository.findAll(Specification.where(
						consigSpec.findByAverbadoraOrgao(usuarioLogado.getEntidade()))
						.and(consigSpec.findByStatus())
						.and(consigSpec.orderByIdDESC()),pageable);
				
			} else if(usuarioLogado.getPerfil().getId() == TipoPerfil.AVERBADORA) {
				
				ConsignatariaSpecification consigSpec = new ConsignatariaSpecification();
				Page<Consignataria> consignatarias = consignatariaRepository.findAll(Specification.where(
							consigSpec.averbadoraEquals(usuarioLogado.getEntidade())
						)
						.and(consigSpec.findByStatus())
						.and(consigSpec.orderByIdDESC()),pageable);;

				lista = consignatarias;
			} else if(usuarioLogado.getPerfil().getId() == TipoPerfil.CONSIGNATARIA) {
				
				Consignataria consignatariaUnica = consignatariaService.findConsignatariaById(usuarioLogado.getEntidade());
				
				lista = new PageImpl<Consignataria>(Arrays.asList(consignatariaUnica));
			}
		}
		
//		for(Consignataria objeto : lista) {
//			ConsignatariaDTO entity = new ConsignatariaDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setAverbadora(objeto.getAverbadora().getNome());
//			entity.setEmail(objeto.getEmail());
//			entity.setCnpj(objeto.getCnpj());
//			entity.setBloqueado(objeto.getStatus() == 2 ? true : false);
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public Consignataria findByCNPJ(String cnpj) {
		
		if(cnpj == null || cnpj.isEmpty()) {
			return null;
		}
		
		ConsignatariaSpecification spec = new ConsignatariaSpecification();
		List<Consignataria> consigs = consignatariaRepository.findAll(Specification.where(
					spec.cnpjEquals(cnpj)
			));
		
		if(consigs == null || consigs.isEmpty()) {
			return null;
		}
		
		return consigs.get(0);
		
	}
	
	public Consignataria findAtivaByCNPJ(String cnpj) {
		
		if(cnpj == null || cnpj.isEmpty()) {
			return null;
		}
		
		ConsignatariaSpecification spec = new ConsignatariaSpecification();
		List<Consignataria> consigs = consignatariaRepository.findAll(Specification.where(
					spec.cnpjAtivaEquals(cnpj)
			));
		
		if(consigs == null || consigs.isEmpty()) {
			return null;
		}
		
		return consigs.get(0);
		
	}
	
	public Consignataria findAtivaByEmail(String email) {
		
		if(email == null || email.isEmpty()) {
			return null;
		}
		
		ConsignatariaSpecification spec = new ConsignatariaSpecification();
		List<Consignataria> consigs = consignatariaRepository.findAll(Specification.where(
					spec.emailEqual(email)
			));
		
		if(consigs == null || consigs.isEmpty()) {
			return null;
		}
		
		return consigs.get(0);
		
	}
	
	
	public ResponseEntity<ConsignatariaCustomDTO> findById(Long id) {
		
		ConsignatariaCustomDTO consignatariaDto = new ConsignatariaCustomDTO();
		Consignataria consignataria = consignatariaRepository.findById(id).get();
		BeanUtils.copyProperties(consignataria, consignatariaDto);
		consignatariaDto.setEndereco(Arrays.asList(funcoes.enderecoToDTO(consignataria.getEndereco())));
		
		consignatariaDto.setAverbadora(consignataria.getAverbadora().getId());
		
		
		return Optional
				.ofNullable(consignatariaDto)
				.map(consignatariaAux -> ResponseEntity.ok().body(consignatariaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Consignataria findConsignatariaById(Long id) {
		
		if(id == null) {
			return null;
		}
		ConsignatariaSpecification consignatariaSpecification = new ConsignatariaSpecification();
		Optional<Consignataria> optional = consignatariaRepository.findOne(Specification.where(
			consignatariaSpecification.idLongEquals(id)
		));
		
		if(optional.isPresent()) {
			return optional.get();
		}
		
		return null;
	}
	
	public List<Consignataria> findConsignatariaByAverbadora(Long id) {
		
		if(id == null) {
			return null;
		}
		
		ConsignatariaSpecification consignatariaSpecification = new ConsignatariaSpecification();
		List<Consignataria> consigs = consignatariaRepository.findAll(Specification.where(
			consignatariaSpecification.averbadoraEquals(id)
		).and(consignatariaSpecification.findByStatus()));
		
		
		return consigs;
	}
	
	public Consignataria save(Consignataria consignataria) {
		return consignatariaRepository.save(consignataria);
	}
	
	public ResponseEntity<Consignataria> create(ConsignatariaCustomDTO consignatariaDto) {
		
		if(consignatariaDto.getEndereco() == null || consignatariaDto.getEndereco().isEmpty()) {
			throw new GenericException("Erro","O Endereço não pode estar vazio");
		} else {
			if(consignatariaDto.getEndereco().get(0).getCep() == null) {
				throw new GenericException("Erro","O CEP não pode estar vazio");
			}
		}
		
		if(consignatariaDto.getAverbadora() == null) {
			throw new GenericException("Erro","A averbadora não pode estar vazia");
		} else {
			Averbadora averbadora = averbadoraService.findAverbadoraById(consignatariaDto.getAverbadora());
			
			if(averbadora == null) {
				throw new GenericException("Erro","Esta averbadora não existe.");
			}
		}
		
		Consignataria consignataria = dtoToConsignataria(consignatariaDto);
		
		validarConsignataria(consignataria);
		validarCNPJConsignataria(consignataria);
		
		enderecoRepository.save(consignataria.getEndereco());
		
		Consignataria consigSave = consignatariaRepository.save(consignataria);
		
		ResponseEntity<Consignataria> responseEntity = Optional
				.ofNullable(consigSave)
				.map(consignatariaAux -> ResponseEntity.ok().body(consignatariaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			LogAcao logAcao = funcoes.logAcao(responseEntity.getBody().getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logConsignataria(null, consignataria, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			ticketService.createEntity(consigSave);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	public ResponseEntity<Consignataria> update(ConsignatariaCustomDTO consignatariaDto,Long id) {
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		if(usuarioLogado.getPerfil().getId() == TipoPerfil.CONSIGNATARIA) {
			throw new GenericException("Erro", "Você não pode editar consignatárias.");
		}
		
		if(consignatariaDto.getAverbadora() == null) {
			throw new GenericException("Erro","Não é possível salvar uma consignataria sem averbadora.");
		} else {
			Averbadora averbadora = averbadoraService.findAverbadoraById(consignatariaDto.getAverbadora());
			
			if(averbadora == null) {
				throw new GenericException("Erro","Esta averbadora não existe.");
			}
		}
		
		Consignataria consignataria = dtoToConsignataria(consignatariaDto);
		
		validarConsignataria(consignataria);
		
		
		Consignataria consignatariaSave = consignatariaRepository.findById(id).get();
		if(consignatariaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long consignatariaId = consignatariaSave.getId();
		consignataria.setId(consignatariaId);
		consignataria.setItsmID(consignatariaSave.getItsmID());
		validarCNPJConsignatariaEdit(consignataria);
		
		try {
			LogAcao logAcao = funcoes.logAcao(consignatariaSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logConsignataria(consignatariaSave, consignataria, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(consignataria, consignatariaSave);
		consignatariaSave.setId(consignatariaId);
		
		return Optional
				.ofNullable(consignatariaRepository.save(consignatariaSave))
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Consignataria> delete(Long id) {
		
		Consignataria consignatariaSave = consignatariaRepository.findById(id).get();
		if(consignatariaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Consignataria consignataria = new Consignataria();
		BeanUtils.copyProperties(consignatariaSave, consignataria);
		consignataria.setStatus(consignatariaSave.getStatus());
		
		consignatariaSave.setStatus(0);
		
		try {
			LogAcao logAcao = funcoes.logAcao(consignataria.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logConsignataria(consignataria, consignatariaSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ResponseEntity<Consignataria> responseEntity = Optional
				.ofNullable(consignatariaRepository.save(consignatariaSave))
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			ticketService.deleteEntity(consignatariaSave);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
		
	}
	
	private void validarConsignataria(Consignataria consignataria) throws GenericException {
		
		if(consignataria.getAverbadora() == null) {
			throw new GenericException("Erro","Averbadora não pode ser nulo");
		}
		if(consignataria.getEndereco() == null) {
			throw new GenericException("Erro","Endereço não pode ser nulo");
		}
		if(consignataria.getNome() == null || consignataria.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		if(consignataria.getMaximoParcela() == null) {
			throw new GenericException("Erro","Parcela máxima não pode ser nulo");
		}
		if(consignataria.getDiaFechamento() == null) {
			throw new GenericException("Erro","Dia de fechamento não pode ser nulo");
		}
		if(consignataria.getDiaFechamento() < 1 || consignataria.getDiaFechamento() > 31) {
			throw new GenericException("Erro","Dia fechamento tem que ser entre 1 e 31");
		}
		if(consignataria.getCnpj() == null || consignataria.getCnpj().isEmpty()) {
			throw new GenericException("Erro","CNPJ não pode estar vazio");
		}
		if(consignataria.getDiaFechamento() == null) {
			throw new GenericException("Erro","Data fechamento não pode estar vazio");
		}
		if(consignataria.getOperaComCarencia() == null) {
			throw new GenericException("Erro","Operação com carência não pode estar vazio");
		}
		if(consignataria.getMaximoParcela() == null) {
			throw new GenericException("Erro","Máximo de parcelas não pode estar vazio");
		}
		if(consignataria.getAverbadora() == null) {
			throw new GenericException("Erro","Não é possível salvar uma consignataria sem averbadora.");
		}
	}	
	
	private void validarCNPJConsignataria(Consignataria consignataria) throws GenericException {
		Consignataria consig = consignatariaService.findAtivaByCNPJ(consignataria.getCnpj());
		
		if(consig != null) {
			throw new GenericException("Erro","Já existe uma consignataria com este CNPJ");
		}
	}
	
	private void validarCNPJConsignatariaEdit(Consignataria consignataria) throws GenericException {
		Consignataria consig = consignatariaService.findAtivaByCNPJ(consignataria.getCnpj());
		
		if(consig != null) {
			if(!consig.getId().equals(consignataria.getId())) {
				throw new GenericException("Erro","Já existe uma consignataria com este CNPJ");
			}
			
		}
	}
	
	@Transactional
	public ResponseEntity<String> blockDisblock(Long id) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		
		String message = "";
		String title = "";
		
		Integer[] status = {500};
		
		Consignataria consignataria = consignatariaRepository.findById(id).get();
		if(consignataria.getStatus() == 1) {
			status[0] = 204;
			
			Consignataria consignatariaSave = new Consignataria();
			BeanUtils.copyProperties(consignataria, consignatariaSave);
			
			consignataria.setStatus(2);
			
			try {
				LogAcao logAcao = funcoes.logAcao(consignatariaSave.getId(), getDescricaoBloquear(), funcoes.getLoggedUser());
				logConsignataria(consignatariaSave, consignataria, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			consignatariaRepository.save(consignataria);
			
			List<Usuario> listaUsuario = usuarioRepository.findAll();
			for(Usuario usuario : listaUsuario) {
				if(usuario.getEntidade() != null && usuario.getEntidade().equals(consignataria.getId())) {
					usuario.setToken(null);
					usuarioRepository.save(usuario);
				}
			}
			
		} else if(consignataria.getStatus() == 2) {
			//Se não, desbloqueie
			status[0] = 205;
			
			Consignataria consignatariaSave = new Consignataria();
			BeanUtils.copyProperties(consignataria, consignatariaSave);
			
			consignataria.setStatus(1);
			
			try {
				LogAcao logAcao = funcoes.logAcao(consignatariaSave.getId(), getDescricaoDesbloquear(), funcoes.getLoggedUser());
				logConsignataria(consignatariaSave, consignataria, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			consignatariaRepository.save(consignataria);
		}
		
		if(status[0] == 500) {
			title = "Operação não realizada!";
			message = "Não foi possível realizar esta operação.";
		} else if (status[0] == 204) {
			title = "Operação realizada com sucesso!";
			message = "Bloqueado com sucesso.";
			status[0] = 200;

		} else if(status[0] == 205) {
			title = "Operação realizada com sucesso!";
			message = "Desbloqueado com sucesso.";
			status[0] = 200;
		}
		
		jsonObject.put("title", title);
		jsonObject.put("message", message);
		jsonObject.put("status", status);
		
		final Integer statusFinal = status[0];
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(orgaoAux -> ResponseEntity.status(statusFinal).body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}	
	
	private Consignataria dtoToConsignataria(ConsignatariaCustomDTO consignatariaDto) {
		
		Consignataria consignataria = new Consignataria();
		BeanUtils.copyProperties(consignatariaDto, consignataria);
		consignataria.setEndereco(funcoes.dtoToEndereco(consignatariaDto.getEndereco().get(0)));
		consignataria.setAverbadora(averbadoraRepository.findById(consignatariaDto.getAverbadora()).get());
		
		return consignataria;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_CONSIGNATARIA;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_CONSIGNATARIA;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_CONSIGNATARIA;
	}
	
	public Long getDescricaoBloquear() {
		return DescricaoLogAcaoHelper.BLOQUEAR_CONSIGNATARIA;
	}
	
	public Long getDescricaoDesbloquear() {
		return DescricaoLogAcaoHelper.DESBLOQUEAR_CONSIGNATARIA;
	}
	
	public void logConsignataria(Consignataria previous, Consignataria current, LogAcao logAcao){
		
		if(previous == null || previous.getId() == null) {
			previous = new Consignataria();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date datAlteracao = new Date();
		
		logUtil.withEntidade("Consignatária");
		
		logs.add(logUtil.fromValues("averbadora_consignataria", 
				previous.getAverbadora() == null ? "-" : previous.getAverbadora().getId().toString(), 
				current.getAverbadora() == null ? "-" : current.getAverbadora().getId().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("endereco_consignataria", 
				previous.getEndereco() == null ? "-" : previous.getEndereco().getId().toString(), 
				current.getEndereco() == null ? "-" : current.getEndereco().getId().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("nome_consignataria", 
				previous.getNome() == null ? "-" : previous.getNome(), 
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("cnpj_consignataria", 
				previous.getCnpj() == null ? "-" : previous.getCnpj(), 
				current.getCnpj() == null ? "-" : current.getCnpj(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("email_consfignataria", 
				previous.getEmail() == null ? "-" : previous.getEmail(), 
				current.getEmail() == null ? "-" : current.getEmail(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("maximo_parcela_consignataria", 
				previous.getMaximoParcela() == null ? "-" : previous.getMaximoParcela().toString(), 
				current.getMaximoParcela() == null ? "-" : current.getMaximoParcela().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("maximo_parcela_compra_consignataria", 
				previous.getMaximoParcelaCompra() == null ? "-" : previous.getMaximoParcelaCompra().toString(), 
				current.getMaximoParcelaCompra() == null ? "-" : current.getMaximoParcelaCompra().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("minimo_parcela_pagas_consignataria", 
				previous.getMinimoParcelaPagas() == null ? "-" : previous.getMinimoParcelaPagas().toString(), 
				current.getMinimoParcelaPagas() == null ? "-" : current.getMinimoParcelaPagas().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("dia_fechamento_consignataria", 
				previous.getDiaFechamento() == null ? "-" : previous.getDiaFechamento().toString(), 
				current.getDiaFechamento() == null ? "-" : current.getDiaFechamento().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("opera_com_carencia", 
				previous.getOperaComCarencia() == null ? "-" : previous.getOperaComCarencia().toString(), 
				current.getOperaComCarencia() == null ? "-" : current.getOperaComCarencia().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_consignataria", 
				previous.getStatus() == null ? "-" : previous.getStatus().toString(), 
				current.getStatus() == null ? "-" : current.getStatus().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("itsm_id", 
				previous.getItsmID() == null ? "-" : previous.getItsmID(), 
				current.getItsmID() == null ? "-" : current.getItsmID(),
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

	public Consignataria findByCodigo(String codigoConsignataria) {
		Consignataria consignataria = consignatariaRepository.findByCodigo(codigoConsignataria);
		if(consignataria != null) {
			return consignataria;
		}
		return null;
	}
}
