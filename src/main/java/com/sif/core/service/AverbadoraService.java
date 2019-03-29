package com.sif.core.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.model.Averbadora;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Usuario;
import com.sif.model.custom.AverbadoraCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.specification.AverbadoraSpecification;

@Service
public class AverbadoraService {

	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	TicketService ticketService;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Averbadora> getAll(Pageable pageable, Averbadora averbadora) {
		AverbadoraSpecification spec = new AverbadoraSpecification();
		
//		List<AverbadoraDTO> listaRetorno = new ArrayList<AverbadoraDTO>();
		Page<Averbadora> lista = averbadoraRepository.findAll(Specification.where(
				spec.idEquals(averbadora.getId() != null ? averbadora.getId().toString() : null))
			.and(spec.nomeLike(averbadora.getNome()))
			.and(spec.orgaoEquals(averbadora.getOrgao() != null ? averbadora.getOrgao().getId() : null))
			.and(spec.codigoLike(averbadora.getCodigo()))
			.and(spec.cnpjLike(averbadora.getCnpj()))
			.and(spec.findByStatus())
			.and(spec.orderByNome()),pageable);
		
//		for(Averbadora objeto : lista) {
//			AverbadoraDTO entity = new AverbadoraDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setOrgao(objeto.getOrgao().getNome());
//			entity.setCodigo(objeto.getCodigo());
//			entity.setBloqueado(objeto.getStatus() == 2 ? true : false);
//			entity.setCnpj(objeto.getCnpj());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<AverbadoraCustomDTO> findById(Long id) {
		
		AverbadoraCustomDTO averbadoraDto = new AverbadoraCustomDTO();
		Averbadora averbadora = averbadoraRepository.findById(id).get();
		BeanUtils.copyProperties(averbadora, averbadoraDto);
		averbadoraDto.setEndereco(Arrays.asList(funcoes.enderecoToDTO(averbadora.getEndereco())));
		averbadoraDto.setOrgao(averbadora.getOrgao().getId());
		
		return Optional
				.ofNullable(averbadoraDto)
				.map(averbadoraAux -> ResponseEntity.ok().body(averbadoraAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Averbadora findAverbadoraById(Long id) {
		
		if(id == null) {
			return  null;
		}
		
		AverbadoraSpecification averbadoraSpecification = new AverbadoraSpecification();
		List<Averbadora> averbadoras = averbadoraRepository.findAll(Specification.where(
			averbadoraSpecification.idEqualsLong(id)
		).and(averbadoraSpecification.findByStatus()));
		
		if(averbadoras != null) {
			if(!averbadoras.isEmpty()) {
				return averbadoras.get(0);
			}
		}
		
		return null;
	}
	
	public List<Averbadora> findAverbadoraByOrgao(Long id) {
		
		if(id == null) {
			return null;
		}
		
		
		AverbadoraSpecification averbadoraSpecification = new AverbadoraSpecification();
		List<Averbadora> averbadoras = averbadoraRepository.findAll(Specification.where(
			averbadoraSpecification.orgaoEquals(id)
		).and(averbadoraSpecification.findByStatus()));
		
		
		return averbadoras;
	}
	
	public ResponseEntity<Averbadora> create(AverbadoraCustomDTO averbadoraDto) {
		
		Averbadora averbadora = dtoToAverbadora(averbadoraDto);
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		averbadora.setDataCadastro(today);
		
		validarAverbadora(averbadora);
		
		enderecoRepository.save(averbadora.getEndereco());
		
		ResponseEntity<Averbadora> responseEntity = Optional
				.ofNullable(averbadoraRepository.save(averbadora))
				.map(averbadoraAux -> ResponseEntity.ok().body(averbadoraAux))
				.orElseGet(() -> ResponseEntity.notFound().build());

		try {
			LogAcao logAcao = funcoes.logAcao(responseEntity.getBody().getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logAverbadora(null, averbadora, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		try {
			ticketService.createEntity(averbadora);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	public ResponseEntity<Averbadora> update(AverbadoraCustomDTO averbadoraDto,Long id) {
		
		Averbadora averbadora = dtoToAverbadora(averbadoraDto);
		
		validarAverbadora(averbadora);
		
		Averbadora averbadoraSave = averbadoraRepository.findById(id).get();
		if(averbadoraSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long idAverbadora = averbadoraSave.getId();
		averbadora.setId(averbadoraSave.getId());
		averbadora.setItsmID(averbadoraSave.getItsmID());
		
		try {
			LogAcao logAcao = funcoes.logAcao(averbadoraSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logAverbadora(averbadoraSave, averbadora, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(averbadora, averbadoraSave);
		averbadoraSave.setId(idAverbadora);
		
		averbadora = null;
//		return Optional
//				.ofNullable(averbadoraRepository.saveAndFlush(averbadoraSave))
//				.map(averbadoraAux -> ResponseEntity.ok().body(averbadoraAux))
//				.orElseGet(() -> ResponseEntity.notFound().build());
		
		averbadoraRepository.save(averbadoraSave);
		
		return ResponseEntity.ok().body(averbadoraSave);
	}
	
	public ResponseEntity<Averbadora> delete(Long id) {
		
		Averbadora averbadoraSave = averbadoraRepository.findById(id).get();
		if(averbadoraSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		averbadoraSave.setStatus(0);
		
		Averbadora averbadora = new Averbadora();
		BeanUtils.copyProperties(averbadoraSave, averbadora);
		averbadora.setStatus(1);
		
		try {
			LogAcao logAcao = funcoes.logAcao(averbadora.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logAverbadora(averbadora, averbadoraSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		ResponseEntity<Averbadora> responseEntity = Optional
				.ofNullable(averbadoraRepository.save(averbadoraSave))
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());		
		
		try {
			ticketService.deleteEntity(averbadoraSave);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	private void validarAverbadora(Averbadora averbadora) throws GenericException {
		
		if(averbadora.getOrgao() == null) {
			throw new GenericException("Erro","Orgão não pode ser nulo");
		}
		if(averbadora.getEndereco() == null) {
			throw new GenericException("Erro","Endereço não pode ser nulo");
		}
		Averbadora averbadoraByCnpj = averbadoraRepository.findByCnpj(averbadora.getCnpj());
		if(averbadoraByCnpj != null) {
			if(averbadora.getId() != null) {
				if(!averbadora.getId().equals(averbadoraByCnpj.getId())) {
					throw new GenericException("Erro","Já existe uma averbadora com este CNPJ!");
				}
			}
			else {
				throw new GenericException("Erro","Já existe uma averbadora com este CNPJ!");
			}
			
		}
	}
	
	@Transactional
	public String blockDisblock(Long id) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		
		String message = "";
		String title = "";
		
		Integer status = 500;
		
		Averbadora averbadora = averbadoraRepository.findById(id).get();
		if(averbadora.getStatus() == 1) {
			status = 204;
			averbadora.setStatus(2);
			
			averbadoraRepository.save(averbadora);
			
			List<Usuario> listaUsuario = usuarioRepository.findAll();
			for(Usuario usuario : listaUsuario) {
				if(usuario.getEntidade() != null && usuario.getEntidade().equals(averbadora.getId())) {
					usuario.setToken(null);
					usuarioRepository.save(usuario);
				}
			}
			
			Averbadora averbadoraNotBlocked = new Averbadora();
			BeanUtils.copyProperties(averbadora, averbadora);
			averbadoraNotBlocked.setStatus(2);
			
			try {
				LogAcao logAcao = funcoes.logAcao(averbadora.getId(), getDescricaoBloquear(), funcoes.getLoggedUser());
				logAverbadora(averbadoraNotBlocked, averbadora, logAcao);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else if(averbadora.getStatus() == 2) {
			//Se não, desbloqueie
			status = 205;
			averbadora.setStatus(1);
			
			averbadoraRepository.save(averbadora);
			
			Averbadora averbadoraBlockd = new Averbadora();
			BeanUtils.copyProperties(averbadora, averbadora);
			averbadoraBlockd.setStatus(2);
			
			try {
				LogAcao logAcao = funcoes.logAcao(averbadora.getId(), getDescricaoDesbloquear(), funcoes.getLoggedUser());
				logAverbadora(averbadoraBlockd, averbadora, logAcao);
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
		}
		
		jsonObject.put("title", title);
		jsonObject.put("message", message);
		jsonObject.put("status", status);
		status = null;
		return jsonObject.toString();
	}	
	
	private Averbadora dtoToAverbadora(AverbadoraCustomDTO averbadoraDto) {
		
		Averbadora averbadora = new Averbadora();
		BeanUtils.copyProperties(averbadoraDto, averbadora);
		averbadora.setEndereco(funcoes.dtoToEndereco(averbadoraDto.getEndereco().get(0)));
		averbadora.setOrgao(orgaoRepository.findById(averbadoraDto.getOrgao()).get());
		
		return averbadora;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_AVERBADORA;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_AVERBADORA;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_AVERBADORA;
	}
	
	public Long getDescricaoBloquear() {
		return DescricaoLogAcaoHelper.BLOQUEAR_AVERBADORA;
	}
	
	public Long getDescricaoDesbloquear() {
		return DescricaoLogAcaoHelper.DESBLOQUEAR_AVERBADORA;
	}
	
	public void logAverbadora(Averbadora previous, Averbadora current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Averbadora();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Averbadora");
		
		logs.add(logUtil.fromValues("id_orgao", 
				(previous.getOrgao() == null || previous.getOrgao().getId() == null) ? "-" : previous.getOrgao().getId().toString(),
				(current.getOrgao() == null || current.getOrgao().getId() == null) ? "-" : current.getOrgao().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("id_endereco", 
				(previous.getEndereco() == null || previous.getEndereco().getId() == null) ? "-" : previous.getEndereco().getId().toString(),
				(current.getEndereco() == null || current.getEndereco().getId() == null) ? "-" : current.getEndereco().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("nome_averbadora", 
				previous.getNome() == null ? "-" : previous.getNome(),
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("cod_averbadora", 
				previous.getCodigo() == null ? "-" : previous.getCodigo().toString(),
				current.getCodigo() == null ? "-" : current.getCodigo().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_averbadora", 
				previous.getStatus() == null ? "-" : previous.getStatus().toString(),
				current.getStatus() == null ? "-" : current.getStatus().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("email_averbadora", 
				previous.getEmail() == null ? "-" : previous.getEmail(),
				current.getEmail() == null ? "-" : current.getEmail(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("data_cadastro", 
				previous.getDataCadastro() == null ? "-" : sdf.format(previous.getDataCadastro()),
				current.getDataCadastro() == null ? "-" : sdf.format(current.getDataCadastro()),
				datAlteracao));
		
		logs.add(logUtil.fromValues("itsm_id", 
				previous.getItsmID() == null ? "-" : previous.getItsmID(),
				current.getItsmID() == null ? "-" : current.getItsmID(),
				datAlteracao));
		
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
