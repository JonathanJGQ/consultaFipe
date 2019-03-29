package com.sif.core.service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.PerfilHelper;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.Contrato;
import com.sif.model.Usuario;
import com.sif.model.custom.ContratoCustomDTO;
import com.sif.model.list.ContratoDTO;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.ContratoRepository;
import com.sif.repository.DescricaoHistoricoExtratoRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.VerbaRepository;
import com.sif.repository.specification.ContratoSpecification;

@Service
public class ContratoService {

	@Autowired
	ContratoRepository contratoRepository;
	
	@Autowired
	VerbaRepository verbaRepository;
	
	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	BilhetagemService bilhetagemService;
	
	@Autowired
	DescricaoHistoricoExtratoRepository descHistExtratoRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	public Page<Contrato> getAll(Pageable pageable, ContratoDTO contrato) {
		ContratoSpecification spec = new ContratoSpecification();
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		Page<Contrato> lista = null;
		
		List<Long> listaEntidades = new ArrayList<Long>();
		
		if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.SUPREMO) 
				|| usuarioLogado.getPerfil().getId().equals(PerfilHelper.ADMINISTRACAO)) {
			
			lista = contratoRepository.findAll(Specification.where(
					spec.idEquals(contrato.getId() != null ? contrato.getId().toString() : null))
						.and(spec.clienteNomeLike(contrato.getNome()))	
						.and(spec.clienteMatriculaLike(contrato.getMatricula()))
						.and(spec.clienteDocumentoLike(contrato.getCpf()))
						.and(spec.consignatariaLike(contrato.getConsignataria()))
						.and(spec.numeroContratoLike(contrato.getNumeroContrato()))
						.and(spec.situacaoEqual(contrato.getIdSituacaoContrato()))
						.and(spec.findByStatus())
						.and(spec.orderByIdDESC()),pageable);
		}
		else {
			if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
				listaEntidades.add(usuarioLogado.getEntidade());
				
				List<Averbadora> averbs = averbadoraRepository.findByOrgao(usuarioLogado.getEntidade());
				for(Averbadora averbadora : averbs) {
					listaEntidades.add(averbadora.getId());
					List<Consignataria> consigs = consignatariaRepository.findByAverbadora(averbadora.getId());
					for(Consignataria consignataria : consigs) {
						listaEntidades.add(consignataria.getId());
					}
				}
			}
			else if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
				
				listaEntidades.add(usuarioLogado.getEntidade());
			
				List<Consignataria> consigs = consignatariaRepository.findByAverbadora(usuarioLogado.getEntidade());
				for(Consignataria consignataria : consigs) {
					listaEntidades.add(consignataria.getId());
				}
			}
			else if(usuarioLogado.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
				listaEntidades.add(usuarioLogado.getEntidade());
			}
			
			lista = contratoRepository.findAll(Specification.where(
					spec.idEquals(contrato.getId() != null ? contrato.getId().toString() : null))
						.and(spec.clienteNomeLike(contrato.getNome()))	
						.and(spec.clienteMatriculaLike(contrato.getMatricula()))
						.and(spec.clienteDocumentoLike(contrato.getCpf()))
						.and(spec.consignatariaLike(contrato.getConsignataria()))
						.and(spec.numeroContratoLike(contrato.getNumeroContrato()))
						.and(spec.situacaoEqual(contrato.getIdSituacaoContrato()))
						.and(spec.entidadesEqual(listaEntidades))
						.and(spec.findByStatus())
						.and(spec.orderByIdDESC()),pageable);
		}
		
//			lista = contratoRepository.findAll(Specification.where(
//					spec.idEquals(contrato.getId() != null ? contrato.getId().toString() : null))
//						.and(spec.clienteNomeLike(contrato.getNome()))	
//						.and(spec.clienteMatriculaLike(contrato.getMatricula()))
//						.and(spec.clienteDocumentoLike(contrato.getCpf()))
//						.and(spec.consignatariaLike(contrato.getConsignataria()))
//						.and(spec.numeroContratoLike(contrato.getNumeroContrato()))
//						.and(spec.situacaoEqual(contrato.getIdSituacaoContrato()))
//						.and(spec.findByStatus())
//						.and(spec.orderByIdDESC()),pageable);
		
//		List<ContratoDTO> listaRetorno = new ArrayList<ContratoDTO>();
		
		
		
//		for(Contrato objeto : lista) {
//			ContratoDTO entity = new ContratoDTO();
//			entity.setId(objeto.getId());
//			entity.setNumeroContrato(objeto.getNumeroContrato());
//			if(objeto.getVerba() != null) {
//				if(objeto.getVerba().getConsignataria() != null) {
//					entity.setConsignataria(objeto.getVerba().getConsignataria().getNome());
//				}
//			}
//			if(objeto.getFuncionario() != null) {
//				entity.setNome(objeto.getFuncionario().getNome());
//				entity.setMatricula(objeto.getFuncionario().getMatricula());
//				entity.setCpf(objeto.getFuncionario().getCpf());
//			}
//			entity.setSituacaoContrato(objeto.getSituacao() == null ? "" : objeto.getSituacao().getNome());
//			entity.setIdSituacaoContrato(objeto.getSituacao() == null ? null : objeto.getSituacao().getId());
//
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<Contrato> findById(Long id) {
		
		Contrato contrato = contratoRepository.findById(id).get();
		
		if(contrato.getSaldoContratacao() != null) {
			contrato.setSaldoContratacaoCurrency(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(contrato.getSaldoContratacao()));
		}
		else {
			contrato.setSaldoContratacaoCurrency(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(0));
		}
		
		if(contrato.getValorParcela() != null) {
			contrato.setValorParcelaCurrency(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(contrato.getValorParcela()));
		}
		else {
			contrato.setValorParcelaCurrency(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(0));
		}
		
		if(contrato.getValorSolicitacao() != null) {
			contrato.setValorSolicitacaoCurrency(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(contrato.getValorSolicitacao()));
		}
		else {
			contrato.setValorSolicitacaoCurrency(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(0));
		}
		
		return Optional
				.ofNullable(contrato)
				.map(contratoAux -> ResponseEntity.ok().body(contratoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Contrato> create(ContratoCustomDTO contratoDto) {
		
		Contrato contrato = dtoToContrato(contratoDto);
		
		validarContrato(contrato);
		
		Contrato contratoSalvo = contratoRepository.save(contrato);
		
		try {
			bilhetagemService.gerar(contratoSalvo, descHistExtratoRepository.findById(2L).get(), funcoes.getLoggedUser());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Optional
				.ofNullable(contratoSalvo)
				.map(contratoAux -> ResponseEntity.ok().body(contratoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Contrato> update(ContratoCustomDTO contratoDto,Long id) {
		
		Contrato contrato = dtoToContrato(contratoDto);
		
		validarContrato(contrato);
		
		Contrato contratoSave = contratoRepository.findById(id).get();
		if(contratoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long contratoId = contratoSave.getId();
		
		BeanUtils.copyProperties(contrato, contratoSave);
		contratoSave.setId(contratoId);
		
		Contrato contratoSalvo = contratoRepository.save(contratoSave);
		
		try {
			bilhetagemService.gerar(contratoSalvo, descHistExtratoRepository.findById(1L).get(), funcoes.getLoggedUser());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(contratoSalvo)
				.map(contratoAux -> ResponseEntity.ok().body(contratoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Contrato> delete(Long id) {
		
		Contrato contratoSave = contratoRepository.findById(id).get();
		if(contratoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		contratoSave.setStatus(false);
		
		return Optional
				.ofNullable(contratoRepository.save(contratoSave))
				.map(contratoAux -> ResponseEntity.ok().body(contratoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarContrato(Contrato contrato) throws GenericException {
		
		if(contrato.getVerba() == null) {
			throw new GenericException("Erro","Verba não pode ser nulo");
		}
		if(contrato.getFuncionario() == null) {
			throw new GenericException("Erro","Funcionario não pode ser nulo");
		}
		if(contrato.getUsuario() == null) {
			throw new GenericException("Erro","Usuário não pode ser nulo");
		}
		if(contrato.getDataLancamento() == null) {
			throw new GenericException("Erro","Data de Lançamento não pode ser nulo");
		}
		if(contrato.getQuantidadeParcelas() == null) {
			throw new GenericException("Erro","Quantidade de parcelas não pode ser nulo");
		}
		if(contrato.getValorParcela() == null) {
			throw new GenericException("Erro","Valor da Parcela não pode ser nulo");
		}
	}	
	
	private Contrato dtoToContrato(ContratoCustomDTO contratoDto) {
		
		Contrato contrato = new Contrato();
		BeanUtils.copyProperties(contratoDto, contrato);
		contrato.setVerba(verbaRepository.findById(contratoDto.getVerba()).get());
		contrato.setFuncionario(funcionarioRepository.findById(contratoDto.getFuncionario()).get());
		contrato.setUsuario(usuarioRepository.findById(contratoDto.getUsuario()).get());
		
		return contrato;
		
	}

	public Contrato save(Contrato contrato) {
		return contratoRepository.save(contrato);
		
	}
	
}
