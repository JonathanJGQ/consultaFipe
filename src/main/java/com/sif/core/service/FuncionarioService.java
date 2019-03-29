package com.sif.core.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.springframework.transaction.annotation.Transactional;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.model.Contrato;
import com.sif.model.Funcionario;
import com.sif.model.FuncionarioMargem;
import com.sif.model.FuncionarioSecretaria;
import com.sif.model.Margem;
import com.sif.model.UnidadeAdministrativa;
import com.sif.model.custom.FuncionarioCustomDTO;
import com.sif.model.custom.FuncionarioMargemCustomDTO;
import com.sif.model.custom.MargemViewCustomDTO;
import com.sif.model.list.FuncionarioDTO;
import com.sif.repository.ContratoRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.FuncionarioMargemRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.FuncionarioSecretariaRepository;
import com.sif.repository.MargemRepository;
import com.sif.repository.UnidadeAdministrativaRepository;
import com.sif.repository.specification.FuncionarioSpecification;

@Service
public class FuncionarioService {

	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired 
	ContratoRepository contratoRepository;
	
	@Autowired
	MargemRepository margemRepository;
	
	@Autowired
	FuncionarioMargemService funcionarioMargemService;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	UnidadeAdministrativaRepository unidadeAdministrativaRepository;
	
	@Autowired
	FuncionarioSecretariaRepository funcionarioSecretariaRepository;
	
	@Autowired
	FuncionarioMargemRepository funcionarioMargemRepository;
	
	public Page<Funcionario> getAll(Pageable pageable, Funcionario funcionario) {
		FuncionarioSpecification spec = new FuncionarioSpecification();
		
		List<FuncionarioDTO> listaRetorno = new ArrayList<FuncionarioDTO>();
		Page<Funcionario> lista = funcionarioRepository.findAll(Specification.where(
				spec.idEquals(funcionario.getId() != null ? funcionario.getId().toString() : null))
			.and(spec.nomeLike(funcionario.getNome()))
			.and(spec.matriculaLike(funcionario.getMatricula()))
			.and(spec.cpfLike(funcionario.getCpf()))
			.and(spec.findByStatus())
			.and(spec.orderByNome()),pageable);
		
//		for(Funcionario objeto : lista) {
//			FuncionarioDTO entity = new FuncionarioDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setMatricula(objeto.getMatricula());
//			entity.setCpf(objeto.getCpf());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
//		return new PageImpl<FuncionarioDTO>(listaRetorno.subList(start, end), pageable, listaRetorno.size());
	}
	
	public ResponseEntity<FuncionarioCustomDTO> findById(Long id) {
		
		FuncionarioCustomDTO clienteDto = new FuncionarioCustomDTO();
		Funcionario cliente = funcionarioRepository.findById(id).get();
		BeanUtils.copyProperties(cliente, clienteDto);
		clienteDto.setEndereco(Arrays.asList(funcoes.enderecoToDTO(cliente.getEndereco())));
		
		return Optional
				.ofNullable(clienteDto)
				.map(clienteAux -> ResponseEntity.ok().body(clienteAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Funcionario> create(FuncionarioCustomDTO clienteDto) {
		
		Funcionario cliente = dtoToCliente(clienteDto);
		
		validarCliente(cliente);
		
		enderecoRepository.save(cliente.getEndereco());
		
		return Optional
				.ofNullable(funcionarioRepository.save(cliente))
				.map(clienteAux -> ResponseEntity.ok().body(clienteAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Funcionario> update(FuncionarioCustomDTO clienteDto,Long id) {
		
		Funcionario cliente = dtoToCliente(clienteDto);
		
		validarCliente(cliente);
		
		Funcionario clienteSave = funcionarioRepository.findById(id).get();
		if(clienteSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long clienteId = clienteSave.getId();
		
		BeanUtils.copyProperties(cliente, clienteSave);
		clienteSave.setId(clienteId);
		
		return Optional
				.ofNullable(funcionarioRepository.save(clienteSave))
				.map(clienteAux -> ResponseEntity.ok().body(clienteAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Funcionario> delete(Long id) {
		
		Funcionario clienteSave = funcionarioRepository.findById(id).get();
		if(clienteSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		clienteSave.setStatus(false);
		
		return Optional
				.ofNullable(funcionarioRepository.save(clienteSave))
				.map(clienteAux -> ResponseEntity.ok().body(clienteAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarCliente(Funcionario funcionario) throws GenericException {
		
		if(funcionario.getEndereco() == null) {
			throw new GenericException("Erro","Endereço não pode ser nulo");
		}
		if(funcionario.getNumeroFuncional() == null || funcionario.getNumeroFuncional().isEmpty()) {
			throw new GenericException("Erro","Número funcionalidade não pode ser nulo");
		}
		if(funcionario.getMatricula() == null || funcionario.getMatricula().isEmpty()) {
			throw new GenericException("Erro","Matrícula não pode ser nulo");
		}
		if(funcionario.getDataAdmissao() == null) {
			throw new GenericException("Erro","Data de Admissão não pode ser nulo");
		}
		if(funcionario.getDataNascimento() == null) {
			throw new GenericException("Erro","Data de Nascimento não pode ser nulo");
		}
		if(funcionario.getDataAposentadoria() == null) {
			throw new GenericException("Erro","Data de Aposentaria não pode ser nulo");
		}
		if(funcionario.getDataVacancia() == null) {
			throw new GenericException("Erro","Data de vacancia não pode ser nulo");
		}
		if(funcionario.getNumeroMaximoParcela() == null) {
			throw new GenericException("Erro","Número máximo de parcelas não pode ser nulo");
		}
		if(funcionario.getSalarioBase().compareTo(new BigDecimal(0)) ==  -1) {
			throw new GenericException("Erro","O valor do salário não pode ser negativo.");
		}
		Funcionario funcionarioByDoc = funcionarioRepository.findByFuncionalAndVinculo(funcionario.getNumeroFuncional(), funcionario.getNumeroVinculo());
		if(funcionarioByDoc != null) {
			if(funcionario.getId() != null) {
				if(!funcionario.getId().equals(funcionarioByDoc.getId())) {
					throw new GenericException("Erro","Já existe um funcionário com este documento!");
				}
			}
			else {
				Funcionario funcionarioByDocumento = funcionarioRepository.findByCpf(funcionario.getCpf());
				
				if(funcionarioByDocumento != null) {
					throw new GenericException("Erro","Já existe um funcionário com este documento!");
				}
			}
			
		}
		
		if(funcionario.getDataNascimento().after(new Date())) {
			throw new GenericException("Erro","Data de Nascimento deve ser menor que o dia de hoje!");
		}
		if(funcionario.getDataVacancia().before(funcionario.getDataAdmissao())
				|| funcionario.getDataVacancia().before(funcionario.getDataAposentadoria())
				|| funcionario.getDataVacancia().before(funcionario.getDataNascimento())) {
			throw new GenericException("Erro","Data de vacancia deve ser superior a data de admissão, data de aposentadoria e data de nascimento");
		}
		if(funcionario.getDataNascimento().after(funcionario.getDataAposentadoria()) 
				|| funcionario.getDataNascimento().after(funcionario.getDataAdmissao())
				|| funcionario.getDataNascimento().after(funcionario.getDataVacancia())) {
			throw new GenericException("Erro","Data de nascimento deve ser inferior a data de admissão, data de aposentadoria e data de vacancia");
		}
	}	
	
	private Funcionario dtoToCliente(FuncionarioCustomDTO funcionarioDto) {
		
		Funcionario cliente = new Funcionario();
		BeanUtils.copyProperties(funcionarioDto, cliente);
		cliente.setEndereco(funcoes.dtoToEndereco(funcionarioDto.getEndereco().get(0)));
		
		return cliente;
		
	}

	public ResponseEntity<FuncionarioMargemCustomDTO> viewFuncionario(Long id) {
		
		Funcionario funcionario = funcionarioRepository.findById(id).get();
		
		FuncionarioMargemCustomDTO retorno = new FuncionarioMargemCustomDTO();
		
		if(funcionario == null) {
			return Optional
					.ofNullable(retorno)
					.map(funcionarioAux -> ResponseEntity.status(400).body(funcionarioAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		List<FuncionarioMargem> lista = funcionarioMargemRepository.findByFuncionario(funcionario.getId());

		retorno.setFuncionario(new FuncionarioCustomDTO());
		retorno.setMargem(new ArrayList<MargemViewCustomDTO>());
		BeanUtils.copyProperties(funcionario, retorno.getFuncionario());
		retorno.getFuncionario().setEndereco(Arrays.asList(funcoes.enderecoToDTO(funcionario.getEndereco())));
		
		for(FuncionarioMargem objeto : lista) {
			
			List<Contrato> listaContrato = contratoRepository.findByFuncionarioAndMargem(funcionario.getId(), objeto.getMargem().getId());
			
			BigDecimal consumo = new BigDecimal(0);
			for(Contrato contrato : listaContrato) {
				consumo = consumo.add(contrato.getValorParcela());
			}
			
			MargemViewCustomDTO margem = new MargemViewCustomDTO();
			margem.setId(objeto.getId());
			margem.setDescricao(objeto.getMargem().getDescricao());
			margem.setMargem(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(objeto.getLimite()));
			margem.setConsumo(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(consumo));
			
			BigDecimal disponivel = objeto.getLimite();
			disponivel = disponivel.add(consumo.negate());
			
			
			
			margem.setDisponivel(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(disponivel));
			margem.setEmUso(String.valueOf(new BigDecimal((consumo.longValue()*100)).divide(objeto.getLimite())) + " %");
			margem.setContratos(String.valueOf(listaContrato.size()));
			
			retorno.getMargem().add(margem);
		}
		
		return Optional
				.ofNullable(retorno)
				.map(funcionarioAux -> ResponseEntity.status(200).body(funcionarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Funcionario findByFuncionalAndVinculo(String funcional, String vinculo) {
		return funcionarioRepository.findByFuncionalAndVinculo(funcional, vinculo);
	}
	
	@Transactional
	public void saveToken(List<Funcionario> listaFuncionario) {
		for(Funcionario funcionario : listaFuncionario) {
			funcionarioRepository.save(funcionario);
		}
	}
	
	@Transactional
	public void save(List<Funcionario> listaFuncionario) {
		
		for(Funcionario funcionario : listaFuncionario) {
			if(funcionario.getEndereco().getId() == null) {
				enderecoRepository.save(funcionario.getEndereco());
			}
			Funcionario funcionarioSave = funcionarioRepository.save(funcionario);
			
			FuncionarioSecretaria funcionarioSecretaria = new FuncionarioSecretaria();
			funcionarioSecretaria.setDataCadastro(new Date());
			funcionarioSecretaria.setFuncionario(funcionarioSave);
			funcionarioSecretaria.setSecretaria(funcionario.getSecretaria());
			funcionarioSecretaria.setStatus(true);
			
			funcionarioSecretaria = funcionarioSecretariaRepository.save(funcionarioSecretaria);
			
			// Gerando margem para o funcionario
			if(funcionario.getSalarioBase() != null) {
				
				List<Margem> margens = getMargensForFuncionario(funcionario);
				
				if(margens != null && !margens.isEmpty()) {
					for(Margem margem : margens) {
						
						//Para editar
						FuncionarioMargem funcionarioMargem = funcionarioMargemService.findByMargemFuncionario(margem.getId(), funcionario.getId());
						
						if(funcionarioMargem == null) {
							funcionarioMargem = new FuncionarioMargem();
						}
						
						funcionarioMargem.setFuncionario(funcionario);
						funcionarioMargem.setMargem(margem);
						
						//Falta trazer o certo
						Long percentRestricao = Long.parseLong(margem.getPercentual());
						funcionarioMargem.setPercentualClienteMargem(percentRestricao);
						
						Long maxParcelas = Long.parseLong(margem.getMaxParcelas());
						funcionarioMargem.setMaximaParcelaCliente(maxParcelas);
						
						//Calculando Limite de Funcionario na Margem
						funcionarioMargem.setLimite(calculateLimit(margem, funcionario, funcionarioMargem.getPercentualClienteMargem()));
						
						funcionarioMargem.setStatus(true);
//						funcionarioMargemRepository.save(funcionarioMargem);
						
						funcionarioMargemService.save(funcionarioMargem);
					}
				}
			}
			
		}
		
	}
	
	private List<Margem> getMargensForFuncionario(Funcionario funcionario){
		if(funcionario != null) {
			if(funcionario.getId() != null) {
				return margemRepository.findByFuncionario(funcionario.getId());
			}
		}
		
		return null;
	}
	
	private BigDecimal calculateLimit(Margem margem, Funcionario funcionario, Long percentualMargem) {
		
		BigDecimal percentagemMargem = new BigDecimal(margem.getPercentual());
		BigDecimal percentagemDecimal = percentagemMargem.divide(new BigDecimal(100));
		
		BigDecimal limit = percentagemDecimal.multiply(funcionario.getSalarioBase());
//		
//		return limit.multiply(new BigDecimal(percentualMargem / 100));
		
		return limit;
		
	}
	
	public Funcionario getFuncionarioForMargemInsert(Funcionario funcionario) {
		
		FuncionarioSpecification funcSpec = new FuncionarioSpecification();
		
		List<Funcionario> funcionarios = funcionarioRepository.findAll(
			Specification.where(
				funcSpec.findByStatus()
			)
			.and(funcSpec.cpfEquals(funcionario.getCpf()))
			.and(funcSpec.nomeEqual(funcionario.getNome()))
			.and(funcSpec.nascimentoEquals(funcionario.getDataNascimento()))
			.and(funcSpec.numFuncEquals(funcionario.getNumeroFuncional()))
			.and(funcSpec.numVincEquals(funcionario.getNumeroVinculo()))
			.and(funcSpec.numPensEquals(funcionario.getNumeroPensionista()))
		);
		
		if(funcionarios != null && !funcionarios.isEmpty()) {
			if(funcionarios.size() == 1) {
				funcionarios.get(0);
			}
		}
		
		
		return null;
	}
}
