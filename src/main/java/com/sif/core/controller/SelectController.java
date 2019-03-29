package com.sif.core.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.service.UsuarioService;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.PerfilHelper;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Administracao;
import com.sif.model.Averbadora;
import com.sif.model.Cidade;
import com.sif.model.Consignataria;
import com.sif.model.DescricaoLogAcao;
import com.sif.model.Funcionario;
import com.sif.model.Margem;
import com.sif.model.Modulo;
import com.sif.model.Orgao;
import com.sif.model.Perfil;
import com.sif.model.PerfilFuncionalidade;
import com.sif.model.Secretaria;
import com.sif.model.SituacaoContrato;
import com.sif.model.TipoUsuario;
import com.sif.model.Uf;
import com.sif.model.Usuario;
import com.sif.repository.AdministracaoRepository;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.CidadeRepository;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.ContratoRepository;
import com.sif.repository.DescricaoLogAcaoRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.FolhaRepository;
import com.sif.repository.FuncionalidadeRepository;
import com.sif.repository.FuncionarioMargemRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.FuncionarioSecretariaRepository;
import com.sif.repository.ItensFolhaRepository;
import com.sif.repository.MargemRepository;
import com.sif.repository.ModuloRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.PerfilFuncionalidadeRepository;
import com.sif.repository.PerfilRepository;
import com.sif.repository.PortabilidadeRepository;
import com.sif.repository.RefinanciamentoRepository;
import com.sif.repository.SecretariaRepository;
import com.sif.repository.SituacaoContratoRepository;
import com.sif.repository.TipoUsuarioRepository;
import com.sif.repository.UfRepository;
import com.sif.repository.UnidadeAdministrativaRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.VerbaRepository;
import com.sif.repository.specification.AdministracaoSpecification;
import com.sif.repository.specification.AverbadoraSpecification;
import com.sif.repository.specification.CidadeSpecification;
import com.sif.repository.specification.ConsignatariaSpecification;
import com.sif.repository.specification.FuncionarioSpecification;
import com.sif.repository.specification.MargemSpecification;
import com.sif.repository.specification.ModuloSpecification;
import com.sif.repository.specification.OrgaoSpecification;
import com.sif.repository.specification.PerfilFuncionalidadeSpecification;
import com.sif.repository.specification.PerfilSpecification;
import com.sif.repository.specification.SecretariaSpecification;
import com.sif.repository.specification.SituacaoContratoSpecification;
import com.sif.repository.specification.TipoUsuarioSpecification;
import com.sif.repository.specification.UfSpecification;
import com.sif.repository.specification.UsuarioSpecification;


@RestController
public class SelectController {
	
	@Autowired
	AdministracaoRepository administracaoRepository;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	CidadeRepository cidadeRepository;
	
	@Autowired
	FuncionarioMargemRepository clienteMargemRepository;
	
	@Autowired
	FuncionarioRepository clienteRepository;
	
	@Autowired
	FuncionarioSecretariaRepository clienteSecretariaRepository;
	
	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	FolhaRepository folhaRepository;
	
	@Autowired
	FuncionalidadeRepository funcionalidadeRepository;
	
	@Autowired
	ItensFolhaRepository itensFolhaRepository;
	
	@Autowired
	MargemRepository margemRepository;
	
	@Autowired
	ModuloRepository moduloRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	PerfilFuncionalidadeRepository perfilFuncionalidadeRepository;
	
	@Autowired
	PerfilRepository perfilRepository;
	
	@Autowired
	PortabilidadeRepository portabilidadeRepository;
	
	@Autowired
	RefinanciamentoRepository refinanciamentoRepository;
	
	@Autowired
	SecretariaRepository secretariaRepository;
	
	@Autowired
	TipoUsuarioRepository tipoUsuarioRepository;
	
	@Autowired
	UfRepository ufRepository;
	
	@Autowired
	UnidadeAdministrativaRepository unidadeAdministrativaRepository;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	ContratoRepository contratoRepository;
	
	@Autowired
	VerbaRepository verbaRepository;
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	SituacaoContratoRepository situacaoContratoRepository;
	
	@Autowired
	DescricaoLogAcaoRepository descricaoLogAcaoRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@PostMapping("/selectusuarios")
	@ResponseBody
	public String selecionarUsuarios(@RequestBody String jsonString) {
		
		JSONObject jsonObject = new JSONObject(jsonString);
		
		List<Usuario> usuarios = new ArrayList<Usuario>();
		
		//Pegar usuarios do orgao		
		if(jsonObject.has("orgaos")) {
			JSONArray orgaos = jsonObject.getJSONArray("orgaos");

			for(int i = 0; i < orgaos.length(); i++) {
				Long id = orgaos.getLong(i);
				
				Orgao orgao = orgaoRepository.findById(id).orElse(null);
				
				if(orgao != null) {
					usuarios.addAll(usuarioService.findByPerfilEntidade(TipoPerfil.ORGAO, id));
				}
				
			}
		}
		
		//Pegar usuarios da averbadoras
		if(jsonObject.has("averbadoras")) {
			JSONArray averbadoras = jsonObject.getJSONArray("averbadoras");
			
			for(int i = 0; i < averbadoras.length(); i++) {
				Long id = averbadoras.getLong(i);
				
				Averbadora averbadora = averbadoraRepository.findById(id).orElse(null);
				
				if(averbadora != null) {
					usuarios.addAll(usuarioService.findByPerfilEntidade(TipoPerfil.AVERBADORA, id));
				}
				
			}
		}
		
		//Pegar usuarios da consignatarias
		if(jsonObject.has("consignatarias")) {
			JSONArray consignatarias = jsonObject.getJSONArray("consignatarias");
			
			for(int i = 0; i < consignatarias.length(); i++) {
				Long id = consignatarias.getLong(i);
				
				Consignataria consignataria = consignatariaRepository.findById(id).orElse(null);
				
				if(consignataria != null) {
					usuarios.addAll(usuarioService.findByPerfilEntidade(TipoPerfil.CONSIGNATARIA, id));
				}
				
			}
		}
		
		//Pegar usuarios da administracoes
		if(jsonObject.has("administracoes")) {
			JSONArray administracoes = jsonObject.getJSONArray("administracoes");
			
			for(int i = 0; i < administracoes.length(); i++) {
				Long id = administracoes.getLong(i);
				
				Administracao administracao = administracaoRepository.findById(id).orElse(null);
				
				if(administracao != null) {
					usuarios.addAll(usuarioService.findByPerfilEntidade(TipoPerfil.ADMINISTRADOR, id));
				}
				
			}
		}
		
		if(funcoes.getLoggedUser().getPerfil().getId() == TipoPerfil.SUPREMO
			|| funcoes.getLoggedUser().getPerfil().getId() == TipoPerfil.ADMINISTRADOR) {
			
			usuarios.addAll(usuarioService.findByPerfil(TipoPerfil.SUPREMO));
//			usuarios.addAll(usuarioService.findByPerfil(TipoPerfil.ADMINISTRADOR));
			
		}
		
		JSONArray jsonArray = new JSONArray();
		for(Usuario usuario : usuarios) {
			JSONObject json = new JSONObject();
			json.put("id", usuario.getId().toString());
			json.put("text", usuario.getNome());
			
			jsonArray.put(json);
		}
		
		return jsonArray.toString();
	}
	
	@GetMapping("/select")
	@ResponseBody
	public String selecionar(@RequestParam Map<String, String> params, HttpSession session) {
		
		if(!params.containsKey("model")) {
			return "[]";
		}
		
		String model = params.get("model");
		
//		ArrayList<Map.Entry> entityParams = new ArrayList<Map.Entry>();
//		
//		for(Map.Entry<String, String> entry : params.entrySet()) {
//			
//			System.out.println(entry.getKey()+" : "+entry.getValue());
//			
//			entityParams.add(entry);
//		}
		
		CrudRepository<?, Long> repository = null;

		List<?> lista = null;
		
		String jsonString = "[";
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		switch(model){
			case "cidade":
				jsonString = selectCidade(params, jsonString);
				break;
			
			case "uf":
				jsonString = selectUf(params, jsonString);
				break;
			
			case "perfil":
				jsonString = selectPerfil(params, jsonString, usuarioLogado);
				break;
				
			case "tipousuario":
				jsonString = selectTipoUsuario(params, jsonString);
				break;
				
			case "cliente":
				jsonString = selectCliente(params, jsonString);
				break;
			
			case "orgao":
				jsonString = selectOrgao(params, jsonString, usuarioLogado);
				break;
				
			case "margem":
				jsonString = selectMargem(params, jsonString);
				break;
				
			case "consignataria":
				jsonString = selectConsignataria(params, jsonString, usuarioLogado);
				break;
				
			case "averbadora":
				jsonString = selectAverbadora(params, jsonString, usuarioLogado);
				break;
				
			case "secretaria":
				jsonString = selectSecretaria(params, jsonString);
				break;
				
			case "modulo":
				jsonString = selectModulo(params, jsonString);
				break;
				
			case "administracao":
				jsonString = selectAdministracao(params, jsonString);
				break;
				
			case "entidade":
				jsonString = selectEntidades(params, jsonString, usuarioLogado);
				return jsonString;
				
			case "usuario":
				jsonString = selectUsuarios(params, jsonString);
				break;
				
			case "tipoTicket":
				jsonString = selectTipoTicket();
				break;
				
			case "prioridadeTicket":
				jsonString = selectPrioridadeTicket();
				break;
				
			case "statusTicket":
				jsonString = selectStatusTicket();
				break;
				
			case "descricaoLogAcao":
				jsonString = selectDescricaoLogAcao();
				break;
				
			case "situacaoContrato":
				jsonString = selectSituacaoContrato(params, jsonString);
				break;
		}
		
		jsonString = jsonString + "]";
		
		return new JSONArray(jsonString).toString();  
	}

	private String selectCidade(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		CidadeSpecification cidadeSpecification = new CidadeSpecification();
		
		//Parametros da cidade
		String idCidade = null;
		String nomeCidade = null;
		String ufCidade = null;
		
		
		if(params.containsKey("id")) {
			idCidade = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeCidade = params.get("nome");
		}
		
		if(params.containsKey("uf")) {
			ufCidade = params.get("uf");
		}
		
		repository = cidadeRepository;
		
		lista = ((CidadeRepository) repository).findAll(
			Specification.where(
				cidadeSpecification.idEquals(idCidade))
			.and(cidadeSpecification.nomeLike(nomeCidade))
			.and(cidadeSpecification.ufEquals(ufCidade))
			.and(cidadeSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(Cidade cidade: (List<Cidade>) lista) {
				jsonString = jsonString + montarJson(cidade.getId(), cidade.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectTipoTicket() {
		
		String jsonString = "[";
		jsonString = jsonString + "{\"id\":\"Question\", \"text\":\"Questão\"},";
		jsonString = jsonString + "{\"id\":\"Incident\", \"text\":\"Incidente\"},";
		jsonString = jsonString + "{\"id\":\"Problem\", \"text\":\"Problema\"},";
		jsonString = jsonString + "{\"id\":\"Request\", \"text\":\"Solicitação de Recurso\"},";
		jsonString = jsonString + "{\"id\":\"Refund\", \"text\":\"Reembolso\"}]";
		
		return jsonString;
	}
	
	private String selectPrioridadeTicket() {
		
		String jsonString = "[";
		jsonString = jsonString + "{\"id\":\"1\", \"text\":\"Baixa\"},";
		jsonString = jsonString + "{\"id\":\"2\", \"text\":\"Média\"},";
		jsonString = jsonString + "{\"id\":\"3\", \"text\":\"Alta\"},";
		jsonString = jsonString + "{\"id\":\"4\", \"text\":\"Urgente\"}]";
		
		return jsonString;
	}
	
	private String selectStatusTicket() {
		
//		<option value="2">Aberto</option>
//		<option value="3">Pendente</option>
//		<option value="4">Resolvido</option>
//		<option value="5">Fechado</option>
		
		String jsonString = "[";
		jsonString = jsonString + "{\"id\":\"2\", \"text\":\"Aberto\"},";
		jsonString = jsonString + "{\"id\":\"3\", \"text\":\"Pendente\"},";
		jsonString = jsonString + "{\"id\":\"4\", \"text\":\"Resolvido\"},";
		jsonString = jsonString + "{\"id\":\"5\", \"text\":\"Fechado\"}]";
		
		return jsonString;
	}
	
	private String selectDescricaoLogAcao() {
		
		
		List<DescricaoLogAcao> descricoes = descricaoLogAcaoRepository.findAll();
		
		String jsonString = "[";
		for(DescricaoLogAcao descricao : descricoes) {
			jsonString = jsonString + "{\"id\":\""+descricao.getId()+"\", \"text\":\""+descricao.getDescricao()+"\"},";
		}
		
		if(jsonString.length() > 1) {
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		jsonString = jsonString + "]";
		
		return jsonString;
	}
	
	private String selectSituacaoContrato(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		SituacaoContratoSpecification situacaoContratoSpecification = new SituacaoContratoSpecification();
		
		//Parametros da cidade
		String idSituacao = null;
		String nomeSituacao = null;
		String siglaSituacao = null;
		
		
		if(params.containsKey("id")) {
			idSituacao = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeSituacao = params.get("nome");
		}
		
		if(params.containsKey("sigla")) {
			siglaSituacao = params.get("sigla");
		}
		
		repository = situacaoContratoRepository;
		
		lista = ((SituacaoContratoRepository) repository).findAll(
			Specification.where(
					situacaoContratoSpecification.idEquals(idSituacao))
			.and(situacaoContratoSpecification.nomeLike(nomeSituacao))
			.and(situacaoContratoSpecification.siglaEquals(siglaSituacao))
			.and(situacaoContratoSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(SituacaoContrato situacaoContrato: (List<SituacaoContrato>) lista) {
				jsonString = jsonString + montarJson(situacaoContrato.getId(), situacaoContrato.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectAdministracao(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		AdministracaoSpecification administracaoSpecification = new AdministracaoSpecification();
		
		//Parametros da cidade
		String idAdministracao = null;
		String nomeAdministracao = null;
		String documentoAdministracao = null;
		String emailAdministracao = null;
		
		
		if(params.containsKey("id")) {
			idAdministracao = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeAdministracao = params.get("nome");
		}
		
		if(params.containsKey("documento")) {
			documentoAdministracao = params.get("documento");
		}
		
		if(params.containsKey("email")) {
			emailAdministracao = params.get("email");
		}
		
		repository = administracaoRepository;
		
		lista = ((AdministracaoRepository) repository).findAll(
			Specification.where(
					administracaoSpecification.idEquals(idAdministracao))
			.and(administracaoSpecification.nomeLike(nomeAdministracao))
			.and(administracaoSpecification.documentoLike(documentoAdministracao))
			.and(administracaoSpecification.emailLike(emailAdministracao))
			.and(administracaoSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(Administracao administracao: (List<Administracao>) lista) {
				jsonString = jsonString + montarJson(administracao.getId(), administracao.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectUsuarios(Map<String, String> params, String jsonString) {
		
		List<Usuario> usuarios = usuarioRepository.findAll(Specification.where(new UsuarioSpecification().findByStatus()));
	
		JSONArray jsonArray = new JSONArray();
		for(Usuario usuario : usuarios) {
			JSONObject json = new JSONObject();
			json.put("id", usuario.getId().toString());
			json.put("text", usuario.getNome());
			
			jsonArray.put(json);
		}
		
		return jsonArray.toString();
	
	}
	
	private String selectEntidades(Map<String, String> params, String jsonString, Usuario usuario) {
		
		CrudRepository<?, Long> repository;
		List<?> lista;
		
		OrgaoSpecification orgaoSpec = new OrgaoSpecification();
		AverbadoraSpecification averbSpec = new AverbadoraSpecification();
		ConsignatariaSpecification consigSpec = new ConsignatariaSpecification();
		AdministracaoSpecification adminsSpec = new AdministracaoSpecification();
		
		List<Orgao> orgaos = new ArrayList<Orgao>();
		List<Averbadora> averbs = new ArrayList<Averbadora>();
		List<Consignataria> consigs = new ArrayList<Consignataria>();
		List<Administracao> admins = new ArrayList<Administracao>();
		
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			orgaos.add(orgao);
			averbs = averbadoraRepository.findByOrgao(orgao.getId());
			for(Averbadora averbadora : averbs) {
				consigs.addAll(consignatariaRepository.findByAverbadora(averbadora.getId()));
			}
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
			Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
			averbs.add(averbadora);
			consigs = consignatariaRepository.findByAverbadora(averbadora.getId());
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			consigs.add(consignatariaRepository.findById(usuario.getEntidade()).get());
		}
		else {
			orgaos = orgaoRepository.findAll(Specification.where(
					orgaoSpec.findByStatus()
				));
			
			averbs = averbadoraRepository.findAll(Specification.where(
						averbSpec.findByStatus()
					));
			
			consigs = consignatariaRepository.findAll(Specification.where(
					consigSpec.findByStatus()
				));
			
			admins = administracaoRepository.findAll(Specification.where(
					adminsSpec.findByStatus()
				));
		}

		JSONArray jsonArrayOrgaos = new JSONArray();
		JSONArray jsonArrayAverbadoras = new JSONArray();
		JSONArray jsonArrayConsigs = new JSONArray();
		JSONArray jsonArrayAdmins = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		
		for(Orgao orgao : orgaos) {
			JSONObject json = new JSONObject();
			json.put("id", orgao.getId().toString());
			json.put("text", orgao.getNome());
			
			jsonArrayOrgaos.put(json);
			
		}
		jsonObject.put("orgaos", jsonArrayOrgaos);
		
		for(Averbadora averbadora : averbs) {
			JSONObject json = new JSONObject();
			json.put("id", averbadora.getId().toString());
			json.put("text", averbadora.getNome());
			
			jsonArrayAverbadoras.put(json);
		}
		jsonObject.put("averbadoras", jsonArrayAverbadoras);
		
		for(Consignataria consig : consigs) {
			JSONObject json = new JSONObject();
			json.put("id", consig.getId().toString());
			json.put("text", consig.getNome());
			
			jsonArrayConsigs.put(json);
		}
		jsonObject.put("consignatarias", jsonArrayConsigs);
		
		for(Administracao admin : admins) {
			JSONObject json = new JSONObject();
			json.put("id", admin.getId().toString());
			json.put("text", admin.getNome());
			
			jsonArrayAdmins.put(json);
		}
		jsonObject.put("administracoes", jsonArrayAdmins);
	
		
		String jsonReturn = jsonObject.toString();
		return jsonReturn;
		
	}
	
	private String selectModulo(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		ModuloSpecification moduloSpecification = new ModuloSpecification();
		
		//Parametros da cidade
		String idModulo = null;
		String nomeModulo = null;
		String pathModulo = null;
		
		
		if(params.containsKey("id")) {
			idModulo = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeModulo = params.get("nome");
		}
		
		if(params.containsKey("path")) {
			pathModulo = params.get("path");
		}
		
		if(params.containsKey("path")) {
			pathModulo = params.get("path");
		}
		
		repository = moduloRepository;
		
		lista = ((ModuloRepository) repository).findAll(
			Specification.where(
				moduloSpecification.idEquals(idModulo))
			.and(moduloSpecification.nomeLike(nomeModulo))
			.and(moduloSpecification.pathLike(pathModulo))
			.and(moduloSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(Modulo modulo: (List<Modulo>) lista) {
				jsonString = jsonString + montarJson(modulo.getId(), modulo.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectSecretaria(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		SecretariaSpecification secretariaSpecification = new SecretariaSpecification();
		
		//Parametros da cidade
		String idSecretaria = null;
		String descricaoSecretaria = null;
		String orgaoSecretaria = null;
		String codigoSecretaria = null;
		String siglaSecretaria = null;
		
		
		if(params.containsKey("id")) {
			idSecretaria = params.get("id");
		}
		
		if(params.containsKey("descricao")) {
			descricaoSecretaria = params.get("descricao");
		}
		
		if(params.containsKey("orgao")) {
			orgaoSecretaria = params.get("orgao");
		}
		
		if(params.containsKey("codigo")) {
			codigoSecretaria = params.get("codigo");
		}
		
		if(params.containsKey("sigla")) {
			siglaSecretaria = params.get("sigla");
		}
		
		repository = secretariaRepository;
		
		lista = ((SecretariaRepository) repository).findAll(
			Specification.where(
				secretariaSpecification.idEquals(idSecretaria))
			.and(secretariaSpecification.descricaoLike(descricaoSecretaria))
			.and(secretariaSpecification.orgaoLike(orgaoSecretaria))
			.and(secretariaSpecification.codigoLike(codigoSecretaria))
			.and(secretariaSpecification.siglaLike(siglaSecretaria))
			.and(secretariaSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(Secretaria secretaria: (List<Secretaria>) lista) {
				jsonString = jsonString + montarJson(secretaria.getId(), secretaria.getDescricao());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectAverbadora(Map<String, String> params, String jsonString, Usuario usuario) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		AverbadoraSpecification averbadoraSpecification = new AverbadoraSpecification();
		
		//Parametros da cidade
		String idAverbadora = null;
		String nomeAverbadora = null;
		String orgaoAverbadora = null;
		String codigoAverbadora = null;
		
		
		if(params.containsKey("id")) {
			idAverbadora = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeAverbadora = params.get("nome");
		}
		
		if(params.containsKey("orgao")) {
			orgaoAverbadora = params.get("orgao");
		}
		
		if(params.containsKey("codigo")) {
			codigoAverbadora = params.get("codigo");
		}
		
		repository = averbadoraRepository;
		
		List<Averbadora> averbs = new ArrayList<Averbadora>();
		
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			averbs = averbadoraRepository.findByOrgao(orgao.getId());
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
			Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
			averbs.add(averbadora);
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			// lista vaizia
		}
		else {
			
			averbs = ((AverbadoraRepository) repository).findAll(
				Specification.where(
					averbadoraSpecification.idEquals(idAverbadora))
				.and(averbadoraSpecification.nomeLike(nomeAverbadora))
				.and(averbadoraSpecification.orgaoLike(orgaoAverbadora))
				.and(averbadoraSpecification.codigoLike(codigoAverbadora != null ? Integer.parseInt(codigoAverbadora) : null))
				.and(averbadoraSpecification.findByStatus())
			);
		}
		if(!averbs.isEmpty()) {
			for(Averbadora averbadora: (List<Averbadora>) averbs) {
				jsonString = jsonString + montarJson(averbadora.getId(), averbadora.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectMargem(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		MargemSpecification margemSpecification = new MargemSpecification();
		
		//Parametros da cidade
		String idMargem = null;
		String descricaoMargem = null;
		String orgaoMargem = null;
		
		
		if(params.containsKey("id")) {
			idMargem = params.get("id");
		}
		
		if(params.containsKey("descricao")) {
			descricaoMargem = params.get("descricao");
		}
		
		if(params.containsKey("orgao")) {
			orgaoMargem = params.get("orgao");
		}
		
		repository = margemRepository;
		
		lista = ((MargemRepository) repository).findAll(
			Specification.where(
				margemSpecification.idEquals(idMargem))
			.and(margemSpecification.descricaoLike(descricaoMargem))
			.and(margemSpecification.orgaoLike(orgaoMargem))
			.and(margemSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(Margem margem: (List<Margem>) lista) {
				jsonString = jsonString + montarJson(margem.getId(), margem.getDescricao());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectConsignataria(Map<String, String> params, String jsonString, Usuario usuario) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		ConsignatariaSpecification consignatariaSpecification = new ConsignatariaSpecification();
		
		//Parametros da cidade
		String idConsignataria = null;
		String averbadoraConsignataria = null;
		String nomeConsignataria = null;
		String emailConsignataria = null;
		
		
		if(params.containsKey("id")) {
			idConsignataria = params.get("id");
		}
		
		if(params.containsKey("averbadora")) {
			averbadoraConsignataria = params.get("averbadora");
		}
		
		if(params.containsKey("nome")) {
			nomeConsignataria = params.get("nome");
		}
		
		if(params.containsKey("email")) {
			emailConsignataria = params.get("email");
		}
		
		repository = consignatariaRepository;
		
		List<Averbadora> averbs = new ArrayList<Averbadora>();
		List<Consignataria> consigs = new ArrayList<Consignataria>();
		
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			averbs = averbadoraRepository.findByOrgao(orgao.getId());
			for(Averbadora averbadora : averbs) {
				consigs.addAll(consignatariaRepository.findByAverbadora(averbadora.getId()));
			}
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
			Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
			consigs = consignatariaRepository.findByAverbadora(averbadora.getId());
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			consigs.add(consignatariaRepository.findById(usuario.getEntidade()).get());
		}
		else {

			consigs = ((ConsignatariaRepository) repository).findAll(
				Specification.where(
					consignatariaSpecification.idEquals(idConsignataria))
				.and(consignatariaSpecification.nomeLike(nomeConsignataria))
				.and(consignatariaSpecification.averbadoraLike(averbadoraConsignataria))
				.and(consignatariaSpecification.emailLike(emailConsignataria))
				.and(consignatariaSpecification.findByStatus())
			);
		}
		if(!consigs.isEmpty()) {
			for(Consignataria consignataria: (List<Consignataria>) consigs) {
				jsonString = jsonString + montarJson(consignataria.getId(), consignataria.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectOrgao(Map<String, String> params, String jsonString, Usuario usuario) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		OrgaoSpecification orgaoSpecification = new OrgaoSpecification();
		
		//Parametros da cidade
		String idOrgao = null;
		String nomeOrgao = null;
		String siglaOrgao = null;
		String emailOrgao = null;
		
		
		if(params.containsKey("id")) {
			idOrgao = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeOrgao = params.get("nome");
		}
		
		if(params.containsKey("sigla")) {
			siglaOrgao = params.get("sigla");
		}
		
		if(params.containsKey("email")) {
			emailOrgao = params.get("email");
		}
		
		repository = orgaoRepository;
		
		List<Orgao> orgaos = new ArrayList<Orgao>();
		
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			orgaos.add(orgao);
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA) 
				|| usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			//lista vazia
		}
		else {
		
			orgaos = ((OrgaoRepository) repository).findAll(
				Specification.where(
					orgaoSpecification.idEquals(idOrgao))
				.and(orgaoSpecification.nomeLike(nomeOrgao))
				.and(orgaoSpecification.siglaLike(siglaOrgao))
				.and(orgaoSpecification.emailLike(emailOrgao))
				.and(orgaoSpecification.findByStatus())
			);
		}
		if(!orgaos.isEmpty()) {
			for(Orgao orgao: (List<Orgao>) orgaos) {
				jsonString = jsonString + montarJson(orgao.getId(), orgao.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectCliente(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		FuncionarioSpecification clienteSpecification = new FuncionarioSpecification();
		
		//Parametros da cidade
		String idCliente = null;
		String nomeCliente = null;
		String cpfCliente = null;
		String matriculaCliente = null;
		
		
		if(params.containsKey("id")) {
			idCliente = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeCliente = params.get("nome");
		}
		
		if(params.containsKey("cpf")) {
			cpfCliente = params.get("cpf");
		}
		if(params.containsKey("matricula")) {
			cpfCliente = params.get("matricula");
		}
		
		repository = clienteRepository;
		
		lista = ((FuncionarioRepository) repository).findAll(
			Specification.where(
				clienteSpecification.idEquals(idCliente))
			.and(clienteSpecification.nomeLike(nomeCliente))
			.and(clienteSpecification.cpfLike(cpfCliente))
			.and(clienteSpecification.matriculaLike(matriculaCliente))
			.and(clienteSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			for(Funcionario cliente: (List<Funcionario>) lista) {
				jsonString = jsonString + montarJson(cliente.getId(), cliente.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectPerfil(Map<String, String> params, String jsonString, Usuario usuario) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		repository = perfilRepository;
		
		PerfilSpecification perfilSpecification = new PerfilSpecification();
		
		//Parametros do perfil
		String idPerfil = null;
		String nomePerfil = null;
		String descricaoPerfil = null;
		String siglaPerfil = null;
		String idFunc = null;
		
		List<PerfilFuncionalidade> listPerfFunc = new ArrayList<PerfilFuncionalidade>(); 
		
		//Pegando dados da entidade
		if(params.containsKey("id")) {
			idPerfil  = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomePerfil = params.get("nome");
		}
		
		if(params.containsKey("descricao")) {
			descricaoPerfil = params.get("descricao");
		}
		
		if(params.containsKey("sigla")) {
			siglaPerfil = params.get("sigla");
		}
		
		if(params.containsKey("idFuncionalidade")) {
			idFunc = params.get("idFuncionalidade");
			
			listPerfFunc = ((PerfilFuncionalidadeRepository) perfilFuncionalidadeRepository).findAll(
					Specification.where(
						new PerfilFuncionalidadeSpecification().idFuncionalidadeEq(
								idFunc == null ? null : Long.parseLong(idFunc))
					));
			
			if(listPerfFunc.isEmpty()) {
				return null;
			}
		}
		
		lista = ((PerfilRepository) repository).findAll(
			Specification.where(
				perfilSpecification.idEquals(idPerfil))
			.and(perfilSpecification.nomeLike(nomePerfil))
			.and(perfilSpecification.siglaEquals(siglaPerfil))
			.and(perfilSpecification.descricaoLike(descricaoPerfil))
			.and(perfilSpecification.idIn(listPerfFunc.stream()
					.map(PerfilFuncionalidade::getPerfil)
					.map(Perfil::getId)
					.collect(Collectors.toList())))
			.and(perfilSpecification.findByStatus())
		);
		if(!lista.isEmpty()) {
			
			for(Perfil perfil: (List<Perfil>) lista) {
				if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
					if(perfil.getId() != PerfilHelper.ORGAO
						&& perfil.getId() != PerfilHelper.AVERBADORA
						&& perfil.getId() != PerfilHelper.CONSIGNATARIA) {
						continue;
					}
				}
				
				if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
					if(perfil.getId() != PerfilHelper.CONSIGNATARIA) {
						continue;
					}
				}
				
				if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
					if(!perfil.getId().equals(PerfilHelper.AVERBADORA)
							&& perfil.getId() != PerfilHelper.CONSIGNATARIA) {
						continue;
					}
				}
				
				jsonString = jsonString + montarJson(perfil.getId(), perfil.getNome());
				jsonString = jsonString + ",";
			}
		
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		
		return jsonString;
	}
	
	private String selectUf(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		repository = ufRepository;
		
		UfSpecification ufSpecification = new UfSpecification();
		
		//Parametros do uf
		String idUF = null;
		String nomeUF = null;
		String siglaUF = null;
		
		//Pegando dados da entidade
		if(params.containsKey("id")) {
			idUF = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeUF = params.get("nome");
		}
		
		if(params.containsKey("sigla")) {
			siglaUF = params.get("sigla");
		}
		
		lista = ((UfRepository) repository).findAll(
			Specification.where(
				ufSpecification.idEquals(idUF))
			.and(ufSpecification.nomeLike(nomeUF))
			.and(ufSpecification.siglaEquals(siglaUF))
			.and(ufSpecification.findByStatus())
		);
		
		if(!lista.isEmpty()) {
			for(Uf uf: (List<Uf>) lista) {
				jsonString = jsonString + montarJson(uf.getId(), uf.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}
	
	private String selectTipoUsuario(Map<String, String> params, String jsonString) {
		CrudRepository<?, Long> repository;
		List<?> lista;
		repository = tipoUsuarioRepository;
		
		TipoUsuarioSpecification tipoUsuarioSpecification = new TipoUsuarioSpecification();
		
		//Parametros do uf
		String idTipoUsuario = null;
		String nomeTipoUsuario = null;
		String codigoTipoUsuario = null;
		
		//Pegando dados da entidade
		if(params.containsKey("id")) {
			idTipoUsuario  = params.get("id");
		}
		
		if(params.containsKey("nome")) {
			nomeTipoUsuario = params.get("nome");
		}
		
		if(params.containsKey("codigo")) {
			codigoTipoUsuario = params.get("codigo");
		}
		
		
		lista = ((TipoUsuarioRepository) repository).findAll(
			Specification.where(
				tipoUsuarioSpecification.idEquals(idTipoUsuario))
			.and(tipoUsuarioSpecification.nomeLike(nomeTipoUsuario))
			.and(tipoUsuarioSpecification.codigoLike(codigoTipoUsuario))
			.and(tipoUsuarioSpecification.findByStatus())
		);
		
		
		if(!lista.isEmpty()) {
			for(TipoUsuario tipoUsuario: (List<TipoUsuario>) lista) {
				jsonString = jsonString + montarJson(tipoUsuario.getId(), tipoUsuario.getNome());
				jsonString = jsonString + ",";
			}
			jsonString = jsonString.substring(0, jsonString.length() - 1);
		}
		return jsonString;
	}

	
	private String montarJson(Long id, String nome) {
		return "{\"id\":\"" + id + "\", \"text\":\"" + nome + "\"}";
	}
}
