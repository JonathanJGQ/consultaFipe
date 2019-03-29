package com.sif.core.controller;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.service.UsuarioService;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.PerfilHelper;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.Contrato;
import com.sif.model.Orgao;
import com.sif.model.Usuario;
import com.sif.model.utils.SituacaoContratoHelper;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.ContratoRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.specification.AverbadoraSpecification;
import com.sif.repository.specification.ConsignatariaSpecification;
import com.sif.repository.specification.OrgaoSpecification;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
	
	@Autowired
	ContratoRepository contratoRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	UsuarioService usuarioRepository;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@GetMapping("/contratos/periodo")
	public ResponseEntity<String> getByMonth() {
		
		
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH,-30);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		Usuario usuario = funcoes.getLoggedUser();
		
		List<Contrato> listaContratos = new ArrayList<Contrato>();
		
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			List<Usuario> usuarioOrgao = usuarioRepository.findByPerfilEntidade(PerfilHelper.ORGAO, usuario.getEntidade());
			List<Averbadora> averbadoras = averbadoraRepository.findByOrgao(usuario.getEntidade());
			List<Consignataria> consignatarias = new ArrayList<Consignataria>();
			for(Averbadora averbadora : averbadoras) {
				usuarioOrgao.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, averbadora.getId()));
				consignatarias.addAll(consignatariaRepository.findByAverbadora(averbadora.getId()));
			}
			for(Consignataria consignataria : consignatarias ) {
				usuarioOrgao.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consignataria.getId()));
			}
			for(Usuario user : usuarioOrgao) {
				listaContratos.addAll(contratoRepository.findContratosByMonthAndUsuario(user.getId(),cal.getTime()));
			}
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
			List<Usuario> usuarioAverbadora = usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, usuario.getEntidade());
			List<Consignataria> consignatarias = consignatariaRepository.findByAverbadora(usuario.getEntidade());
			for(Consignataria consig : consignatarias) {
				usuarioAverbadora.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consig.getId()));
			}
			for(Usuario user : usuarioAverbadora) {
				listaContratos.addAll(contratoRepository.findContratosByMonthAndUsuario(user.getId(),cal.getTime()));
			}
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			List<Usuario> usuarioConsignataria = usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, usuario.getEntidade());
			for(Usuario user : usuarioConsignataria) {
				listaContratos.addAll(contratoRepository.findContratosByMonthAndUsuario(user.getId(),cal.getTime()));
			}
		}
		else {
			listaContratos = contratoRepository.findContratosByMonth(cal.getTime());
		}
		
		List<Contrato> contratosAtivos = new ArrayList<Contrato>();
		List<Contrato> contratosQuitados = new ArrayList<Contrato>();
		List<Contrato> contratosCancelados = new ArrayList<Contrato>();
		
		for(Contrato contrato : listaContratos) {
			if(contrato.getSituacao().getId().equals(SituacaoContratoHelper.SITUACAO_ATIVO)) {
				contratosAtivos.add(contrato);
			}
			else if(contrato.getSituacao().getId().equals(SituacaoContratoHelper.SITUACAO_QUITADO)) {
				contratosQuitados.add(contrato);
			}
			else if(contrato.getSituacao().getId().equals(SituacaoContratoHelper.SITUACAO_CANCELADO)) {
				contratosCancelados.add(contrato);
			}
		}
		
		Float ativoPorcentagem = null;
		Float quitadoPorcentagem = null;
		Float canceladoPorcentagem = null;
		
		if(listaContratos.size() == 0) {
			ativoPorcentagem = 0F;
			quitadoPorcentagem = 0F;
			canceladoPorcentagem = 0F;
		}
		else {
			ativoPorcentagem = Float.valueOf(contratosAtivos.size()) / Float.valueOf(listaContratos.size());
			quitadoPorcentagem = Float.valueOf(contratosQuitados.size()) / Float.valueOf(listaContratos.size());
			canceladoPorcentagem = Float.valueOf(contratosCancelados.size()) / Float.valueOf(listaContratos.size());
		}
		
		String jsonString = "[";
		jsonString = jsonString + "{"
				+ "\"total\": \"" + contratosAtivos.size() + "\","
				+ "\"titulo\": \"Contratos Ativos - 30 dias\","
				+ "\"porcentagem\": \"" + formatarPorcentagem(ativoPorcentagem) + "\"},";
		jsonString = jsonString + "{"
				+ "\"total\": \"" + contratosQuitados.size() + "\","
				+ "\"titulo\": \"Contratos Quitados - 30 dias\","
				+ "\"porcentagem\": \"" + formatarPorcentagem(quitadoPorcentagem) + "\"},";
		jsonString = jsonString + "{"
				+ "\"total\": \"" + contratosCancelados.size() + "\","
				+ "\"titulo\": \"Contratos Cancelados - 30 dias\","
				+ "\"porcentagem\": \"" + formatarPorcentagem(canceladoPorcentagem) + "\"},";
		jsonString = jsonString + "{"
				+ "\"total\": \"" + listaContratos.size() + "\","
				+ "\"titulo\": \"Total de Contratos - 30 dias\","
				+ "\"porcentagem\": \"100%\"}]";

		return ResponseEntity.ok().body(jsonString);
	}
	
	@GetMapping("/contratos/anual")
	public ResponseEntity<String> getByYear() {
		
		HashMap<Integer,Integer> ativo = new HashMap<>();
		HashMap<Integer,Integer> quitado = new HashMap<>();
		HashMap<Integer,Integer> cancelado = new HashMap<>();
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			Calendar cal = Calendar.getInstance();
			int ano = cal.get(Calendar.YEAR);
			
			Date dataFim = sdf.parse("31/12/" + ano);
			Date dataInicio = sdf.parse("01/01/" + ano);
			
			Usuario usuario = funcoes.getLoggedUser();
			
			List<Contrato> listaContratos = new ArrayList<Contrato>();
			
			if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
				List<Usuario> usuarioOrgao = usuarioRepository.findByPerfilEntidade(PerfilHelper.ORGAO, usuario.getEntidade());
				List<Averbadora> averbadoras = averbadoraRepository.findByOrgao(usuario.getEntidade());
				List<Consignataria> consignatarias = new ArrayList<Consignataria>();
				for(Averbadora averbadora : averbadoras) {
					usuarioOrgao.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, averbadora.getId()));
					consignatarias.addAll(consignatariaRepository.findByAverbadora(averbadora.getId()));
				}
				for(Consignataria consignataria : consignatarias ) {
					usuarioOrgao.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consignataria.getId()));
				}
				for(Usuario user : usuarioOrgao) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
			}
			else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
				List<Usuario> usuarioAverbadora = usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, usuario.getEntidade());
				List<Consignataria> consignatarias = consignatariaRepository.findByAverbadora(usuario.getEntidade());
				for(Consignataria consig : consignatarias) {
					usuarioAverbadora.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consig.getId()));
				}
				for(Usuario user : usuarioAverbadora) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
			}
			else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
				List<Usuario> usuarioConsignataria = usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, usuario.getEntidade());
				for(Usuario user : usuarioConsignataria) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
			}
			else {
				listaContratos = contratoRepository.findContratosByYear(dataInicio, dataFim);
			}
			
			for(Contrato contrato : listaContratos) {
				
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(contrato.getDataLancamento());
				int month = calendar.get(Calendar.MONTH);
				if(contrato.getSituacao().getId().equals(SituacaoContratoHelper.SITUACAO_ATIVO)) {
					if(ativo.get(month) == null) {
						ativo.put(month, 0);
					}
					ativo.put(month,ativo.get(month) + 1);
				}
				else if(contrato.getSituacao().getId().equals(SituacaoContratoHelper.SITUACAO_QUITADO)) {
					if(quitado.get(month) == null) {
						quitado.put(month, 0);
					}
					quitado.put(month,quitado.get(month) + 1);
				}
				else if(contrato.getSituacao().getId().equals(SituacaoContratoHelper.SITUACAO_CANCELADO)) {
					if(cancelado.get(month) == null) {
						cancelado.put(month, 0);
					}
					cancelado.put(month,cancelado.get(month) + 1);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		int i = 0;
		String auxAtivo = "[";
		while(i < 12) {
			if(ativo.get(i) == null)
				auxAtivo = auxAtivo + "0";
			else
				auxAtivo = auxAtivo + ativo.get(i);
			if(i != 11)
				auxAtivo = auxAtivo + ",";
			i++;
		}
		auxAtivo = auxAtivo + "]";
		
		i = 0;
		String auxQuitado = "[";
		while(i < 12) {
			if(ativo.get(i) == null)
				auxQuitado = auxQuitado + "0";
			else
				auxQuitado = auxQuitado + quitado.get(i);
			if(i != 11)
				auxQuitado = auxQuitado + ",";
			i++;
		}
		auxQuitado = auxQuitado + "]";
		
		i = 0;
		String auxCancelado = "[";
		while(i < 12) {
			if(ativo.get(i) == null)
				auxCancelado = auxCancelado + "0";
			else
				auxCancelado = auxCancelado + cancelado.get(i);
			if(i != 11)
				auxCancelado = auxCancelado + ",";
			i++;
		}
		auxCancelado = auxCancelado + "]";
		
		String jsonString = "{";
		jsonString = jsonString + "\"labels\": [\"Janeiro\", \"Fevereiro\",\"Março\",\"Abril\",\"Maio\",\"Junho\",\"Julho\","
				+ "\"Agosto\",\"Setembro\",\"Outubro\",\"Novembro\",\"Dezembro\"],"
				+ "\"datasets\": [";
		jsonString = jsonString + "{ \"label\": \"Ativo\","
				+ "\"data\": " + auxAtivo + ","
				+ "\"fill\": false,"
				+ "\"borderColor\": \"#188ae2\"},";
		jsonString = jsonString + "{ \"label\": \"Quitado\","
				+ "\"data\": " + auxQuitado + ","
				+ "\"fill\": false,"
				+ "\"borderColor\": \"#dc3545\"},";
		jsonString = jsonString + "{ \"label\": \"Cancelado\","
				+ "\"data\": " + auxCancelado + ","
				+ "\"fill\": false,"
				+ "\"borderColor\": \"#28a745\"}]}";
		
		return ResponseEntity.ok().body(jsonString);
	}
	
	
	@GetMapping("/contratos/carteiraativa")
	public ResponseEntity<String> getCarteiraAtiva() {
		
		HashMap<String, Integer> entidades = new HashMap<String, Integer>();
		List<Contrato> listaContratos = new ArrayList<Contrato>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Calendar cal = Calendar.getInstance();
		int ano = cal.get(Calendar.YEAR);
		Date dataFim = new Date();
		Date dataInicio = new Date();
		try {
			dataFim = sdf.parse("31/12/" + ano);
			dataInicio = sdf.parse("01/01/" + ano);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		Usuario usuario = funcoes.getLoggedUser();
		if(usuario.getPerfil().getId().equals(PerfilHelper.ORGAO)) {
			List<Usuario> usuarioOrgao = usuarioRepository.findByPerfilEntidade(PerfilHelper.ORGAO, usuario.getEntidade());
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			for(Usuario user : usuarioOrgao) {
				listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
			}
			entidades.put(orgao.getNome(), listaContratos.size());
			listaContratos = new ArrayList<Contrato>();
			usuarioOrgao = new ArrayList<Usuario>();
			List<Averbadora> averbadoras = averbadoraRepository.findByOrgao(orgao.getId());
			
			List<Consignataria> consignatarias = new ArrayList<Consignataria>(); 
			for(Averbadora averb : averbadoras) {
				usuarioOrgao.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, averb.getId()));
				consignatarias.addAll(consignatariaRepository.findByAverbadora(averb.getId()));
				for(Usuario user : usuarioOrgao) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
				entidades.put(averb.getNome(), listaContratos.size());
				
				listaContratos = new ArrayList<Contrato>();
				usuarioOrgao = new ArrayList<Usuario>();
			}
			
			for(Consignataria consig : consignatarias) {
				usuarioOrgao.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consig.getId()));
				for(Usuario user : usuarioOrgao) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
				entidades.put(consig.getNome(), listaContratos.size());
				
				listaContratos = new ArrayList<Contrato>();
				usuarioOrgao = new ArrayList<Usuario>();
			}
			
			
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.AVERBADORA)) {
			List<Usuario> usuarioAverbadora = usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, usuario.getEntidade());
			Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
			for(Usuario user : usuarioAverbadora) {
				listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
			}
			entidades.put(averbadora.getNome(), listaContratos.size());
			listaContratos = new ArrayList<Contrato>();
			usuarioAverbadora = new ArrayList<Usuario>();
			
			List<Consignataria> consignatarias = consignatariaRepository.findByAverbadora(usuario.getEntidade());
			for(Consignataria consig : consignatarias) {
				usuarioAverbadora.addAll(usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consig.getId()));
				for(Usuario user : usuarioAverbadora) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
				entidades.put(consig.getNome(), listaContratos.size());
				listaContratos = new ArrayList<Contrato>();
				usuarioAverbadora = new ArrayList<Usuario>();
			}
			
		}
		else if(usuario.getPerfil().getId().equals(PerfilHelper.CONSIGNATARIA)) {
			List<Usuario> usuarioConsignataria = usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, usuario.getEntidade());
			Consignataria consignataria = consignatariaRepository.findById(usuario.getEntidade()).get();
			for(Usuario user : usuarioConsignataria) {
				listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
			}
			entidades.put(consignataria.getNome(), listaContratos.size());
		}
		else {
			
			List<Orgao> orgaos = orgaoRepository.findAll(Specification.where(new OrgaoSpecification().findByStatus()));
			List<Averbadora> averbadoras = averbadoraRepository.findAll(Specification.where(new AverbadoraSpecification().findByStatus()));		
			List<Consignataria> consignatarias = consignatariaRepository.findAll(Specification.where(new ConsignatariaSpecification().findByStatus()));
		
			for(Orgao orgao : orgaos) {
				List<Usuario> usuarioOrgao = usuarioRepository.findByPerfilEntidade(PerfilHelper.ORGAO, orgao.getId());
				for(Usuario user : usuarioOrgao) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
				entidades.put(orgao.getNome(), listaContratos.size());
				listaContratos = new ArrayList<Contrato>();
				usuarioOrgao = new ArrayList<Usuario>();
			}
			for(Averbadora averbadora : averbadoras) {
				List<Usuario> usuarioAverbadora = usuarioRepository.findByPerfilEntidade(PerfilHelper.AVERBADORA, averbadora.getId());
				for(Usuario user : usuarioAverbadora) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
				entidades.put(averbadora.getNome(), listaContratos.size());
				listaContratos = new ArrayList<Contrato>();
				usuarioAverbadora = new ArrayList<Usuario>();
			}
			for(Consignataria consignataria : consignatarias) {
				List<Usuario> usuarioConsignataria = usuarioRepository.findByPerfilEntidade(PerfilHelper.CONSIGNATARIA, consignataria.getId());
				for(Usuario user : usuarioConsignataria) {
					listaContratos.addAll(contratoRepository.findContratosByYearAndUsuario(dataInicio, dataFim, user.getId()));
				}
				entidades.put(consignataria.getNome(), listaContratos.size());
				listaContratos = new ArrayList<Contrato>();
				usuarioConsignataria = new ArrayList<Usuario>();
			}
			
		}
		
		Map<String, Integer> result = entidades.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
		
		String labels = "[";
		String data = "[";
		for(String key : result.keySet()) {
			labels = labels + "\"" + key + "\","; 
			data = data + result.get(key) + ",";	
		}
		labels = labels.substring(0, labels.length() - 1) + "]";
		data = data.substring(0,data.length() - 1) + "]";
        
		String jsonString = "{";
		jsonString = jsonString + "\"labels\": " + labels + ","
				+ "\"datasets\": [ {"
				+ "\"data\": " + data + ", "
				+ "\"backgroundColor\": ["
				+ "\"#FF6384\","
				+ "\"#36A2EB\","
				+ "\"#FFCE56\","
				+ "\"#2ECC71\","
				+ "\"#E67E22\","
				+ "\"#95A5A6\","
				+ "\"#34495E\","
				+ "\"#9B59B6\","
				+ "\"#E74C3C\","
				+ "\"#ECF0F1\"],"
				+ "\"hoverBackgroundColor\": ["
				+ "\"#FF6384\","
				+ "\"#36A2EB\","
				+ "\"#FFCE56\","
				+ "\"#2ECC71\","
				+ "\"#E67E22\","
				+ "\"#95A5A6\","
				+ "\"#34495E\","
				+ "\"#9B59B6\","
				+ "\"#E74C3C\","
				+ "\"#ECF0F1\"]}]}";
		
		return ResponseEntity.ok().body(jsonString);
	}
	
	private String formatarPorcentagem(Float numero){
		String retorno = "";
		DecimalFormat formatter = new DecimalFormat("#.00");
		try{
			if(numero > 0 && numero != null)
				retorno = formatter.format(numero * 100) + "%";
			else
				retorno = "0%";
		}catch(Exception ex){
			System.err.println("Erro ao formatar numero: " + ex);
		}
		return retorno;
	}
	
}
